-- Phase8 H2 Test Migration (compatible with H2 syntax)

-- 1. Community Post 增强
ALTER TABLE community_post ADD COLUMN company_id BIGINT DEFAULT NULL;
ALTER TABLE community_post ADD COLUMN hot_score INT NOT NULL DEFAULT 0;

-- 2. Robot Question
CREATE TABLE IF NOT EXISTS robot_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    robot_id BIGINT DEFAULT NULL,
    title VARCHAR(200) NOT NULL,
    content CLOB,
    status TINYINT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    follow_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    has_accepted TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_rq_robot_id ON robot_question(robot_id);
CREATE INDEX IF NOT EXISTS idx_rq_user_id ON robot_question(user_id);
CREATE INDEX IF NOT EXISTS idx_rq_status ON robot_question(status);

-- 3. Robot Answer
CREATE TABLE IF NOT EXISTS robot_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content CLOB NOT NULL,
    helpful_count INT NOT NULL DEFAULT 0,
    accepted TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ra_question_id ON robot_answer(question_id);
CREATE INDEX IF NOT EXISTS idx_ra_user_id ON robot_answer(user_id);
CREATE INDEX IF NOT EXISTS idx_ra_status ON robot_answer(status);

-- 4. Question Follow
CREATE TABLE IF NOT EXISTS question_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_qf_user_question UNIQUE (user_id, question_id)
);

-- 5. Answer Helpful
CREATE TABLE IF NOT EXISTS answer_helpful (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_ah_user_answer UNIQUE (user_id, answer_id)
);

-- 6. Inquiry 扩展
ALTER TABLE inquiry ADD COLUMN requirement_type VARCHAR(32) DEFAULT 'SPECIFIC';
ALTER TABLE inquiry ADD COLUMN category VARCHAR(64) DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN usage_scene VARCHAR(64) DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN technical_requirements CLOB DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN need_demo TINYINT DEFAULT 0;
ALTER TABLE inquiry ADD COLUMN need_solution TINYINT DEFAULT 0;
ALTER TABLE inquiry ADD COLUMN lead_score INT DEFAULT 0;
ALTER TABLE inquiry ADD COLUMN assigned_to BIGINT DEFAULT NULL;

-- 7. Search Alias
CREATE TABLE IF NOT EXISTS search_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alias VARCHAR(100) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(200) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sa_alias_type UNIQUE (alias, target_type)
);

-- 8. Search Zero Result
CREATE TABLE IF NOT EXISTS search_zero_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalized_keyword VARCHAR(200) NOT NULL,
    search_count INT NOT NULL DEFAULT 1,
    last_search_time TIMESTAMP NOT NULL,
    suggested_action VARCHAR(32) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_szr_keyword UNIQUE (normalized_keyword)
);

-- 9. Robot Quality Score
CREATE TABLE IF NOT EXISTS robot_quality_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id BIGINT NOT NULL,
    total_score INT NOT NULL DEFAULT 0,
    basic_info_score INT DEFAULT 0,
    param_score INT DEFAULT 0,
    image_score INT DEFAULT 0,
    video_score INT DEFAULT 0,
    doc_score INT DEFAULT 0,
    price_score INT DEFAULT 0,
    spec_score INT DEFAULT 0,
    contact_score INT DEFAULT 0,
    brand_score INT DEFAULT 0,
    category_score INT DEFAULT 0,
    desc_score INT DEFAULT 0,
    qa_score INT DEFAULT 0,
    review_score INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_rqs_robot_id UNIQUE (robot_id)
);

-- 10. Robot Quality Issue
CREATE TABLE IF NOT EXISTS robot_quality_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id BIGINT NOT NULL,
    issue_type VARCHAR(64) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    severity INT NOT NULL DEFAULT 3,
    status TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL
);
CREATE INDEX IF NOT EXISTS idx_rqi_robot_id ON robot_quality_issue(robot_id);

-- 11. Robot Selection Log
CREATE TABLE IF NOT EXISTS robot_selection_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL,
    category VARCHAR(64) DEFAULT NULL,
    budget_min DECIMAL(12,2) DEFAULT NULL,
    budget_max DECIMAL(12,2) DEFAULT NULL,
    usage VARCHAR(64) DEFAULT NULL,
    result_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT NULL
);

-- Test data for Phase8
INSERT INTO search_alias (alias, target_type, target_id, target_name, status, create_time, update_time) VALUES
('协作机器人', 'robot', 1, '测试机器人', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('工业臂', 'robot', 2, '测试机器人2', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO search_zero_result (normalized_keyword, search_count, last_search_time, suggested_action, create_time, update_time) VALUES
('水下机器人', 15, CURRENT_TIMESTAMP, 'ADD_ROBOT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('农业采摘机器人', 8, CURRENT_TIMESTAMP, 'ADD_ALIAS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);