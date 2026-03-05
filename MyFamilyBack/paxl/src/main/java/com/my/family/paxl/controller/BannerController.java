package com.my.family.paxl.controller;

import com.my.family.paxl.domain.entity.BannerDO;
import com.my.family.paxl.domain.entity.BannerCommentDO;
import com.my.family.paxl.service.BannerCommentService;
import com.my.family.paxl.service.BannerService;
import com.my.family.paxl.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 轮播图片控制器
 * 提供轮播图片上传、删除、查询以及图片查看接口
 *
 * @author fan
 * @date 2026/01/05
 */
@RestController
@RequestMapping("/api/banner")
@Slf4j
public class BannerController {

    private final BannerService bannerService;
    private final ImageService imageService;
    private final BannerCommentService bannerCommentService;

    public BannerController(BannerService bannerService, ImageService imageService, BannerCommentService bannerCommentService) {
        this.bannerService = bannerService;
        this.imageService = imageService;
        this.bannerCommentService = bannerCommentService;
    }

    /**
     * 上传图片文件
     * 用户选择图片后，先调用此接口上传图片，获取图片存储路径
     *
     * @param file 图片文件
     * @return 图片相对路径，用于后续保存轮播图时使用
     */
    @PostMapping("/image/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("[UploadImage] 文件为空");
                return ResponseEntity.badRequest().body("图片文件不能为空");
            }

            String imagePath = imageService.uploadImage(file);
            log.info("[UploadImage] 图片上传成功, imagePath={}", imagePath);
            return ResponseEntity.ok(imagePath);

        } catch (IllegalArgumentException e) {
            log.warn("[UploadImage] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[UploadImage] 上传图片异常", e);
            return ResponseEntity.internalServerError().body("上传图片失败: " + e.getMessage());
        }
    }

    /**
     * 保存轮播图片
     * 用户上传图片后，调用此接口保存图片信息到数据库
     *
     * @param bannerDO 轮播图片数据对象，必须包含imagePath
     * @return 操作结果
     */
    @PostMapping("/save")
    public ResponseEntity<String> saveBanner(@RequestBody BannerDO bannerDO) {
        try {
            if (bannerDO == null) {
                return ResponseEntity.badRequest().body("轮播图片信息不能为空");
            }
            if (!StringUtils.hasText(bannerDO.getImagePath())) {
                return ResponseEntity.badRequest().body("图片路径不能为空");
            }

            BannerDO savedBanner = bannerService.saveBanner(bannerDO);
            log.info("[SaveBanner] 保存轮播图片成功, bannerId={}", savedBanner.getBannerId());
            return ResponseEntity.ok("保存轮播图片成功");

        } catch (IllegalArgumentException e) {
            log.warn("[SaveBanner] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[SaveBanner] 保存轮播图片异常", e);
            return ResponseEntity.internalServerError().body("保存轮播图片失败: " + e.getMessage());
        }
    }

    /**
     * 删除轮播图片
     *
     * @param bannerId 轮播图ID，业务编号
     * @return 操作结果
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteBanner(@RequestParam("bannerId") Long bannerId) {
        try {
            if (bannerId == null || bannerId <= 0) {
                return ResponseEntity.badRequest().body("轮播图ID不合法");
            }

            bannerService.deleteBanner(bannerId);
            log.info("[DeleteBanner] 删除轮播图片成功, bannerId={}", bannerId);
            return ResponseEntity.ok("删除轮播图片成功");

        } catch (IllegalArgumentException e) {
            log.warn("[DeleteBanner] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[DeleteBanner] 删除轮播图片异常", e);
            return ResponseEntity.internalServerError().body("删除轮播图片失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有启用的轮播图片
     * 返回图片元素信息，不包含图片内容，防止一次调用返回多张图片导致报文过大
     *
     * @return 轮播图片列表，按位置排序
     */
    @GetMapping("/list")
    public ResponseEntity<List<BannerDO>> listBanners() {
        try {
            List<BannerDO> banners = bannerService.listEnabledBanners();
            log.info("[ListBanners] 查询轮播图片列表成功, count={}", banners.size());
            return ResponseEntity.ok(banners);

        } catch (Exception e) {
            log.error("[ListBanners] 查询轮播图片列表异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查看轮播图片
     * 根据图片相对路径返回图片流，支持懒加载
     * 前端在轮播图列表中，先获取轮播图信息（包含图片路径），然后逐个调用此接口加载图片
     *
     * @param imagePath 图片相对路径，从轮播图信息中的imagePath字段获取
     * @return 图片字节流
     */
    @GetMapping("/image/view")
    public ResponseEntity<byte[]> viewImage(@RequestParam("path") String imagePath) {
        try {
            if (!StringUtils.hasText(imagePath)) {
                log.warn("[ViewImage] 图片路径为空");
                return ResponseEntity.badRequest().build();
            }

            // 步骤1 - 读取图片字节
            byte[] imageBytes = imageService.getImageBytes(imagePath);

            // 步骤2 - 获取Content-Type
            String contentType = imageService.getImageContentType(imagePath);

            // 步骤3 - 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            // 设置缓存头，提高性能
            headers.setCacheControl("public, max-age=3600");
            headers.setContentLength(imageBytes.length);

            log.info("[ViewImage] 返回图片成功, imagePath={}, size={}", imagePath, imageBytes.length);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("[ViewImage] 参数校验失败, imagePath={}, msg={}", imagePath, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[ViewImage] 查看图片异常, imagePath={}", imagePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 保存评论
     *
     * @param commentDO 评论数据对象，必须包含bannerId和content
     * @return 操作结果
     */
    @PostMapping("/comment/save")
    public ResponseEntity<String> saveComment(@RequestBody BannerCommentDO commentDO) {
        try {
            if (commentDO == null) {
                return ResponseEntity.badRequest().body("评论信息不能为空");
            }
            if (commentDO.getBannerId() == null || commentDO.getBannerId() <= 0) {
                return ResponseEntity.badRequest().body("轮播图ID不能为空");
            }
            if (!StringUtils.hasText(commentDO.getContent())) {
                return ResponseEntity.badRequest().body("评论内容不能为空");
            }

            BannerCommentDO savedComment = bannerCommentService.saveComment(commentDO);
            log.info("[SaveComment] 保存评论成功, commentId={}", savedComment.getCommentId());
            return ResponseEntity.ok("保存评论成功");

        } catch (IllegalArgumentException e) {
            log.warn("[SaveComment] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[SaveComment] 保存评论异常", e);
            return ResponseEntity.internalServerError().body("保存评论失败: " + e.getMessage());
        }
    }

    /**
     * 查询评论列表
     *
     * @param bannerId 轮播图ID
     * @return 评论列表
     */
    @GetMapping("/comment/list")
    public ResponseEntity<List<BannerCommentDO>> listComments(@RequestParam("bannerId") Long bannerId) {
        try {
            if (bannerId == null || bannerId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            List<BannerCommentDO> comments = bannerCommentService.listCommentsByBannerId(bannerId);
            log.info("[ListComments] 查询评论列表成功, bannerId={}, count={}", bannerId, comments.size());
            return ResponseEntity.ok(comments);

        } catch (IllegalArgumentException e) {
            log.warn("[ListComments] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[ListComments] 查询评论列表异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

