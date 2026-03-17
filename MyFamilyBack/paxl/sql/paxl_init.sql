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


