package com.example.fcmcommon.util;

import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonUtil 单元测试：覆盖往返序列化与 transient 字段不参与缓存序列化的约定
 */
class JsonUtilTest {

    @Test
    void roundTripShouldKeepValues() {
        Sample sample = new Sample();
        sample.setId(2L);
        sample.setName("平板");

        Sample parsed = JsonUtil.fromJson(JsonUtil.toJson(sample), Sample.class);

        assertThat(parsed.getId()).isEqualTo(2L);
        assertThat(parsed.getName()).isEqualTo("平板");
    }

    @Test
    void transientFieldShouldNotBeSerialized() {
        Sample sample = new Sample();
        sample.setId(1L);
        sample.setName("手机");
        sample.setRuntimeOnly("Feign 组装字段，不应进缓存");

        String json = JsonUtil.toJson(sample);

        assertThat(json).contains("\"name\":\"手机\"").doesNotContain("runtimeOnly");
    }

    /** 测试载体：模拟实体的 transient 组装字段 */
    @Data
    private static class Sample {
        private Long id;
        private String name;
        /** 非表字段：验证不参与 JSON 序列化 */
        private transient String runtimeOnly;
    }
}
