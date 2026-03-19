package com.wangxia.liu.aitest.migrate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangxia.liu.aitest.migrate.config.MigrationProperties;
import com.wangxia.liu.aitest.migrate.config.MilvusProperties;
import com.wangxia.liu.aitest.migrate.embedding.EmbeddingService;
import com.wangxia.liu.aitest.migrate.milvus.MilvusService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
 * 达梦到 Milvus 的迁移服务（基于 JPA 原生查询读取数据）。
 */
@Service
public class DamengToMilvusMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DamengToMilvusMigrationService.class);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_$.]+");

    private final MigrationProperties migrationProperties;
    private final MilvusProperties milvusProperties;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 构造迁移服务。
     *
     * @param migrationProperties 迁移配置
     * @param milvusProperties Milvus 配置
     * @param embeddingService 向量化服务
     * @param milvusService Milvus 服务
     * @param objectMapper JSON 工具
     * @param entityManager JPA 实体管理器
     */
    public DamengToMilvusMigrationService(
            MigrationProperties migrationProperties,
            MilvusProperties milvusProperties,
            EmbeddingService embeddingService,
            MilvusService milvusService,
            ObjectMapper objectMapper,
            EntityManager entityManager) {
        this.migrationProperties = migrationProperties;
        this.milvusProperties = milvusProperties;
        this.embeddingService = embeddingService;
        this.milvusService = milvusService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    /**
     * 执行迁移流程。
     *
     * @return 迁移结果统计
     */
    @Transactional(readOnly = true)
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
            SelectSpec selectSpec = buildSelectSpec();

            Query query = entityManager.createNativeQuery(selectSpec.sql());
            if (migrationProperties.getMaxRows() > 0) {
                query.setMaxResults(migrationProperties.getMaxRows());
            }

            List<RowData> batch = new ArrayList<>(migrationProperties.getBatchSize());
            List<?> results = query.getResultList();
            for (Object rowObject : results) {
                total++;
                try {
                    RowData row = mapRow(rowObject, selectSpec.metadataColumns(), idAsInt64);
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

            if (!batch.isEmpty()) {
                BatchOutcome outcome = flushBatch(batch, idAsInt64);
                inserted += outcome.inserted;
                failed += outcome.failed;
                batch.clear();
            }

            milvusService.flush();
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
     * 构建查询源表的 SQL 与元数据列。
     *
     * @return 查询规格
     */
    private SelectSpec buildSelectSpec() {
        String table = requireIdentifier(migrationProperties.getSourceTable(), "migration.source-table");
        String idColumn = requireIdentifier(migrationProperties.getIdColumn(), "migration.id-column");
        String textColumn = requireIdentifier(migrationProperties.getTextColumn(), "migration.text-column");

        List<String> metadataColumns = new ArrayList<>();
        Set<String> columns = new LinkedHashSet<>();
        columns.add(idColumn);
        columns.add(textColumn);
        for (String column : migrationProperties.getMetadataColumns()) {
            if (StringUtils.hasText(column)) {
                String safeColumn = requireIdentifier(column, "migration.metadata-columns");
                metadataColumns.add(safeColumn);
                columns.add(safeColumn);
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

        return new SelectSpec(sql.toString(), metadataColumns);
    }

    /**
     * 将查询结果行映射为迁移数据。
     *
     * @param rowObject 查询行
     * @param metadataColumns 元数据列
     * @param idAsInt64 主键是否为 Long
     * @return 行数据封装
     * @throws Exception 映射失败时抛出
     */
    private RowData mapRow(Object rowObject, List<String> metadataColumns, boolean idAsInt64) throws Exception {
        Object[] row = normalizeRow(rowObject);
        if (row.length < 2) {
            throw new IllegalStateException("查询结果列数不足，至少需要主键和文本列。");
        }

        Object idRaw = row[0];
        if (idRaw == null) {
            throw new IllegalStateException("主键列为空。");
        }

        String text = row[1] == null ? null : row[1].toString();
        if (!StringUtils.hasText(text)) {
            if (migrationProperties.isFailFast()) {
                throw new IllegalStateException("文本列为空，id=" + idRaw);
            }
            return null;
        }

        Object idValue;
        if (idAsInt64) {
            if (idRaw instanceof Number number) {
                idValue = number.longValue();
            } else {
                idValue = Long.parseLong(idRaw.toString());
            }
        } else {
            String idString = String.valueOf(idRaw);
            idValue = truncate(idString, milvusProperties.getIdMaxLength());
        }

        String metadataJson = "{}";
        if (!metadataColumns.isEmpty()) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            for (int i = 0; i < metadataColumns.size(); i++) {
                int index = i + 2;
                Object value = index < row.length ? row[index] : null;
                metadata.put(metadataColumns.get(i), value);
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
     * 统一将查询行转换为 Object[]。
     *
     * @param rowObject 查询行对象
     * @return 行数据数组
     */
    private Object[] normalizeRow(Object rowObject) {
        if (rowObject instanceof Object[] row) {
            return row;
        }
        return new Object[]{rowObject};
    }

    /**
     * 查询规格。
     */
    private record SelectSpec(
            /**
             * SQL 语句。
             */
            String sql,
            /**
             * 元数据列列表。
             */
            List<String> metadataColumns
    ) {
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
