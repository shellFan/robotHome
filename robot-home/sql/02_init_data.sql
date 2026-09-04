-- ============================================================
-- 机器人之家 初始化数据（RBAC / 配置 / 字典 / 分类 / 圈子 / 推荐位）
-- 说明：管理员账号由后端 AdminDataInitializer 保证存在；此处提供完整角色/菜单/权限与参考字典
-- ============================================================
USE robot_home;

-- ---------------------------
-- 角色
-- ---------------------------
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'ADMIN', '超级管理员', '拥有全部权限', 1, NOW(), NOW(), 0),
(2, 'EDITOR', '内容编辑', '可管理内容类目', 1, NOW(), NOW(), 0);

-- ---------------------------
-- 管理员账号
-- 注意：生产环境密码由 AdminDataInitializer 从环境变量 ADMIN_INIT_PASSWORD 读取
-- 此处 INSERT 仅作为开发/演示用途，首次启动后由 AdminDataInitializer 覆盖
-- ---------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'admin', '82d72c30561fa5b5f7c3c46c72e9e424108283f89c6b697c3c3ab07d07d1fb0a', '超级管理员', '13800000000', 1, NOW(), NOW(), 0);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ---------------------------
-- 菜单（后台）
-- ---------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `permission`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 0, 'Dashboard', 1, '/dashboard', 'dashboard/index', 'dashboard', 'dashboard:view', 1, 1, NOW(), NOW(), 0),
(2, 0, '用户管理', 1, '/user', 'user/index', 'user', NULL, 2, 1, NOW(), NOW(), 0),
(3, 0, '机器人管理', 1, '/robot', 'robot/index', 'robot', NULL, 3, 1, NOW(), NOW(), 0),
(4, 0, '品牌管理', 1, '/brand', 'brand/index', 'brand', NULL, 4, 1, NOW(), NOW(), 0),
(5, 0, '企业管理', 1, '/company', 'company/index', 'company', NULL, 5, 1, NOW(), NOW(), 0),
(6, 0, '资讯管理', 1, '/article', 'article/index', 'article', NULL, 6, 1, NOW(), NOW(), 0),
(7, 0, '视频管理', 1, '/video', 'video/index', 'video', NULL, 7, 1, NOW(), NOW(), 0),
(8, 0, '社区管理', 1, '/community', 'community/index', 'community', NULL, 8, 1, NOW(), NOW(), 0),
(9, 0, '教程管理', 1, '/tutorial', 'tutorial/index', 'tutorial', NULL, 9, 1, NOW(), NOW(), 0),
(10, 0, '询价管理', 1, '/inquiry', 'inquiry/index', 'inquiry', NULL, 10, 1, NOW(), NOW(), 0),
(11, 0, '运营配置', 1, '/operation', 'operation/index', 'operation', NULL, 11, 1, NOW(), NOW(), 0),
(12, 0, '系统管理', 1, '/system', 'system/index', 'system', NULL, 12, 1, NOW(), NOW(), 0),

-- 机器人管理子菜单
(31, 3, '分类管理', 1, '/robot/category', 'robot/category', 'category', 'robot:category', 1, 1, NOW(), NOW(), 0),
(32, 3, '系列管理', 1, '/robot/series', 'robot/series', 'series', 'robot:series', 2, 1, NOW(), NOW(), 0),
(33, 3, '型号管理', 1, '/robot/model', 'robot/model', 'model', 'robot:model', 3, 1, NOW(), NOW(), 0),
(34, 3, '参数模板', 1, '/robot/template', 'robot/template', 'template', 'robot:template', 4, 1, NOW(), NOW(), 0),
(35, 3, '图片管理', 1, '/robot/image', 'robot/image', 'image', 'robot:image', 5, 1, NOW(), NOW(), 0),
(36, 3, '视频管理', 1, '/robot/video', 'robot/video', 'video', 'robot:video', 6, 1, NOW(), NOW(), 0),

-- 资讯子菜单
(61, 6, '栏目管理', 1, '/article/category', 'article/category', 'category', 'article:category', 1, 1, NOW(), NOW(), 0),
(62, 6, '文章管理', 1, '/article/list', 'article/list', 'list', 'article:list', 2, 1, NOW(), NOW(), 0),

-- 视频子菜单
(71, 7, '频道管理', 1, '/video/category', 'video/category', 'category', 'video:category', 1, 1, NOW(), NOW(), 0),
(72, 7, '视频管理', 1, '/video/list', 'video/list', 'list', 'video:list', 2, 1, NOW(), NOW(), 0),

