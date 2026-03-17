package com.my.family.paxl.service;

import com.my.family.paxl.domain.entity.UserDO;
import com.my.family.paxl.domain.vo.LoginResponseVO;
import com.my.family.paxl.domain.vo.UpdateProfileRequest;
import com.my.family.paxl.domain.vo.UserInfoVO;

/**
 * 用户服务接口
 *
 * @author ai
 * @date 2026/03/13
 */
public interface UserService {

    /**
     * 用户登录：根据 openid 查询或创建用户，并返回用户信息与家庭列表
     * openid 由微信云托管平台注入到请求 Header（X-WX-OPENID），后端直接信任
     *
     * @param openid 微信云托管注入的用户 openid
     * @return 登录响应，包含用户信息和 familyList
     */
    LoginResponseVO login(String openid);

    /**
     * 根据主键 ID 查询用户信息（用于鉴权拦截器与 /api/user/me 接口）
     *
     * @param userId 用户主键 ID
     * @return 用户信息 VO，用户不存在返回 null
     */
    UserInfoVO getUserById(Long userId);

    /**
     * 根据 openid 查询用户数据对象（用于鉴权拦截器）
     *
     * @param openid 微信 openid
     * @return 用户 DO，不存在返回 null
     */
    UserDO getUserByOpenid(String openid);

    /**
     * 更新用户基础资料（昵称、头像），仅更新非空字段
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求，包含 nickname 和 avatarUrl（均为可选）
     * @return 更新后的用户信息 VO
     */
    UserInfoVO updateProfile(Long userId, UpdateProfileRequest request);
}
