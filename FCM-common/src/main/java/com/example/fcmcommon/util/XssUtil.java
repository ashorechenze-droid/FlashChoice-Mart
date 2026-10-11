package com.example.fcmcommon.util;

import java.util.regex.Pattern;

/**
 * XSS 过滤工具：对纯文本字段剥离 HTML 标签（详细设计 8.2 安全防护）
 *
 * <p>适用范围：商品名称/别名/摘要、订单留言等"纯文本"字段；
 * 商品富文本详情（detail）需保留 HTML，不适用本工具。
 * 前端渲染配合 Vue/React 默认转义即可覆盖脚本注入场景。
 */
public final class XssUtil {

    /** HTML 标签匹配（DOTALL 允许标签跨行，如 &lt;script\\ndata=...&gt;） */
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);

    private XssUtil() {
    }

    /** 剥离全部 HTML 标签并去除首尾空白；入参为空时原样返回 */
    public static String stripTags(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return TAG_PATTERN.matcher(input).replaceAll("").trim();
    }
}
