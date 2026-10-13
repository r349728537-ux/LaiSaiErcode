package com.lesaire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 莱赛尔平台启动入口，由 Spring Boot 加载配置、扫描组件并启动 Web 服务。
 */
@SpringBootApplication
public class LesaireApplication {
    public static void main(String[] args) {
        SpringApplication.run(LesaireApplication.class, args);
    }
}
