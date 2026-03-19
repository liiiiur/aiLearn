package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 达梦数据源配置，供 JPA 使用。
 */
@Configuration
public class DamengDataSourceConfig {

    /**
     * 基于 dameng.* 配置创建数据源。
     *
     * @return 数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "dameng")
    public DataSource damengDataSource() {
        return DataSourceBuilder.create().build();
    }
}
