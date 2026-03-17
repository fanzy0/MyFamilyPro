package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.UserDO;
import com.my.family.paxl.domain.vo.FamilyBriefVO;
import com.my.family.paxl.domain.vo.LoginResponseVO;
import com.my.family.paxl.domain.vo.UpdateProfileRequest;
import com.my.family.paxl.domain.vo.UserInfoVO;
import com.my.family.paxl.mapper.UserMapper;
import com.my.family.paxl.service.FamilyService;
import com.my.family.paxl.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现
 *
 * @author ai
 * @date 2026/03/13
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final FamilyService familyService;

    public UserServiceImpl(UserMapper userMapper, FamilyService familyService) {
        this.userMapper = userMapper;
        this.familyService = familyService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO login(String openid) {
        log.info("[Login] 开始处理登录, openid={}", maskOpenid(openid));

        UserDO user = userMapper.selectByOpenid(openid);
        LocalDateTime now = LocalDateTime.now();

        if (user == null) {
            log.info("[Login] 新用户首次登录，创建用户记录, openid={}", maskOpenid(openid));
            user = new UserDO();
            user.setOpenid(openid);
            user.setStatus(UserDO.STATUS_NORMAL);
            user.setLastLoginTime(now);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            userMapper.insertUser(user);
            log.info("[Login] 新用户创建成功, userId={}", user.getId());
        } else {
            log.info("[Login] 已有用户登录，更新登录时间, userId={}", user.getId());
            UserDO updateParam = new UserDO();
            updateParam.setId(user.getId());
            updateParam.setLastLoginTime(now);
            updateParam.setUpdateTime(now);
            userMapper.updateLastLoginTime(updateParam);
            user.setLastLoginTime(now);
        }

        LoginResponseVO response = new LoginResponseVO();
        response.setUser(convertToUserInfoVO(user));
        response.setFamilyList(queryFamilyList(user.getId()));

        log.info("[Login] 登录处理完成, userId={}", user.getId());
        return response;
    }

    @Override
    public UserInfoVO getUserById(Long userId) {
        log.info("[GetUserById] 查询用户, userId={}", userId);
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("[GetUserById] 用户不存在, userId={}", userId);
            return null;
        }
        return convertToUserInfoVO(user);
    }

    @Override
    public UserDO getUserByOpenid(String openid) {
        log.info("[GetUserByOpenid] 查询用户, openid={}", maskOpenid(openid));
        return userMapper.selectByOpenid(openid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("[UpdateProfile] 开始更新用户资料, userId={}", userId);

        if (!StringUtils.hasText(request.getNickname()) && !StringUtils.hasText(request.getAvatarUrl())) {
            log.warn("[UpdateProfile] 昵称和头像均为空，无需更新, userId={}", userId);
            return getUserById(userId);
        }

        UserDO updateParam = new UserDO();
        updateParam.setId(userId);
        updateParam.setNickname(request.getNickname());
        updateParam.setAvatarUrl(request.getAvatarUrl());
        updateParam.setUpdateTime(LocalDateTime.now());

        userMapper.updateProfile(updateParam);
        log.info("[UpdateProfile] 用户资料更新成功, userId={}", userId);

        return getUserById(userId);
    }

    /**
     * 查询用户已加入的家庭列表
     *
     * @param userId 用户主键 ID
     * @return 家庭简要信息列表
     */
    private List<FamilyBriefVO> queryFamilyList(Long userId) {
        return familyService.listMyFamilies(userId);
    }

    /**
     * 将用户 DO 转换为用户信息 VO
     *
     * @param userDO 用户数据对象
     * @return 用户信息视图对象
     */
    private UserInfoVO convertToUserInfoVO(UserDO userDO) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(userDO.getId());
        vo.setNickname(userDO.getNickname());
        vo.setAvatarUrl(userDO.getAvatarUrl());
        vo.setStatus(userDO.getStatus());
        return vo;
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
