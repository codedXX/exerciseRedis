package com.example.redisdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 【为什么要自定义 RedisTemplate？】
 * Spring Boot 默认提供的 RedisTemplate 使用的是 JDK 自带的序列化方式，
 * 存进 Redis 的 key 和 value 会变成一串乱码（类似 \xac\xed\x00\x05t\x00\x04name），
 * 用客户端工具查看时非常不直观。
 *
 * 所以我们自定义序列化规则：
 *   key   —— 用 String 序列化（存进去就是普通字符串，方便查看）
 *   value —— 用 JSON 序列化（对象会自动转成 JSON 字符串，任何语言都能读）
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate，替代 Spring Boot 默认的那个
     *
     * @param connectionFactory Redis 连接工厂（Spring Boot 根据 application.yml 自动创建）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key 和 hashKey 使用 String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 和 hashValue 使用 JSON 序列化器（存对象时会自动转 JSON，取出时自动转回对象）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 初始化模板（必须调用，否则配置不生效）
        template.afterPropertiesSet();
        return template;
    }
}
