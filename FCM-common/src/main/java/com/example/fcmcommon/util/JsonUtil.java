package com.example.fcmcommon.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 工具类：对象与 JSON 字符串互转（Jackson 2）
 *
 * <p>缓存读写统一用字符串 JSON 存储（配合 StringRedisTemplate），
 * 避免 JDK 序列化对实体类结构变更（serialVersionUID）的兼容性问题。
 */
public final class JsonUtil {

    /** 统一 ObjectMapper（线程安全）：忽略未知字段（容忍缓存旧结构）；
     *  transient 字段不参与序列化（实体中均为 Feign 组装/树形等非表字段，缓存无需保留） */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

    private JsonUtil() {
    }

    /** 对象转 JSON 字符串；序列化失败抛出 IllegalStateException */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    /** JSON 字符串转对象；反序列化失败抛出 IllegalArgumentException */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /** JSON 字符串转泛型对象（如 List&lt;CategoryEntity&gt;）；反序列化失败抛出 IllegalArgumentException */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }
}
