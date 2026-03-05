package com.my.family.paxl.service.impl;

import com.my.family.paxl.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 图片服务实现类
 * 负责图片的上传、存储和读取
 *
 * @author fan
 * @date 2026/01/05
 */
@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    /**
     * 图片存储根目录，从配置文件读取，默认：./uploads/images
     */
    @Value("${paxl.image.upload-path:./uploads/images}")
    private String uploadPath;

    /**
     * 允许的图片格式
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    /**
     * 最大文件大小，单位：字节，默认5MB
     */
    @Value("${paxl.image.max-size:5242880}")
    private long maxFileSize;

    /**
     * 日期格式化器，用于创建按日期分类的目录
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String uploadImage(MultipartFile file) {
        log.info("[UploadImage] 开始上传图片, fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        // 步骤1 - 参数校验
        if (file == null || file.isEmpty()) {
            log.warn("[UploadImage] 文件为空");
            throw new IllegalArgumentException("图片文件不能为空");
        }

        // 步骤2 - 文件大小校验
        if (file.getSize() > maxFileSize) {
            log.warn("[UploadImage] 文件大小超限, size={}, maxSize={}", file.getSize(), maxFileSize);
            throw new IllegalArgumentException("图片大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 步骤3 - 文件格式校验
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            log.warn("[UploadImage] 文件名为空");
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("[UploadImage] 文件格式不支持, extension={}", extension);
            throw new IllegalArgumentException("不支持的图片格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        try {
            // 步骤4 - 生成文件路径
            String relativePath = generateRelativePath(extension);
            Path fullPath = Paths.get(uploadPath, relativePath);

            // 步骤5 - 创建目录（如果不存在）
            File parentDir = fullPath.getParent().toFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    log.error("[UploadImage] 创建目录失败, path={}", parentDir.getAbsolutePath());
                    throw new RuntimeException("创建存储目录失败");
                }
                log.info("[UploadImage] 创建目录成功, path={}", parentDir.getAbsolutePath());
            }

            // 步骤6 - 保存文件
            file.transferTo(fullPath.toFile());
            log.info("[UploadImage] 图片上传成功, relativePath={}, fullPath={}", relativePath, fullPath.toAbsolutePath());

            // 返回相对路径，前端可以通过此路径访问图片
            return relativePath;

        } catch (IOException e) {
            log.error("[UploadImage] 保存图片失败, fileName={}", originalFilename, e);
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] getImageBytes(String imagePath) {
        log.info("[GetImageBytes] 开始读取图片, imagePath={}", imagePath);

        if (!StringUtils.hasText(imagePath)) {
            log.warn("[GetImageBytes] 图片路径为空");
            throw new IllegalArgumentException("图片路径不能为空");
        }

        try {
            // 步骤1 - 构建完整路径
            Path fullPath = Paths.get(uploadPath, imagePath);

            // 步骤2 - 安全检查：防止路径遍历攻击
            Path normalizedPath = fullPath.normalize();
            Path uploadBasePath = Paths.get(uploadPath).normalize();
            if (!normalizedPath.startsWith(uploadBasePath)) {
                log.warn("[GetImageBytes] 非法路径访问, imagePath={}", imagePath);
                throw new IllegalArgumentException("非法的图片路径");
            }

            // 步骤3 - 检查文件是否存在
            File file = normalizedPath.toFile();
            if (!file.exists() || !file.isFile()) {
                log.warn("[GetImageBytes] 图片不存在, imagePath={}", imagePath);
                throw new IllegalArgumentException("图片不存在: " + imagePath);
            }

            // 步骤4 - 读取文件字节
            byte[] bytes = Files.readAllBytes(normalizedPath);
            log.info("[GetImageBytes] 读取图片成功, imagePath={}, size={}", imagePath, bytes.length);
            return bytes;

        } catch (IOException e) {
            log.error("[GetImageBytes] 读取图片失败, imagePath={}", imagePath, e);
            throw new RuntimeException("读取图片失败: " + e.getMessage());
        }
    }

    @Override
    public String getImageContentType(String imagePath) {
        if (!StringUtils.hasText(imagePath)) {
            return "application/octet-stream";
        }

        String extension = getFileExtension(imagePath);
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名，不包含点号
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 生成相对路径
     * 格式：yyyy/MM/dd/uuid.扩展名
     * 例如：2026/01/05/550e8400e29b41d4a716446655440000.jpg
     *
     * @param extension 文件扩展名
     * @return 相对路径
     */
    private String generateRelativePath(String extension) {
        // 按日期创建目录
        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        // 生成UUID作为文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return dateDir + "/" + uuid + "." + extension;
    }
}

