-- 用户表
CREATE TABLE IF NOT EXISTS `mf_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    `openid`          VARCHAR(100) NOT NULL                COMMENT '微信小程序 openid，全局唯一用户标识',
    `unionid`         VARCHAR(100) DEFAULT NULL            COMMENT '微信开放平台 unionid，预留字段，首版不处理',
    `nickname`        VARCHAR(100) DEFAULT NULL            COMMENT '用户昵称，由用户主动设置，初始为空',
    `avatar_url`      VARCHAR(500) DEFAULT NULL            COMMENT '用户头像地址，由用户主动设置，初始为空',
    `mobile`          VARCHAR(20)  DEFAULT NULL            COMMENT '手机号，预留字段，首版不处理绑定逻辑',
    `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '用户状态：0-正常，1-禁用',
    `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最近一次登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 轮播图片表
CREATE TABLE IF NOT EXISTS `t_banner` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `banner_id`   BIGINT       NOT NULL                COMMENT '轮播图业务ID',
    `family_id`   BIGINT       NOT NULL                COMMENT '所属家庭ID，对应 mf_family.id',
    `image_path`  VARCHAR(500) NOT NULL                COMMENT '图片存储路径（相对路径）',
    `position`    INT          NOT NULL DEFAULT 0      COMMENT '图片位置，用于排序，数字越小越靠前',
    `title`       VARCHAR(200)          DEFAULT NULL   COMMENT '图片标题',
    `description` VARCHAR(500)          DEFAULT NULL   COMMENT '图片描述',
    `link_url`    VARCHAR(500)          DEFAULT NULL   COMMENT '点击跳转链接',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用，1-启用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否删除：0-未删除，1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_banner_id` (`banner_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_family_status` (`family_id`, `status`, `deleted`),
    KEY `idx_position` (`position`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图片表';

-- 存量数据迁移（已有库执行此语句补加字段）
-- ALTER TABLE `t_banner` ADD COLUMN `family_id` BIGINT NOT NULL DEFAULT 0 COMMENT '所属家庭ID，对应 mf_family.id' AFTER `banner_id`;
-- ALTER TABLE `t_banner` ADD KEY `idx_family_id` (`family_id`);
-- ALTER TABLE `t_banner` ADD KEY `idx_family_status` (`family_id`, `status`, `deleted`);

-- 评论表
CREATE TABLE IF NOT EXISTS `t_banner_comment` (
                                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                  `comment_id` BIGINT NOT NULL COMMENT '评论业务ID',
                                                  `banner_id` BIGINT NOT NULL COMMENT '关联的轮播图ID',
                                                  `user_name` VARCHAR(100) DEFAULT NULL COMMENT '评论用户名称',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-显示',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_id` (`comment_id`),
    KEY `idx_banner_id` (`banner_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图评论表';

-- 家庭表
CREATE TABLE IF NOT EXISTS `mf_family` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `family_name`  VARCHAR(64)  NOT NULL                COMMENT '家庭名称',
    `family_code`  VARCHAR(16)  NOT NULL                COMMENT '家庭编号（短码），用于成员加入',
    `owner_user_id` BIGINT      NOT NULL                COMMENT '户主用户ID，对应 mf_user.id',
    `member_count` INT          NOT NULL DEFAULT 1      COMMENT '成员数量，包含户主在内',
    `meet_date`    DATE                  DEFAULT NULL   COMMENT '相识相遇日期，用于首页展示"一起走过的天数"，可为空',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-正常，1-禁用，2-已解散',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_code` (`family_code`),
    KEY `idx_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

-- 家庭成员关系表（同时承载加入申请）
CREATE TABLE IF NOT EXISTS `mf_family_member` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `family_id`   BIGINT       NOT NULL                COMMENT '家庭ID，对应 mf_family.id',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID，对应 mf_user.id',
    `role`        VARCHAR(16)  NOT NULL                COMMENT '角色：OWNER-户主，MEMBER-成员',
    `join_status` VARCHAR(16)  NOT NULL                COMMENT '加入状态：PENDING/APPROVED/REJECTED/QUIT',
    `join_time`   DATETIME              DEFAULT NULL   COMMENT '加入时间（审批通过时间）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（申请时间）',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_user` (`family_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id_status` (`family_id`, `join_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员关系表';


CREATE TABLE `t_image_file` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
                                `content_type` VARCHAR(100) NOT NULL COMMENT 'Content-Type',
                                `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
                                `file_data` LONGBLOB NOT NULL COMMENT '图片二进制数据',
                                `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0否1是)',
                                `create_time` DATETIME NOT NULL COMMENT '创建时间',
                                `update_time` DATETIME NOT NULL COMMENT '修改时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_deleted_id` (`deleted`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片文件表';


-- =============================================
-- 五、家庭重要事项表
-- =============================================
CREATE TABLE IF NOT EXISTS `mf_event` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    `family_id`           BIGINT       NOT NULL                    COMMENT '所属家庭ID，关联 mf_family.id',
    `creator_user_id`     BIGINT       NOT NULL                    COMMENT '创建人用户ID，关联 mf_user.id',
    `title`               VARCHAR(100) NOT NULL                    COMMENT '事项名称',
    `category`            VARCHAR(50)  NOT NULL                    COMMENT '类别：BIRTHDAY/ANNIVERSARY/HOLIDAY/DOCUMENT_EXPIRY/HEALTH/PAYMENT/OTHER',
    `date_type`           TINYINT      NOT NULL DEFAULT 0          COMMENT '日期类型：0-阳历，1-农历',
    `month`               INT          NOT NULL                    COMMENT '月份（1-12；date_type=0 表示阳历月；date_type=1 表示农历月）',
    `day`                 INT          NOT NULL                    COMMENT '日期（1-31；date_type=0 表示阳历日；date_type=1 表示农历日）',
    `description`         VARCHAR(500) DEFAULT NULL                COMMENT '事项描述/备注',
    `remind_enabled`      TINYINT      NOT NULL DEFAULT 0          COMMENT '是否开启提醒：0-否，1-是',
    `remind_advance_days` INT          NOT NULL DEFAULT 0          COMMENT '提前几天提醒（0=当天提醒）',
    `remind_target`       VARCHAR(10)  NOT NULL DEFAULT 'ALL'      COMMENT '提醒对象：ALL-全部家庭成员，SPECIFIC-指定某人',
    `remind_user_id`      BIGINT       DEFAULT NULL                COMMENT '指定提醒用户ID（remind_target=SPECIFIC时有效）',
    `status`              TINYINT      NOT NULL DEFAULT 0          COMMENT '状态：0-正常，1-禁用',
    `deleted`             TINYINT      NOT NULL DEFAULT 0          COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_creator_user_id` (`creator_user_id`),
    KEY `idx_month_day` (`month`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭重要事项';


-- =============================================
-- 七、提醒记录表
-- =============================================
CREATE TABLE IF NOT EXISTS `mf_remind_log` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键ID',
    `event_id`            BIGINT       NOT NULL                             COMMENT '关联事项ID，对应 mf_event.id',
    `family_id`           BIGINT       NOT NULL                             COMMENT '家庭ID（冗余字段），便于按家庭查询',
    `user_id`             BIGINT       NOT NULL                             COMMENT '被提醒的用户ID，对应 mf_user.id',
    `trigger_year`        INT          NOT NULL                             COMMENT '本条记录对应的年份（如 2026）；与 event_id/user_id 联合去重',
    `event_date`          DATE         NOT NULL                             COMMENT '当年事件实际发生日期（已转换为阳历）；过期判断依据',
    `remind_date`         DATE         NOT NULL                             COMMENT '当年应开始提醒日期（event_date - remind_advance_days）',
    `status`              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'           COMMENT '状态：PENDING-待处理，READ-已查看，DONE-已完成，CLOSED_BY_USER-用户关闭，CLOSED_BY_SYSTEM-系统关闭',
    `action_time`         DATETIME              DEFAULT NULL                COMMENT '用户或系统产生终态动作的时间（DONE/关闭时写入）',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_user_year`   (`event_id`, `user_id`, `trigger_year`),
    KEY `idx_user_status`             (`user_id`, `status`),
    KEY `idx_family_status`           (`family_id`, `status`),
    KEY `idx_event_date`              (`event_date`),
    KEY `idx_event_id_status`         (`event_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒记录表';
