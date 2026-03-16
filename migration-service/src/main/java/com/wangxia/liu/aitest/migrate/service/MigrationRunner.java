package com.wangxia.liu.aitest.migrate.service;

import com.wangxia.liu.aitest.migrate.config.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final MigrationProperties migrationProperties;
    private final DamengToMilvusMigrationService migrationService;

    public MigrationRunner(MigrationProperties migrationProperties, DamengToMilvusMigrationService migrationService) {
        this.migrationProperties = migrationProperties;
        this.migrationService = migrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!migrationProperties.isAutoRun()) {
            return;
        }
        log.info("Auto migration enabled, starting...");
        migrationService.migrate();
    }
}
