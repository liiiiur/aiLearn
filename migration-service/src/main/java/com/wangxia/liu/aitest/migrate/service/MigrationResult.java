package com.wangxia.liu.aitest.migrate.service;

public record MigrationResult(long totalRows, long insertedRows, long failedRows, long durationMs) {
}
