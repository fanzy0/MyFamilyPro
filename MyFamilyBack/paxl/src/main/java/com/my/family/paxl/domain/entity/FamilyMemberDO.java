package com.my.family.paxl.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭成员关系数据对象，对应数据库表：mf_family_member
 * 承载成员与家庭的关系，以及加入申请状态
 *
 * @author ai
 * @date 2026/03/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色：户主
     */
    public static final String ROLE_OWNER = "OWNER";

    /**
     * 角色：普通成员
     */
    public static final String ROLE_MEMBER = "MEMBER";

    /**
     * 加入状态：待审批
     */
    public static final String STATUS_PENDING = "PENDING";

    /**
     * 加入状态：已通过
     */
    public static final String STATUS_APPROVED = "APPROVED";

    /**
     * 加入状态：已拒绝
     */
    public static final String STATUS_REJECTED = "REJECTED";

    /**
     * 加入状态：已退出
     */
    public static final String STATUS_QUIT = "QUIT";

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 家庭ID，对应 mf_family.id
     */
    private Long familyId;

    /**
     * 用户ID，对应 mf_user.id
     */
    private Long userId;

    /**
     * 角色：OWNER-户主，MEMBER-成员
     */
    private String role;

    /**
     * 加入状态：PENDING/APPROVED/REJECTED/QUIT
     */
    private String joinStatus;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 加入时间（审批通过时间）
     */
    private LocalDateTime joinTime;

    /**
     * 创建时间（申请时间）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}

