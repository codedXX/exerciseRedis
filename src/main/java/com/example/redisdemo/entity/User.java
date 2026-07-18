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
