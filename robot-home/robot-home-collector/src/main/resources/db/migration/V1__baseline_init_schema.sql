-- ============================================================
-- V1 基线：完整初始 Schema
-- 合并自：01_schema.sql + 04_crawler_schema.sql + 05_brand_alias_and_indexes.sql
-- ============================================================
-- 此迁移包含机器人之家项目的完整基线 Schema。
-- baseline-on-migrate: true（已有数据库自动基线化，跳过此迁移）
-- baseline-version: 1（标记 V1 为已应用）
-- 新数据库：Flyway 从 V1 开始执行，创建完整 Schema
-- 已有数据库：Flyway 基线化后执行 V2+ 增量迁移
-- ============================================================

-- ---------------------------
-- 用户（前台统一用户体系：PC / 小程序）
-- ---------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名',
  `password` VARCHAR(128) DEFAULT NULL COMMENT '密码哈希',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `gender` TINYINT DEFAULT 0 COMMENT '0未知 1男 2女',
  `birthday` DATE DEFAULT NULL,
  `intro` VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
  `province` VARCHAR(32) DEFAULT NULL,
  `city` VARCHAR(32) DEFAULT NULL,
  `fans_count` INT DEFAULT 0 COMMENT '粉丝数',
  `follow_count` INT DEFAULT 0 COMMENT '关注数',
  `post_count` INT DEFAULT 0 COMMENT '发帖数',
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid',
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
  `user_type` TINYINT DEFAULT 1 COMMENT '1普通 2后台',
  `source` VARCHAR(16) DEFAULT NULL COMMENT 'pc / miniapp',
  `last_login_time` DATETIME DEFAULT NULL,
  `last_login_ip` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前台用户';

-- ============================================================
-- 后台 RBAC
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL COMMENT '密码哈希',
  `nickname` VARCHAR(64) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码 ADMIN/EDITOR',
  `role_name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单',
  `menu_name` VARCHAR(64) NOT NULL,
  `menu_type` TINYINT DEFAULT 1 COMMENT '1菜单 2按钮',
  `path` VARCHAR(128) DEFAULT NULL COMMENT '路由',
  `component` VARCHAR(128) DEFAULT NULL COMMENT '前端组件',
  `icon` VARCHAR(64) DEFAULT NULL,
  `permission` VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限标识，如 robot:add',
  `permission_name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rp` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

-- ============================================================
-- 机器人分类 / 系列 / 型号（产品）
-- ============================================================
CREATE TABLE IF NOT EXISTS `robot_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类（一级行业大类）',
  `name` VARCHAR(64) NOT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `level` TINYINT DEFAULT 1,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人分类（可扩展）';

CREATE TABLE IF NOT EXISTS `robot_series` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_id` BIGINT DEFAULT NULL,
  `category_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_brand` (`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人系列';

