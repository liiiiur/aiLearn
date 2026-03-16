package com.wangxia.liu.aitest.migrate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangxia.liu.aitest.migrate.config.DamengProperties;
import com.wangxia.liu.aitest.migrate.config.MigrationProperties;
import com.wangxia.liu.aitest.migrate.config.MilvusProperties;
import com.wangxia.liu.aitest.migrate.embedding.EmbeddingService;
import com.wangxia.liu.aitest.migrate.milvus.MilvusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 达梦到 Milvus 的迁移服务。
 */
@Service
public class DamengToMilvusMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DamengToMilvusMigrationService.class);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_$.]+");

    private final DamengProperties damengProperties;
    private final MigrationProperties migrationProperties;
    private final MilvusProperties milvusProperties;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 构造迁移服务。
     *
     * @param damengProperties 达梦配置
     * @param migrationProperties 迁移配置
     * @param milvusProperties Milvus 配置
     * @param embeddingService 向量化服务
     * @param milvusService Milvus 服务
     * @param objectMapper JSON 工具
     */
    public DamengToMilvusMigrationService(
            DamengProperties damengProperties,
            MigrationProperties migrationProperties,
            MilvusProperties milvusProperties,
            EmbeddingService embeddingService,
            MilvusService milvusService,
            ObjectMapper objectMapper) {
        this.damengProperties = damengProperties;
        this.migrationProperties = migrationProperties;
        this.milvusProperties = milvusProperties;
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行迁移流程。
     *
     * @return 迁移结果统计
     */
    public MigrationResult migrate() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("迁移任务正在运行中。");
        }

        long start = System.currentTimeMillis();
        long total = 0;
        long inserted = 0;
        long failed = 0;
        boolean idAsInt64 = isIdAsInt64();

        try {
            validateConfig();
            String sql = buildSelectSql();

            try (Connection connection = openConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                statement.setFetchSize(Math.max(1, migrationProperties.getBatchSize()));
                if (migrationProperties.getMaxRows() > 0) {
                    statement.setMaxRows(migrationProperties.getMaxRows());
                }

                List<RowData> batch = new ArrayList<>(migrationProperties.getBatchSize());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        total++;
                        try {
                            RowData row = mapRow(resultSet, idAsInt64);
                            if (row == null) {
                                failed++;
                                continue;
                            }
                            batch.add(row);
                        } catch (Exception ex) {
                            if (migrationProperties.isFailFast()) {
                                throw ex;
                            }
                            failed++;
                            log.warn("跳过第 {} 行，原因：{}", total, ex.getMessage());
                        }

                        if (batch.size() >= migrationProperties.getBatchSize()) {
                            BatchOutcome outcome = flushBatch(batch, idAsInt64);
                            inserted += outcome.inserted;
                            failed += outcome.failed;
                            batch.clear();
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    BatchOutcome outcome = flushBatch(batch, idAsInt64);
                    inserted += outcome.inserted;
                    failed += outcome.failed;
                    batch.clear();
                }

                milvusService.flush();
            }
        } catch (Exception ex) {
            log.error("迁移失败。", ex);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("迁移失败。", ex);
        } finally {
            running.set(false);
        }

        long duration = System.currentTimeMillis() - start;
        return new MigrationResult(total, inserted, failed, duration);
    }

    /**
     * 打开达梦数据库连接。
     *
     * @return 数据库连接
     * @throws SQLException 连接失败时抛出
     */
    private Connection openConnection() throws SQLException {
        if (!StringUtils.hasText(damengProperties.getUrl())) {
            throw new IllegalStateException("dameng.url 未配置。");
        }
        if (StringUtils.hasText(damengProperties.getDriverClassName())) {
            try {
                Class.forName(damengProperties.getDriverClassName());
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("未找到达梦 JDBC 驱动：" + damengProperties.getDriverClassName(), ex);
            }
        }
        return DriverManager.getConnection(damengProperties.getUrl(), damengProperties.getUsername(), damengProperties.getPassword());
    }

    /**
     * 校验迁移配置是否齐全。
     */
    private void validateConfig() {
        if (!StringUtils.hasText(migrationProperties.getSourceTable())) {
            throw new IllegalStateException("migration.source-table 未配置。");
        }
        if (!StringUtils.hasText(migrationProperties.getIdColumn())) {
            throw new IllegalStateException("migration.id-column 未配置。");
        }
        if (!StringUtils.hasText(migrationProperties.getTextColumn())) {
            throw new IllegalStateException("migration.text-column 未配置。");
        }
    }

    /**
     * 构建查询源表的 SQL。
     *
     * @return SQL 语句
     */
    private String buildSelectSql() {
        String table = requireIdentifier(migrationProperties.getSourceTable(), "migration.source-table");
        String idColumn = requireIdentifier(migrationProperties.getIdColumn(), "migration.id-column");
        String textColumn = requireIdentifier(migrationProperties.getTextColumn(), "migration.text-column");

        Set<String> columns = new LinkedHashSet<>();
        columns.add(idColumn);
        columns.add(textColumn);
        for (String column : migrationProperties.getMetadataColumns()) {
            if (StringUtils.hasText(column)) {
                columns.add(requireIdentifier(column, "migration.metadata-columns"));
            }
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", columns));
        sql.append(" FROM ").append(table);

        if (StringUtils.hasText(migrationProperties.getWhereClause())) {
            sql.append(" WHERE ").append(migrationProperties.getWhereClause());
        }

        if (StringUtils.hasText(migrationProperties.getOrderBy())) {
            sql.append(" ORDER BY ").append(migrationProperties.getOrderBy());
        }

        return sql.toString();
    }

    /**
     * 将数据库行映射为迁移数据。
     *
     * @param resultSet 查询结果集
     * @param idAsInt64 主键是否为 Long
     * @return 行数据封装
     * @throws Exception 映射失败时抛出
     */
    private RowData mapRow(ResultSet resultSet, boolean idAsInt64) throws Exception {
        String text = resultSet.getString(migrationProperties.getTextColumn());
        if (!StringUtils.hasText(text)) {
            if (migrationProperties.isFailFast()) {
                throw new IllegalStateException("文本列为空，id=" + resultSet.getObject(migrationProperties.getIdColumn()));
            }
            return null;
        }

        Object idValue;
        if (idAsInt64) {
            long id = resultSet.getLong(migrationProperties.getIdColumn());
            if (resultSet.wasNull()) {
                throw new IllegalStateException("主键列为空。");
            }
            idValue = id;
        } else {
            Object raw = resultSet.getObject(migrationProperties.getIdColumn());
            if (raw == null) {
                throw new IllegalStateException("主键列为空。");
            }
            String idString = String.valueOf(raw);
            idValue = truncate(idString, milvusProperties.getIdMaxLength());
        }

        String metadataJson = "{}";
        if (!migrationProperties.getMetadataColumns().isEmpty()) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            for (String column : migrationProperties.getMetadataColumns()) {
                if (StringUtils.hasText(column)) {
                    metadata.put(column, resultSet.getObject(column));
                }
            }
            metadataJson = objectMapper.writeValueAsString(metadata);
        }

        text = truncate(text, milvusProperties.getTextMaxLength());
        metadataJson = truncate(metadataJson, milvusProperties.getMetadataMaxLength());

        return new RowData(idValue, text, metadataJson);
    }

    /**
     * 处理并写入一批数据。
     *
     * @param rows 行数据列表
     * @param idAsInt64 主键是否为 Long
     * @return 批次处理结果
     */
    private BatchOutcome flushBatch(List<RowData> rows, boolean idAsInt64) {
        if (rows.isEmpty()) {
            return new BatchOutcome(0, 0);
        }

        List<String> texts = new ArrayList<>(rows.size());
        List<Object> ids = new ArrayList<>(rows.size());
        List<String> metadata = new ArrayList<>(rows.size());
        for (RowData row : rows) {
            texts.add(row.text());
            ids.add(row.id());
            metadata.add(row.metadataJson());
        }

        try {
            List<List<Float>> vectors = embeddingService.embedBatch(texts);
            if (vectors.isEmpty()) {
                throw new IllegalStateException("向量化结果为空。");
            }

            int dimension = vectors.get(0).size();
            if (dimension <= 0) {
                throw new IllegalStateException("向量维度不合法：" + dimension);
            }
            for (List<Float> vector : vectors) {
                if (vector.size() != dimension) {
                    throw new IllegalStateException("批次内向量维度不一致。");
                }
            }

            milvusService.ensureCollection(dimension, idAsInt64);
            milvusService.insertBatch(ids, vectors, texts, metadata);
            return new BatchOutcome(rows.size(), 0);
        } catch (Exception ex) {
            if (migrationProperties.isFailFast()) {
                if (ex instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("批次写入失败。", ex);
            }
            log.warn("批次失败：{}", ex.getMessage());
            return new BatchOutcome(0, rows.size());
        }
    }

    /**
     * 校验并返回合法标识符。
     *
     * @param value 标识符值
     * @param fieldName 配置字段名
     * @return 合法标识符
     */
    private String requireIdentifier(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(fieldName + " 未配置。");
        }
        if (!IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("非法标识符：" + value);
        }
        return value;
    }

    /**
     * 判断主键是否按 Long 类型处理。
     *
     * @return 是否为 Long 类型
     */
    private boolean isIdAsInt64() {
        String idType = migrationProperties.getIdType();
        if (!StringUtils.hasText(idType)) {
            return false;
        }
        String normalized = idType.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("INT64") || normalized.equals("LONG") || normalized.equals("BIGINT");
    }

    /**
     * 截断字符串到最大长度。
     *
     * @param value 原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 行数据封装。
     */
    private record RowData(
            /**
             * 主键值。
             */
            Object id,
            /**
             * 文本内容。
             */
            String text,
            /**
             * 元数据 JSON。
             */
            String metadataJson
    ) {
    }

    /**
     * 批次处理结果。
     */
    private record BatchOutcome(
            /**
             * 成功写入条数。
             */
            long inserted,
            /**
             * 失败条数。
             */
            long failed
    ) {
    }
}
