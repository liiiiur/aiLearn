package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public int getShardsNum() {
        return shardsNum;
    }

    public void setShardsNum(int shardsNum) {
        this.shardsNum = shardsNum;
    }

    public int getIdMaxLength() {
        return idMaxLength;
    }

    public void setIdMaxLength(int idMaxLength) {
        this.idMaxLength = idMaxLength;
    }

    public int getTextMaxLength() {
        return textMaxLength;
    }

    public void setTextMaxLength(int textMaxLength) {
        this.textMaxLength = textMaxLength;
    }

    public int getMetadataMaxLength() {
        return metadataMaxLength;
    }

    public void setMetadataMaxLength(int metadataMaxLength) {
        this.metadataMaxLength = metadataMaxLength;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getIndexParams() {
        return indexParams;
    }

    public void setIndexParams(String indexParams) {
        this.indexParams = indexParams;
    }
}
