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

    public MigrationResult migrate() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Migration is already running.");
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
                            log.warn("Skip row #{} due to error: {}", total, ex.getMessage());
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
            log.error("Migration failed.", ex);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Migration failed.", ex);
        } finally {
            running.set(false);
        }

        long duration = System.currentTimeMillis() - start;
        return new MigrationResult(total, inserted, failed, duration);
    }

    private Connection openConnection() throws SQLException {
        if (!StringUtils.hasText(damengProperties.getUrl())) {
            throw new IllegalStateException("dameng.url is required.");
        }
        if (StringUtils.hasText(damengProperties.getDriverClassName())) {
            try {
                Class.forName(damengProperties.getDriverClassName());
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Dameng JDBC driver not found: " + damengProperties.getDriverClassName(), ex);
            }
        }
        return DriverManager.getConnection(damengProperties.getUrl(), damengProperties.getUsername(), damengProperties.getPassword());
    }

    private void validateConfig() {
        if (!StringUtils.hasText(migrationProperties.getSourceTable())) {
            throw new IllegalStateException("migration.source-table is required.");
        }
        if (!StringUtils.hasText(migrationProperties.getIdColumn())) {
            throw new IllegalStateException("migration.id-column is required.");
        }
        if (!StringUtils.hasText(migrationProperties.getTextColumn())) {
            throw new IllegalStateException("migration.text-column is required.");
        }
    }

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

    private RowData mapRow(ResultSet resultSet, boolean idAsInt64) throws Exception {
        String text = resultSet.getString(migrationProperties.getTextColumn());
        if (!StringUtils.hasText(text)) {
            if (migrationProperties.isFailFast()) {
                throw new IllegalStateException("Text column is blank for id=" + resultSet.getObject(migrationProperties.getIdColumn()));
            }
            return null;
        }

        Object idValue;
        if (idAsInt64) {
            long id = resultSet.getLong(migrationProperties.getIdColumn());
            if (resultSet.wasNull()) {
                throw new IllegalStateException("ID column is null.");
            }
            idValue = id;
        } else {
            Object raw = resultSet.getObject(migrationProperties.getIdColumn());
            if (raw == null) {
                throw new IllegalStateException("ID column is null.");
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
                throw new IllegalStateException("Embedding result is empty.");
            }

            int dimension = vectors.get(0).size();
            if (dimension <= 0) {
                throw new IllegalStateException("Embedding dimension is invalid: " + dimension);
            }
            for (List<Float> vector : vectors) {
                if (vector.size() != dimension) {
                    throw new IllegalStateException("Embedding dimension mismatch in batch.");
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
                throw new IllegalStateException("Failed to insert batch.", ex);
            }
            log.warn("Batch failed: {}", ex.getMessage());
            return new BatchOutcome(0, rows.size());
        }
    }

    private String requireIdentifier(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(fieldName + " is required.");
        }
        if (!IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid identifier for " + fieldName + ": " + value);
        }
        return value;
    }

    private boolean isIdAsInt64() {
        String idType = migrationProperties.getIdType();
        if (!StringUtils.hasText(idType)) {
            return false;
        }
        String normalized = idType.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("INT64") || normalized.equals("LONG") || normalized.equals("BIGINT");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record RowData(Object id, String text, String metadataJson) {
    }

    private record BatchOutcome(long inserted, long failed) {
    }
}
