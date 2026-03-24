package com.my.family.paxl.controller;

import com.my.family.paxl.enums.StaticFileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 静态协议文件控制器
 * 读取 resources/stastic 目录下的协议文本文件并返回内容
 * 接口无需鉴权，用于登录前展示用户协议和隐私声明
 *
 * @author ai
 * @date 2026/03/24
 */
@RestController
@RequestMapping("/api/static")
@Slf4j
public class StaticFileController {

    /**
     * 读取协议文件内容
     * GET /api/static/file?type=USER_PROTOCOL
     * GET /api/static/file?type=PRIVACY_STATEMENT
     *
     * @param type 文件类型枚举名称
     * @return 包含 title 和 content 的响应体
     */
    @GetMapping("/file")
    public ResponseEntity<Map<String, String>> getFile(@RequestParam("type") String type) {
        StaticFileType fileType;
        try {
            fileType = StaticFileType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("[StaticFile] 未知文件类型: {}", type);
            return ResponseEntity.badRequest().build();
        }

        try {
            String filePath = "stastic/" + fileType.getFileName();
            ClassPathResource resource = new ClassPathResource(filePath);

            if (!resource.exists()) {
                log.warn("[StaticFile] 文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String content;
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                content = FileCopyUtils.copyToString(reader);
            }

            Map<String, String> result = new HashMap<>();
            result.put("title", fileType.getDisplayName());
            result.put("content", content);

            log.info("[StaticFile] 读取文件成功: {}", fileType.getFileName());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[StaticFile] 读取文件异常, type={}", type, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
