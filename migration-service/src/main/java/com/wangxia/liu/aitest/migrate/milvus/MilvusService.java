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

/**
 * Milvus 客户端封装服务。
 */
@Component
public class MilvusService {

    private static final Logger log = LoggerFactory.getLogger(MilvusService.class);

    private final MilvusProperties properties;
    private final MilvusServiceClient client;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);
    private volatile Integer ensuredDimension;
    private volatile Boolean ensuredIdAsInt64;

    /**
     * 构造 Milvus 服务。
     *
     * @param properties Milvus 配置
     */
    public MilvusService(MilvusProperties properties) {
        this.properties = properties;
        this.client = createClient(properties);
    }

    /**
     * 确保集合存在且维度与主键类型匹配。
     *
     * @param dimension 向量维度
     * @param idAsInt64 主键是否为 Long
     */
    public synchronized void ensureCollection(int dimension, boolean idAsInt64) {
        Integer configuredDimension = properties.getDimension();
        if (configuredDimension != null && configuredDimension > 0 && configuredDimension != dimension) {
            throw new IllegalStateException("Milvus 向量维度不一致：配置=" + configuredDimension + "，实际=" + dimension);
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
            log.info("Milvus 集合 '{}' 已创建。", properties.getCollection());
        }

        collectionReady.set(true);
        ensuredDimension = dimension;
        ensuredIdAsInt64 = idAsInt64;
    }

    /**
     * 批量插入向量与文本。
     *
     * @param ids 主键列表
     * @param vectors 向量列表
     * @param texts 文本列表
     * @param metadataJson 元数据 JSON 列表
     */
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

    /**
     * 刷新集合数据到持久化存储。
     */
    public void flush() {
        client.flush(FlushParam.newBuilder()
                .withCollectionNames(List.of(properties.getCollection()))
                .build());
    }

    /**
     * 关闭 Milvus 客户端。
     */
    @PreDestroy
    public void close() {
        try {
            client.close();
        } catch (Exception ex) {
            log.warn("关闭 Milvus 客户端失败。", ex);
        }
    }

    /**
     * 创建 Milvus 客户端。
     *
     * @param properties 配置参数
     * @return Milvus 客户端
     */
    private MilvusServiceClient createClient(MilvusProperties properties) {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(properties.getHost())
                .withPort(properties.getPort());
        if (StringUtils.hasText(properties.getUsername())) {
            builder.withAuthorization(properties.getUsername(), properties.getPassword());
        }
        return new MilvusServiceClient(builder.build());
    }

    /**
     * 构建主键字段定义。
     *
     * @param idAsInt64 主键是否为 Long
     * @return 字段定义
     */
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

    /**
     * 创建向量索引。
     *
     * @param dimension 向量维度
     */
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
        log.info("Milvus 索引已创建。indexType={}, metricType={}, params={}", indexType, metricType, properties.getIndexParams());
    }

    /**
     * 解析索引类型。
     *
     * @param value 索引类型字符串
     * @return 索引类型
     */
    private IndexType parseIndexType(String value) {
        if (!StringUtils.hasText(value)) {
            return IndexType.IVF_FLAT;
        }
        return IndexType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 解析度量类型。
     *
     * @param value 度量类型字符串
     * @return 度量类型
     */
    private MetricType parseMetricType(String value) {
        if (!StringUtils.hasText(value)) {
            return MetricType.L2;
        }
        return MetricType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断维度是否一致。
     *
     * @param dimension 当前维度
     * @return 是否一致
     */
    private boolean dimensionEquals(int dimension) {
        return ensuredDimension != null && ensuredDimension == dimension;
    }

    /**
     * 判断主键类型是否一致。
     *
     * @param idAsInt64 主键是否为 Long
     * @return 是否一致
     */
    private boolean idTypeEquals(boolean idAsInt64) {
        return ensuredIdAsInt64 != null && ensuredIdAsInt64 == idAsInt64;
    }
}
