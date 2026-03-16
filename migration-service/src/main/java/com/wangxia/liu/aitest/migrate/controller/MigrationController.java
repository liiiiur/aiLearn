package com.wangxia.liu.aitest.migrate.controller;

import com.wangxia.liu.aitest.migrate.service.DamengToMilvusMigrationService;
import com.wangxia.liu.aitest.migrate.service.MigrationResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final DamengToMilvusMigrationService migrationService;

    public MigrationController(DamengToMilvusMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/run")
    public MigrationResult run() {
        return migrationService.migrate();
    }
}
