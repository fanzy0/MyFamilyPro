package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 * 所有 SQL 均在 UserMapper.xml 中手动编写
 *
 * @author ai
 * @date 2026/03/13
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据 openid 查询用户
     *
     * @param openid 微信 openid
     * @return 用户对象，不存在返回 null
     */
    UserDO selectByOpenid(@Param("openid") String openid);

    /**
     * 根据主键 ID 查询用户
     *
     * @param id 用户主键 ID
     * @return 用户对象，不存在返回 null
     */
    UserDO selectById(@Param("id") Long id);

    /**
     * 新增用户
     *
     * @param userDO 用户对象（openid、status、createTime、updateTime 必填）
     * @return 受影响行数
     */
    int insertUser(UserDO userDO);

    /**
     * 更新用户最近登录时间
     *
     * @param userDO 包含 id 和 lastLoginTime 的用户对象
     * @return 受影响行数
     */
    int updateLastLoginTime(UserDO userDO);

    /**
     * 更新用户基础资料（昵称、头像）
     *
     * @param userDO 包含 id、nickname、avatarUrl 的用户对象，仅更新非空字段
     * @return 受影响行数
     */
    int updateProfile(UserDO userDO);
}
