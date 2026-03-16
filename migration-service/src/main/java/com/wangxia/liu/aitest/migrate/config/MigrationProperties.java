package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 迁移任务相关配置。
 */
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {
    private String sourceTable;
    private String idColumn = "id";
    private String textColumn = "content";
    private List<String> metadataColumns = new ArrayList<>();
    private String whereClause;
    private String orderBy;
    private int batchSize = 200;
    private boolean autoRun = false;
    private boolean failFast = true;
    private int maxRows = 0;
    private String idType = "VARCHAR";

    /**
     * 获取源表名。
     *
     * @return 源表名
     */
    public String getSourceTable() {
        return sourceTable;
    }

    /**
     * 设置源表名。
     *
     * @param sourceTable 源表名
     */
    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    /**
     * 获取主键列名。
     *
     * @return 主键列名
     */
    public String getIdColumn() {
        return idColumn;
    }

    /**
     * 设置主键列名。
     *
     * @param idColumn 主键列名
     */
    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    /**
     * 获取文本列名。
     *
     * @return 文本列名
     */
    public String getTextColumn() {
        return textColumn;
    }

    /**
     * 设置文本列名。
     *
     * @param textColumn 文本列名
     */
    public void setTextColumn(String textColumn) {
        this.textColumn = textColumn;
    }

    /**
     * 获取元数据列名列表。
     *
     * @return 元数据列名列表
     */
    public List<String> getMetadataColumns() {
        return metadataColumns;
    }

    /**
     * 设置元数据列名列表。
     *
     * @param metadataColumns 元数据列名列表
     */
    public void setMetadataColumns(List<String> metadataColumns) {
        this.metadataColumns = metadataColumns;
    }

    /**
     * 获取筛选条件。
     *
     * @return WHERE 条件
     */
    public String getWhereClause() {
        return whereClause;
    }

    /**
     * 设置筛选条件。
     *
     * @param whereClause WHERE 条件
     */
    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * 获取排序字段。
     *
     * @return ORDER BY 字段
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * 设置排序字段。
     *
     * @param orderBy ORDER BY 字段
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * 获取批量大小。
     *
     * @return 批量大小
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置批量大小。
     *
     * @param batchSize 批量大小
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 是否启动时自动执行迁移。
     *
     * @return 是否自动执行
     */
    public boolean isAutoRun() {
        return autoRun;
    }

    /**
     * 设置启动时是否自动执行迁移。
     *
     * @param autoRun 是否自动执行
     */
    public void setAutoRun(boolean autoRun) {
        this.autoRun = autoRun;
    }

    /**
     * 是否遇到错误立即失败。
     *
     * @return 是否快速失败
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * 设置是否遇到错误立即失败。
     *
     * @param failFast 是否快速失败
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * 获取最大处理行数。
     *
     * @return 最大行数
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * 设置最大处理行数。
     *
     * @param maxRows 最大行数
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * 获取主键类型配置。
     *
     * @return 主键类型
     */
    public String getIdType() {
        return idType;
    }

    /**
     * 设置主键类型配置。
     *
     * @param idType 主键类型
     */
    public void setIdType(String idType) {
        this.idType = idType;
    }
}
