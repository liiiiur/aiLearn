package com.wangxia.liu.aitest.migrate.service;

import com.wangxia.liu.aitest.migrate.config.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时自动触发迁移任务。
 */
@Component
public class MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final MigrationProperties migrationProperties;
    private final DamengToMilvusMigrationService migrationService;

    /**
     * 构造启动器。
     *
     * @param migrationProperties 迁移配置
     * @param migrationService 迁移服务
     */
    public MigrationRunner(MigrationProperties migrationProperties, DamengToMilvusMigrationService migrationService) {
        this.migrationProperties = migrationProperties;
        this.migrationService = migrationService;
    }

    /**
     * 应用启动后执行。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!migrationProperties.isAutoRun()) {
            return;
        }
        log.info("已开启自动迁移，开始执行...");
        migrationService.migrate();
    }
}
