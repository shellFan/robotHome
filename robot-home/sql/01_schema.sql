-- ============================================================
-- 机器人之家 数据库 Schema
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 约定：所有业务表含 id / create_time / update_time / deleted / status
-- 外键逻辑由程序维护，不在数据库层建立物理外键
-- ============================================================
CREATE DATABASE IF NOT EXISTS robot_home DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
USE robot_home;

-- ---------------------------
-- 用户（前台统一用户体系：PC / 小程序）
-- ---------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
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
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
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

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
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

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
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

DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
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

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单';

DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rp` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

-- ============================================================
-- 机器人分类 / 系列 / 型号（产品）
-- ============================================================
DROP TABLE IF EXISTS `robot_category`;
CREATE TABLE `robot_category` (
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

DROP TABLE IF EXISTS `robot_series`;
CREATE TABLE `robot_series` (
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

DROP TABLE IF EXISTS `robot`;
CREATE TABLE `robot` (
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
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人型号（产品本体）';

DROP TABLE IF EXISTS `robot_image`;
CREATE TABLE `robot_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `url` VARCHAR(255) NOT NULL,
  `type` VARCHAR(16) DEFAULT 'normal' COMMENT 'normal/detail',
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人图片';

DROP TABLE IF EXISTS `robot_video`;
CREATE TABLE `robot_video` (
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

DROP TABLE IF EXISTS `robot_price`;
CREATE TABLE `robot_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL,
  `channel` VARCHAR(64) DEFAULT NULL COMMENT '渠道',
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
  `price` DECIMAL(12,2) DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人价格';

DROP TABLE IF EXISTS `robot_tag`;
CREATE TABLE `robot_tag` (
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
DROP TABLE IF EXISTS `robot_param_template`;
CREATE TABLE `robot_param_template` (
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

DROP TABLE IF EXISTS `robot_param_group`;
CREATE TABLE `robot_param_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL COMMENT '分组名（基础参数/运动参数/AI...）',
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数分组';

DROP TABLE IF EXISTS `robot_param_def`;
CREATE TABLE `robot_param_def` (
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

DROP TABLE IF EXISTS `robot_param_value`;
CREATE TABLE `robot_param_value` (
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
-- 品牌 / 企业
-- ============================================================
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `logo` VARCHAR(255) DEFAULT NULL,
  `company_id` BIGINT DEFAULT NULL,
  `intro` VARCHAR(1024) DEFAULT NULL,
  `found_year` INT DEFAULT NULL,
  `country` VARCHAR(32) DEFAULT NULL,
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

DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `logo` VARCHAR(255) DEFAULT NULL,
  `intro` VARCHAR(2048) DEFAULT NULL,
  `found_year` INT DEFAULT NULL,
  `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
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
DROP TABLE IF EXISTS `article_category`;
CREATE TABLE `article_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯栏目';

DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
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
  KEY `idx_source_url` (`source_url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯文章';

DROP TABLE IF EXISTS `video_category`;
CREATE TABLE `video_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频频道';

DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
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

DROP TABLE IF EXISTS `tutorial_category`;
CREATE TABLE `tutorial_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教程分类';

DROP TABLE IF EXISTS `tutorial`;
CREATE TABLE `tutorial` (
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
DROP TABLE IF EXISTS `community_circle`;
CREATE TABLE `community_circle` (
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

DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
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
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
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
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用评论';

DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(16) NOT NULL COMMENT 'robot/article/video/post/tutorial',
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_biz` (`user_id`,`biz_type`,`biz_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一收藏';

DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `follow_type` VARCHAR(16) NOT NULL COMMENT 'user/brand/company/robot',
  `follow_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`,`follow_type`,`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注';

DROP TABLE IF EXISTS `user_like`;
CREATE TABLE `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(16) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`,`biz_type`,`biz_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞';

DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `biz_type` VARCHAR(16) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览历史';

DROP TABLE IF EXISTS `search_history`;
CREATE TABLE `search_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `keyword` VARCHAR(128) NOT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史';

DROP TABLE IF EXISTS `hot_search`;
CREATE TABLE `hot_search` (
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
DROP TABLE IF EXISTS `inquiry`;
CREATE TABLE `inquiry` (
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
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价';

-- ============================================================
-- 消息
-- ============================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
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
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
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

DROP TABLE IF EXISTS `recommend_position`;
CREATE TABLE `recommend_position` (
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

DROP TABLE IF EXISTS `recommend_item`;
CREATE TABLE `recommend_item` (
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
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
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

DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
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

DROP TABLE IF EXISTS `sys_log_login`;
CREATE TABLE `sys_log_login` (
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

DROP TABLE IF EXISTS `sys_log_oper`;
CREATE TABLE `sys_log_oper` (
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

DROP TABLE IF EXISTS `sys_log_error`;
CREATE TABLE `sys_log_error` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `message` VARCHAR(1024) DEFAULT NULL,
  `stack_trace` TEXT,
  `ip` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常日志';
