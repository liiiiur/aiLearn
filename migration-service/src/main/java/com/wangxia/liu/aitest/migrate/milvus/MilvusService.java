package com.wangxia.liu.aitest.migrate.milvus;

import com.wangxia.liu.aitest.migrate.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.FlushParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.IndexType;
import io.milvus.param.index.MetricType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MilvusService {

    private static final Logger log = LoggerFactory.getLogger(MilvusService.class);

    private final MilvusProperties properties;
    private final MilvusServiceClient client;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);
    private volatile Integer ensuredDimension;
    private volatile Boolean ensuredIdAsInt64;

    public MilvusService(MilvusProperties properties) {
        this.properties = properties;
        this.client = createClient(properties);
    }

    public synchronized void ensureCollection(int dimension, boolean idAsInt64) {
        Integer configuredDimension = properties.getDimension();
        if (configuredDimension != null && configuredDimension > 0 && configuredDimension != dimension) {
            throw new IllegalStateException("Milvus dimension mismatch: configured=" + configuredDimension + ", actual=" + dimension);
        }

        if (collectionReady.get() && dimensionEquals(dimension) && idTypeEquals(idAsInt64)) {
            return;
        }

        R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(properties.getCollection())
                .build());

        if (!Boolean.TRUE.equals(exists.getData())) {
            FieldType idField = buildIdField(idAsInt64);
            FieldType vectorField = FieldType.newBuilder()
                    .withName("vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();
            FieldType textField = FieldType.newBuilder()
                    .withName("text")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(properties.getTextMaxLength())
                    .build();
            FieldType metaField = FieldType.newBuilder()
                    .withName("metadata")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(properties.getMetadataMaxLength())
                    .build();

            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(properties.getCollection())
                    .withDescription("dameng-to-milvus-migration")
                    .withShardsNum(properties.getShardsNum())
                    .addFieldType(idField)
                    .addFieldType(vectorField)
                    .addFieldType(textField)
                    .addFieldType(metaField)
                    .build();

            client.createCollection(createCollectionParam);
            createIndex(dimension);
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(properties.getCollection())
                    .build());
            log.info("Milvus collection '{}' created.", properties.getCollection());
        }

        collectionReady.set(true);
        ensuredDimension = dimension;
        ensuredIdAsInt64 = idAsInt64;
    }

    public void insertBatch(List<?> ids, List<List<Float>> vectors, List<String> texts, List<String> metadataJson) {
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(properties.getCollection())
                .withFields(List.of(
                        new InsertParam.Field("id", ids),
                        new InsertParam.Field("vector", vectors),
                        new InsertParam.Field("text", texts),
                        new InsertParam.Field("metadata", metadataJson)
                ))
                .build();
        client.insert(insertParam);
    }

    public void flush() {
        client.flush(FlushParam.newBuilder()
                .withCollectionNames(List.of(properties.getCollection()))
                .build());
    }

    @PreDestroy
    public void close() {
        try {
            client.close();
        } catch (Exception ex) {
            log.warn("Failed to close Milvus client.", ex);
        }
    }

    private MilvusServiceClient createClient(MilvusProperties properties) {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort());
        if (StringUtils.hasText(properties.getUsername())) {
            builder.withAuthorization(properties.getUsername(), properties.getPassword());
        }
        return new MilvusServiceClient(builder.build());
    }

    private FieldType buildIdField(boolean idAsInt64) {
        FieldType.Builder builder = FieldType.newBuilder()
                .withName("id")
                .withPrimaryKey(true)
                .withAutoID(false);
        if (idAsInt64) {
            builder.withDataType(DataType.Int64);
        } else {
            builder.withDataType(DataType.VarChar)
                    .withMaxLength(properties.getIdMaxLength());
        }
        return builder.build();
    }

    private void createIndex(int dimension) {
        IndexType indexType = parseIndexType(properties.getIndexType());
        MetricType metricType = parseMetricType(properties.getMetricType());

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(properties.getCollection())
                .withFieldName("vector")
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam(properties.getIndexParams())
                .build();
        client.createIndex(indexParam);
        log.info("Milvus index created. indexType={}, metricType={}, params={}", indexType, metricType, properties.getIndexParams());
    }

    private IndexType parseIndexType(String value) {
        if (!StringUtils.hasText(value)) {
            return IndexType.IVF_FLAT;
        }
        return IndexType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private MetricType parseMetricType(String value) {
        if (!StringUtils.hasText(value)) {
            return MetricType.L2;
        }
        return MetricType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private boolean dimensionEquals(int dimension) {
        return ensuredDimension != null && ensuredDimension == dimension;
    }

    private boolean idTypeEquals(boolean idAsInt64) {
        return ensuredIdAsInt64 != null && ensuredIdAsInt64 == idAsInt64;
    }
}
