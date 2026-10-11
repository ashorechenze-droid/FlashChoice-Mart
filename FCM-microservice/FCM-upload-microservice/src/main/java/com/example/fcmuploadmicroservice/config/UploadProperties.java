package com.example.fcmuploadmicroservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 上传配置（upload.*，详见详细设计 5.8 系统支撑）：
 * 存储目录、单文件大小上限、扩展名白名单
 */
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /** 上传文件存储目录（不存在时启动自动创建） */
    private String location;

    /** 单文件大小上限（字节），默认 5MB */
    private long maxSize = 5 * 1024 * 1024;

    /** 允许的扩展名白名单（小写，逗号分隔） */
    private List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");
}