-- 社区子菜单
(81, 8, '圈子管理', 1, '/community/circle', 'community/circle', 'circle', 'community:circle', 1, 1, NOW(), NOW(), 0),
(82, 8, '帖子管理', 1, '/community/post', 'community/post', 'post', 'community:post', 2, 1, NOW(), NOW(), 0),
(83, 8, '评论管理', 1, '/community/comment', 'community/comment', 'comment', 'community:comment', 3, 1, NOW(), NOW(), 0),

-- 教程子菜单
(91, 9, '分类管理', 1, '/tutorial/category', 'tutorial/category', 'category', 'tutorial:category', 1, 1, NOW(), NOW(), 0),
(92, 9, '教程管理', 1, '/tutorial/list', 'tutorial/list', 'list', 'tutorial:list', 2, 1, NOW(), NOW(), 0),

-- 运营配置子菜单
(111, 11, 'Banner管理', 1, '/operation/banner', 'operation/banner', 'banner', 'operation:banner', 1, 1, NOW(), NOW(), 0),
(112, 11, '推荐位管理', 1, '/operation/recommend', 'operation/recommend', 'recommend', 'operation:recommend', 2, 1, NOW(), NOW(), 0),

-- 系统管理子菜单
(121, 12, '管理员', 1, '/system/admin', 'system/admin', 'admin', 'system:admin', 1, 1, NOW(), NOW(), 0),
(122, 12, '角色', 1, '/system/role', 'system/role', 'role', 'system:role', 2, 1, NOW(), NOW(), 0),
(123, 12, '菜单', 1, '/system/menu', 'system/menu', 'menu', 'system:menu', 3, 1, NOW(), NOW(), 0),
(124, 12, '权限', 1, '/system/permission', 'system/permission', 'permission', 'system:permission', 4, 1, NOW(), NOW(), 0),
(125, 12, '日志', 1, '/system/log', 'system/log', 'log', 'system:log', 5, 1, NOW(), NOW(), 0),
(126, 12, '字典', 1, '/system/dict', 'system/dict', 'dict', 'system:dict', 6, 1, NOW(), NOW(), 0),
(127, 12, '配置', 1, '/system/config', 'system/config', 'config', 'system:config', 7, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 权限标识
-- ---------------------------
INSERT INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'robot:add', '机器人新增', NULL, NOW(), NOW(), 0),
(2, 'robot:edit', '机器人编辑', NULL, NOW(), NOW(), 0),
(3, 'robot:delete', '机器人删除', NULL, NOW(), NOW(), 0),
(4, 'brand:add', '品牌新增', NULL, NOW(), NOW(), 0),
(5, 'brand:edit', '品牌编辑', NULL, NOW(), NOW(), 0),
(6, 'brand:delete', '品牌删除', NULL, NOW(), NOW(), 0),
(7, 'company:add', '企业新增', NULL, NOW(), NOW(), 0),
(8, 'company:edit', '企业编辑', NULL, NOW(), NOW(), 0),
(9, 'company:delete', '企业删除', NULL, NOW(), NOW(), 0),
(10, 'article:add', '资讯新增', NULL, NOW(), NOW(), 0),
(11, 'article:edit', '资讯编辑', NULL, NOW(), NOW(), 0),
(12, 'article:delete', '资讯删除', NULL, NOW(), NOW(), 0),
(13, 'article:publish', '资讯发布', NULL, NOW(), NOW(), 0),
(14, 'video:add', '视频新增', NULL, NOW(), NOW(), 0),
(15, 'video:edit', '视频编辑', NULL, NOW(), NOW(), 0),
(16, 'video:delete', '视频删除', NULL, NOW(), NOW(), 0),
(17, 'tutorial:add', '教程新增', NULL, NOW(), NOW(), 0),
(18, 'tutorial:edit', '教程编辑', NULL, NOW(), NOW(), 0),
(19, 'tutorial:delete', '教程删除', NULL, NOW(), NOW(), 0),
(20, 'community:post:audit', '帖子审核', NULL, NOW(), NOW(), 0),
(21, 'community:comment:delete', '评论删除', NULL, NOW(), NOW(), 0),
(22, 'inquiry:handle', '询价跟进', NULL, NOW(), NOW(), 0),
(23, 'operation:banner:edit', 'Banner管理', NULL, NOW(), NOW(), 0),
(24, 'operation:recommend:edit', '推荐位管理', NULL, NOW(), NOW(), 0),
(25, 'system:admin', '管理员管理', NULL, NOW(), NOW(), 0),
(26, 'system:role', '角色管理', NULL, NOW(), NOW(), 0),
(27, 'system:menu', '菜单管理', NULL, NOW(), NOW(), 0),
(28, 'system:permission', '权限管理', NULL, NOW(), NOW(), 0),
(29, 'system:dict', '字典管理', NULL, NOW(), NOW(), 0),
(30, 'system:config', '配置管理', NULL, NOW(), NOW(), 0),
(31, 'robot:list', '机器人查看', NULL, NOW(), NOW(), 0),
(32, 'brand:list', '品牌查看', NULL, NOW(), NOW(), 0),
(33, 'company:list', '企业查看', NULL, NOW(), NOW(), 0),
(34, 'article:list', '资讯查看', NULL, NOW(), NOW(), 0),
(35, 'video:list', '视频查看', NULL, NOW(), NOW(), 0),
(36, 'tutorial:list', '教程查看', NULL, NOW(), NOW(), 0),
(37, 'inquiry:list', '询价查看', NULL, NOW(), NOW(), 0),
(38, 'community:circle', '圈子管理', NULL, NOW(), NOW(), 0),
(39, 'community:post', '帖子管理', NULL, NOW(), NOW(), 0),
(40, 'community:comment', '评论管理', NULL, NOW(), NOW(), 0),
(41, 'operation:banner', 'Banner查看', NULL, NOW(), NOW(), 0),
(42, 'operation:recommend', '推荐位查看', NULL, NOW(), NOW(), 0),
(43, 'system:log', '日志查看', NULL, NOW(), NOW(), 0),
(44, 'user:list', '前台用户查看', NULL, NOW(), NOW(), 0),
(45, 'user:edit', '前台用户编辑', NULL, NOW(), NOW(), 0),
(46, 'dashboard:view', '仪表盘查看', NULL, NOW(), NOW(), 0);