CREATE TABLE IF NOT EXISTS `robot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL COMMENT '末级分类',
  `series_id` BIGINT DEFAULT NULL,
  `brand_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(128) NOT NULL COMMENT '产品名称',
  `model` VARCHAR(64) DEFAULT NULL COMMENT '型号',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '一句话简介',
  `guide_price` DECIMAL(12,2) DEFAULT NULL COMMENT '指导价',
  `market_price` DECIMAL(12,2) DEFAULT NULL COMMENT '市场价',
  `status` TINYINT DEFAULT 1 COMMENT '1在售 2预售 3停产',
  `release_date` DATE DEFAULT NULL COMMENT '发布时间',
  `cover_image` VARCHAR(255) DEFAULT NULL,
  `images` TEXT COMMENT '图集 JSON',
  `detail` LONGTEXT COMMENT '图文详情（富文本）',
  `video_count` INT DEFAULT 0,
  `main_params` TEXT COMMENT '核心参数摘要 JSON',
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间',
  `hot_score` BIGINT DEFAULT 0 COMMENT '热度分',
  `view_count` INT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `compare_count` INT DEFAULT 0,
  `inquiry_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `score` DECIMAL(3,1) DEFAULT 0 COMMENT '评分',
  `is_example` TINYINT DEFAULT 0 COMMENT '1示例数据',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_hot` (`hot_score`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人型号（产品本体）';

CREATE TABLE IF NOT EXISTS `robot_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `url` VARCHAR(255) NOT NULL,
  `type` VARCHAR(16) DEFAULT 'normal' COMMENT 'normal/detail',
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人图片';

CREATE TABLE IF NOT EXISTS `robot_video` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `title` VARCHAR(128) DEFAULT NULL,
  `url` VARCHAR(255) NOT NULL COMMENT '视频地址',
  `cover` VARCHAR(255) DEFAULT NULL,
  `duration` INT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人视频';

CREATE TABLE IF NOT EXISTS `robot_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `channel` VARCHAR(64) DEFAULT NULL COMMENT '渠道',
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
  `price` DECIMAL(12,2) DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人价格';

CREATE TABLE IF NOT EXISTS `robot_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `tag_type` VARCHAR(16) NOT NULL COMMENT 'scene使用场景/dev开发能力/ai人工智能/feature特性',
  `tag_value` VARCHAR(64) NOT NULL COMMENT '标签值',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_robot_tag` (`robot_id`,`tag_type`,`tag_value`),
  KEY `idx_type_value` (`tag_type`,`tag_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人标签（场景/开发/AI能力筛选）';

-- ============================================================
-- 参数模板系统（分类 → 模板 → 分组 → 参数定义 → 参数值）
-- ============================================================
CREATE TABLE IF NOT EXISTS `robot_param_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT NOT NULL COMMENT '关联分类',
  `name` VARCHAR(64) NOT NULL COMMENT '模板名称',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数模板';

CREATE TABLE IF NOT EXISTS `robot_param_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL COMMENT '分组名（基础参数/运动参数/AI...）',
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数分组';

CREATE TABLE IF NOT EXISTS `robot_param_def` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL COMMENT '参数名',
  `unit` VARCHAR(16) DEFAULT NULL,
  `type` VARCHAR(16) DEFAULT 'text' COMMENT 'text/number/select',
  `options` VARCHAR(512) DEFAULT NULL COMMENT 'select 选项，逗号分隔',
  `sort` INT DEFAULT 0,
  `is_compare` TINYINT DEFAULT 0 COMMENT '是否用于对比',
  `is_show` TINYINT DEFAULT 1 COMMENT '是否展示',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数定义';

CREATE TABLE IF NOT EXISTS `robot_param_value` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `def_id` BIGINT NOT NULL,
  `value` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_robot_def` (`robot_id`,`def_id`),
  KEY `idx_def` (`def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人参数值';

-- ============================================================
-- 品牌 / 企业（含 05 新增字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS `brand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `brand_name_en` VARCHAR(128) DEFAULT NULL COMMENT '品牌英文名',
  `short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称',
  `logo` VARCHAR(255) DEFAULT NULL,
  `company_id` BIGINT DEFAULT NULL,
  `intro` VARCHAR(1024) DEFAULT NULL,
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `found_year` INT DEFAULT NULL,
  `country` VARCHAR(32) DEFAULT NULL,
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL',
  `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间',
  `website` VARCHAR(255) DEFAULT NULL,
  `initial` VARCHAR(8) DEFAULT NULL COMMENT '首字母（用于字母索引）',
  `robot_count` INT DEFAULT 0 COMMENT '机器人数量（冗余，程序维护）',
  `hot_score` BIGINT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_initial` (`initial`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌';

CREATE TABLE IF NOT EXISTS `company` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `company_name_en` VARCHAR(128) DEFAULT NULL COMMENT '公司英文名',
  `short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称',
  `logo` VARCHAR(255) DEFAULT NULL,
  `intro` VARCHAR(2048) DEFAULT NULL,
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `business_scope` VARCHAR(512) DEFAULT NULL COMMENT '经营范围',
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL',
  `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间',
  `found_year` INT DEFAULT NULL,
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
  `country` VARCHAR(64) DEFAULT NULL COMMENT '国家',
  `province` VARCHAR(32) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(32) DEFAULT NULL COMMENT '城市',
  `website` VARCHAR(255) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `contact_email` VARCHAR(128) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签 JSON',
  `brand_count` INT DEFAULT 0,
  `product_count` INT DEFAULT 0,
  `hot_score` BIGINT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业';

-- ============================================================
-- 内容：资讯 / 视频 / 教程（含各自分类）
-- ============================================================
CREATE TABLE IF NOT EXISTS `article_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯栏目';

CREATE TABLE IF NOT EXISTS `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL,
  `robot_id` BIGINT DEFAULT NULL COMMENT '关联机器人',
  `brand_id` BIGINT DEFAULT NULL COMMENT '关联品牌',
  `title` VARCHAR(255) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `summary` VARCHAR(512) DEFAULT NULL,
  `content` LONGTEXT COMMENT '富文本正文',
  `author` VARCHAR(64) DEFAULT NULL,
  `source` VARCHAR(64) DEFAULT NULL,
  `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL',
  `tags` VARCHAR(512) DEFAULT NULL COMMENT 'JSON',
  `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶',
  `view_count` INT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '0下架 1上架',
  `publish_time` DATETIME DEFAULT NULL,
  `is_example` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_publish` (`publish_time`),
  KEY `idx_source_url` (`source_url`(191)),
  KEY `idx_title` (`title`(191)),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯文章';

CREATE TABLE IF NOT EXISTS `video_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频频道';

CREATE TABLE IF NOT EXISTS `video` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL,
  `robot_id` BIGINT DEFAULT NULL COMMENT '关联机器人',
  `brand_id` BIGINT DEFAULT NULL COMMENT '关联品牌',
  `title` VARCHAR(255) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `url` VARCHAR(512) NOT NULL COMMENT '视频地址',
  `duration` INT DEFAULT 0,
  `summary` VARCHAR(512) DEFAULT NULL,
  `author` VARCHAR(64) DEFAULT NULL,
  `tags` VARCHAR(512) DEFAULT NULL,
  `view_count` INT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `publish_time` DATETIME DEFAULT NULL,
  `is_example` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频';

CREATE TABLE IF NOT EXISTS `tutorial_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教程分类';

CREATE TABLE IF NOT EXISTS `tutorial` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(255) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `summary` VARCHAR(512) DEFAULT NULL,
  `content` LONGTEXT COMMENT 'Markdown 正文',
  `author` VARCHAR(64) DEFAULT NULL,
  `tags` VARCHAR(512) DEFAULT NULL,
  `view_count` INT DEFAULT 0,
  `like_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `publish_time` DATETIME DEFAULT NULL,
  `is_example` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教程';

-- ============================================================
-- 社区：圈子 / 帖子
-- ============================================================
CREATE TABLE IF NOT EXISTS `community_circle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `logo` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `post_count` INT DEFAULT 0,
  `follow_count` INT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区圈子';

CREATE TABLE IF NOT EXISTS `community_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `circle_id` BIGINT DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `content` TEXT,
  `images` VARCHAR(1024) DEFAULT NULL COMMENT 'JSON',
  `video_url` VARCHAR(512) DEFAULT NULL,
  `robot_id` BIGINT DEFAULT NULL,
  `brand_id` BIGINT DEFAULT NULL,
  `topic` VARCHAR(64) DEFAULT NULL,
  `like_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `is_top` TINYINT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '0待审 1正常 2下架',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_circle` (`circle_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子';

-- ============================================================
-- 通用：评论 / 收藏 / 关注 / 点赞 / 浏览 / 搜索
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `biz_type` VARCHAR(16) NOT NULL COMMENT 'robot/article/video/post/tutorial',
  `biz_id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(1024) NOT NULL,
  `parent_id` BIGINT DEFAULT 0 COMMENT '0一级评论，否则为回复',
  `reply_to` BIGINT DEFAULT NULL COMMENT '被回复用户id',
  `like_count` INT DEFAULT 0,
  `reply_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用评论';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(16) NOT NULL COMMENT 'robot/article/video/post/tutorial',
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_biz` (`user_id`,`biz_type`,`biz_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一收藏';

CREATE TABLE IF NOT EXISTS `follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `follow_type` VARCHAR(16) NOT NULL COMMENT 'user/brand/company/robot',
  `follow_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`,`follow_type`,`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注';

CREATE TABLE IF NOT EXISTS `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(16) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`,`biz_type`,`biz_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞';

CREATE TABLE IF NOT EXISTS `browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `biz_type` VARCHAR(16) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览历史';

CREATE TABLE IF NOT EXISTS `search_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `keyword` VARCHAR(128) NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史';

CREATE TABLE IF NOT EXISTS `hot_search` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `keyword` VARCHAR(128) NOT NULL,
  `search_count` INT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热搜';

-- ============================================================
-- 询价
-- ============================================================
CREATE TABLE IF NOT EXISTS `inquiry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT DEFAULT NULL,
  `robot_name` VARCHAR(128) DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(64) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `region` VARCHAR(64) DEFAULT NULL,
  `customer_type` TINYINT DEFAULT 1 COMMENT '1个人 2企业',
  `company_name` VARCHAR(128) DEFAULT NULL,
  `quantity` INT DEFAULT 1,
  `budget` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(1024) DEFAULT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1待处理2处理中3已联系4已成交5已关闭',
  `handle_note` VARCHAR(512) DEFAULT NULL,
  `handle_records` TEXT COMMENT '跟进记录 JSON',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价';

-- ============================================================
-- 消息
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(16) DEFAULT 'system' COMMENT 'system/reply/like/follow',
  `title` VARCHAR(128) DEFAULT NULL,
  `content` VARCHAR(1024) DEFAULT NULL,
  `related_id` BIGINT DEFAULT NULL,
  `is_read` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息';

-- ============================================================
-- Banner / 推荐位
-- ============================================================
CREATE TABLE IF NOT EXISTS `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position` VARCHAR(16) NOT NULL COMMENT 'pc / app',
  `title` VARCHAR(128) DEFAULT NULL,
  `image` VARCHAR(255) NOT NULL,
  `url` VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_position` (`position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner';

CREATE TABLE IF NOT EXISTS `recommend_position` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL COMMENT 'hot_robot/new_robot...',
  `name` VARCHAR(64) NOT NULL,
  `biz_type` VARCHAR(16) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐位';

CREATE TABLE IF NOT EXISTS `recommend_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(16) DEFAULT NULL,
  `biz_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(128) DEFAULT NULL,
  `image` VARCHAR(255) DEFAULT NULL,
  `url` VARCHAR(255) DEFAULT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_position` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐项';

-- ============================================================
-- 系统：配置 / 字典 / 日志
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(64) NOT NULL,
  `config_value` VARCHAR(1024) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dict_type` VARCHAR(64) NOT NULL,
  `dict_label` VARCHAR(64) NOT NULL,
  `dict_value` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

CREATE TABLE IF NOT EXISTS `sys_log_login` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(64) DEFAULT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

CREATE TABLE IF NOT EXISTS `sys_log_oper` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(64) DEFAULT NULL,
  `module` VARCHAR(64) DEFAULT NULL COMMENT '操作模块',
  `action` VARCHAR(64) DEFAULT NULL COMMENT '动作',
  `method` VARCHAR(255) DEFAULT NULL,
  `params` TEXT COMMENT '参数（脱敏后）',
  `ip` VARCHAR(64) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `error_msg` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE IF NOT EXISTS `sys_log_error` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `message` VARCHAR(1024) DEFAULT NULL,
  `stack_trace` TEXT,
  `ip` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常日志';

-- ============================================================
-- 采集数据源配置
-- ============================================================
CREATE TABLE IF NOT EXISTS `crawler_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'WEBSITE/RSS/SITEMAP/WECHAT/MANUAL',
  `base_url` VARCHAR(512) NOT NULL COMMENT '基础URL',
  `brand_id` BIGINT DEFAULT NULL COMMENT '关联品牌',
  `company_id` BIGINT DEFAULT NULL COMMENT '关联企业',
  `crawl_enabled` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `crawl_strategy` VARCHAR(32) DEFAULT 'GENERIC' COMMENT 'GENERIC/SITEMAP/RSS/NEWS/PRODUCT/WECHAT',
  `crawl_interval` INT DEFAULT 3600 COMMENT '采集间隔(秒)',
  `max_depth` INT DEFAULT 3 COMMENT '最大爬取深度',
  `max_pages` INT DEFAULT 500 COMMENT '单次最大页面数',
  `include_rules` VARCHAR(1024) DEFAULT NULL COMMENT '包含URL规则(正则,逗号分隔)',
  `exclude_rules` VARCHAR(1024) DEFAULT NULL COMMENT '排除URL规则(正则,逗号分隔)',
  `title_selector` VARCHAR(255) DEFAULT NULL COMMENT '标题CSS选择器',
  `content_selector` VARCHAR(255) DEFAULT NULL COMMENT '正文CSS选择器',
  `date_selector` VARCHAR(255) DEFAULT NULL COMMENT '日期CSS选择器',
  `author_selector` VARCHAR(255) DEFAULT NULL COMMENT '作者CSS选择器',
  `cover_selector` VARCHAR(255) DEFAULT NULL COMMENT '封面CSS选择器',
  `list_selector` VARCHAR(255) DEFAULT NULL COMMENT '列表页CSS选择器',
  `detail_link_selector` VARCHAR(255) DEFAULT NULL COMMENT '详情链接CSS选择器',
  `auto_publish` TINYINT DEFAULT 0 COMMENT '0需审核 1自动发布',
  `respect_robots` TINYINT DEFAULT 1 COMMENT '是否遵守robots.txt',
  `request_delay` INT DEFAULT 1000 COMMENT '请求间隔(毫秒)',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '自定义User-Agent',
  `extra_config` TEXT COMMENT '额外配置JSON',
  `last_crawl_time` DATETIME DEFAULT NULL COMMENT '最后采集时间',
  `last_crawl_status` VARCHAR(32) DEFAULT NULL COMMENT '最后采集状态',
  `total_articles` INT DEFAULT 0 COMMENT '累计文章数',
  `total_products` INT DEFAULT 0 COMMENT '累计产品数',
  `total_errors` INT DEFAULT 0 COMMENT '累计错误数',
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
  `seed_urls` TEXT COMMENT '种子URL列表(换行分隔)',
  `sitemap_url` VARCHAR(512) DEFAULT NULL COMMENT 'Sitemap URL',
  `fetch_mode` VARCHAR(16) DEFAULT 'http' COMMENT '抓取模式: http/browser',
  `follow_external` TINYINT DEFAULT 0 COMMENT '是否跟踪外部链接',
  `wait_selector` VARCHAR(255) DEFAULT NULL COMMENT '等待选择器(browser模式)',
  `wait_after_load_ms` INT DEFAULT 2000 COMMENT '页面加载后等待时间(browser模式,毫秒)',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`source_type`),
  KEY `idx_enabled` (`crawl_enabled`),
  KEY `idx_brand` (`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集数据源';

-- ---------------------------
-- 采集任务
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_type` VARCHAR(32) NOT NULL COMMENT 'FULL/INCREMENTAL/SINGLE_URL/RETRY',
  `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/STOPPED',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `urls_discovered` INT DEFAULT 0 COMMENT '发现URL数',
  `urls_success` INT DEFAULT 0 COMMENT '成功URL数',
  `urls_failed` INT DEFAULT 0 COMMENT '失败URL数',
  `articles_new` INT DEFAULT 0 COMMENT '新增文章数',
  `articles_updated` INT DEFAULT 0 COMMENT '更新文章数',
  `articles_duplicate` INT DEFAULT 0 COMMENT '重复文章数',
  `products_new` INT DEFAULT 0 COMMENT '新增产品数',
  `products_updated` INT DEFAULT 0 COMMENT '更新产品数',
  `images_downloaded` INT DEFAULT 0 COMMENT '下载图片数',
  `images_failed` INT DEFAULT 0 COMMENT '失败图片数',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务';

-- ---------------------------
-- URL队列
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_url` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) NOT NULL COMMENT '原始URL',
  `normalized_url` VARCHAR(1024) NOT NULL COMMENT '规范化URL',
  `url_hash` VARCHAR(64) NOT NULL COMMENT 'URL SHA-256',
  `parent_url` VARCHAR(1024) DEFAULT NULL COMMENT '父页面URL',
  `depth` INT DEFAULT 0 COMMENT '爬取深度',
  `content_type` VARCHAR(32) DEFAULT NULL COMMENT 'HTML/RSS/IMAGE/JSON',
  `url_status` VARCHAR(32) DEFAULT 'DISCOVERED' COMMENT 'DISCOVERED/QUEUED/FETCHING/SUCCESS/FAILED/SKIPPED/BLOCKED',
  `http_status` INT DEFAULT NULL COMMENT 'HTTP状态码',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `last_error` VARCHAR(512) DEFAULT NULL COMMENT '最后错误',
  `etag` VARCHAR(255) DEFAULT NULL COMMENT 'HTTP ETag',
  `last_modified` VARCHAR(64) DEFAULT NULL COMMENT 'HTTP Last-Modified',
  `crawl_time` DATETIME DEFAULT NULL COMMENT '抓取时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_hash` (`url_hash`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_status` (`url_status`),
  KEY `idx_crawl` (`crawl_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='URL队列';

-- ---------------------------
-- 页面原始内容
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_page` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `url_id` BIGINT NOT NULL COMMENT 'URL ID',
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) NOT NULL,
  `title` VARCHAR(512) DEFAULT NULL COMMENT '页面标题',
  `html_content` MEDIUMTEXT COMMENT '原始HTML',
  `text_content` MEDIUMTEXT COMMENT '提取的文本',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容SHA-256',
  `simhash` VARCHAR(64) DEFAULT NULL COMMENT 'SimHash指纹',
  `page_type` VARCHAR(32) DEFAULT NULL COMMENT 'ARTICLE/PRODUCT/LIST/OTHER',
  `links_count` INT DEFAULT 0 COMMENT '页面链接数',
  `images_count` INT DEFAULT 0 COMMENT '页面图片数',
  `crawl_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_url_id` (`url_id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_type` (`page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面原始内容';

-- ---------------------------
-- 采集文章（含 V3 的 fail_reason/retry_count）
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `page_id` BIGINT DEFAULT NULL COMMENT '页面ID',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `source_url` VARCHAR(1024) NOT NULL COMMENT '来源URL',
  `source_site` VARCHAR(255) DEFAULT NULL COMMENT '来源站点',
  `title` VARCHAR(512) DEFAULT NULL COMMENT '标题',
  `subtitle` VARCHAR(512) DEFAULT NULL COMMENT '副标题',
  `author` VARCHAR(64) DEFAULT NULL COMMENT '作者',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图(原始URL)',
  `cover_image_local` VARCHAR(512) DEFAULT NULL COMMENT '封面图(本地URL)',
  `summary` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
  `content_html` MEDIUMTEXT COMMENT '正文HTML',
  `content_text` MEDIUMTEXT COMMENT '正文纯文本',
  `images` TEXT COMMENT '图片列表JSON',
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签JSON',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希',
  `simhash` VARCHAR(64) DEFAULT NULL COMMENT 'SimHash',
  `brand_id` BIGINT DEFAULT NULL COMMENT '匹配品牌ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '匹配分类ID',
  `series_id` BIGINT DEFAULT NULL COMMENT '匹配系列ID',
  `company_id` BIGINT DEFAULT NULL COMMENT '匹配企业ID',
  `robot_id` BIGINT DEFAULT NULL COMMENT '匹配机器人ID',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/MATCHED/UNMATCHED',
  `article_status` VARCHAR(32) DEFAULT 'CRAWLED' COMMENT 'CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED',
  `article_id` BIGINT DEFAULT NULL COMMENT '同步到article表后的ID',
  `synced` TINYINT DEFAULT 0 COMMENT '0未同步 1已同步',
  `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间（指数退避）',
  `crawl_time` DATETIME DEFAULT NULL COMMENT '抓取时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_status` (`article_status`),
  KEY `idx_match` (`match_status`),
  KEY `idx_sync` (`synced`),
  KEY `idx_publish` (`publish_time`),
  KEY `idx_source_url` (`source_url`(191)),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集文章';

-- ---------------------------
-- 采集产品（含 V3 的 fail_reason/retry_count）
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `page_id` BIGINT DEFAULT NULL COMMENT '页面ID',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `source_url` VARCHAR(1024) NOT NULL COMMENT '来源URL',
  `source_site` VARCHAR(255) DEFAULT NULL COMMENT '来源站点',
  `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `model` VARCHAR(128) DEFAULT NULL COMMENT '型号',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(原始)',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
  `summary` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
  `description` MEDIUMTEXT COMMENT '描述HTML',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图(原始)',
  `cover_image_local` VARCHAR(512) DEFAULT NULL COMMENT '封面图(本地)',
  `gallery` TEXT COMMENT '图集JSON(原始URL)',
  `gallery_local` TEXT COMMENT '图集JSON(本地URL)',
  `status` VARCHAR(32) DEFAULT NULL COMMENT '在售/预售/停产',
  `release_date` DATE DEFAULT NULL COMMENT '发布日期',
  `price` VARCHAR(128) DEFAULT NULL COMMENT '价格(原始)',
  `official_url` VARCHAR(512) DEFAULT NULL COMMENT '官方链接',
  `raw_params` TEXT COMMENT '原始参数JSON',
  `normalized_params` TEXT COMMENT '标准化参数JSON',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希',
  `brand_id` BIGINT DEFAULT NULL COMMENT '匹配品牌ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '匹配分类ID',
  `series_id` BIGINT DEFAULT NULL COMMENT '匹配系列ID',
  `company_id` BIGINT DEFAULT NULL COMMENT '匹配企业ID',
  `robot_id` BIGINT DEFAULT NULL COMMENT '匹配机器人ID',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/MATCHED/UNMATCHED',
  `product_status` VARCHAR(32) DEFAULT 'CRAWLED' COMMENT 'CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED',
  `robot_id_synced` BIGINT DEFAULT NULL COMMENT '同步到robot表后的ID',
  `synced` TINYINT DEFAULT 0 COMMENT '0未同步 1已同步',
  `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `crawl_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_status` (`product_status`),
  KEY `idx_match` (`match_status`),
  KEY `idx_sync` (`synced`),
  KEY `idx_source_url` (`source_url`(191)),
  KEY `idx_robot_id_synced` (`robot_id_synced`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集产品';

-- ---------------------------
-- 采集媒体(图片)
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `original_url` VARCHAR(1024) NOT NULL COMMENT '原始URL',
  `url_hash` VARCHAR(64) NOT NULL COMMENT 'URL哈希',
  `storage_url` VARCHAR(512) DEFAULT NULL COMMENT '本地存储URL',
  `file_hash` VARCHAR(64) DEFAULT NULL COMMENT '文件内容哈希',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `mime_type` VARCHAR(64) DEFAULT NULL COMMENT 'MIME类型',
  `width` INT DEFAULT NULL COMMENT '宽度',
  `height` INT DEFAULT NULL COMMENT '高度',
  `media_type` VARCHAR(16) DEFAULT 'IMAGE' COMMENT 'IMAGE/VIDEO',
  `download_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  `retry_count` INT DEFAULT 0,
  `error_message` VARCHAR(512) DEFAULT NULL,
  `ref_article_id` BIGINT DEFAULT NULL COMMENT '关联文章ID',
  `ref_product_id` BIGINT DEFAULT NULL COMMENT '关联产品ID',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_hash` (`url_hash`),
  KEY `idx_file_hash` (`file_hash`),
  KEY `idx_status` (`download_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集媒体';

-- ---------------------------
-- 采集错误
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_error` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '相关URL',
  `error_type` VARCHAR(32) NOT NULL COMMENT 'NETWORK/TIMEOUT/HTTP_403/HTTP_404/PARSE_ERROR/JS_RENDER_ERROR/DUPLICATE/DATABASE/IMAGE/BLOCKED/UNKNOWN',
  `error_message` VARCHAR(1024) DEFAULT NULL,
  `error_detail` TEXT COMMENT '详细错误信息',
  `http_status` INT DEFAULT NULL,
  `retry_count` INT DEFAULT 0,
  `resolved` TINYINT DEFAULT 0 COMMENT '0未解决 1已解决',
  `resolve_note` VARCHAR(512) DEFAULT NULL COMMENT '解决备注',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_type` (`error_type`),
  KEY `idx_resolved` (`resolved`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集错误';

-- ---------------------------
-- 匹配记录
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_match_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT 'ARTICLE/PRODUCT',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID(crawler_article/crawler_product)',
  `match_type` VARCHAR(32) NOT NULL COMMENT 'BRAND/COMPANY/ROBOT',
  `match_target_id` BIGINT DEFAULT NULL COMMENT '匹配目标ID',
  `match_keyword` VARCHAR(255) DEFAULT NULL COMMENT '匹配关键词',
  `confidence` DECIMAL(5,4) DEFAULT 0 COMMENT '匹配置信度',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/AUTO_MATCHED/MANUAL_MATCHED/REJECTED',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人',
  `handle_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_match_type` (`match_type`),
  KEY `idx_status` (`match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匹配记录';

-- ---------------------------
-- 采集规则
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID(NULL=全局规则)',
  `rule_type` VARCHAR(32) NOT NULL COMMENT 'SELECTOR/REGEX/KEYWORD/PARAM_MAP/CATEGORY',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_key` VARCHAR(128) DEFAULT NULL COMMENT '规则键(如title_selector)',
  `rule_value` TEXT COMMENT '规则值(如CSS选择器/正则/JSON)',
  `priority` INT DEFAULT 0 COMMENT '优先级(越大越优先)',
  `enabled` TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集规则';

-- ---------------------------
-- 采集日志
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '相关URL',
  `action` VARCHAR(64) DEFAULT NULL COMMENT '动作',
  `level` VARCHAR(16) DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR',
  `message` VARCHAR(1024) DEFAULT NULL,
  `duration` INT DEFAULT NULL COMMENT '耗时(毫秒)',
  `http_status` INT DEFAULT NULL,
  `result` VARCHAR(32) DEFAULT NULL COMMENT 'SUCCESS/FAILED/SKIPPED',
  `extra_data` TEXT COMMENT '额外数据JSON',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_level` (`level`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集日志';

-- ---------------------------
-- 产品参数变更记录
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_product_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '采集产品ID',
  `field_name` VARCHAR(64) NOT NULL COMMENT '字段名',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL',
  `change_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品参数变更记录';

-- ---------------------------
-- 品牌别名(用于匹配)
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_brand_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_id` BIGINT NOT NULL COMMENT '品牌ID',
  `alias_name` VARCHAR(128) NOT NULL COMMENT '别名',
  `alias_type` VARCHAR(32) DEFAULT 'NAME' COMMENT 'NAME/ENGLISH/ABBREVIATION/OTHER',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(冗余)',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_alias` (`brand_id`, `alias_name`),
  KEY `idx_alias` (`alias_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌别名';

-- ---------------------------
-- 参数标准化映射
-- ---------------------------
CREATE TABLE IF NOT EXISTS `crawler_param_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `normalized_name` VARCHAR(64) NOT NULL COMMENT '标准化名称',
  `raw_name` VARCHAR(128) NOT NULL COMMENT '原始名称',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '机器人分类',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `aliases` TEXT COMMENT '别名列表JSON',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_name` (`raw_name`, `category`),
  KEY `idx_normalized` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数标准化映射';

-- ============================================================
-- Phase 2 新增表（来自 05_brand_alias_and_indexes.sql）
-- ============================================================

-- ---------------------------
-- 品牌别名表（用于采集匹配）
-- ---------------------------
CREATE TABLE IF NOT EXISTS `brand_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_id` BIGINT NOT NULL COMMENT '品牌ID',
  `alias_name` VARCHAR(128) NOT NULL COMMENT '别名',
  `alias_type` VARCHAR(32) DEFAULT 'NAME' COMMENT 'NAME/ENGLISH/ABBREVIATION/OTHER',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(冗余)',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_alias` (`brand_id`, `alias_name`),
  KEY `idx_alias` (`alias_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌别名';

-- ---------------------------
-- 参数标准化映射表
-- ---------------------------
CREATE TABLE IF NOT EXISTS `param_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `normalized_name` VARCHAR(64) NOT NULL COMMENT '标准化名称',
  `raw_name` VARCHAR(128) NOT NULL COMMENT '原始名称',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '机器人分类',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `aliases` TEXT COMMENT '别名列表JSON',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_name` (`raw_name`, `category`),
  KEY `idx_normalized` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数标准化映射';

-- ---------------------------
-- 产品参数变更记录
-- ---------------------------
CREATE TABLE IF NOT EXISTS `product_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `param_key` VARCHAR(64) NOT NULL COMMENT '参数键',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `detected_time` DATETIME DEFAULT NULL COMMENT '检测时间',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`),
  KEY `idx_param` (`param_key`),
  KEY `idx_detected` (`detected_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品参数变更记录';

-- ============================================================
-- 初始化种子数据
-- ============================================================

-- 初始化参数标准化映射数据
INSERT IGNORE INTO `crawler_param_mapping` (`normalized_name`, `raw_name`, `unit`, `create_time`) VALUES
('weight', '重量', 'kg', NOW()),
('weight', '整机重量', 'kg', NOW()),
('weight', '机器重量', 'kg', NOW()),
('weight', '产品重量', 'kg', NOW()),
('weight', '净重', 'kg', NOW()),
('weight', '自重', 'kg', NOW()),
('height', '身高', 'cm', NOW()),
('height', '产品高度', 'cm', NOW()),
('height', '整机高度', 'cm', NOW()),
('height', '站立高度', 'cm', NOW()),
('dof', '自由度', NULL, NOW()),
('dof', '关节自由度', NULL, NOW()),
('dof', '运动自由度', NULL, NOW()),
('battery_life', '续航', 'h', NOW()),
('battery_life', '续航时间', 'h', NOW()),
('battery_life', '工作时间', 'h', NOW()),
('battery_life', '续航能力', 'h', NOW()),
('speed', '速度', 'm/s', NOW()),
('speed', '最大速度', 'm/s', NOW()),
('speed', '行走速度', 'm/s', NOW()),
('speed', '移动速度', 'm/s', NOW()),
('payload', '负载', 'kg', NOW()),
('payload', '最大负载', 'kg', NOW()),
('payload', '载重', 'kg', NOW()),
('payload', '有效载荷', 'kg', NOW()),
('battery_capacity', '电池容量', 'mAh', NOW()),
('battery_capacity', '电量', 'mAh', NOW()),
('charging_time', '充电时间', 'h', NOW()),
('width', '宽度', 'cm', NOW()),
('width', '产品宽度', 'cm', NOW()),
('length', '长度', 'cm', NOW()),
('length', '产品长度', 'cm', NOW()),
('arm_reach', '臂展', 'cm', NOW()),
('arm_reach', '工作半径', 'cm', NOW()),
('arm_reach', '最大伸展', 'cm', NOW()),
('ip_rating', '防护等级', NULL, NOW()),
('ip_rating', '防水等级', NULL, NOW()),
('operating_temp', '工作温度', '℃', NOW()),
('operating_temp', '使用温度', '℃', NOW()),
('noise_level', '噪音', 'dB', NOW()),
('noise_level', '噪音等级', 'dB', NOW());

-- 初始化品牌别名(基于现有brand表数据)
INSERT IGNORE INTO `crawler_brand_alias` (`brand_id`, `alias_name`, `alias_type`, `create_time`)
SELECT b.id, b.name, 'NAME', NOW() FROM brand b WHERE b.deleted = 0;