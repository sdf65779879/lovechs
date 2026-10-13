-- ====================================================================
-- 情侣点餐系统（couple-ordering）数据库初始化脚本
-- 数据库：MySQL 8.0   引擎：InnoDB   字符集：utf8mb4
--
-- 说明：
--   1. Docker 部署时，本文件挂载到容器 /docker-entrypoint-initdb.d/，
--      仅在 MySQL 数据卷首次初始化时自动执行一次；
--   2. 也可手动执行：mysql -uroot -p < init.sql；
--   3. 脚本含 DROP TABLE IF EXISTS，重复执行会重建表并清空数据，
--      仅适用于初始化/重置场景，切勿在生产环境重复执行。
-- ====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `couple_ordering`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `couple_ordering`;

-- 重置业务数据时一并清除旧通知；邮件表由后端增量初始化自动创建。
DROP TABLE IF EXISTS `email_delivery`;

-- ====================================================================
-- 1. 用户表 sys_user（含管理员）
-- ====================================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(32)  NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `nickname`    VARCHAR(32)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN 管理员 / USER 普通用户',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 / 0 禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表（含管理员）';

-- ====================================================================
-- 2. 分类表 category
-- ====================================================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(32) NOT NULL COMMENT '分类名称',
  `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用 / 0 禁用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜品分类表';

-- ====================================================================
-- 3. 菜品表 dish
-- ====================================================================
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属分类 id',
  `name`         VARCHAR(64)  NOT NULL COMMENT '菜品名称',
  `description`  VARCHAR(500) NOT NULL DEFAULT '' COMMENT '菜品描述',
  `image`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '菜品图片地址',
  `tags`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标签，多个用英文逗号分隔，如：辣,拿手菜',
  `love_value`   INT          NOT NULL DEFAULT 1 COMMENT '爱心值（非负整数）',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 上架 / 0 下架',
  `is_recommend` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否推荐：1 是 / 0 否',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_category_status_sort` (`category_id`, `status`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜品表';

-- ====================================================================
-- 4. 购物车表 cart_item
--    taste / remark 为需求确认后新增字段；
--    使用 NOT NULL DEFAULT '' 以保证唯一索引在无口味备注时仍可合并。
-- ====================================================================
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `dish_id`     BIGINT UNSIGNED NOT NULL COMMENT '菜品 id',
  `quantity`    INT          NOT NULL DEFAULT 1 COMMENT '数量',
  `taste`       VARCHAR(100) NOT NULL DEFAULT '' COMMENT '口味快照：辣度/甜度选择，如「微辣/正常糖」',
  `remark`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '单品忌口/留言',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dish_taste` (`user_id`, `dish_id`, `taste`, `remark`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '购物车表';

-- ====================================================================
-- 5. 订单表 orders
-- ====================================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`      VARCHAR(32)  NOT NULL COMMENT '订单号，规则：yyyyMMddHHmmss + 4 位随机数',
  `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '下单用户 id',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING 待接单 / ACCEPTED 已接单 / COOKING 制作中 / COMPLETED 已完成 / CANCELLED 已取消 / REJECTED 已拒绝',
  `total_love`    INT          NOT NULL DEFAULT 0 COMMENT '合计爱心值',
  `remark`        VARCHAR(500) NOT NULL DEFAULT '' COMMENT '整单备注（口味/忌口/留言）',
  `expect_time`   DATETIME     DEFAULT NULL COMMENT '期望用餐时间，NULL 表示尽快',
  `reject_reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '管理员拒绝理由',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `accept_time`   DATETIME     DEFAULT NULL COMMENT '接单时间',
  `finish_time`   DATETIME     DEFAULT NULL COMMENT '完成时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_time` (`user_id`, `status`, `create_time`),
  KEY `idx_status_time` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表';

-- ====================================================================
-- 6. 订单明细表 order_item（下单时冗余菜品快照，历史订单不受菜品改动影响）
-- ====================================================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`   BIGINT UNSIGNED NOT NULL COMMENT '所属订单 id',
  `dish_id`    BIGINT UNSIGNED NOT NULL COMMENT '菜品 id',
  `dish_name`  VARCHAR(64)  NOT NULL COMMENT '菜品名称快照',
  `dish_image` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '菜品图片快照',
  `love_value` INT          NOT NULL DEFAULT 0 COMMENT '下单时爱心值快照',
  `quantity`   INT          NOT NULL DEFAULT 1 COMMENT '数量',
  `taste`      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '口味快照：辣度/甜度选择',
  `remark`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '单品忌口/留言',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单明细表';

-- ====================================================================
-- 7. 收藏表 favorite
-- ====================================================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `dish_id`     BIGINT UNSIGNED NOT NULL COMMENT '菜品 id',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dish` (`user_id`, `dish_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜品收藏表';

-- ====================================================================
-- 8. 系统配置表 sys_config（KV 结构）
-- ====================================================================
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '配置值',
  `remark`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '配置说明',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表';

-- ====================================================================
-- 初始化数据
-- ====================================================================

-- --------------------------------------------------------------------
-- 默认账号（BCrypt 密文，cost=10）
--   admin / admin123  昵称「大厨」 ROLE_ADMIN
--   love  / love123   昵称「宝贝」 ROLE_USER
-- --------------------------------------------------------------------
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `avatar`, `role`, `status`) VALUES
('admin', '$2a$10$M5JLaRzIaQZ41HxQJxKIneIpvZ6laaI8QvXQ5inaCTnmStIIFDxhm', '大厨', NULL, 'ADMIN', 1),
('love',  '$2a$10$FDPsaLMyEuLNKAPtJlMZLOvXSUvjCiQ244BtR29ElxYib1eiSP/li', '宝贝', NULL, 'USER',  1);

-- --------------------------------------------------------------------
-- 6 个分类
-- --------------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `sort`, `status`) VALUES
(1, '主食', 1, 1),
(2, '小炒', 2, 1),
(3, '汤羹', 3, 1),
(4, '甜品', 4, 1),
(5, '饮品', 5, 1),
(6, '夜宵', 6, 1);

-- --------------------------------------------------------------------
-- 15 个示例菜品（图片为前端内置 SVG 占位图）
-- --------------------------------------------------------------------
INSERT INTO `dish`
  (`id`, `category_id`, `name`, `description`, `image`, `tags`, `love_value`, `sort`, `status`, `is_recommend`)
VALUES
(1,  1, '番茄牛腩拌面', '手工面条配上慢炖一小时的番茄牛腩，酸甜浓郁，每一口都满足', '/images/placeholder/1.svg',  '拿手菜,不辣', 8, 1, 1, 1),
(2,  1, '葱油拌面',     '小葱小火慢熬出金汤葱油，拌一拌，简单却让人上头',       '/images/placeholder/2.svg',  '清淡',        5, 2, 1, 0),
(3,  1, '火腿蛋炒饭',   '粒粒分明的米饭裹着蛋香，冰箱里有什么就放什么',         '/images/placeholder/3.svg',  '家常',        5, 3, 1, 0),
(4,  2, '可乐鸡翅',     '咕嘟咕嘟炖到发亮，甜滋滋的，就像你一样',               '/images/placeholder/4.svg',  '拿手菜,微甜', 9, 1, 1, 1),
(5,  2, '麻婆豆腐',     '麻辣烫嘴，豆腐嫩得一抿就化，超级下饭',                 '/images/placeholder/5.svg',  '辣',          7, 2, 1, 0),
(6,  2, '青椒肉丝',     '家常小炒之王，青椒脆甜，肉丝滑嫩',                     '/images/placeholder/6.svg',  '微辣',        6, 3, 1, 0),
(7,  2, '蒜蓉西兰花',   '绿油油清爽解腻，给爱吃肉的你配点草',                   '/images/placeholder/7.svg',  '清淡',        4, 4, 1, 0),
(8,  3, '番茄蛋花汤',   '饭前一口汤，暖洋洋地从嘴里甜到心里',                   '/images/placeholder/8.svg',  '清淡',        4, 1, 1, 0),
(9,  3, '玉米排骨汤',   '玉米的甜炖进汤里，是小火慢炖一小时的心意',             '/images/placeholder/9.svg',  '拿手菜,清淡', 8, 2, 1, 1),
(10, 4, '红糖冰粉',     '滑溜溜吸进嘴里，是夏天会发光的快乐',                   '/images/placeholder/10.svg', '甜,冰凉',     6, 1, 1, 0),
(11, 4, '双皮奶',       '奶香浓郁，滑得像在舌头上跳舞',                         '/images/placeholder/11.svg', '拿手菜,甜',   7, 2, 1, 1),
(12, 5, '手打柠檬茶',   '暴打出香气的柠檬配红茶，一口醒神',                     '/images/placeholder/12.svg', '冰饮,微酸',   6, 1, 1, 0),
(13, 5, '热可可',       '棉花糖在杯子里泡澡，喝完手心都是暖的',                 '/images/placeholder/13.svg', '甜,热饮',     7, 2, 1, 0),
(14, 5, '蜂蜜柚子茶',   '润润的微甜，像冬天里的一个抱抱',                       '/images/placeholder/14.svg', '热饮,微甜',   5, 3, 1, 0),
(15, 6, '芝士泡面',     '深夜的罪恶感：加蛋、加芝士、再加一片火腿',             '/images/placeholder/15.svg', '夜宵,浓郁',   8, 1, 1, 1);

-- --------------------------------------------------------------------
-- 系统配置
-- --------------------------------------------------------------------
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('business_open', '1', '营业开关：1 营业，0 歇业（歇业时用户端无法下单）'),
('notice',        '',  '首页公告文案，为空则不展示');

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 初始化完成。核验 SQL（可选）：
--   SELECT COUNT(*) FROM sys_user;   -- 2
--   SELECT COUNT(*) FROM category;   -- 6
--   SELECT COUNT(*) FROM dish;       -- 15
-- ====================================================================
