-- 轮播图片表
CREATE TABLE IF NOT EXISTS `t_banner` (
                                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                          `banner_id` BIGINT NOT NULL COMMENT '轮播图业务ID',
                                          `image_path` VARCHAR(500) NOT NULL COMMENT '图片存储路径（相对路径）',
    `position` INT NOT NULL DEFAULT 0 COMMENT '图片位置，用于排序，数字越小越靠前',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '图片标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '图片描述',
    `link_url` VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_banner_id` (`banner_id`),
    KEY `idx_position` (`position`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图片表';

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

