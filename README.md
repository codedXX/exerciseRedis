# Spring Boot 3 + Redis 增删改查练习项目

> 一个适合初学者的 Spring Boot 3.x + Redis 入门项目，包含**完整的增删改查（CRUD）示例**，
> 所有代码均带详细中文注释，并已通过真实接口调用测试。

---

## 目录

- [一、环境要求](#一环境要求)
- [二、项目结构](#二项目结构)
- [三、第一步：引入依赖（pom.xml）](#三第一步引入依赖pomxml)
- [四、第二步：配置 Redis 连接（application.yml）](#四第二步配置-redis-连接applicationyml)
- [五、第三步：自定义 Redis 配置类（RedisConfig）](#五第三步自定义-redis-配置类redisconfig)
- [六、第四步：编写实体类（User）](#六第四步编写实体类user)
- [七、第五步：编写 Service 层（增删改查核心）](#七第五步编写-service-层增删改查核心)
- [八、第六步：编写 Controller 层（REST 接口）](#八第六步编写-controller-层rest-接口)
- [九、第七步：启动类](#九第七步启动类)
- [十、第八步：启动项目并测试所有接口](#十第八步启动项目并测试所有接口)
- [十一、常见问题（FAQ）](#十一常见问题faq)
- [十二、官方文档参考](#十二官方文档参考)

---

## 一、环境要求

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17 及以上 | Spring Boot 3.x 最低要求 JDK 17 |
| Maven | 3.5+ | 用于构建项目 |
| Redis | 5.0+ | 需提前启动，默认地址 `localhost:6379` |
| IDEA | 任意版本 | 推荐，也可以纯命令行运行 |

> 检查 Redis 是否已启动：Mac 终端执行 `nc -z localhost 6379`，显示 succeeded 即正常。

---

## 二、项目结构

```
exerciseRedis/
├── pom.xml                                        # Maven 配置文件（引入依赖）
└── src/main/
    ├── java/com/example/redisdemo/
    │   ├── RedisDemoApplication.java              # 【启动类】项目入口
    │   ├── config/
    │   │   └── RedisConfig.java                   # 【配置类】自定义序列化规则
    │   ├── entity/
    │   │   └── User.java                          # 【实体类】演示对象存储
    │   ├── service/
    │   │   └── RedisService.java                  # 【业务层】增删改查核心逻辑
    │   └── controller/
    │       └── RedisController.java               # 【控制层】对外 REST 接口
    └── resources/
        └── application.yml                        # 【配置文件】Redis 连接信息
```

**各层职责一句话总结：**

```
浏览器/curl  →  Controller（接收请求） →  Service（操作 Redis） →  Redis 数据库
```

---

## 三、第一步：引入依赖（pom.xml）

Spring Boot 操作 Redis 最核心的依赖就是 **`spring-boot-starter-data-redis`**。
它底层默认使用 **Lettuce** 客户端（基于 Netty，线程安全）。

完整 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 Spring Boot 官方父工程，由它统一管理各种依赖的版本号 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <!-- 当前项目的基本信息 -->
    <groupId>com.example</groupId>
    <artifactId>redis-demo</artifactId>
    <version>1.0.0</version>
    <name>redis-demo</name>
    <description>Spring Boot 3 + Redis 增删改查练习项目</description>

    <properties>
        <!-- 指定使用 JDK 17 编译 -->
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web 模块：让我们可以用 REST 接口来测试 Redis 操作 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Redis 模块：Spring 操作 Redis 的核心依赖（默认底层客户端是 Lettuce） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- 测试模块：Spring Boot 自带的测试支持（可选） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：可以把项目打成可直接运行的 jar 包 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**关键点说明：**

1. `spring-boot-starter-parent`：父工程，帮我们管理所有依赖的版本，所以引入依赖时**不需要写版本号**。
2. `spring-boot-starter-web`：提供 Tomcat 和 Spring MVC，让我们能通过 HTTP 接口测试 Redis。
3. `spring-boot-starter-data-redis`：核心依赖，引入了 Spring Data Redis + Lettuce 客户端。
4. `<java.version>17</java.version>`：Spring Boot 3.x 最低要求 JDK 17。

---

## 四、第二步：配置 Redis 连接（application.yml）

在 `src/main/resources/` 下新建 `application.yml`：

```yaml
# 服务端口
server:
  port: 8080

spring:
  # 注意：Spring Boot 3.x 的 Redis 配置前缀是 spring.data.redis
  # （Spring Boot 2.x 是 spring.redis，写法不一样）
  data:
    redis:
      host: localhost        # Redis 服务器地址
      port: 6379             # Redis 端口（默认 6379）
      database: 0            # 使用几号数据库（Redis 默认有 0~15 共 16 个库）
      # password:            # 如果 Redis 设置了密码，取消注释并填写
      timeout: 5000ms        # 连接超时时间

# ========== 以下是本项目的自定义配置（不是 Spring Boot 官方属性）==========
app:
  redis:
    key-prefix: "exerciseRedis:"   # key 统一前缀：所有 key 存进 Redis 前都会自动加上它
                                   # 改成 "" 表示不加前缀
```

**注意事项：**

| 版本 | 配置前缀 |
|------|---------|
| Spring Boot 2.x | `spring.redis.*` |
| Spring Boot 3.x | `spring.data.redis.*`（本项目用这个） |

> 如果你的 Redis 就在本机且没有密码，其实**不配也能跑**（Spring Boot 默认连接 `localhost:6379`），
> 但显式写出来更清晰，也方便以后改成远程地址。

---

## 五、第三步：自定义 Redis 配置类（RedisConfig）

### 5.1 为什么要自定义？

Spring Boot 会自动配置一个 `RedisTemplate`，但它**没有设置任何序列化器**，
最终会使用 **JDK 原生序列化**（`JdkSerializationRedisSerializer`），带来三个问题：

| 问题 | 说明 |
|------|------|
| **乱码** | 存进 Redis 的 key/value 是 `\xac\xed\x00\x05...` 二进制数据，客户端工具里完全看不懂 |
| **跨语言障碍** | JDK 序列化的字节只有 Java 能反序列化，Python/Go/Node 都读不了 |
| **安全隐患** | 官方文档明确警告 Java 原生序列化存在反序列化漏洞风险，推荐用 JSON |

**官方文档依据**（[Spring Data Redis - Serializers](https://docs.spring.io/spring-data/redis/reference/redis/template.html#redis:serializer)）：

> "By default, `RedisCache` and `RedisTemplate` are configured to use Java native serialization...
> In general, we **strongly recommend any other message format (such as JSON) instead.**"

**替换机制依据**（[Spring Boot - Redis](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis)）：

> "If you add your own `@Bean`... it replaces the default (except in the case of `RedisTemplate`,
> when the exclusion is based on the **bean name**, `redisTemplate`, not its type)."

也就是说：**只要我们定义一个名为 `redisTemplate` 的 Bean，Spring Boot 默认的那个就会自动失效。**

### 5.2 配置类代码

新建 `config/RedisConfig.java`：

```java
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
```

### 5.3 自定义前后对比

假设执行 `set("name", "张三")`：

| | key 在 Redis 中的样子 | value 在 Redis 中的样子 |
|---|---|---|
| **默认（JDK 序列化）** | `\xac\xed\x00\x05t\x00\x04name` | `\xac\xed\x00\x05t\x00\x06\xe5\xbc\xa0...` |
| **自定义（String+JSON）** | `name` | `"\xe5\xbc\xa0\xe4\xb8\x89"`（即 `"张三"` 的 UTF-8，可读） |

---

## 六、第四步：编写实体类（User）

为了演示"**把 Java 对象存进 Redis**"，新建一个简单的 `entity/User.java`：

```java
package com.example.redisdemo.entity;

/**
 * 用户实体类
 * 用来演示"把 Java 对象存进 Redis"（对象会被序列化成 JSON 字符串存储）
 *
 * 注意：必须提供【无参构造方法】，否则从 Redis 取出 JSON 反序列化回对象时会报错
 */
public class User {

    private Long id;      // 用户 id
    private String name;  // 用户名
    private Integer age;  // 年龄

    /** 无参构造方法（JSON 反序列化必须要用） */
    public User() {
    }

    /** 全参构造方法（方便代码里直接 new 对象） */
    public User(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + "}";
    }
}
```

> **重点**：实体类必须有无参构造方法和 getter/setter，否则 JSON 序列化/反序列化会失败。

---

## ⭐️七、第五步：编写 Service 层（增删改查核心）

新建 `service/RedisService.java`，**所有 Redis 操作都封装在这里**：

```java
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
```

⭐️**方法速查表：**

| 操作分类 | 方法 | 对应的 Redis 命令 |
|---------|------|------------------|
| 新增/修改 | `set(key, value)` | `SET key value` |
| 新增（带过期） | `setWithExpire(key, value, seconds)` | `SETEX key seconds value` |
| 查询 | `get(key)` | `GET key` |
| 查询 | `hasKey(key)` | `EXISTS key` |
| 查询 | `getExpire(key)` | `TTL key` |
| 删除 | `delete(key)` | `DEL key` |

> ⭐️**redisTemplate.opsForValue().set(buildKey(key), value); 这个是为什么这样写？**
>
> 文档里的对照表就是答案：
>
> | 方法            | 操作的类型       | 里面的方法对应                                     |
> | --------------- | ---------------- | -------------------------------------------------- |
> | `opsForValue()` | String           | `set` → `SET`、`get` → `GET`、`increment` → `INCR` |
> | `opsForHash()`  | Hash             | `put` → `HSET`、`get` → `HGET`                     |
> | `opsForList()`  | List             | `leftPush` → `LPUSH`、`range` → `LRANGE`           |
> | `opsForSet()`   | Set              | `add` → `SADD`、`members` → `SMEMBERS`             |
> | `opsForZSet()`  | ZSet（有序集合） | `add` → `ZADD`、`range` → `ZRANGE`                 |



> ⭐️**redis的前缀和key之间一定要用冒号:来进行分割吗？**
>
> 
>
> **结论：**分隔符随便选，但推荐冒号
>
> | 分隔符        | 能用吗 | 说明                        |
> | ------------- | ------ | --------------------------- |
> | `:`（冒号）   | ✅      | 社区标准惯例，推荐          |
> | `.`（点号）   | ✅      | 合法，但工具默认不按它分组  |
> | `_`（下划线） | ✅      | 合法，同上                  |
> | `/`、`-` 等   | ✅      | 合法，同上                  |
> | 不用分隔符    | ✅      | 合法，但 key 一长就没法读了 |
>
> **为什么实际工作中大家都用冒号**
>
> 1. **GUI 工具默认按 : 分组**：你的客户端开箱就能显示树状目录；用别的符号就得去工具设置里手动改分隔符
> 2. 官方文档示例就是冒号：Redis 文档建议的命名模式是 对象类型:id，如 user:1000
> 3. 生态惯例：看看成熟框架怎么命名的——
> - Spring Session：spring:session:sessions:xxx
> - Spring Cache：缓存名::key（双冒号）
> - 各种中间件几乎都遵循 项目:模块:业务:id 的冒号风格
>
> 可读性：user:1001:orders 比 user_1001_orders 层次感更清晰
> 对我们项目来说
> **前缀就是 yml 里的一行配置，字符串拼接而已，用什么符号随你：**
>
> ~~~yml
> app:
>   redis:
>  key-prefix: "exerciseRedis:"   # 改成 "exerciseRedis." 或 "exerciseRedis_" 都能跑只是改完后，记得客户端工
> ~~~
>
> 只是改完后，记得客户端工具里可能也要同步调整分隔符设置，才能继续享受树状分组的效果。结论：不强制，但随大流用 : 最省心。

## 八、第六步：编写 Controller 层（REST 接口）

新建 `controller/RedisController.java`，把 Service 的方法暴露成 HTTP 接口，方便测试：

```java
package com.example.redisdemo.controller;

import com.example.redisdemo.entity.User;
import com.example.redisdemo.service.RedisService;
import org.springframework.web.bind.annotation.*;

/**
 * Redis 增删改查接口
 *
 * 启动项目后，可以用浏览器 / Postman / curl 调用下面这些接口，
 * 每个方法上都写了对应的 curl 测试命令，复制到终端就能用。
 */
@RestController
@RequestMapping("/redis")   // 所有接口的统一前缀：/redis
public class RedisController {

    private final RedisService redisService;

    /** 构造方法注入 Service */
    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    // ==================== 新增 / 修改 ====================

    /**
     * 【新增/修改】保存一个字符串
     * 测试命令：
     *   curl -X POST "http://localhost:8080/redis/set?key=name&value=张三"
     */
    // 注意：注解里显式写上参数名（如 "key"、"value"），而不是只写 @RequestParam String key。
    // 原因：不显式写时，Spring 要靠反射读取方法的参数名，这要求编译时加 -parameters 参数。
    // Maven 打包默认会加，但 IDEA 直接运行默认不加，就会报
    // "Name for argument of type [java.lang.String] not specified..." 的错误。
    // 显式写上参数名后，任何环境运行都不会有这个问题。
    @PostMapping("/set")
    public String set(@RequestParam("key") String key, @RequestParam("value") String value) {
        redisService.set(key, value);
        return "保存成功：" + key + " = " + value;
    }

    /**
     * 【新增】保存一个字符串，并设置过期时间（秒）
     * 测试命令：
     *   curl -X POST "http://localhost:8080/redis/setExpire?key=code&value=123456&seconds=60"
     */
    @PostMapping("/setExpire")
    public String setExpire(@RequestParam("key") String key,
                            @RequestParam("value") String value,
                            @RequestParam("seconds") long seconds) {
        redisService.setWithExpire(key, value, seconds);
        return "保存成功：" + key + " = " + value + "，" + seconds + " 秒后自动过期";
    }

    /**
     * 【新增】保存一个对象（演示 JSON 序列化，key 统一用 user:id 格式）
     * 测试命令：
     *   curl -X POST -H "Content-Type: application/json" \
     *        -d '{"id":1,"name":"张三","age":20}' \
     *        http://localhost:8080/redis/user
     */
    @PostMapping("/user")
    public String saveUser(@RequestBody User user) {
        redisService.set("user:" + user.getId(), user);
        return "用户保存成功：" + user;
    }

    // ==================== 查询 ====================

    /**
     * 【查询】根据 key 查询 value
     * 测试命令：
     *   curl http://localhost:8080/redis/get/name
     *   curl "http://localhost:8080/redis/get/user:1"   （查询上面保存的用户对象）
     */
    @GetMapping("/get/{key}")
    public Object get(@PathVariable("key") String key) {
        Object value = redisService.get(key);
        return value == null ? "key 不存在（或已过期）：" + key : value;
    }

    /**
     * 【查询】判断 key 是否存在
     * 测试命令：
     *   curl http://localhost:8080/redis/exists/name
     */
    @GetMapping("/exists/{key}")
    public String exists(@PathVariable("key") String key) {
        boolean exists = redisService.hasKey(key);
        return exists ? "key 存在：" + key : "key 不存在：" + key;
    }

    /**
     * 【查询】查看 key 的剩余过期时间（秒）
     * 测试命令：
     *   curl http://localhost:8080/redis/expire/code
     */
    @GetMapping("/expire/{key}")
    public String expire(@PathVariable("key") String key) {
        Long seconds = redisService.getExpire(key);
        return "key=" + key + "，剩余 " + seconds + " 秒（-1 表示永不过期，-2 表示 key 不存在）";
    }

    // ==================== 删除 ====================

    /**
     * 【删除】根据 key 删除数据
     * 测试命令：
     *   curl -X DELETE http://localhost:8080/redis/delete/name
     */
    @DeleteMapping("/delete/{key}")
    public String delete(@PathVariable("key") String key) {
        boolean success = redisService.delete(key);
        return success ? "删除成功：" + key : "删除失败，key 不存在：" + key;
    }
}
```

---

## 九、第七步：启动类

新建 `RedisDemoApplication.java`（这是创建 Spring Boot 项目时自带的，无需改动）：

```java
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
```

---

## 十、第八步：启动项目并测试所有接口

### 10.1 启动方式（三选一）

```bash
# 方式一：IDEA 中直接运行 RedisDemoApplication 的 main 方法

# 方式二：Maven 命令启动
mvn spring-boot:run

# 方式三：打包成 jar 后运行
mvn package
java -jar target/redis-demo-1.0.0.jar
```

看到控制台输出 `Started RedisDemoApplication` 即启动成功。

### 10.2 完整测试流程（以下结果均已实际验证通过）

#### ① 新增 —— 保存字符串

```bash
curl -X POST "http://localhost:8080/redis/set?key=name&value=%E5%BC%A0%E4%B8%89"
# 注意：curl 传中文需要 URL 编码，%E5%BC%A0%E4%B8%89 就是"张三"
# 用 Postman/Apifox 则直接写中文即可，工具会自动编码
```

返回：
```
保存成功：name = 张三
```

#### ② 新增 —— 保存并设置 60 秒过期时间

```bash
curl -X POST "http://localhost:8080/redis/setExpire?key=code&value=123456&seconds=60"
```

返回：
```
保存成功：code = 123456，60 秒后自动过期
```

#### ③ 新增 —— 保存对象（自动 JSON 序列化）

```bash
curl -X POST -H "Content-Type: application/json" \
     -d '{"id":1,"name":"李四","age":20}' \
     http://localhost:8080/redis/user
```

返回：
```
用户保存成功：User{id=1, name='李四', age=20}
```

#### ④ 查询 —— 获取字符串

```bash
curl http://localhost:8080/redis/get/name
```

返回：
```
张三
```

#### ⑤ 查询 —— 获取对象（自动反序列化）

```bash
curl "http://localhost:8080/redis/get/user:1"
```

返回（JSON 格式）：
```json
{"id":1,"name":"李四","age":20}
```

#### ⑥ 查询 —— 判断 key 是否存在

```bash
curl http://localhost:8080/redis/exists/name
```

返回：
```
key 存在：name
```

#### ⑦ 查询 —— 查看剩余过期时间

```bash
curl http://localhost:8080/redis/expire/code
```

返回：
```
key=code，剩余 59 秒（-1 表示永不过期，-2 表示 key 不存在）
```

#### ⑧ 修改 —— 同 key 覆盖旧值

Redis 的"修改"就是**用同一个 key 再 set 一次**，直接覆盖：

```bash
# 把 name 的值从"张三"改成"王五"（%E7%8E%8B%E4%BA%94 是"王五"的 URL 编码）
curl -X POST "http://localhost:8080/redis/set?key=name&value=%E7%8E%8B%E4%BA%94"
curl http://localhost:8080/redis/get/name
```

返回：
```
王五
```

#### ⑨ 删除

```bash
curl -X DELETE http://localhost:8080/redis/delete/name
```

返回：
```
删除成功：name
```

#### ⑩ 删除后验证

```bash
curl http://localhost:8080/redis/get/name
curl http://localhost:8080/redis/exists/name
curl -X DELETE http://localhost:8080/redis/delete/name   # 重复删除
```

返回：
```
key 不存在（或已过期）：name
key 不存在：name
删除失败，key 不存在：name
```

### 10.3 接口总览表

| 操作 | 请求方式 | 接口路径 | 说明 |
|------|---------|---------|------|
| 新增 | POST | `/redis/set?key=xx&value=xx` | 保存字符串 |
| 新增 | POST | `/redis/setExpire?key=xx&value=xx&seconds=60` | 保存并设置过期时间 |
| 新增 | POST | `/redis/user`（JSON 请求体） | 保存对象 |
| 查询 | GET | `/redis/get/{key}` | 根据 key 查值 |
| 查询 | GET | `/redis/exists/{key}` | 判断 key 是否存在 |
| 查询 | GET | `/redis/expire/{key}` | 查看剩余过期时间 |
| 修改 | POST | 同 `/redis/set` | key 相同时直接覆盖 |
| 删除 | DELETE | `/redis/delete/{key}` | 根据 key 删除 |

---

## 十一、常见问题（FAQ）

### 1. curl 传中文报 400 Bad Request？

**原因**：HTTP 协议规定 URL 中只能出现 ASCII 字符，curl 不会自动编码中文。

**解决方案**（任选其一）：
- 手动 URL 编码：`张三` → `%E5%BC%A0%E4%B8%89`
- 使用 `--data-urlencode`：
  ```bash
  curl -X POST "http://localhost:8080/redis/set" --data-urlencode "key=name" --data-urlencode "value=张三"
  ```
- 直接用 **Postman / Apifox** 等工具（自动编码，最推荐）

### 2. 不自定义 RedisConfig 会怎样？

能跑，但用 RedisInsight/redis-cli 查看数据时，key 和 value 都是 `\xac\xed\x00\x05...` 乱码。
详细原因见 [第五步](#五第三步自定义-redis-配置类redisconfig)。

### 3. 只存字符串，可以不写 RedisConfig 吗？

可以。Spring Boot 自动配置了一个 `StringRedisTemplate`，它的 key 和 value 都用 String 序列化，
直接注入使用即可，一行配置都不用写：

```java
@Autowired
private StringRedisTemplate stringRedisTemplate;
```

但代价是**只能存字符串**，存对象需要自己手动转 JSON。所以实际项目中一般还是自定义 `RedisTemplate`。

### 4. Spring Boot 2.x 的配置为什么报错？

2.x 的 Redis 配置前缀是 `spring.redis.*`，3.x 改成了 `spring.data.redis.*`，注意区分。

### 5. 连接远程 Redis 怎么配？

修改 `application.yml` 中的 host/port，有密码再加上 password：

```yaml
spring:
  data:
    redis:
      host: 你的Redis服务器IP
      port: 6379
      password: 你的密码
```

### 6. IDEA 里运行报 "Name for argument of type [java.lang.String] not specified, and parameter name information not available via reflection. Ensure that the compiler uses the '-parameters' flag" ？

**原因**：如果注解写成 `@RequestParam String key`（不显式指定参数名），Spring 需要通过 Java 反射
读取方法参数名 `key` 去匹配 URL 中的 `?key=xxx`。而反射能读到参数名的前提是**编译时加了
`-parameters` 参数**。

- Maven 打包：`spring-boot-starter-parent` 默认开启 `-parameters`，所以 `java -jar` 运行正常
- IDEA 直接运行：IDEA 使用自己的编译器，**默认不加** `-parameters`，参数名编译后丢失 → 报错

**解决方案**（本项目已采用第一种）：

1. **注解里显式写参数名**（推荐，一劳永逸，不依赖编译环境）：
   ```java
   public String set(@RequestParam("key") String key, @RequestParam("value") String value) { ... }
   ```
2. 或者给 IDEA 加编译参数：
   `Settings → Build, Execution, Deployment → Compiler → Java Compiler`
   → `Additional command line parameters` 填入 `-parameters`
3. 或者让 IDEA 委托 Maven 构建：
   `Settings → Build, Execution, Deployment → Build Tools → Maven → Runner`
   → 勾选 `Delegate IDE build/run actions to Maven`

### 7. 能不能直接在 application.yml 里配置 key 前缀？和 database 有什么区别？

**官方配置不支持，但可以自定义配置项来实现——本项目已经做好了**。

先明确一点：`spring.data.redis.*` 官方配置项里**没有** key 前缀这个属性（翻源码
`RedisProperties.java` 可以看到只有 database / host / port / password / timeout /
lettuce 等连接相关配置）。网上能搜到的 `spring.cache.redis.key-prefix` 只作用于
`@Cacheable` 缓存注解，对 RedisTemplate 无效。

本项目的做法：在 `application.yml` 里**自定义**一个配置项，代码里用 `@Value` 注入：

```yaml
app:
  redis:
    key-prefix: "exerciseRedis:"   # 改成 "" 表示不加前缀
```

```java
@Value("${app.redis.key-prefix:}")
private String keyPrefix;
```

这样前缀就**完全由 yml 控制**：改 yml 重启即生效，代码一行不用动。

前缀和 database 的区别：

| 机制 | 本质 | 特点 |
|------|------|------|
| `database: 0~15` | Redis 自带的逻辑分库 | 库之间完全隔离，但**集群模式（Cluster）只支持 db0** |
| key 前缀（如 `exerciseRedis:`） | 只是命名规范，数据都在同一个库里 | 任何模式都可用，是实际工作中更通用的"命名空间"方案 |

注意：改了前缀后，**之前存的旧前缀数据就读不到了**（`name` 和 `exerciseRedis:name` 是两个不同的 key）。

---

## 十二、官方文档参考

| 文档 | 链接 |
|------|------|
| Spring Boot - Redis 自动配置 | https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis |
| Spring Data Redis - RedisTemplate 与序列化器 | https://docs.spring.io/spring-data/redis/reference/redis/template.html |
| Redis 官方命令文档 | https://redis.io/commands |
# exerciseRedis
