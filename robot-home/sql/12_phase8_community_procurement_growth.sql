-- ============================================================
-- Phase8: Community + Procurement Growth
-- 从Phase7 DB执行此脚本必须成功
-- ============================================================

-- 1. Community Post 增强: 添加 company_id + hot_score
ALTER TABLE community_post ADD COLUMN company_id BIGINT DEFAULT NULL COMMENT '关联企业ID' AFTER brand_id;
ALTER TABLE community_post ADD COLUMN hot_score INT NOT NULL DEFAULT 0 COMMENT '热度分' AFTER view_count;
ALTER TABLE community_post ADD INDEX idx_company_id (company_id);
ALTER TABLE community_post ADD INDEX idx_hot_score (hot_score);

-- 2. Robot Question 问答
CREATE TABLE IF NOT EXISTS robot_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '提问者ID',
    robot_id BIGINT DEFAULT NULL COMMENT '关联机器人ID',
    title VARCHAR(200) NOT NULL COMMENT '问题标题',
    content TEXT COMMENT '问题详情',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审 1正常 2隐藏 3删除',
    answer_count INT NOT NULL DEFAULT 0 COMMENT '回答数',
    follow_count INT NOT NULL DEFAULT 0 COMMENT '关注数',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    has_accepted TINYINT NOT NULL DEFAULT 0 COMMENT '是否有采纳答案',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_robot_id (robot_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人问答-问题';

-- 3. Robot Answer 回答
CREATE TABLE IF NOT EXISTS robot_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '问题ID',
    user_id BIGINT NOT NULL COMMENT '回答者ID',
    content TEXT NOT NULL COMMENT '回答内容',
    helpful_count INT NOT NULL DEFAULT 0 COMMENT '有帮助数',
    accepted TINYINT NOT NULL DEFAULT 0 COMMENT '是否被采纳',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审 1正常 2隐藏 3删除',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_question_id (question_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_accepted (accepted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人问答-回答';

-- 4. Question Follow 关注问题
CREATE TABLE IF NOT EXISTS question_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT NULL,
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题关注';

-- 5. Answer Helpful 回答点赞
CREATE TABLE IF NOT EXISTS answer_helpful (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT NULL,
    UNIQUE KEY uk_user_answer (user_id, answer_id),
    INDEX idx_answer_id (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回答有帮助';

-- 6. Inquiry 扩展: 采购需求增强
ALTER TABLE inquiry ADD COLUMN requirement_type VARCHAR(32) DEFAULT 'SPECIFIC' COMMENT '需求类型: SPECIFIC具体型号/OPEN开放式' AFTER inquiry_type;
ALTER TABLE inquiry ADD COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '机器人分类(开放式需求)' AFTER requirement_type;
ALTER TABLE inquiry ADD COLUMN usage_scene VARCHAR(64) DEFAULT NULL COMMENT '使用场景' AFTER category;
ALTER TABLE inquiry ADD COLUMN technical_requirements TEXT DEFAULT NULL COMMENT '技术要求' AFTER usage_scene;
ALTER TABLE inquiry ADD COLUMN need_demo TINYINT DEFAULT 0 COMMENT '需要演示' AFTER technical_requirements;
ALTER TABLE inquiry ADD COLUMN need_solution TINYINT DEFAULT 0 COMMENT '需要方案' AFTER need_demo;
ALTER TABLE inquiry ADD COLUMN lead_score INT DEFAULT 0 COMMENT '线索评分' AFTER need_solution;
ALTER TABLE inquiry ADD COLUMN assigned_to BIGINT DEFAULT NULL COMMENT '分配给(Admin用户ID)' AFTER lead_score;
ALTER TABLE inquiry ADD INDEX idx_requirement_type (requirement_type);

-- 7. Search Alias 搜索别名
CREATE TABLE IF NOT EXISTS search_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alias VARCHAR(100) NOT NULL COMMENT '别名',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型: robot/brand/company',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(200) NOT NULL COMMENT '目标名称(冗余)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_alias_type (alias, target_type),
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索别名';

-- 8. Search Zero Result 零结果搜索
CREATE TABLE IF NOT EXISTS search_zero_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalized_keyword VARCHAR(190) NOT NULL COMMENT '标准化关键词',
    search_count INT NOT NULL DEFAULT 1 COMMENT '搜索次数',
    last_search_time DATETIME NOT NULL COMMENT '最后搜索时间',
    suggested_action VARCHAR(32) DEFAULT NULL COMMENT '建议操作: ADD_ALIAS/ADD_ROBOT/ADD_BRAND/IGNORE',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    UNIQUE KEY uk_keyword (normalized_keyword),
    INDEX idx_search_count (search_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零结果搜索';

-- 9. Robot Quality Score 数据质量评分
CREATE TABLE IF NOT EXISTS robot_quality_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id BIGINT NOT NULL COMMENT '机器人ID',
    total_score INT NOT NULL DEFAULT 0 COMMENT '质量总分(0-130)',
    basic_info_score INT DEFAULT 0 COMMENT '基本信息分(0-10)',
    param_score INT DEFAULT 0 COMMENT '参数分(0-10)',
    image_score INT DEFAULT 0 COMMENT '图片分(0-10)',
    video_score INT DEFAULT 0 COMMENT '视频分(0-10)',
    doc_score INT DEFAULT 0 COMMENT '文档分(0-10)',
    price_score INT DEFAULT 0 COMMENT '价格分(0-10)',
    spec_score INT DEFAULT 0 COMMENT '规格分(0-10)',
    contact_score INT DEFAULT 0 COMMENT '联系分(0-10)',
    brand_score INT DEFAULT 0 COMMENT '品牌分(0-10)',
    category_score INT DEFAULT 0 COMMENT '分类分(0-10)',
    desc_score INT DEFAULT 0 COMMENT '描述分(0-10)',
    qa_score INT DEFAULT 0 COMMENT 'Q&A分(0-10)',
    review_score INT DEFAULT 0 COMMENT '评价分(0-10)',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    UNIQUE KEY uk_robot_id (robot_id),
    INDEX idx_total_score (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人数据质量评分';

-- 10. Robot Quality Issue 数据质量问题
CREATE TABLE IF NOT EXISTS robot_quality_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id BIGINT NOT NULL COMMENT '机器人ID',
    issue_type VARCHAR(64) NOT NULL COMMENT '问题类型: MISSING_BASIC_INFO/MISSING_IMAGES/MISSING_VIDEO/MISSING_PRICE/MISSING_PARAMS/MISSING_CONTACT/MISSING_CATEGORY/LOW_QUALITY_DESC',
    description VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
    severity INT NOT NULL DEFAULT 3 COMMENT '严重度: 1高 2中 3低',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理 2忽略',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    INDEX idx_robot_id (robot_id),
    INDEX idx_issue_type (issue_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人数据质量问题';

-- 11. Robot Selection Log 选型记录(用于统计)
CREATE TABLE IF NOT EXISTS robot_selection_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID(可选)',
    category VARCHAR(64) DEFAULT NULL COMMENT '分类',
    budget_min DECIMAL(12,2) DEFAULT NULL,
    budget_max DECIMAL(12,2) DEFAULT NULL,
    `usage` VARCHAR(64) DEFAULT NULL COMMENT '使用场景',
    result_count INT DEFAULT 0 COMMENT '结果数',
    create_time DATETIME DEFAULT NULL,
    INDEX idx_category (category),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选型记录';