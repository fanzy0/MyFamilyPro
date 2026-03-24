package com.my.family.paxl.controller;

import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.entity.UserDO;
import com.my.family.paxl.domain.vo.UpdateProfileRequest;
import com.my.family.paxl.domain.vo.UserInfoVO;
import com.my.family.paxl.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 所有接口需要鉴权（由 AuthInterceptor 统一处理），当前用户从 UserContext 获取
 *
 * @author ai
 * @date 2026/03/13
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户信息
     * 供小程序启动时自动校验用户是否存在，并获取最新用户信息和家庭列表
     *
     * @return 当前用户信息 VO
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoVO> me() {
        try {
            UserDO currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                log.warn("[Me] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            UserInfoVO userInfo = userService.getUserById(currentUser.getId());
            if (userInfo == null) {
                log.warn("[Me] 用户不存在, userId={}", currentUser.getId());
                return ResponseEntity.status(401).build();
            }

            log.info("[Me] 获取用户信息成功, userId={}", currentUser.getId());
            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("[Me] 获取用户信息异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 注销当前用户账号
     * 将用户状态更新为禁用（status=1），注销后鉴权拦截器将拒绝该账号的所有请求
     * 前端注销成功后应清除本地缓存并跳转至登录页
     * 注意：前端 cloudRequest.js 仅识别 200 为成功，此处返回 200 而非 204
     *
     * @return 200 OK
     */
    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivate() {
        try {
            UserDO currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                log.warn("[Deactivate] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            userService.deactivateAccount(currentUser.getId());
            log.info("[Deactivate] 账号注销成功, userId={}", currentUser.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("[Deactivate] 注销账号异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新当前用户基础资料
     * 用户通过 type="nickname" 输入框或 open-type="chooseAvatar" 按钮设置昵称/头像后调用
     * nickname 和 avatarUrl 均为可选，仅更新非空字段
     *
     * @param request 更新请求，包含 nickname 和 avatarUrl
     * @return 更新后的用户信息 VO
     */
    @PostMapping("/updateProfile")
    public ResponseEntity<UserInfoVO> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().build();
            }

            if (!StringUtils.hasText(request.getNickname()) && !StringUtils.hasText(request.getAvatarUrl())) {
                log.warn("[UpdateProfile] 昵称和头像均为空，参数不合法");
                return ResponseEntity.badRequest().build();
            }

            UserDO currentUser = UserContext.getCurrentUser();
            if (currentUser == null) {
                log.warn("[UpdateProfile] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            UserInfoVO updatedUser = userService.updateProfile(currentUser.getId(), request);
            log.info("[UpdateProfile] 更新用户资料成功, userId={}", currentUser.getId());
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("[UpdateProfile] 更新用户资料异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
