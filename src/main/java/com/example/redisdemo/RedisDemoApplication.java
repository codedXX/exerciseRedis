package com.example.redisdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类
 *
 * @SpringBootApplication 是一个组合注解，包含：
 *   1. @SpringBootConfiguration —— 标识这是一个配置类
 *   2. @EnableAutoConfiguration —— 开启自动配置（比如自动装配 RedisTemplate）
 *   3. @ComponentScan —— 自动扫描当前包及子包下的所有组件（@Service、@Controller 等）
 */
@SpringBootApplication
public class RedisDemoApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(RedisDemoApplication.class, args);
        System.out.println("===== 项目启动成功，访问 http://localhost:8080 =====");
    }
}
