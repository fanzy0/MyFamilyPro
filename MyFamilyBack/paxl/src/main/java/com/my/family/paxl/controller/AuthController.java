package com.my.family.paxl.controller;

import com.my.family.paxl.domain.vo.LoginResponseVO;
import com.my.family.paxl.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 鉴权控制器
 * 提供登录接口，openid 由微信云托管平台自动注入到 X-WX-OPENID Header，前端无需传参
 *
 * @author ai
 * @date 2026/03/13
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    /**
     * 微信云托管注入的用户 openid Header 名称
     */
    private static final String HEADER_WX_OPENID = "X-WX-OPENID";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * 无需请求体参数，openid 由微信云托管平台注入到 Header X-WX-OPENID
     * 根据 openid 查询或创建用户，返回用户基础信息和已加入的家庭列表
     *
     * @param request HTTP 请求，用于读取 X-WX-OPENID Header
     * @return 登录响应，包含用户信息和 familyList
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseVO> login(HttpServletRequest request) {
        String openid = request.getHeader(HEADER_WX_OPENID);

        try {
            if (!StringUtils.hasText(openid)) {
                log.warn("[Login] X-WX-OPENID 为空，登录失败");
                return ResponseEntity.status(401).build();
            }

            LoginResponseVO response = userService.login(openid);
            log.info("[Login] 登录成功, userId={}", response.getUser().getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Login] 登录异常, openid={}", maskOpenid(openid), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * openid 脱敏，用于日志输出
     *
     * @param openid 原始 openid
     * @return 脱敏后的 openid
     */
    private String maskOpenid(String openid) {
        if (openid == null || openid.length() <= 6) {
            return "***";
        }
        return openid.substring(0, 6) + "***";
    }
}
