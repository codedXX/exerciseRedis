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
     *   curl http://localhost:8080/redis/get/user:1   （查询上面保存的用户对象）
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
