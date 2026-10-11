package com.example.fcmuploadmicroservice.api;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.response.JsonResp;
import com.example.fcmuploadmicroservice.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传与静态资源读取（网关路由 /upload/**）：
 * <ul>
 *   <li>POST /upload/image：图片上传（multipart 字段 file），类型/大小白名单校验后存本地目录，返回可访问 URL；</li>
 *   <li>GET /upload/files/{filename}：读取上传文件（文件名白名单校验，防路径穿越）。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("upload")
public class UploadApi {

    /** 文件名安全白名单：仅字母/数字/点/下划线/连字符（存储文件名由服务端生成，读取时再校验防穿越） */
    private static final String SAFE_FILENAME_PATTERN = "[A-Za-z0-9._-]+";

    private final UploadProperties uploadProperties;

    public UploadApi(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
        initStorageDirectory();
    }

    /** 启动时初始化存储目录（不可用时仅告警，不阻断服务启动） */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(storageRoot());
        } catch (IOException e) {
            log.error("上传目录创建失败 location={}: {}", uploadProperties.getLocation(), e.getMessage());
        }
    }

    private Path storageRoot() {
        return Paths.get(uploadProperties.getLocation()).toAbsolutePath().normalize();
    }

    /** 图片上传：返回 {url, name, size}，url 为经网关可访问的相对路径 */
    @PostMapping("/image")
    public JsonResp uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResponseCode.BAD_REQUEST, "上传文件为空");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BizException(ResponseCode.BAD_REQUEST,
                    "文件大小超出限制（最大 " + uploadProperties.getMaxSize() / 1024 / 1024 + "MB）");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!uploadProperties.getAllowedExtensions().contains(extension)) {
            throw new BizException(ResponseCode.BAD_REQUEST, "不支持的文件类型: " + extension);
        }
        // 唯一文件名（UUID）防止覆盖
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.createDirectories(storageRoot());
            file.transferTo(storageRoot().resolve(filename));
        } catch (IOException e) {
            log.error("文件保存失败 filename={}: {}", filename, e.getMessage());
            throw new BizException(ResponseCode.INTERNAL_ERROR, "文件保存失败，请稍后重试");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", "/upload/files/" + filename);
        data.put("name", file.getOriginalFilename());
        data.put("size", file.getSize());
        return JsonResp.ok(data);
    }

    /** 读取上传文件（公开）：文件名白名单校验，杜绝 ../ 路径穿越 */
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> readFile(@PathVariable String filename) {
        if (!filename.matches(SAFE_FILENAME_PATTERN)) {
            return ResponseEntity.badRequest().build();
        }
        Path path = storageRoot().resolve(filename).normalize();
        if (!path.startsWith(storageRoot()) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(path));
    }

    /** 提取小写扩展名（无扩展名时返回空串，落入白名单校验失败分支） */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase();
    }
}
