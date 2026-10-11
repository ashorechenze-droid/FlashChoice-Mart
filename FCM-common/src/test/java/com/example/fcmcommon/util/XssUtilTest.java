package com.example.fcmcommon.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XssUtil 单元测试：覆盖标签剥离、空值透传与首尾空白裁剪
 */
class XssUtilTest {

    @Test
    void plainTextShouldRemainUnchanged() {
        assertThat(XssUtil.stripTags("普通商品名称 A&B")).isEqualTo("普通商品名称 A&B");
    }

    @Test
    void scriptTagShouldBeStripped() {
        assertThat(XssUtil.stripTags("<script>alert(1)</script>正常文本"))
                .isEqualTo("alert(1)正常文本");
    }

    @Test
    void eventAttributeTagShouldBeStripped() {
        assertThat(XssUtil.stripTags("<img src=x onerror=alert(1)>商品"))
                .isEqualTo("商品");
    }

    @Test
    void multilineTagShouldBeStripped() {
        assertThat(XssUtil.stripTags("<div\nclass=\"x\">内容</div>")).isEqualTo("内容");
    }

    @Test
    void nullAndBlankShouldPassThrough() {
        assertThat(XssUtil.stripTags(null)).isNull();
        assertThat(XssUtil.stripTags("   ")).isEqualTo("   ");
    }

    @Test
    void surroundingWhitespaceShouldBeTrimmed() {
        assertThat(XssUtil.stripTags("  <b>名称</b>  ")).isEqualTo("名称");
    }
}
