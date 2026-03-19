package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.ImageFileDO;
import com.my.family.paxl.mapper.ImageFileMapper;
import com.my.family.paxl.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 图片服务实现类
 * 负责图片的上传、存储和读取（存储于数据库）
 *
 * @author fan
 * @date 2026/01/05
 */
@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Resource
    private ImageFileMapper imageFileMapper;

    /**
     * 允许的图片格式
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    /**
     * 最大文件大小，单位：字节，默认5MB
     */
    @Value("${paxl.image.max-size:5242880}")
    private long maxFileSize;

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
            // 步骤4 - 组装入库对象
            ImageFileDO imageFileDO = new ImageFileDO();
            imageFileDO.setFileName(originalFilename);
            imageFileDO.setContentType(guessContentType(file.getContentType(), extension));
            imageFileDO.setFileSize(file.getSize());
            imageFileDO.setFileData(file.getBytes());
            imageFileDO.setDeleted(0);
            LocalDateTime now = LocalDateTime.now();
            imageFileDO.setCreateTime(now);
            imageFileDO.setUpdateTime(now);

            // 步骤5 - 写入数据库
            int result = imageFileMapper.insert(imageFileDO);
            if (result <= 0 || imageFileDO.getId() == null) {
                log.error("[UploadImage] 图片入库失败, fileName={}", originalFilename);
                throw new IllegalStateException("保存图片失败");
            }

            String imageId = String.valueOf(imageFileDO.getId());
            log.info("[UploadImage] 图片入库成功, imageId={}, fileName={}, size={}", imageId, originalFilename, file.getSize());
            return imageId;

        } catch (IOException e) {
            log.error("[UploadImage] 读取上传文件字节失败, fileName={}", originalFilename, e);
            throw new IllegalStateException("读取图片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getImageBytes(String imagePath) {
        log.info("[GetImageBytes] 开始读取图片, imagePath={}", imagePath);

        if (!StringUtils.hasText(imagePath)) {
            log.warn("[GetImageBytes] 图片路径为空");
            throw new IllegalArgumentException("图片路径不能为空");
        }

        Long imageId = parseImageId(imagePath);
        ImageFileDO data = imageFileMapper.selectDataById(imageId);
        if (data == null || data.getFileData() == null) {
            log.warn("[GetImageBytes] 图片不存在, imageId={}", imageId);
            throw new IllegalArgumentException("图片不存在: " + imagePath);
        }

        log.info("[GetImageBytes] 读取图片成功, imageId={}, size={}", imageId, data.getFileData().length);
        return data.getFileData();
    }

    @Override
    public String getImageContentType(String imagePath) {
        if (!StringUtils.hasText(imagePath)) {
            return DEFAULT_CONTENT_TYPE;
        }

        Long imageId = parseImageId(imagePath);
        ImageFileDO meta = imageFileMapper.selectMetaById(imageId);
        if (meta == null || !StringUtils.hasText(meta.getContentType())) {
            return DEFAULT_CONTENT_TYPE;
        }
        return meta.getContentType();
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

    private Long parseImageId(String imagePath) {
        String idStr = imagePath.trim();
        try {
            Long id = Long.valueOf(idStr);
            if (id <= 0) {
                throw new NumberFormatException("id<=0");
            }
            return id;
        } catch (NumberFormatException e) {
            log.warn("[ParseImageId] 非法图片标识, imagePath={}", imagePath);
            throw new IllegalArgumentException("非法的图片标识: " + imagePath);
        }
    }

    private String guessContentType(String multipartContentType, String extension) {
        if (StringUtils.hasText(multipartContentType)) {
            return multipartContentType;
        }

        if (!StringUtils.hasText(extension)) {
            return DEFAULT_CONTENT_TYPE;
        }

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
                return DEFAULT_CONTENT_TYPE;
        }
    }
}

