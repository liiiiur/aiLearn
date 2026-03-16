package com.wangxia.liu.aitest.migrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 达梦数据库连接配置。
 */
@ConfigurationProperties(prefix = "dameng")
public class DamengProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName = "dm.jdbc.driver.DmDriver";

    /**
     * 获取 JDBC 连接地址。
     *
     * @return JDBC 地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置 JDBC 连接地址。
     *
     * @param url JDBC 地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取数据库用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置数据库用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取数据库密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置数据库密码。
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取 JDBC 驱动类名。
     *
     * @return 驱动类名
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * 设置 JDBC 驱动类名。
     *
     * @param driverClassName 驱动类名
     */
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }
}
