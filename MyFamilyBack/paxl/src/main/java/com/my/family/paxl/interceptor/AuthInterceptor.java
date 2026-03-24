package com.my.family.paxl.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.entity.UserDO;
import com.my.family.paxl.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权拦截器
 * 从微信云托管注入的 X-WX-OPENID Header 中读取用户 openid，加载用户上下文
 * 白名单路径（如 /api/auth/login）无需鉴权，直接放行
 *
 * @author ai
 * @date 2026/03/13
 */
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 微信云托管注入的用户 openid Header 名称
     */
    private static final String HEADER_WX_OPENID = "X-WX-OPENID";

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String openid = request.getHeader(HEADER_WX_OPENID);

        if (!StringUtils.hasText(openid)) {
            log.warn("[AuthInterceptor] X-WX-OPENID 为空，拒绝请求, uri={}", request.getRequestURI());
            writeUnauthorized(response, "AUTH_FAILED", "身份验证失败，请重新登录");
            return false;
        }

        UserDO user = userService.getUserByOpenid(openid);

        if (user == null) {
            log.warn("[AuthInterceptor] 用户不存在, openid={}, uri={}", maskOpenid(openid), request.getRequestURI());
            writeUnauthorized(response, "USER_NOT_FOUND", "用户不存在，请先登录");
            return false;
        }

        if (UserDO.STATUS_DISABLED == user.getStatus()) {
            log.warn("[AuthInterceptor] 用户已被禁用, userId={}, uri={}", user.getId(), request.getRequestURI());
            writeForbidden(response, "USER_DISABLED", "账号已被禁用，请重新授权登录");
            return false;
        }

        UserContext.setCurrentUser(user);
        log.info("[AuthInterceptor] 鉴权通过, userId={}, uri={}", user.getId(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 写入 401 未授权响应
     *
     * @param response HTTP 响应
     * @param code     业务错误码
     * @param message  错误描述
     */
    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), code, message);
    }

    /**
     * 写入 403 禁止访问响应
     *
     * @param response HTTP 响应
     * @param code     业务错误码
     * @param message  错误描述
     */
    private void writeForbidden(HttpServletResponse response, String code, String message) throws IOException {
        writeErrorResponse(response, HttpStatus.FORBIDDEN.value(), code, message);
    }

    /**
     * 写入 JSON 格式的错误响应
     *
     * @param response   HTTP 响应
     * @param httpStatus HTTP 状态码
     * @param code       业务错误码
     * @param message    错误描述
     */
    private void writeErrorResponse(HttpServletResponse response, int httpStatus, String code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(body));
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
