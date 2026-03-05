package com.my.family.paxl.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片服务接口
 * 提供图片上传和查看功能
 *
 * @author fan
 * @date 2026/01/05
 */
public interface ImageService {

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 图片相对路径，用于存储到数据库，格式：yyyy/MM/dd/uuid.扩展名
     * @throws IllegalArgumentException 文件为空、格式不支持或大小超限时抛出
     * @throws RuntimeException 文件保存失败时抛出
     */
    String uploadImage(MultipartFile file);

    /**
     * 根据相对路径获取图片字节数组
     *
     * @param imagePath 图片相对路径
     * @return 图片字节数组
     * @throws IllegalArgumentException 路径为空或图片不存在时抛出
     */
    byte[] getImageBytes(String imagePath);

    /**
     * 根据相对路径获取图片的Content-Type
     *
     * @param imagePath 图片相对路径
     * @return Content-Type，如 image/jpeg
     */
    String getImageContentType(String imagePath);
}

