package com.wangxia.liu.aitest.migrate.controller;

import com.wangxia.liu.aitest.migrate.service.DamengToMilvusMigrationService;
import com.wangxia.liu.aitest.migrate.service.MigrationResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 迁移任务接口控制器。
 */
@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final DamengToMilvusMigrationService migrationService;

    /**
     * 构造控制器。
     *
     * @param migrationService 迁移服务
     */
    public MigrationController(DamengToMilvusMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * 手动触发迁移任务。
     *
     * @return 迁移结果
     */
    @PostMapping("/run")
    public MigrationResult run() {
        return migrationService.migrate();
    }
}
