package com.wangxia.liu.aitest.migrate.service;

/**
 * 迁移结果统计。
 */
public record MigrationResult(
        /**
         * 总处理行数。
         */
        long totalRows,
        /**
         * 成功写入行数。
         */
        long insertedRows,
        /**
         * 失败行数。
         */
        long failedRows,
        /**
         * 耗时毫秒数。
         */
        long durationMs
) {
}
