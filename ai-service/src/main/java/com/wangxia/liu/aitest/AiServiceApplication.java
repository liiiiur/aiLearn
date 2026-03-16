package com.wangxia.liu.aitest;

import com.wangxia.liu.aitest.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI 服务启动类。
 */
@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class AiServiceApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }

}
