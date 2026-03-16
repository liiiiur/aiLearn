package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus 连接与集合配置。
 */
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {
    private String host = "127.0.0.1";
    private int port = 19530;
    private String username = "root";
    private String password = "milvus";
    private String collection = "knowledge_base";
    private Integer dimension;
    private int shardsNum = 2;
    private int idMaxLength = 128;
    private int textMaxLength = 4096;
    private int metadataMaxLength = 8192;
    private String indexType = "IVF_FLAT";
    private String metricType = "L2";
    private String indexParams = "{\"nlist\":128}";

    /**
     * 获取 Milvus 主机地址。
     *
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置 Milvus 主机地址。
     *
     * @param host 主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取 Milvus 端口。
     *
     * @return 端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置 Milvus 端口。
     *
     * @param port 端口
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码。
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取集合名称。
     *
     * @return 集合名称
     */
    public String getCollection() {
        return collection;
    }

    /**
     * 设置集合名称。
     *
     * @param collection 集合名称
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * 获取向量维度。
     *
     * @return 向量维度
     */
    public Integer getDimension() {
        return dimension;
    }

    /**
     * 设置向量维度。
     *
     * @param dimension 向量维度
     */
    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    /**
     * 获取分片数量。
     *
     * @return 分片数量
     */
    public int getShardsNum() {
        return shardsNum;
    }

    /**
     * 设置分片数量。
     *
     * @param shardsNum 分片数量
     */
    public void setShardsNum(int shardsNum) {
        this.shardsNum = shardsNum;
    }

    /**
     * 获取主键最大长度。
     *
     * @return 主键最大长度
     */
    public int getIdMaxLength() {
        return idMaxLength;
    }

    /**
     * 设置主键最大长度。
     *
     * @param idMaxLength 主键最大长度
     */
    public void setIdMaxLength(int idMaxLength) {
        this.idMaxLength = idMaxLength;
    }

    /**
     * 获取文本最大长度。
     *
     * @return 文本最大长度
     */
    public int getTextMaxLength() {
        return textMaxLength;
    }

    /**
     * 设置文本最大长度。
     *
     * @param textMaxLength 文本最大长度
     */
    public void setTextMaxLength(int textMaxLength) {
        this.textMaxLength = textMaxLength;
    }

    /**
     * 获取元数据最大长度。
     *
     * @return 元数据最大长度
     */
    public int getMetadataMaxLength() {
        return metadataMaxLength;
    }

    /**
     * 设置元数据最大长度。
     *
     * @param metadataMaxLength 元数据最大长度
     */
    public void setMetadataMaxLength(int metadataMaxLength) {
        this.metadataMaxLength = metadataMaxLength;
    }

    /**
     * 获取索引类型。
     *
     * @return 索引类型
     */
    public String getIndexType() {
        return indexType;
    }

    /**
     * 设置索引类型。
     *
     * @param indexType 索引类型
     */
    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    /**
     * 获取距离度量类型。
     *
     * @return 度量类型
     */
    public String getMetricType() {
        return metricType;
    }

    /**
     * 设置距离度量类型。
     *
     * @param metricType 度量类型
     */
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    /**
     * 获取索引参数 JSON。
     *
     * @return 索引参数
     */
    public String getIndexParams() {
        return indexParams;
    }

    /**
     * 设置索引参数 JSON。
     *
     * @param indexParams 索引参数
     */
    public void setIndexParams(String indexParams) {
        this.indexParams = indexParams;
    }
}
