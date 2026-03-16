package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    public String getTextColumn() {
        return textColumn;
    }

    public void setTextColumn(String textColumn) {
        this.textColumn = textColumn;
    }

    public List<String> getMetadataColumns() {
        return metadataColumns;
    }

    public void setMetadataColumns(List<String> metadataColumns) {
        this.metadataColumns = metadataColumns;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isAutoRun() {
        return autoRun;
    }

    public void setAutoRun(boolean autoRun) {
        this.autoRun = autoRun;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
}
