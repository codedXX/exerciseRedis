package com.example.redisdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 增删改查服务类
 *
 * 所有对 Redis 的操作都封装在这一层，Controller 只负责接收请求、调用这里的方法。
 *
 * RedisTemplate 常用入口：
 *   opsForValue()  —— 操作 String 字符串类型（最常用，本项目用它演示）
 *   opsForHash()   —— 操作 Hash 类型
 *   opsForList()   —— 操作 List 类型
 *   opsForSet()    —— 操作 Set 类型
 *   opsForZSet()   —— 操作 ZSet（有序集合）类型
 */
@Service
public class RedisService {

    /**
     * key 的统一前缀：相当于给本项目的 key 划一个"命名空间"。
     * 比如代码里操作 key=name，实际存进 Redis 的是 exerciseRedis:name。
     * 好处：多个项目共用一个 Redis 时不会互相冲突，用客户端工具查看时也一目了然。
     *
     * 注意：这个值来自 application.yml 里的【自定义配置项】 app.redis.key-prefix，
     * Spring Boot 官方并没有提供 key 前缀的配置（spring.data.redis.* 只有连接相关配置），
     * 所以我们自己定义一个，用 @Value 注进来；冒号后面的 "" 是默认值（不配则不加前缀）。
     */
    @Value("${app.redis.key-prefix:}")
    private String keyPrefix;

    /**
     * RedisTemplate 是 Spring 提供的操作 Redis 的核心工具类，
     * 泛型 <String, Object> 表示：key 是字符串，value 可以是任意对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 通过构造方法注入 RedisTemplate（推荐写法，比 @Autowired 字段注入更规范） */
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拼装真正的 key：传入 name，返回 exerciseRedis:name
     * 本类所有方法都通过它统一加前缀，Controller 层完全无感知
     */
    private String buildKey(String key) {
        return keyPrefix + key;
    }

    // ==================== 新增 / 修改 ====================

    /**
     * 保存数据（新增）
     * 注意：如果 key 已经存在，会直接覆盖旧值 —— 所以这个方法同时也是"修改"
     *
     * @param key   键
     * @param value 值（可以是字符串，也可以是对象，对象会被序列化成 JSON）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(buildKey(key), value);
    }

    /**
     * 保存数据，并设置过期时间（常用于验证码、登录 token 等场景）
     * <p>
     * 说明：底层调用的是 4 个参数的 set 重载方法 set(key, value, timeout, unit)，
     * 对应 Redis 的 SETEX 命令。其中 unit 是 Java 标准库的时间单位枚举 TimeUnit
     * （可选 DAYS / HOURS / MINUTES / SECONDS / MILLISECONDS 等），
     * 作用是告诉框架 timeout 这个数字的"单位"是什么，避免"60 到底是 60 秒还是 60 毫秒"的歧义。
     * 本方法固定传 TimeUnit.SECONDS，所以入参直接用"秒"即可；
     * 若想支持其他单位，可以把入参改成 TimeUnit 类型由调用方传入。
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间，单位：秒。时间一到，key 会被 Redis 自动删除
     */
    public void setWithExpire(String key, Object value, long seconds) {
        // TimeUnit.SECONDS：声明 seconds 这个数的单位是"秒"
        redisTemplate.opsForValue().set(buildKey(key), value, seconds, TimeUnit.SECONDS);
    }

    // ==================== 查询 ====================

    /**
     * 根据 key 查询 value
     *
     * @return 查到的值；如果 key 不存在（或已过期），返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(buildKey(key));
    }

    /**
     * 判断某个 key 是否存在
     *
     * @return true=存在，false=不存在
     */
    public boolean hasKey(String key) {
        // redisTemplate.hasKey() 返回 Boolean 包装类，可能为 null，所以用 TRUE.equals 判空
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(key)));
    }

    /**
     * 查看 key 的剩余过期时间（单位：秒）
     *
     * @return 正数=剩余秒数；-1=永不过期；-2=key 不存在
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(buildKey(key), TimeUnit.SECONDS);
    }

    // ==================== 删除 ====================

    /**
     * 根据 key 删除数据
     *
     * @return true=删除成功；false=删除失败（key 本来就不存在）
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(buildKey(key)));
    }
}
