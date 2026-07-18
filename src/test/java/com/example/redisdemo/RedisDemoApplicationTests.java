package com.example.redisdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基础的测试：验证 Spring 容器能否正常启动（能启动说明配置没问题）
 * 注意：Lettuce 客户端是懒加载连接的，所以即使 Redis 没启动，这个测试也能通过
 */
@SpringBootTest
class RedisDemoApplicationTests {

    @Test
    void contextLoads() {
        // 什么都不用写，能跑完不报错就说明 Spring 上下文加载成功
    }
}