-- ADMIN 角色关联所有菜单与权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE `deleted` = 0;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- EDITOR 角色：内容相关菜单与权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2,1),(2,6),(2,7),(2,8),(2,9),(2,61),(2,62),(2,71),(2,72),(2,81),(2,82),(2,83),(2,91),(2,92);
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2,10),(2,11),(2,12),(2,13),(2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),
-- 内容编辑另需具备各模块的查看权限
(2,31),(2,32),(2,33),(2,34),(2,35),(2,36),(2,37),(2,38),(2,39),(2,40),(2,41),(2,42);

-- ---------------------------
-- 系统配置
-- ---------------------------
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`, `create_time`, `update_time`, `deleted`) VALUES
('site_name', '机器人之家', '站点名称', NOW(), NOW(), 0),
('site_description', '机器人行业的懂车帝+汽车之家：产品库/品牌库/企业库/资讯/视频/社区/参数对比/排行榜/询价采购', 'SEO描述', NOW(), NOW(), 0),
('site_keywords', '机器人,人形机器人,机器狗,工业机器人,服务机器人,机器人评测,机器人参数对比', 'SEO关键词', NOW(), NOW(), 0),
('icp', '示例ICP备00000000号', '备案号', NOW(), NOW(), 0),
('contact_email', 'admin@robot-home.com', '联系邮箱', NOW(), NOW(), 0);

-- ---------------------------
-- 数据字典
-- ---------------------------
INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `create_time`) VALUES
('robot_status', '在售', '1', 1, 1, NOW()),
('robot_status', '预售', '2', 2, 1, NOW()),
('robot_status', '停产', '3', 3, 1, NOW()),
('inquiry_status', '待处理', '1', 1, 1, NOW()),
('inquiry_status', '处理中', '2', 2, 1, NOW()),
('inquiry_status', '已联系', '3', 3, 1, NOW()),
('inquiry_status', '已成交', '4', 4, 1, NOW()),
('inquiry_status', '已关闭', '5', 5, 1, NOW()),
('customer_type', '个人', '1', 1, 1, NOW()),
('customer_type', '企业', '2', 2, 1, NOW()),
('post_status', '待审核', '0', 1, 1, NOW()),
('post_status', '正常', '1', 2, 1, NOW()),
('post_status', '下架', '2', 3, 1, NOW());

-- ---------------------------
-- 机器人分类（可扩展）
-- ---------------------------
INSERT INTO `robot_category` (`id`, `parent_id`, `name`, `level`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 0, '人形机器人', 1, 1, 1, NOW(), NOW(), 0),
(2, 0, '四足机器人', 1, 2, 1, NOW(), NOW(), 0),
(3, 0, '服务机器人', 1, 3, 1, NOW(), NOW(), 0),
(4, 0, '家庭机器人', 1, 4, 1, NOW(), NOW(), 0),
(5, 0, '工业机器人', 1, 5, 1, NOW(), NOW(), 0),
(6, 0, '医疗机器人', 1, 6, 1, NOW(), NOW(), 0),
(7, 0, '物流机器人', 1, 7, 1, NOW(), NOW(), 0),
(8, 0, '农业机器人', 1, 8, 1, NOW(), NOW(), 0),
(9, 0, '教育开发机器人', 1, 9, 1, NOW(), NOW(), 0),
-- 人形子分类
(11, 1, '通用人形', 2, 1, 1, NOW(), NOW(), 0),
(12, 1, '工业人形', 2, 2, 1, NOW(), NOW(), 0),
(13, 1, '家庭人形', 2, 3, 1, NOW(), NOW(), 0),
(14, 1, '教育人形', 2, 4, 1, NOW(), NOW(), 0),
(15, 1, '科研人形', 2, 5, 1, NOW(), NOW(), 0),
-- 四足子分类
(21, 2, '机器狗', 2, 1, 1, NOW(), NOW(), 0),
(22, 2, '巡检机器人', 2, 2, 1, NOW(), NOW(), 0),
(23, 2, '消防机器人', 2, 3, 1, NOW(), NOW(), 0),
(24, 2, '教育机器狗', 2, 4, 1, NOW(), NOW(), 0),
-- 服务子分类
(31, 3, '送餐机器人', 2, 1, 1, NOW(), NOW(), 0),
(32, 3, '配送机器人', 2, 2, 1, NOW(), NOW(), 0),
(33, 3, '酒店机器人', 2, 3, 1, NOW(), NOW(), 0),
(34, 3, '迎宾机器人', 2, 4, 1, NOW(), NOW(), 0),
(35, 3, '导览机器人', 2, 5, 1, NOW(), NOW(), 0),
(36, 3, '消毒机器人', 2, 6, 1, NOW(), NOW(), 0),
(37, 3, '清洁机器人', 2, 7, 1, NOW(), NOW(), 0),
-- 家庭子分类
(41, 4, '扫地机器人', 2, 1, 1, NOW(), NOW(), 0),
(42, 4, 'AI宠物', 2, 2, 1, NOW(), NOW(), 0),
(43, 4, '陪伴机器人', 2, 3, 1, NOW(), NOW(), 0),
(44, 4, '家庭助手', 2, 4, 1, NOW(), NOW(), 0),
(45, 4, '教育机器人', 2, 5, 1, NOW(), NOW(), 0),
-- 工业子分类
(51, 5, '机械臂', 2, 1, 1, NOW(), NOW(), 0),
(52, 5, '协作机器人', 2, 2, 1, NOW(), NOW(), 0),
(53, 5, '焊接机器人', 2, 3, 1, NOW(), NOW(), 0),
(54, 5, '搬运机器人', 2, 4, 1, NOW(), NOW(), 0),
(55, 5, '装配机器人', 2, 5, 1, NOW(), NOW(), 0),
-- 教育开发子分类
(91, 9, 'ESP32', 2, 1, 1, NOW(), NOW(), 0),
(92, 9, 'Arduino', 2, 2, 1, NOW(), NOW(), 0),
(93, 9, 'STM32', 2, 3, 1, NOW(), NOW(), 0),
(94, 9, 'ROS', 2, 4, 1, NOW(), NOW(), 0),
(95, 9, 'ROS2', 2, 5, 1, NOW(), NOW(), 0),
(96, 9, 'Raspberry Pi', 2, 6, 1, NOW(), NOW(), 0),
(97, 9, 'Jetson', 2, 7, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 资讯栏目
-- ---------------------------
INSERT INTO `article_category` (`id`, `name`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '推荐', 1, 1, NOW(), NOW(), 0),
(2, '行业', 2, 1, NOW(), NOW(), 0),
(3, '新品', 3, 1, NOW(), NOW(), 0),
(4, '人形机器人', 4, 1, NOW(), NOW(), 0),
(5, '机器狗', 5, 1, NOW(), NOW(), 0),
(6, '工业机器人', 6, 1, NOW(), NOW(), 0),
(7, '服务机器人', 7, 1, NOW(), NOW(), 0),
(8, 'AI', 8, 1, NOW(), NOW(), 0),
(9, '技术', 9, 1, NOW(), NOW(), 0),
(10, '融资', 10, 1, NOW(), NOW(), 0),
(11, '政策', 11, 1, NOW(), NOW(), 0),
(12, '企业', 12, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 视频频道
-- ---------------------------
INSERT INTO `video_category` (`id`, `name`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '推荐', 1, 1, NOW(), NOW(), 0),
(2, '测评', 2, 1, NOW(), NOW(), 0),
(3, '实拍', 3, 1, NOW(), NOW(), 0),
(4, '产品介绍', 4, 1, NOW(), NOW(), 0),
(5, '发布会', 5, 1, NOW(), NOW(), 0),
(6, '技术', 6, 1, NOW(), NOW(), 0),
(7, '用户作品', 7, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 教程分类
-- ---------------------------
INSERT INTO `tutorial_category` (`id`, `name`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '新手入门', 1, 1, NOW(), NOW(), 0),
(2, 'ESP32', 2, 1, NOW(), NOW(), 0),
(3, 'Arduino', 3, 1, NOW(), NOW(), 0),
(4, 'STM32', 4, 1, NOW(), NOW(), 0),
(5, 'ROS', 5, 1, NOW(), NOW(), 0),
(6, 'ROS2', 6, 1, NOW(), NOW(), 0),
(7, 'Raspberry Pi', 7, 1, NOW(), NOW(), 0),
(8, 'Jetson', 8, 1, NOW(), NOW(), 0),
(9, 'AI', 9, 1, NOW(), NOW(), 0),
(10, '大模型', 10, 1, NOW(), NOW(), 0),
(11, '计算机视觉', 11, 1, NOW(), NOW(), 0),
(12, '机器人语音', 12, 1, NOW(), NOW(), 0),
(13, '机械结构', 13, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 社区圈子
-- ---------------------------
INSERT INTO `community_circle` (`id`, `name`, `description`, `post_count`, `follow_count`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '人形机器人', '讨论通用人形、工业人形、家庭人形机器人', 0, 0, 1, 1, NOW(), NOW(), 0),
(2, '宇树', '宇树科技产品与生态讨论', 0, 0, 2, 1, NOW(), NOW(), 0),
(3, '机器狗', '四足机器狗玩家聚集地', 0, 0, 3, 1, NOW(), NOW(), 0),
(4, 'ROS', 'ROS/ROS2 开发与教程分享', 0, 0, 4, 1, NOW(), NOW(), 0),
(5, 'ROS2', 'ROS2 新一代机器人操作系统', 0, 0, 5, 1, NOW(), NOW(), 0),
(6, 'ESP32', 'ESP32 机器人开发', 0, 0, 6, 1, NOW(), NOW(), 0),
(7, 'AI机器人', '大模型与AI赋能机器人', 0, 0, 7, 1, NOW(), NOW(), 0),
(8, '机器人开发', '从零开始做机器人', 0, 0, 8, 1, NOW(), NOW(), 0),
(9, '机器人创业', '机器人行业创业与融资', 0, 0, 9, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 推荐位
-- ---------------------------
INSERT INTO `recommend_position` (`id`, `code`, `name`, `biz_type`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'hot_robot', '热门机器人', 'robot', 1, NOW(), NOW(), 0),
(2, 'new_robot', '新品机器人', 'robot', 1, NOW(), NOW(), 0),
(3, 'recommend_article', '推荐资讯', 'article', 1, NOW(), NOW(), 0),
(4, 'recommend_video', '推荐视频', 'video', 1, NOW(), NOW(), 0),
(5, 'recommend_company', '推荐企业', 'company', 1, NOW(), NOW(), 0);

-- ---------------------------
-- 热搜种子
-- ---------------------------
INSERT INTO `hot_search` (`keyword`, `search_count`, `sort`, `create_time`) VALUES
('宇树 H1', 320, 1, NOW()),
('人形机器人', 280, 2, NOW()),
('机器狗', 210, 3, NOW()),
('工业机器人', 180, 4, NOW()),
('扫地机器人', 150, 5, NOW()),
('ROS2', 120, 6, NOW()),
('协作机器人', 90, 7, NOW());
