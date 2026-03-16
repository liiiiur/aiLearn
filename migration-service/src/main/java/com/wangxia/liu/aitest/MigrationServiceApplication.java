package com.wangxia.liu.aitest;

import com.wangxia.liu.aitest.config.AiProperties;
import com.wangxia.liu.aitest.migrate.config.DamengProperties;
import com.wangxia.liu.aitest.migrate.config.MigrationProperties;
import com.wangxia.liu.aitest.migrate.config.MilvusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 迁移服务启动类。
 */
@SpringBootApplication
@EnableConfigurationProperties({
        AiProperties.class,
        DamengProperties.class,
        MilvusProperties.class,
        MigrationProperties.class
})
public class MigrationServiceApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MigrationServiceApplication.class, args);
    }

}
