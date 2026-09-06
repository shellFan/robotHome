-- ============================================================
-- 机器人之家 Phase 4 迁移：搜索性能优化索引
-- 兼容 MySQL 5.6（不使用 FULLTEXT + ngram，需 5.7.6+）
-- 策略：前缀索引支持 LIKE 'keyword%' 前缀匹配查询
-- ============================================================
USE robot_home;

-- ---------------------------
-- 品牌表：name 搜索索引
-- 现状：仅有 idx_initial(initial)，缺少 name 索引
-- 查询：AdminBrandController/BrandServiceImpl/DatabaseSearchServiceImpl
-- ---------------------------
ALTER TABLE `brand` ADD INDEX `idx_brand_name` (`name`(20));

-- ---------------------------
-- 企业表：name 搜索索引
-- 现状：无任何搜索索引（仅 PRIMARY KEY）
-- 查询：AdminCompanyController/CompanyServiceImpl/DatabaseSearchServiceImpl
-- ---------------------------
ALTER TABLE `company` ADD INDEX `idx_company_name` (`name`(20));

-- ---------------------------
-- 视频表：title 搜索索引
-- 现状：仅有 idx_category(category_id)，缺少 title 索引
-- 查询：AdminVideoController/VideoServiceImpl/DatabaseSearchServiceImpl
-- ---------------------------
ALTER TABLE `video` ADD INDEX `idx_video_title` (`title`(20));

-- ---------------------------
-- 教程表：title 搜索索引
-- 现状：仅有 idx_category(category_id)，缺少 title 索引
-- 查询：AdminTutorialController/TutorialServiceImpl/DatabaseSearchServiceImpl
-- ---------------------------
ALTER TABLE `tutorial` ADD INDEX `idx_tutorial_title` (`title`(20));

-- ---------------------------
-- 社区帖子：title 搜索索引
-- 现状：仅有 idx_circle(circle_id) + idx_user(user_id)
-- 查询：AdminCommunityController/CommunityServiceImpl/DatabaseSearchServiceImpl
-- ---------------------------
ALTER TABLE `community_post` ADD INDEX `idx_post_title` (`title`(20));

-- ---------------------------
-- 用户表：nickname 搜索索引
-- 现状：uk_username/uk_phone/uk_openid（唯一键，不支持 LIKE 搜索）
-- 查询：UserServiceImpl 管理后台按昵称搜索
-- ---------------------------
ALTER TABLE `user` ADD INDEX `idx_user_nickname` (`nickname`(20));

-- ---------------------------
-- 询价表：name 搜索索引（管理后台搜索）
-- 现状：idx_status + idx_user
-- 查询：AdminInquiryController 按 name/phone/robotName/companyName 搜索
-- ---------------------------
ALTER TABLE `inquiry` ADD INDEX `idx_inquiry_name` (`name`(20));

-- ---------------------------
-- 评论表：content 搜索索引（管理后台搜索）
-- 现状：idx_biz(biz_type,biz_id) + idx_create_time
-- 查询：AdminCommunityController 按 content 搜索
-- 注意：content 为 TEXT 类型，前缀索引长度受限，取 20 字符
-- ---------------------------
ALTER TABLE `comment` ADD INDEX `idx_comment_content` (`content`(20));

-- ---------------------------
-- 补充：robot 表 model 字段索引
-- 现状：已有 idx_name(name(191))，但 model 字段无索引
-- 查询：DatabaseSearchServiceImpl 按 model 搜索
-- ---------------------------
ALTER TABLE `robot` ADD INDEX `idx_robot_model` (`model`(20));

-- ---------------------------
-- 补充：article 表 summary 前缀索引
-- 现状：已有 idx_title(title(191))，但 summary 无索引
-- 查询：ArticleServiceImpl/DatabaseSearchServiceImpl 按 summary 搜索
-- ---------------------------
ALTER TABLE `article` ADD INDEX `idx_article_summary` (`summary`(20));