# 家庭重要事项 — 后端设计方案

> 本文档对应开发计划「五、家庭重要事项基础功能」，字段设计同步兼容「六、提醒配置」，供后续两个阶段共同参考。

---

## 一、设计原则

| 原则 | 说明 |
|------|------|
| 响应格式 | `ResponseEntity<T>`，沿用现有风格，无统一 Result 包装 |
| 鉴权 | `AuthInterceptor` + `UserContext`（`X-WX-OPENID` Header） |
| SQL 风格 | 手写 XML Mapper，继承 `BaseMapper<T>` |
| 表名前缀 | `mf_`（与 `mf_user`、`mf_family` 保持一致） |
| 字段映射 | 数据库下划线 → Java 驼峰，MyBatis-Plus 自动映射 |
| DO 类风格 | 显式 getter/setter（同 `ImageFileDO`），不使用 `@Data` |
| VO/Request | 使用 Lombok `@Data`，简化样板代码 |

---

## 二、数据库表设计

### 新增表：`mf_event`（家庭重要事项）

```sql
CREATE TABLE `mf_event` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT     COMMENT '主键',
  `family_id`           bigint       NOT NULL                    COMMENT '所属家庭ID（关联 mf_family.id）',
  `creator_user_id`     bigint       NOT NULL                    COMMENT '创建人用户ID（关联 mf_user.id）',
  `title`               varchar(100) NOT NULL                    COMMENT '事项名称',
  `category`            varchar(50)  NOT NULL                    COMMENT '类别：BIRTHDAY/ANNIVERSARY/HOLIDAY/DOCUMENT_EXPIRY/HEALTH/PAYMENT/OTHER',
  `date_type`           tinyint      NOT NULL DEFAULT 0          COMMENT '日期类型：0-阳历，1-农历',
  `month`               int          NOT NULL                    COMMENT '月份（1-12；date_type=0 表示阳历月；date_type=1 表示农历月）',
  `day`                 int          NOT NULL                    COMMENT '日期（1-31；date_type=0 表示阳历日；date_type=1 表示农历日）',
  `description`         varchar(500) DEFAULT NULL                COMMENT '事项描述/备注',
  `remind_enabled`      tinyint      NOT NULL DEFAULT 0          COMMENT '是否开启提醒：0-否，1-是',
  `remind_advance_days` int          NOT NULL DEFAULT 0          COMMENT '提前几天提醒（0=当天）',
  `remind_target`       varchar(10)  NOT NULL DEFAULT 'ALL'      COMMENT '提醒对象：ALL-全部家庭成员，SPECIFIC-指定某人',
  `remind_user_id`      bigint       DEFAULT NULL                COMMENT '指定提醒用户ID（remind_target=SPECIFIC 时有效）',
  `status`              tinyint      NOT NULL DEFAULT 0          COMMENT '状态：0-正常，1-禁用',
  `deleted`             tinyint      NOT NULL DEFAULT 0          COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_creator_user_id` (`creator_user_id`),
  KEY `idx_month_day` (`month`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭重要事项';
```

### 字段补充说明

| 字段 | 补充说明 |
|------|----------|
| `category` | 内置枚举：`BIRTHDAY`（生日）`ANNIVERSARY`（纪念日）`HOLIDAY`（节假日）`DOCUMENT_EXPIRY`（证件到期）`HEALTH`（健康体检）`PAYMENT`（还款缴费）`OTHER`（其他） |
| `date_type + month + day` | **用户选择“阳历/农历”后，只需要填写月、日**。提醒周期固定为“年”（每年一次），后端调度（Plan 七）根据 `date_type` + `month` + `day` 计算当年的提醒触发日期（并结合 `remind_advance_days` 做提前提醒）。 |
| `remind_*` 字段 | Plan 六提前设计，Plan 五阶段接收并持久化，调度逻辑在 Plan 七实现（其中周期固定为年，不再存储周期字段）。 |

---

## 三、新增文件清单

```
src/main/java/com/my/family/paxl/
├── domain/
│   ├── entity/
│   │   └── EventDO.java
│   └── vo/
│       ├── CreateEventRequest.java
│       ├── EventBriefVO.java
│       └── EventDetailVO.java
├── mapper/
│   └── EventMapper.java
├── service/
│   ├── EventService.java
│   └── impl/
│       └── EventServiceImpl.java
└── controller/
    └── EventController.java

src/main/resources/mapper/
└── EventMapper.xml

sql/
└── paxl_init.sql   （末尾追加 mf_event 建表语句）
```

---

## 四、EventDO.java

```java
package com.my.family.paxl.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭重要事项数据对象，对应数据库表：mf_event
 *
 * @author fan
 * @date 2026/03/17
 */
@TableName("mf_event")
public class EventDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属家庭ID */
    private Long familyId;

    /** 创建人用户ID */
    private Long creatorUserId;

    /** 事项名称 */
    private String title;

    /**
     * 事项类别
     * 内置枚举：BIRTHDAY / ANNIVERSARY / HOLIDAY / DOCUMENT_EXPIRY / HEALTH / PAYMENT / OTHER
     */
    private String category;

    /**
     * 日期类型：0-阳历，1-农历
     */
    private Integer dateType;

    /**
     * 月份（1-12）
     * dateType=0 表示阳历月；dateType=1 表示农历月
     */
    private Integer month;

    /**
     * 日期（1-31）
     * dateType=0 表示阳历日；dateType=1 表示农历日
     */
    private Integer day;

    /** 事项描述/备注 */
    private String description;

    /** 是否开启提醒：0-否，1-是 */
    private Integer remindEnabled;

    /** 提前几天提醒（0=当天提醒） */
    private Integer remindAdvanceDays;

    /**
     * 提醒对象：ALL-全部家庭成员，SPECIFIC-指定某人
     */
    private String remindTarget;

    /** 指定提醒用户ID（remindTarget=SPECIFIC 时有效） */
    private Long remindUserId;

    /** 状态：0-正常，1-禁用 */
    private Integer status;

    /** 逻辑删除：0-未删除，1-已删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ===== getter / setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public Long getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getDateType() { return dateType; }
    public void setDateType(Integer dateType) { this.dateType = dateType; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRemindEnabled() { return remindEnabled; }
    public void setRemindEnabled(Integer remindEnabled) { this.remindEnabled = remindEnabled; }

    public Integer getRemindAdvanceDays() { return remindAdvanceDays; }
    public void setRemindAdvanceDays(Integer remindAdvanceDays) { this.remindAdvanceDays = remindAdvanceDays; }

    public String getRemindTarget() { return remindTarget; }
    public void setRemindTarget(String remindTarget) { this.remindTarget = remindTarget; }

    public Long getRemindUserId() { return remindUserId; }
    public void setRemindUserId(Long remindUserId) { this.remindUserId = remindUserId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "EventDO{id=" + id + ", familyId=" + familyId + ", title='" + title + "', category='" + category
                + "', month=" + month + ", day=" + day + ", remindEnabled=" + remindEnabled + '}';
    }
}
```

---

## 五、VO / Request 类设计

### CreateEventRequest.java（创建请求体）

```java
package com.my.family.paxl.domain.vo;

import lombok.Data;
import java.time.LocalDate;

/**
 * 创建家庭重要事项请求体
 */
@Data
public class CreateEventRequest {

    /** 所属家庭ID（必填） */
    private Long familyId;

    /** 事项名称（必填） */
    private String title;

    /**
     * 类别（必填）
     * 可选值：BIRTHDAY / ANNIVERSARY / HOLIDAY / DOCUMENT_EXPIRY / HEALTH / PAYMENT / OTHER
     */
    private String category;

    /**
     * 日期类型（必填）：0=阳历，1=农历
     */
    private Integer dateType;

    /**
     * 月份（必填，1-12）
     * dateType=0 表示阳历月；dateType=1 表示农历月
     */
    private Integer month;

    /**
     * 日期（必填，1-31）
     * dateType=0 表示阳历日；dateType=1 表示农历日
     */
    private Integer day;

    /** 事项描述/备注（选填） */
    private String description;

    /** 是否开启提醒：0-否，1-是（默认 0） */
    private Integer remindEnabled;

    /** 提前几天提醒（默认 0，即当天提醒） */
    private Integer remindAdvanceDays;

    /**
     * 提醒对象（默认 ALL）：ALL=全部家庭成员，SPECIFIC=指定某人
     */
    private String remindTarget;

    /** 指定提醒用户ID（remindTarget=SPECIFIC 时必填） */
    private Long remindUserId;
}
```

### EventBriefVO.java（列表项 VO）

```java
package com.my.family.paxl.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 家庭重要事项列表项 VO（不含提醒配置详情）
 */
@Data
public class EventBriefVO {

    private Long id;

    private Long familyId;

    /** 事项名称 */
    private String title;

    /** 类别 */
    private String category;

    /** 日期类型：0-阳历，1-农历 */
    private Integer dateType;

    /** 月份（1-12） */
    private Integer month;

    /** 日期（1-31） */
    private Integer day;

    /** 是否开启提醒：0-否，1-是 */
    private Integer remindEnabled;

    /** 创建人用户ID */
    private Long creatorUserId;

    /** 创建人昵称（JOIN mf_user 获取） */
    private String creatorNickname;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 是否为当前用户创建（true=可删除） */
    private Boolean isOwner;
}
```

### EventDetailVO.java（详情 VO）

```java
package com.my.family.paxl.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 家庭重要事项详情 VO（包含全部字段，含提醒配置）
 */
@Data
public class EventDetailVO {

    private Long id;
    private Long familyId;
    private String title;
    private String category;
    private Integer dateType;
    private Integer month;
    private Integer day;
    private String description;

    // 提醒配置
    private Integer remindEnabled;
    private Integer remindAdvanceDays;
    private String remindTarget;
    private Long remindUserId;

    // 创建人信息
    private Long creatorUserId;
    private String creatorNickname;
    private String creatorAvatarUrl;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 是否为当前用户创建（true=可删除） */
    private Boolean isOwner;
}
```

---

## 六、EventMapper.java

```java
package com.my.family.paxl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.family.paxl.domain.entity.EventDO;
import com.my.family.paxl.domain.vo.EventBriefVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家庭重要事项 Mapper
 */
public interface EventMapper extends BaseMapper<EventDO> {

    /**
     * 新增事项
     */
    int insertEvent(EventDO eventDO);

    /**
     * 查询家庭事项列表（含创建人昵称），按 month/day 升序
     */
    List<EventBriefVO> selectByFamilyId(@Param("familyId") Long familyId);

    /**
     * 根据主键查询事项详情（含创建人信息）
     */
    EventDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 逻辑删除
     */
    int logicDeleteById(@Param("id") Long id);
}
```

> 注意：`selectDetailById` 返回 `EventDetailVO`，需在 XML 中 JOIN `mf_user` 补充创建人信息。

---

## 七、EventMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.my.family.paxl.mapper.EventMapper">

    <!-- ===================== ResultMap ===================== -->

    <resultMap id="EventBriefMap" type="com.my.family.paxl.domain.vo.EventBriefVO">
        <id     column="id"               property="id"/>
        <result column="family_id"        property="familyId"/>
        <result column="title"            property="title"/>
        <result column="category"         property="category"/>
        <result column="date_type"        property="dateType"/>
        <result column="month"            property="month"/>
        <result column="day"              property="day"/>
        <result column="remind_enabled"   property="remindEnabled"/>
        <result column="creator_user_id"  property="creatorUserId"/>
        <result column="creator_nickname" property="creatorNickname"/>
        <result column="create_time"      property="createTime"/>
    </resultMap>

    <resultMap id="EventDetailMap" type="com.my.family.paxl.domain.vo.EventDetailVO">
        <id     column="id"                   property="id"/>
        <result column="family_id"            property="familyId"/>
        <result column="title"                property="title"/>
        <result column="category"             property="category"/>
        <result column="date_type"            property="dateType"/>
        <result column="month"                property="month"/>
        <result column="day"                  property="day"/>
        <result column="description"          property="description"/>
        <result column="remind_enabled"       property="remindEnabled"/>
        <result column="remind_advance_days"  property="remindAdvanceDays"/>
        <result column="remind_target"        property="remindTarget"/>
        <result column="remind_user_id"       property="remindUserId"/>
        <result column="creator_user_id"      property="creatorUserId"/>
        <result column="creator_nickname"     property="creatorNickname"/>
        <result column="creator_avatar_url"   property="creatorAvatarUrl"/>
        <result column="create_time"          property="createTime"/>
        <result column="update_time"          property="updateTime"/>
    </resultMap>

    <!-- ===================== SQL 片段 ===================== -->

    <sql id="Base_Column_List">
        e.id, e.family_id, e.creator_user_id, e.title, e.category,
        e.date_type, e.month, e.day,
        e.description, e.remind_enabled,
        e.remind_advance_days, e.remind_target, e.remind_user_id,
        e.status, e.deleted, e.create_time, e.update_time
    </sql>

    <!-- ===================== 新增 ===================== -->

    <insert id="insertEvent" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO mf_event (
            family_id, creator_user_id, title, category,
            date_type, month, day, description,
            remind_enabled, remind_advance_days,
            remind_target, remind_user_id,
            status, deleted, create_time, update_time
        ) VALUES (
            #{familyId}, #{creatorUserId}, #{title}, #{category},
            #{dateType}, #{month}, #{day}, #{description},
            #{remindEnabled}, #{remindAdvanceDays},
            #{remindTarget}, #{remindUserId},
            0, 0, NOW(), NOW()
        )
    </insert>

    <!-- ===================== 查询列表 ===================== -->

    <select id="selectByFamilyId" resultMap="EventBriefMap">
        SELECT
            e.id, e.family_id, e.creator_user_id, e.title, e.category,
            e.date_type, e.month, e.day,
            e.remind_enabled, e.create_time,
            u.nickname AS creator_nickname
        FROM mf_event e
        LEFT JOIN mf_user u ON u.id = e.creator_user_id
        WHERE e.family_id = #{familyId}
          AND e.deleted = 0
          AND e.status  = 0
        ORDER BY e.month ASC, e.day ASC
    </select>

    <!-- ===================== 查询详情 ===================== -->

    <select id="selectDetailById" resultMap="EventDetailMap">
        SELECT
            e.id, e.family_id, e.creator_user_id, e.title, e.category,
            e.date_type, e.month, e.day,
            e.description, e.remind_enabled,
            e.remind_advance_days, e.remind_target, e.remind_user_id,
            e.create_time, e.update_time,
            u.nickname   AS creator_nickname,
            u.avatar_url AS creator_avatar_url
        FROM mf_event e
        LEFT JOIN mf_user u ON u.id = e.creator_user_id
        WHERE e.id = #{id}
          AND e.deleted = 0
    </select>

    <!-- ===================== 逻辑删除 ===================== -->

    <update id="logicDeleteById">
        UPDATE mf_event
        SET deleted     = 1,
            update_time = NOW()
        WHERE id = #{id}
    </update>

</mapper>
```

---

## 八、EventService.java（接口）

```java
package com.my.family.paxl.service;

import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;

import java.util.List;

/**
 * 家庭重要事项 Service
 */
public interface EventService {

    /**
     * 创建重要事项
     * 校验：当前用户必须是该家庭 join_status=APPROVED 的成员
     */
    EventDetailVO createEvent(Long currentUserId, CreateEventRequest request);

    /**
     * 查询家庭事项列表
     * 校验：当前用户必须是该家庭 APPROVED 成员
     */
    List<EventBriefVO> listEvents(Long currentUserId, Long familyId);

    /**
     * 查询事项详情
     * 校验：当前用户必须是事项所属家庭的 APPROVED 成员
     */
    EventDetailVO getEventDetail(Long currentUserId, Long eventId);

    /**
     * 删除事项（仅创建人可操作）
     * 校验：creator_user_id == currentUserId，否则抛 403
     */
    void deleteEvent(Long currentUserId, Long eventId);
}
```

---

## 九、EventServiceImpl.java（实现）

```java
package com.my.family.paxl.service.impl;

import com.my.family.paxl.domain.entity.EventDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;
import com.my.family.paxl.mapper.EventMapper;
import com.my.family.paxl.mapper.FamilyMemberMapper;
import com.my.family.paxl.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 家庭重要事项 Service 实现
 */
@Slf4j
@Service
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final FamilyMemberMapper familyMemberMapper;

    public EventServiceImpl(EventMapper eventMapper, FamilyMemberMapper familyMemberMapper) {
        this.eventMapper = eventMapper;
        this.familyMemberMapper = familyMemberMapper;
    }

    @Override
    public EventDetailVO createEvent(Long currentUserId, CreateEventRequest req) {
        // 1. 校验：当前用户是该家庭的正式成员
        checkApprovedMember(currentUserId, req.getFamilyId());

        // 2. 构建 DO
        EventDO event = new EventDO();
        event.setFamilyId(req.getFamilyId());
        event.setCreatorUserId(currentUserId);
        event.setTitle(req.getTitle());
        event.setCategory(req.getCategory());
        event.setDateType(req.getDateType() != null ? req.getDateType() : 0);
        event.setEventDate(req.getEventDate());
        event.setLunarMonth(req.getLunarMonth());
        event.setLunarDay(req.getLunarDay());
        event.setDescription(req.getDescription());
        event.setRemindEnabled(req.getRemindEnabled() != null ? req.getRemindEnabled() : 0);
        event.setRemindCycle(req.getRemindCycle());
        event.setRemindAdvanceDays(req.getRemindAdvanceDays() != null ? req.getRemindAdvanceDays() : 0);
        event.setRemindTimes(req.getRemindTimes() != null ? req.getRemindTimes() : 1);
        event.setRemindTarget(req.getRemindTarget() != null ? req.getRemindTarget() : "ALL");
        event.setRemindUserId(req.getRemindUserId());
        event.setReviewStatus("APPROVED"); // 预留审核，当前直接通过

        // 3. 持久化
        eventMapper.insertEvent(event);
        log.info("[Event] 创建成功: id={}, userId={}, familyId={}", event.getId(), currentUserId, req.getFamilyId());

        // 4. 返回详情
        EventDetailVO detail = eventMapper.selectDetailById(event.getId());
        if (detail != null) {
            detail.setIsOwner(true);
        }
        return detail;
    }

    @Override
    public List<EventBriefVO> listEvents(Long currentUserId, Long familyId) {
        // 校验成员身份
        checkApprovedMember(currentUserId, familyId);

        List<EventBriefVO> list = eventMapper.selectByFamilyId(familyId);
        // 标记是否为当前用户创建
        list.forEach(v -> v.setIsOwner(currentUserId.equals(v.getCreatorUserId())));
        return list;
    }

    @Override
    public EventDetailVO getEventDetail(Long currentUserId, Long eventId) {
        EventDetailVO detail = eventMapper.selectDetailById(eventId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "事项不存在");
        }
        // 校验成员身份
        checkApprovedMember(currentUserId, detail.getFamilyId());
        detail.setIsOwner(currentUserId.equals(detail.getCreatorUserId()));
        return detail;
    }

    @Override
    public void deleteEvent(Long currentUserId, Long eventId) {
        EventDetailVO detail = eventMapper.selectDetailById(eventId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "事项不存在");
        }
        // 仅创建人可删除
        if (!currentUserId.equals(detail.getCreatorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有创建人才能删除该事项");
        }
        eventMapper.logicDeleteById(eventId);
        log.info("[Event] 删除成功: id={}, userId={}", eventId, currentUserId);
    }

    // ===== 私有辅助 =====

    private void checkApprovedMember(Long userId, Long familyId) {
        FamilyMemberDO member = familyMemberMapper.selectByFamilyIdAndUserId(familyId, userId);
        if (member == null || !"APPROVED".equals(member.getJoinStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您不是该家庭的成员");
        }
    }
}
```

---

## 十、EventController.java

```java
package com.my.family.paxl.controller;

import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;
import com.my.family.paxl.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭重要事项控制器
 * 所有接口需鉴权（AuthInterceptor 自动拦截 /api/** ）
 *
 * @author fan
 * @date 2026/03/17
 */
@RestController
@RequestMapping("/api/event")
@Slf4j
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 创建重要事项
     * POST /api/event/create
     */
    @PostMapping("/create")
    public ResponseEntity<EventDetailVO> createEvent(@RequestBody CreateEventRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        EventDetailVO result = eventService.createEvent(currentUserId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 查询家庭事项列表
     * GET /api/event/list?familyId=xxx
     */
    @GetMapping("/list")
    public ResponseEntity<List<EventBriefVO>> listEvents(@RequestParam Long familyId) {
        Long currentUserId = UserContext.getCurrentUserId();
        List<EventBriefVO> list = eventService.listEvents(currentUserId, familyId);
        return ResponseEntity.ok(list);
    }

    /**
     * 查询事项详情
     * GET /api/event/detail?eventId=xxx
     */
    @GetMapping("/detail")
    public ResponseEntity<EventDetailVO> getEventDetail(@RequestParam Long eventId) {
        Long currentUserId = UserContext.getCurrentUserId();
        EventDetailVO detail = eventService.getEventDetail(currentUserId, eventId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 删除事项（仅创建人可操作）
     * DELETE /api/event/delete?eventId=xxx
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteEvent(@RequestParam Long eventId) {
        Long currentUserId = UserContext.getCurrentUserId();
        eventService.deleteEvent(currentUserId, eventId);
        return ResponseEntity.ok("删除成功");
    }
}
```

---

## 十一、接口汇总

| 方法 | 路径 | 请求参数 | 响应体 | 说明 |
|------|------|----------|--------|------|
| `POST` | `/api/event/create` | Body: `CreateEventRequest` | `EventDetailVO` | 创建事项 |
| `GET` | `/api/event/list` | Query: `familyId` | `List<EventBriefVO>` | 家庭事项列表 |
| `GET` | `/api/event/detail` | Query: `eventId` | `EventDetailVO` | 事项详情 |
| `DELETE` | `/api/event/delete` | Query: `eventId` | `String` | 删除事项（创建人） |

### 错误响应约定

| HTTP 状态码 | 场景 |
|-------------|------|
| `200 OK` | 操作成功 |
| `400 Bad Request` | 请求参数缺失或非法 |
| `403 Forbidden` | 非家庭成员 / 非创建人尝试删除 |
| `404 Not Found` | 事项不存在 |

---

## 十二、权限控制流程图

```
请求进入 AuthInterceptor
       │
       ▼
  读取 X-WX-OPENID → 查 mf_user → 写入 UserContext
       │
       ▼
  EventController → EventService
       │
       ├─ createEvent
       │       └─ 查 mf_family_member（familyId + userId）→ join_status=APPROVED? ✓
       │
       ├─ listEvents / getEventDetail
       │       └─ 同上
       │
       └─ deleteEvent
               ├─ 查 mf_family_member ✓
               └─ creator_user_id == currentUserId? 否 → 403
```

---

## 十三、与现有代码集成点

| 集成点 | 说明 |
|--------|------|
| `AuthInterceptor` | `/api/event/**` 自动受鉴权保护，无需改白名单 |
| `UserContext` | `UserContext.getCurrentUserId()` 获取当前用户 |
| `FamilyMemberMapper.selectByFamilyIdAndUserId` | 复用已有方法校验成员身份 |
| `mf_user.nickname` / `mf_user.avatar_url` | Mapper XML 中 JOIN 获取创建人信息 |

---

## 十四、SQL 追加（paxl_init.sql）

在 `paxl/sql/paxl_init.sql` 文件末尾追加以下内容：

```sql
-- =============================================
-- 五、家庭重要事项表
-- =============================================
CREATE TABLE IF NOT EXISTS `mf_event` (
  `id`                  bigint       NOT NULL AUTO_INCREMENT     COMMENT '主键',
  `family_id`           bigint       NOT NULL                    COMMENT '所属家庭ID',
  `creator_user_id`     bigint       NOT NULL                    COMMENT '创建人用户ID',
  `title`               varchar(100) NOT NULL                    COMMENT '事项名称',
  `category`            varchar(50)  NOT NULL                    COMMENT '类别',
  `date_type`           tinyint      NOT NULL DEFAULT 0          COMMENT '日期类型：0-阳历，1-农历',
  `month`               int          NOT NULL                    COMMENT '月份（1-12；date_type=0 表示阳历月；date_type=1 表示农历月）',
  `day`                 int          NOT NULL                    COMMENT '日期（1-31；date_type=0 表示阳历日；date_type=1 表示农历日）',
  `description`         varchar(500) DEFAULT NULL                COMMENT '描述',
  `remind_enabled`      tinyint      NOT NULL DEFAULT 0          COMMENT '是否开启提醒',
  `remind_advance_days` int          NOT NULL DEFAULT 0          COMMENT '提前天数',
  `remind_target`       varchar(10)  NOT NULL DEFAULT 'ALL'      COMMENT '提醒对象',
  `remind_user_id`      bigint       DEFAULT NULL                COMMENT '指定提醒用户',
  `status`              tinyint      NOT NULL DEFAULT 0          COMMENT '状态',
  `deleted`             tinyint      NOT NULL DEFAULT 0          COMMENT '逻辑删除',
  `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_creator_user_id` (`creator_user_id`),
  KEY `idx_month_day` (`month`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭重要事项';
```

---

*后端设计方案结束。前端设计方案待审核确认后另行输出。*
