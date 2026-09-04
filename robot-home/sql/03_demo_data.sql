-- ============================================================
-- 机器人之家 演示数据（示例数据，is_example=1）
-- 说明：以下为可用于项目展示的示例数据，关键商业参数均为示例，不代表真实产品规格
-- ============================================================
USE robot_home;

-- ---------------------------
-- 企业（12 家）
-- ---------------------------
INSERT INTO `company` (`id`, `name`, `logo`, `intro`, `found_year`, `region`, `website`, `contact_phone`, `contact_email`, `address`, `tags`, `brand_count`, `product_count`, `hot_score`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '宇树科技', 'https://picsum.photos/seed/unitree/200/200', '全球领先的四足机器人与通用人形机器人公司，专注于运动控制与机器人本体研发。', 2016, '杭州', 'https://www.unitree.com', '0571-88888888', 'bd@unitree.com', '浙江省杭州市余杭区', '["四足机器人","人形机器人","运动控制"]', 1, 6, 980, 1, 1, NOW(), NOW(), 0),
(2, '优必选科技', 'https://picsum.photos/seed/ubtech/200/200', '专注于人形机器人与智能服务机器人，Walker 系列面向家庭与工业场景。', 2012, '深圳', 'https://www.ubtrobot.com', '0755-88888888', 'bd@ubtrobot.com', '广东省深圳市南山区', '["人形机器人","服务机器人"]', 1, 3, 870, 2, 1, NOW(), NOW(), 0),
(3, '波士顿动力', 'https://picsum.photos/seed/bd/200/200', '示例数据：以 Atlas 人形机器人与 Spot 机器狗闻名，运动能力业界领先。', 1992, '美国', 'https://www.bostondynamics.com', '', '', '美国马萨诸塞州', '["人形机器人","四足机器人"]', 1, 2, 760, 3, 1, NOW(), NOW(), 0),
(4, '大疆创新', 'https://picsum.photos/seed/dji/200/200', '全球领先的无人机与影像技术公司，亦布局教育机器人。', 2006, '深圳', 'https://www.dji.com', '0755-88888888', 'bd@dji.com', '广东省深圳市南山区', '["无人机","教育机器人"]', 1, 1, 650, 4, 1, NOW(), NOW(), 0),
(5, '小米集团', 'https://picsum.photos/seed/xiaomi/200/200', '以 CyberDog 铁蛋与扫地机器人切入消费级机器人市场。', 2010, '北京', 'https://www.mi.com', '010-88888888', 'bd@mi.com', '北京市海淀区', '["消费机器人","扫地机器人"]', 1, 3, 720, 5, 1, NOW(), NOW(), 0),
(6, '傅利叶智能', 'https://picsum.photos/seed/fourier/200/200', '专注于康复机器人与通用人形机器人 GR 系列。', 2015, '上海', 'https://www.fourierintelligence.com', '021-88888888', 'bd@fourierinc.com', '上海市浦东新区', '["人形机器人","康复机器人"]', 1, 1, 540, 6, 1, NOW(), NOW(), 0),
(7, '智元机器人', 'https://picsum.photos/seed/agibot/200/200', '远征 A1 通用人形机器人，面向工业与服务业。', 2023, '上海', 'https://www.agibot.com', '021-88888888', 'bd@agibot.com', '上海市', '["人形机器人"]', 1, 1, 600, 7, 1, NOW(), NOW(), 0),
(8, '达闼科技', 'https://picsum.photos/seed/cloudminds/200/200', '云端智能机器人运营商，提供迎宾、配送等服务机器人。', 2015, '北京', 'https://www.cloudminds.com', '010-88888888', 'bd@cloudminds.com', '北京市', '["服务机器人","云端智能"]', 1, 3, 480, 8, 1, NOW(), NOW(), 0),
(9, '新松机器人', 'https://picsum.photos/seed/siasun/200/200', '中国机器人产业领军企业，工业机械臂与自动化解决方案。', 2000, '沈阳', 'https://www.siasun.com', '024-88888888', 'bd@siasun.com', '辽宁省沈阳市', '["工业机器人","机械臂"]', 1, 2, 700, 9, 1, NOW(), NOW(), 0),
(10, '埃斯顿自动化', 'https://picsum.photos/seed/estun/200/200', '国产工业机器人领军企业，六轴机器人出货量领先。', 1993, '南京', 'https://www.estun.com', '025-88888888', 'bd@estun.com', '江苏省南京市', '["工业机器人","协作机器人"]', 1, 2, 560, 10, 1, NOW(), NOW(), 0),
(11, '科沃斯', 'https://picsum.photos/seed/ecovacs/200/200', '家用服务机器人龙头，扫地机器人与空气净化机器人。', 1998, '苏州', 'https://www.ecovacs.com', '0512-88888888', 'bd@ecovacs.com', '江苏省苏州市', '["扫地机器人","家庭机器人"]', 1, 2, 680, 11, 1, NOW(), NOW(), 0),
(12, '特斯拉', 'https://picsum.photos/seed/tesla/200/200', '示例数据：Optimus 人形机器人项目，面向制造与家庭场景。', 2003, '美国', 'https://www.tesla.com', '', '', '美国得克萨斯州', '["人形机器人","自动驾驶"]', 1, 1, 800, 12, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 品牌（12 个，initial 为首字母）
-- ---------------------------
INSERT INTO `brand` (`id`, `name`, `logo`, `company_id`, `intro`, `found_year`, `country`, `website`, `initial`, `hot_score`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'Unitree 宇树', 'https://picsum.photos/seed/unitree/200/200', 1, '四足与人形机器人先锋', 2016, '中国', 'https://www.unitree.com', 'U', 980, 1, 1, NOW(), NOW(), 0),
(2, 'UBTECH 优必选', 'https://picsum.photos/seed/ubtech/200/200', 2, '人形机器人领军品牌', 2012, '中国', 'https://www.ubtrobot.com', 'U', 870, 2, 1, NOW(), NOW(), 0),
(3, 'Boston Dynamics', 'https://picsum.photos/seed/bd/200/200', 3, '示例：运动控制标杆', 1992, '美国', 'https://www.bostondynamics.com', 'B', 760, 3, 1, NOW(), NOW(), 0),
(4, 'DJI 大疆', 'https://picsum.photos/seed/dji/200/200', 4, '无人机与教育机器人', 2006, '中国', 'https://www.dji.com', 'D', 650, 4, 1, NOW(), NOW(), 0),
(5, 'Xiaomi 小米', 'https://picsum.photos/seed/xiaomi/200/200', 5, '消费级机器人', 2010, '中国', 'https://www.mi.com', 'X', 720, 5, 1, NOW(), NOW(), 0),
(6, 'Fourier 傅利叶', 'https://picsum.photos/seed/fourier/200/200', 6, '康复与人形机器人', 2015, '中国', 'https://www.fourierintelligence.com', 'F', 540, 6, 1, NOW(), NOW(), 0),
(7, 'AgiBot 智元', 'https://picsum.photos/seed/agibot/200/200', 7, '通用人形机器人', 2023, '中国', 'https://www.agibot.com', 'A', 600, 7, 1, NOW(), NOW(), 0),
(8, 'CloudMinds 达闼', 'https://picsum.photos/seed/cloudminds/200/200', 8, '云端智能服务机器人', 2015, '中国', 'https://www.cloudminds.com', 'C', 480, 8, 1, NOW(), NOW(), 0),
(9, 'SIASUN 新松', 'https://picsum.photos/seed/siasun/200/200', 9, '工业机器人', 2000, '中国', 'https://www.siasun.com', 'S', 700, 9, 1, NOW(), NOW(), 0),
(10, 'ESTUN 埃斯顿', 'https://picsum.photos/seed/estun/200/200', 10, '工业机器人', 1993, '中国', 'https://www.estun.com', 'E', 560, 10, 1, NOW(), NOW(), 0),
(11, 'ECOVACS 科沃斯', 'https://picsum.photos/seed/ecovacs/200/200', 11, '家庭服务机器人', 1998, '中国', 'https://www.ecovacs.com', 'E', 680, 11, 1, NOW(), NOW(), 0),
(12, 'Tesla 特斯拉', 'https://picsum.photos/seed/tesla/200/200', 12, '示例：Optimus', 2003, '美国', 'https://www.tesla.com', 'T', 800, 12, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 机器人型号（24 台，示例数据）
-- ---------------------------
INSERT INTO `robot` (`id`, `category_id`, `series_id`, `brand_id`, `name`, `model`, `subtitle`, `guide_price`, `market_price`, `status`, `release_date`, `cover_image`, `images`, `video_count`, `main_params`, `hot_score`, `view_count`, `favorite_count`, `compare_count`, `inquiry_count`, `comment_count`, `score`, `is_example`, `create_time`, `update_time`, `deleted`) VALUES
(1, 11, NULL, 1, '宇树 H1', 'H1', '全尺寸通用人形机器人，运动能力业界领先', 90000.00, 90000.00, 1, '2023-08-01', 'https://picsum.photos/seed/robot1/800/600', '["https://picsum.photos/seed/robot1a/800/600","https://picsum.photos/seed/robot1b/800/600"]', 3, '{"身高":"1800mm","重量":"47kg","续航":"2h","最大速度":"1.5m/s"}', 960, 12000, 800, 320, 210, 56, 9.2, 1, NOW(), NOW(), 0),
(2, 11, NULL, 1, '宇树 G1', 'G1', '高性价比入门级人形机器人', 35000.00, 35000.00, 1, '2024-05-01', 'https://picsum.photos/seed/robot2/800/600', '["https://picsum.photos/seed/robot2a/800/600"]', 2, '{"身高":"1270mm","重量":"35kg","续航":"2h","最大速度":"2m/s"}', 890, 9000, 700, 280, 180, 48, 9.0, 1, NOW(), NOW(), 0),
(3, 11, NULL, 2, '优必选 Walker X', 'Walker X', '大型双足人形机器人，具备交互能力', 598000.00, 598000.00, 1, '2021-07-01', 'https://picsum.photos/seed/robot3/800/600', '["https://picsum.photos/seed/robot3a/800/600"]', 2, '{"身高":"1645mm","重量":"74kg","续航":"2h","自由度":"41"}', 820, 7000, 520, 200, 150, 40, 8.8, 1, NOW(), NOW(), 0),
(4, 12, NULL, 2, '优必选 Walker S', 'Walker S', '工业人形机器人，面向智能制造', 680000.00, 680000.00, 2, '2024-01-01', 'https://picsum.photos/seed/robot4/800/600', NULL, 1, '{"身高":"1720mm","重量":"78kg","负载":"15kg"}', 780, 5000, 400, 160, 120, 30, 8.6, 1, NOW(), NOW(), 0),
(5, 11, NULL, 6, '傅利叶 GR-1', 'GR-1', '通用人形机器人，康复基因', 268000.00, 268000.00, 1, '2023-09-01', 'https://picsum.photos/seed/robot5/800/600', NULL, 1, '{"身高":"1650mm","重量":"55kg","自由度":"44"}', 700, 4800, 360, 140, 100, 28, 8.5, 1, NOW(), NOW(), 0),
(6, 11, NULL, 7, '智元 远征 A1', 'A1', '面向量产的通用人形机器人', 0.00, 0.00, 2, '2023-08-01', 'https://picsum.photos/seed/robot6/800/600', NULL, 1, '{"身高":"1750mm","重量":"55kg","最大速度":"2m/s"}', 750, 5200, 380, 150, 110, 26, 8.7, 1, NOW(), NOW(), 0),
(7, 11, NULL, 3, '波士顿动力 Atlas', 'Atlas', '示例：液压人形机器人标杆', 0.00, 0.00, 2, '2021-04-01', 'https://picsum.photos/seed/robot7/800/600', NULL, 2, '{"身高":"1500mm","重量":"89kg","最大速度":"2.5m/s"}', 790, 6000, 420, 180, 90, 35, 9.0, 1, NOW(), NOW(), 0),
(8, 11, NULL, 12, '特斯拉 Optimus', 'Optimus', '示例：量产目标的人形机器人', 0.00, 0.00, 2, '2022-09-01', 'https://picsum.photos/seed/robot8/800/600', NULL, 1, '{"身高":"1730mm","重量":"73kg","负载":"20kg"}', 800, 6500, 450, 190, 95, 33, 8.9, 1, NOW(), NOW(), 0),
(9, 21, NULL, 1, '宇树 Go2', 'Go2', 'AI 机器狗，陪伴与开发首选', 1600.00, 1600.00, 1, '2023-07-01', 'https://picsum.photos/seed/robot9/800/600', '["https://picsum.photos/seed/robot9a/800/600"]', 4, '{"重量":"15kg","续航":"1.5h","最大速度":"3.5m/s"}', 910, 11000, 900, 300, 200, 50, 9.1, 1, NOW(), NOW(), 0),
(10, 21, NULL, 1, '宇树 B2', 'B2', '工业级四足机器人，强负载', 28000.00, 28000.00, 1, '2023-11-01', 'https://picsum.photos/seed/robot10/800/600', NULL, 2, '{"重量":"46kg","负载":"20kg","续航":"4h"}', 870, 8000, 600, 240, 160, 42, 9.0, 1, NOW(), NOW(), 0),
(11, 21, NULL, 1, '宇树 Aliengo', 'Aliengo', '科研教育四足开发平台', 35000.00, 35000.00, 1, '2022-03-01', 'https://picsum.photos/seed/robot11/800/600', NULL, 1, '{"重量":"20kg","负载":"10kg","续航":"2.5h"}', 760, 6000, 450, 190, 120, 36, 8.8, 1, NOW(), NOW(), 0),
(12, 21, NULL, 3, '波士顿动力 Spot', 'Spot', '示例：四足机器人标杆', 52000.00, 52000.00, 1, '2019-09-01', 'https://picsum.photos/seed/robot12/800/600', NULL, 2, '{"重量":"32kg","负载":"14kg","续航":"90min"}', 780, 5500, 420, 170, 100, 32, 8.9, 1, NOW(), NOW(), 0),
(13, 31, NULL, 8, '达闼 送餐机器人 T5', 'T5', '餐厅配送服务机器人', 68000.00, 68000.00, 1, '2022-06-01', 'https://picsum.photos/seed/robot13/800/600', NULL, 1, '{"负载":"40kg","续航":"8h"}', 520, 3000, 240, 90, 70, 18, 8.3, 1, NOW(), NOW(), 0),
(14, 33, NULL, 8, '达闼 酒店机器人 H2', 'H2', '酒店迎宾配送机器人', 75000.00, 75000.00, 1, '2022-08-01', 'https://picsum.photos/seed/robot14/800/600', NULL, 1, '{"负载":"35kg","续航":"10h"}', 480, 2600, 200, 80, 60, 15, 8.2, 1, NOW(), NOW(), 0),
(15, 41, NULL, 11, '科沃斯 X1 Omni', 'X1', '全能扫拖机器人', 5999.00, 5999.00, 1, '2022-09-01', 'https://picsum.photos/seed/robot15/800/600', '["https://picsum.photos/seed/robot15a/800/600"]', 3, '{"续航":"260min","吸力":"5000Pa"}', 880, 9500, 760, 260, 180, 45, 9.1, 1, NOW(), NOW(), 0),
(16, 51, NULL, 9, '新松 SR10 机械臂', 'SR10', '六轴工业机器人', 180000.00, 180000.00, 1, '2020-05-01', 'https://picsum.photos/seed/robot16/800/600', NULL, 1, '{"负载":"10kg","重复定位精度":"0.02mm"}', 640, 4200, 320, 130, 90, 22, 8.6, 1, NOW(), NOW(), 0),
(17, 51, NULL, 10, '埃斯顿 ER6', 'ER6', '六轴通用工业机器人', 150000.00, 150000.00, 1, '2021-03-01', 'https://picsum.photos/seed/robot17/800/600', NULL, 1, '{"负载":"6kg","重复定位精度":"0.03mm"}', 600, 3800, 300, 120, 80, 20, 8.5, 1, NOW(), NOW(), 0),
(18, 52, NULL, 10, '埃斯顿 协作机器人 EC612', 'EC612', '六轴协作机器人', 220000.00, 220000.00, 1, '2022-02-01', 'https://picsum.photos/seed/robot18/800/600', NULL, 1, '{"负载":"12kg","重复定位精度":"0.03mm"}', 580, 3400, 280, 110, 75, 19, 8.4, 1, NOW(), NOW(), 0),
(19, 53, NULL, 9, '新松 焊接机器人', 'SRW', '六轴焊接专用机器人', 210000.00, 210000.00, 1, '2021-07-01', 'https://picsum.photos/seed/robot19/800/600', NULL, 1, '{"负载":"8kg","臂展":"1400mm"}', 560, 3000, 240, 100, 70, 17, 8.3, 1, NOW(), NOW(), 0),
(20, 21, NULL, 5, '小米 CyberDog 铁蛋', 'CyberDog', '开源四足机器人开发平台', 9999.00, 9999.00, 1, '2021-08-01', 'https://picsum.photos/seed/robot20/800/600', NULL, 2, '{"重量":"14kg","续航":"1h","最大速度":"3.2m/s"}', 720, 7000, 520, 200, 130, 30, 8.7, 1, NOW(), NOW(), 0),
(21, 41, NULL, 5, '小米 扫地机器人', 'Mi Robot', '激光导航扫拖机器人', 1999.00, 1999.00, 1, '2020-10-01', 'https://picsum.photos/seed/robot21/800/600', NULL, 1, '{"续航":"120min","吸力":"4000Pa"}', 660, 6000, 460, 180, 120, 28, 8.6, 1, NOW(), NOW(), 0),
(22, 43, NULL, 11, '科沃斯 陪伴机器人', 'ATMOBot', '家庭陪伴 AI 机器人', 4999.00, 4999.00, 2, '2023-03-01', 'https://picsum.photos/seed/robot22/800/600', NULL, 1, '{"屏幕尺寸":"10inch","续航":"6h"}', 500, 2800, 220, 85, 60, 16, 8.1, 1, NOW(), NOW(), 0),
(23, 94, NULL, 4, '大疆 教育机器人 RoboMaster', 'EP', '面向 STEM 的教育机器人', 3499.00, 3499.00, 1, '2020-12-01', 'https://picsum.photos/seed/robot23/800/600', NULL, 1, '{"适配":"Scratch/Python","续航":"2h"}', 540, 3200, 260, 95, 65, 18, 8.4, 1, NOW(), NOW(), 0),
(24, 13, NULL, 6, '傅利叶 科研人形 EX', 'EX', '科研教学人形平台', 320000.00, 320000.00, 2, '2024-02-01', 'https://picsum.photos/seed/robot24/800/600', NULL, 1, '{"身高":"1600mm","自由度":"40"}', 470, 2200, 180, 70, 50, 12, 8.0, 1, NOW(), NOW(), 0);

-- 机器人图片（部分）
INSERT INTO `robot_image` (`robot_id`, `url`, `type`, `sort`, `create_time`) VALUES
(1,'https://picsum.photos/seed/robot1a/800/600','normal',1,NOW()),
(1,'https://picsum.photos/seed/robot1b/800/600','normal',2,NOW()),
(2,'https://picsum.photos/seed/robot2a/800/600','normal',1,NOW()),
(9,'https://picsum.photos/seed/robot9a/800/600','normal',1,NOW()),
(15,'https://picsum.photos/seed/robot15a/800/600','normal',1,NOW());

-- 机器人价格（部分渠道价）
INSERT INTO `robot_price` (`robot_id`, `channel`, `region`, `price`, `update_time`) VALUES
(1,'官网直营','全国',90000.00,NOW()),
(9,'官网直营','全国',1600.00,NOW()),
(15,'京东自营','全国',5999.00,NOW()),
(20,'小米商城','全国',9999.00,NOW());

-- ---------------------------
-- 参数模板（分类 → 模板 → 分组 → 定义）
-- ---------------------------
INSERT INTO `robot_param_template` (`id`, `category_id`, `name`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 1, '人形机器人参数模板', 1, 1, NOW(), NOW(), 0),
(2, 2, '四足机器人参数模板', 2, 1, NOW(), NOW(), 0),
(3, 3, '服务机器人参数模板', 3, 1, NOW(), NOW(), 0),
(4, 5, '工业机器人参数模板', 4, 1, NOW(), NOW(), 0),
(5, 4, '家庭机器人参数模板', 5, 1, NOW(), NOW(), 0);

INSERT INTO `robot_param_group` (`id`, `template_id`, `name`, `sort`, `create_time`) VALUES
(101, 1, '基础参数', 1, NOW()),
(102, 1, '运动参数', 2, NOW()),
(103, 1, 'AI', 3, NOW()),
(104, 1, '视觉', 4, NOW()),
(105, 1, '通信', 5, NOW()),
(106, 1, '开发', 6, NOW()),
(201, 2, '基础参数', 1, NOW()),
(202, 2, '运动参数', 2, NOW()),
(203, 2, '感知', 3, NOW()),
(204, 2, '电池', 4, NOW()),
(301, 3, '基础参数', 1, NOW()),
(302, 3, '功能参数', 2, NOW()),
(401, 4, '基础参数', 1, NOW()),
(402, 4, '精度参数', 2, NOW()),
(501, 5, '基础参数', 1, NOW()),
(502, 5, '智能参数', 2, NOW());

INSERT INTO `robot_param_def` (`id`, `group_id`, `name`, `unit`, `type`, `options`, `sort`, `is_compare`, `is_show`, `create_time`) VALUES
(1001, 101, '身高', 'mm', 'number', NULL, 1, 1, 1, NOW()),
(1002, 101, '重量', 'kg', 'number', NULL, 2, 1, 1, NOW()),
(1003, 101, '发布时间', '', 'text', NULL, 3, 0, 1, NOW()),
(1004, 101, '续航', 'h', 'number', NULL, 4, 1, 1, NOW()),
(1005, 101, '电池', '', 'text', NULL, 5, 0, 1, NOW()),
(1006, 101, '充电时间', 'h', 'number', NULL, 6, 0, 1, NOW()),
(1011, 102, '自由度', '', 'number', NULL, 1, 1, 1, NOW()),
(1012, 102, '最大速度', 'm/s', 'number', NULL, 2, 1, 1, NOW()),
(1013, 102, '最大负载', 'kg', 'number', NULL, 3, 1, 1, NOW()),
(1014, 102, '最大坡度', '°', 'number', NULL, 4, 0, 1, NOW()),
(1021, 103, 'CPU', '', 'text', NULL, 1, 0, 1, NOW()),
(1022, 103, 'GPU', '', 'text', NULL, 2, 0, 1, NOW()),
(1023, 103, 'NPU算力', 'TOPS', 'number', NULL, 3, 0, 1, NOW()),
(1024, 103, '大模型', '', 'text', NULL, 4, 0, 1, NOW()),
(1031, 104, '摄像头', '', 'text', NULL, 1, 0, 1, NOW()),
(1032, 104, '深度相机', '', 'text', NULL, 2, 0, 1, NOW()),
(1033, 104, '激光雷达', '', 'text', NULL, 3, 0, 1, NOW()),
(1041, 105, 'WiFi', '', 'text', NULL, 1, 0, 1, NOW()),
(1042, 105, 'Bluetooth', '', 'text', NULL, 2, 0, 1, NOW()),
(1043, 105, '5G', '', 'text', NULL, 3, 0, 1, NOW()),
(1051, 106, 'SDK', '', 'text', NULL, 1, 0, 1, NOW()),
(1052, 106, 'ROS', '', 'select', '支持,不支持', 2, 0, 1, NOW()),
(1053, 106, 'Python', '', 'select', '支持,不支持', 3, 0, 1, NOW()),
(1054, 106, 'C++', '', 'select', '支持,不支持', 4, 0, 1, NOW()),
(2001, 201, '重量', 'kg', 'number', NULL, 1, 1, 1, NOW()),
(2002, 201, '续航', 'h', 'number', NULL, 2, 1, 1, NOW()),
(2003, 202, '最大速度', 'm/s', 'number', NULL, 1, 1, 1, NOW()),
(2004, 202, '最大负载', 'kg', 'number', NULL, 2, 1, 1, NOW()),
(2005, 203, '摄像头', '', 'text', NULL, 1, 0, 1, NOW()),
(2006, 204, '电池容量', 'Wh', 'number', NULL, 1, 0, 1, NOW()),
(3001, 301, '负载', 'kg', 'number', NULL, 1, 1, 1, NOW()),
(3002, 301, '续航', 'h', 'number', NULL, 2, 1, 1, NOW()),
(4001, 401, '负载', 'kg', 'number', NULL, 1, 1, 1, NOW()),
(4002, 402, '重复定位精度', 'mm', 'number', NULL, 1, 1, 1, NOW()),
(5001, 501, '续航', 'min', 'number', NULL, 1, 1, 1, NOW()),
(5002, 502, '智能语音', '', 'select', '支持,不支持', 2, 0, 1, NOW());

-- 参数值（为代表性机器人填充关键参数，示例数据）
INSERT INTO `robot_param_value` (`robot_id`, `def_id`, `value`, `create_time`) VALUES
(1, 1001, '1800', NOW()),(1, 1002, '47', NOW()),(1, 1004, '2', NOW()),(1, 1011, '19', NOW()),(1, 1012, '1.5', NOW()),(1, 1013, '30', NOW()),
(2, 1001, '1270', NOW()),(2, 1002, '35', NOW()),(2, 1004, '2', NOW()),(2, 1011, '23', NOW()),(2, 1012, '2', NOW()),
(3, 1001, '1645', NOW()),(3, 1002, '74', NOW()),(3, 1004, '2', NOW()),(3, 1011, '41', NOW()),
(5, 1001, '1650', NOW()),(5, 1002, '55', NOW()),(5, 1011, '44', NOW()),
(6, 1001, '1750', NOW()),(6, 1002, '55', NOW()),(6, 1012, '2', NOW()),
(7, 1001, '1500', NOW()),(7, 1002, '89', NOW()),(7, 1012, '2.5', NOW()),
(8, 1001, '1730', NOW()),(8, 1002, '73', NOW()),(8, 1013, '20', NOW()),
(9, 2001, '15', NOW()),(9, 2002, '1.5', NOW()),(9, 2003, '3.5', NOW()),(9, 2004, '0', NOW()),
(10, 2001, '46', NOW()),(10, 2002, '4', NOW()),(10, 2004, '20', NOW()),
(11, 2001, '20', NOW()),(11, 2002, '2.5', NOW()),(11, 2004, '10', NOW()),
(12, 2001, '32', NOW()),(12, 2002, '1.5', NOW()),(12, 2004, '14', NOW()),
(15, 5001, '260', NOW()),(15, 5002, '支持', NOW()),
(16, 4001, '10', NOW()),(16, 4002, '0.02', NOW()),
(17, 4001, '6', NOW()),(17, 4002, '0.03', NOW()),
(18, 4001, '12', NOW()),(18, 4002, '0.03', NOW()),
(19, 4001, '8', NOW()),(19, 4002, '0.05', NOW()),
(20, 2001, '14', NOW()),(20, 2002, '1', NOW()),(20, 2003, '3.2', NOW());

-- ---------------------------
-- 资讯（22 篇）
-- ---------------------------
INSERT INTO `article` (`id`, `category_id`, `title`, `cover`, `summary`, `content`, `author`, `source`, `tags`, `view_count`, `like_count`, `favorite_count`, `comment_count`, `status`, `publish_time`, `is_example`, `create_time`, `update_time`, `deleted`) VALUES
(1, 4, '宇树 H1 发布：全尺寸人形机器人新标杆', 'https://picsum.photos/seed/news1/800/450', '宇树发布 H1，运动性能引发行业关注。', '<p>宇树科技正式发布全尺寸通用人形机器人 H1，具备高动态运动能力……（示例内容）</p>', '机器人之家', '官方', '["人形机器人","宇树"]', 5200, 320, 180, 45, 1, '2023-08-02', 1, NOW(), NOW(), 0),
(2, 5, '机器狗走进家庭：Go2 体验评测', 'https://picsum.photos/seed/news2/800/450', 'Go2 凭借亲民价格成为开发者新宠。', '<p>我们对宇树 Go2 进行了为期一周的体验……（示例内容）</p>', '评测组', '原创', '["机器狗","宇树"]', 4800, 290, 160, 40, 1, '2023-07-10', 1, NOW(), NOW(), 0),
(3, 6, '工业机械臂国产化加速', 'https://picsum.photos/seed/news3/800/450', '新松、埃斯顿领跑国产工业机器人。', '<p>近年来国产工业机器人市占率持续提升……（示例内容）</p>', '行业观察', '原创', '["工业机器人"]', 3600, 200, 120, 30, 1, '2023-05-20', 1, NOW(), NOW(), 0),
(4, 8, '大模型如何重塑机器人', 'https://picsum.photos/seed/news4/800/450', '从感知到决策，大模型带来新范式。', '<p>大模型正在改变机器人交互与规划……（示例内容）</p>', 'AI组', '原创', '["AI","大模型"]', 6100, 410, 220, 60, 1, '2023-09-01', 1, NOW(), NOW(), 0),
(5, 10, '机器人行业融资盘点', 'https://picsum.photos/seed/news5/800/450', '2023 年机器人赛道融资火热。', '<p>据不完全统计，今年机器人领域融资超百亿……（示例内容）</p>', '财经组', '原创', '["融资"]', 4200, 260, 140, 35, 1, '2023-10-01', 1, NOW(), NOW(), 0),
(6, 3, '优必选 Walker S 切入智能制造', 'https://picsum.photos/seed/news6/800/450', 'Walker S 面向工业场景落地。', '<p>优必选推出工业人形机器人 Walker S……（示例内容）</p>', '机器人之家', '官方', '["优必选","工业人形"]', 3900, 230, 130, 32, 1, '2024-01-05', 1, NOW(), NOW(), 0),
(7, 7, '服务机器人在餐饮行业规模化落地', 'https://picsum.photos/seed/news7/800/450', '送餐机器人成为餐饮标配。', '<p>送餐机器人正在快速普及……（示例内容）</p>', '行业观察', '原创', '["服务机器人"]', 3100, 180, 100, 25, 1, '2023-06-15', 1, NOW(), NOW(), 0),
(8, 2, '2023 机器人行业年度盘点', 'https://picsum.photos/seed/news8/800/450', '一年间机器人行业发生了什么。', '<p>年度盘点：技术、产品、资本……（示例内容）</p>', '机器人之家', '原创', '["行业"]', 5500, 350, 190, 50, 1, '2023-12-30', 1, NOW(), NOW(), 0),
(9, 4, '智元远征 A1 亮相', 'https://picsum.photos/seed/news9/800/450', '智元发布通用人形机器人 A1。', '<p>智元机器人发布远征 A1……（示例内容）</p>', '机器人之家', '官方', '["智元","人形机器人"]', 4700, 280, 150, 38, 1, '2023-08-15', 1, NOW(), NOW(), 0),
(10, 5, '波士顿动力 Spot 应用案例', 'https://picsum.photos/seed/news10/800/450', 'Spot 在巡检场景的应用。', '<p>Spot 被用于工业巡检……（示例内容）</p>', '编译', '外媒', '["波士顿动力","机器狗"]', 3400, 190, 110, 28, 1, '2023-04-10', 1, NOW(), NOW(), 0),
(11, 9, 'ROS2 与 ROS 的区别', 'https://picsum.photos/seed/news11/800/450', '开发者必读：ROS2 新特性。', '<p>ROS2 在实时性、安全性上的改进……（示例内容）</p>', '技术组', '原创', '["ROS2","技术"]', 6800, 450, 250, 70, 1, '2023-09-20', 1, NOW(), NOW(), 0),
(12, 11, '机器人产业新政解读', 'https://picsum.photos/seed/news12/800/450', '政策利好机器人产业发展。', '<p>最新政策对行业的影响……（示例内容）</p>', '政策组', '原创', '["政策"]', 2900, 160, 90, 20, 1, '2023-07-01', 1, NOW(), NOW(), 0),
(13, 4, '傅利叶 GR-1 开启预售', 'https://picsum.photos/seed/news13/800/450', 'GR-1 面向科研与开发者。', '<p>傅利叶 GR-1 开启预售……（示例内容）</p>', '机器人之家', '官方', '["傅利叶","人形机器人"]', 3600, 210, 120, 30, 1, '2023-09-10', 1, NOW(), NOW(), 0),
(14, 1, '扫地机器人选购指南', 'https://picsum.photos/seed/news14/800/450', '2023 扫地机器人怎么选。', '<p>从导航到吸力，一文看懂……（示例内容）</p>', '选购组', '原创', '["扫地机器人","家庭"]', 7200, 500, 280, 80, 1, '2023-10-20', 1, NOW(), NOW(), 0),
(15, 6, '协作机器人如何提升产线效率', 'https://picsum.photos/seed/news15/800/450', '协作机器人落地制造。', '<p>协作机器人部署案例……（示例内容）</p>', '行业观察', '原创', '["协作机器人"]', 3300, 190, 100, 26, 1, '2023-05-30', 1, NOW(), NOW(), 0),
(16, 3, '小米 CyberDog 开源生态', 'https://picsum.photos/seed/news16/800/450', '铁蛋面向开发者开源。', '<p>CyberDog 开源社区进展……（示例内容）</p>', '机器人之家', '官方', '["小米","机器狗"]', 5000, 300, 170, 42, 1, '2021-08-10', 1, NOW(), NOW(), 0),
(17, 8, '具身智能：机器人下一个十年', 'https://picsum.photos/seed/news17/800/450', '具身智能成为风口。', '<p>具身智能的定义与趋势……（示例内容）</p>', 'AI组', '原创', '["AI","具身智能"]', 6400, 420, 230, 65, 1, '2023-11-01', 1, NOW(), NOW(), 0),
(18, 12, '达闼服务机器人出海', 'https://picsum.photos/seed/news18/800/450', '达闼拓展海外市场。', '<p>达闼海外布局……（示例内容）</p>', '企业组', '原创', '["达闼","企业"]', 2700, 150, 80, 18, 1, '2023-06-05', 1, NOW(), NOW(), 0),
(19, 2, '2024 CES 机器人亮点', 'https://picsum.photos/seed/news19/800/450', 'CES 上的机器人新品。', '<p>CES 2024 机器人综述……（示例内容）</p>', '编译', '外媒', '["行业","新品"]', 4100, 250, 140, 33, 1, '2024-01-12', 1, NOW(), NOW(), 0),
(20, 5, '机器狗在救援场景的应用', 'https://picsum.photos/seed/news20/800/450', '四足机器人在搜救中发挥作用。', '<p>救援机器人实战……（示例内容）</p>', '行业观察', '原创', '["机器狗","救援"]', 3800, 220, 120, 31, 1, '2023-08-25', 1, NOW(), NOW(), 0),
(21, 9, '从零搭建 ROS 开发环境', 'https://picsum.photos/seed/news21/800/450', '新手 ROS 环境搭建教程。', '<p>一步步搭建 ROS……（示例内容）</p>', '技术组', '原创', '["ROS","技术"]', 5900, 380, 210, 55, 1, '2023-09-25', 1, NOW(), NOW(), 0),
(22, 4, '特斯拉 Optimus 进展更新', 'https://picsum.photos/seed/news22/800/450', 'Optimus 量产进展。', '<p>特斯拉分享 Optimus 最新进展……（示例内容）</p>', '编译', '外媒', '["特斯拉","人形机器人"]', 7000, 460, 260, 75, 1, '2023-09-30', 1, NOW(), NOW(), 0);

-- ---------------------------
-- 视频（12 个）
-- ---------------------------
INSERT INTO `video` (`id`, `category_id`, `title`, `cover`, `url`, `duration`, `summary`, `author`, `tags`, `view_count`, `like_count`, `favorite_count`, `comment_count`, `status`, `publish_time`, `is_example`, `create_time`, `update_time`, `deleted`) VALUES
(1, 2, '宇树 H1 实机演示', 'https://picsum.photos/seed/v1/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 120, 'H1 行走与运动演示', '官方', '["人形机器人"]', 8800, 520, 300, 80, 1, '2023-08-05', 1, NOW(), NOW(), 0),
(2, 3, 'Go2 开箱实测', 'https://picsum.photos/seed/v2/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 240, 'Go2 开箱与功能演示', '测评', '["机器狗"]', 7600, 450, 260, 70, 1, '2023-07-15', 1, NOW(), NOW(), 0),
(3, 6, '工业机器人焊接演示', 'https://picsum.photos/seed/v3/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 180, '六轴机器人焊接', '官方', '["工业机器人"]', 5200, 300, 180, 50, 1, '2023-05-25', 1, NOW(), NOW(), 0),
(4, 4, 'Walker X 交互演示', 'https://picsum.photos/seed/v4/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 200, 'Walker X 人机交互', '官方', '["优必选"]', 6100, 360, 200, 55, 1, '2021-07-10', 1, NOW(), NOW(), 0),
(5, 7, '送餐机器人工作日常', 'https://picsum.photos/seed/v5/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 150, '餐厅送餐机器人', '用户', '["服务机器人"]', 4300, 250, 140, 40, 1, '2023-06-20', 1, NOW(), NOW(), 0),
(6, 5, 'Spot 巡检应用', 'https://picsum.photos/seed/v6/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 220, 'Spot 工业巡检', '编译', '["波士顿动力"]', 4900, 280, 160, 45, 1, '2023-04-15', 1, NOW(), NOW(), 0),
(7, 6, '协作机器人装配线', 'https://picsum.photos/seed/v7/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 190, '协作机器人应用', '官方', '["协作机器人"]', 3700, 200, 110, 30, 1, '2022-03-01', 1, NOW(), NOW(), 0),
(8, 2, 'CyberDog 开发者版', 'https://picsum.photos/seed/v8/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 210, '铁蛋功能展示', '官方', '["小米","机器狗"]', 6700, 400, 230, 60, 1, '2021-08-20', 1, NOW(), NOW(), 0),
(9, 4, '傅利叶 GR-1 发布', 'https://picsum.photos/seed/v9/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 230, 'GR-1 发布会', '官方', '["傅利叶"]', 4400, 260, 150, 42, 1, '2023-09-05', 1, NOW(), NOW(), 0),
(10, 6, '扫地机器人横评', 'https://picsum.photos/seed/v10/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 300, '多款扫地机对比', '测评', '["扫地机器人"]', 9200, 540, 310, 85, 1, '2023-10-25', 1, NOW(), NOW(), 0),
(11, 7, 'ROS2 入门实操', 'https://picsum.photos/seed/v11/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 360, 'ROS2 实操教学', '技术', '["ROS2"]', 8100, 480, 270, 75, 1, '2023-09-22', 1, NOW(), NOW(), 0),
(12, 5, 'Atlas 高难度动作', 'https://picsum.photos/seed/v12/800/450', 'https://www.w3schools.com/html/mov_bbb.mp4', 160, 'Atlas 跑酷演示', '编译', '["波士顿动力"]', 9900, 580, 330, 90, 1, '2021-04-20', 1, NOW(), NOW(), 0);

-- ---------------------------
-- 教程（12 篇）
-- ---------------------------
INSERT INTO `tutorial` (`id`, `category_id`, `title`, `cover`, `summary`, `content`, `author`, `tags`, `view_count`, `like_count`, `favorite_count`, `comment_count`, `status`, `publish_time`, `is_example`, `create_time`, `update_time`, `deleted`) VALUES
(1, 1, '机器人开发入门：从零开始', 'https://picsum.photos/seed/t1/800/450', '新手如何进入机器人开发。', '# 机器人开发入门\n\n本文介绍机器人开发的基础路线与工具链……（示例 Markdown）', '教程组', '["新手"]', 8200, 520, 300, 90, 1, '2023-09-01', 1, NOW(), NOW(), 0),
(2, 2, 'ESP32 控制舵机实战', 'https://picsum.photos/seed/t2/800/450', '用 ESP32 驱动舵机。', '# ESP32 舵机控制\n\n```cpp\n#include <Servo.h>\nServo s;\nvoid setup(){ s.attach(13); }\n```\n……（示例）', '教程组', '["ESP32"]', 6400, 400, 230, 70, 1, '2023-09-05', 1, NOW(), NOW(), 0),
(3, 3, 'Arduino 入门：点亮第一个 LED', 'https://picsum.photos/seed/t3/800/450', 'Arduino 基础。', '# Arduino 入门\n\n```cpp\nvoid setup(){ pinMode(13,OUTPUT); }\n```\n……（示例）', '教程组', '["Arduino"]', 7200, 450, 260, 80, 1, '2023-09-08', 1, NOW(), NOW(), 0),
(4, 4, 'STM32 定时器详解', 'https://picsum.photos/seed/t4/800/450', 'STM32 定时器配置。', '# STM32 定时器\n\n本文讲解定时器 PWM 输出……（示例）', '教程组', '["STM32"]', 5100, 300, 170, 50, 1, '2023-09-12', 1, NOW(), NOW(), 0),
(5, 5, 'ROS 核心概念与安装', 'https://picsum.photos/seed/t5/800/450', 'ROS 节点与通信。', '# ROS 入门\n\n```bash\nsudo apt install ros-noetic-desktop-full\n```\n……（示例）', '教程组', '["ROS"]', 9100, 560, 320, 95, 1, '2023-09-15', 1, NOW(), NOW(), 0),
(6, 6, 'ROS2 与 ROS 差异速览', 'https://picsum.photos/seed/t6/800/450', 'ROS2 DDS 通信。', '# ROS2 速览\n\nROS2 使用 DDS 中间件……（示例）', '教程组', '["ROS2"]', 7800, 480, 270, 85, 1, '2023-09-18', 1, NOW(), NOW(), 0),
(7, 7, '树莓派 GPIO 控制外设', 'https://picsum.photos/seed/t7/800/450', '树莓派入门。', '# 树莓派 GPIO\n\n```python\nimport RPi.GPIO as GPIO\n```\n……（示例）', '教程组', '["Raspberry Pi"]', 6000, 360, 210, 60, 1, '2023-09-20', 1, NOW(), NOW(), 0),
(8, 8, 'Jetson 部署目标检测模型', 'https://picsum.photos/seed/t8/800/450', 'Jetson 边缘推理。', '# Jetson 推理\n\n在 Jetson 上部署 YOLO……（示例）', '教程组', '["Jetson","CV"]', 6800, 420, 240, 72, 1, '2023-09-22', 1, NOW(), NOW(), 0),
(9, 9, '大模型驱动机器人对话', 'https://picsum.photos/seed/t9/800/450', 'LLM + 机器人。', '# LLM 机器人\n\n用大模型做机器人指令理解……（示例）', '教程组', '["AI","大模型"]', 8500, 540, 310, 88, 1, '2023-09-25', 1, NOW(), NOW(), 0),
(10, 10, '大模型微调实践', 'https://picsum.photos/seed/t10/800/450', '微调方法综述。', '# 大模型微调\n\nLoRA / QLoRA 简介……（示例）', '教程组', '["大模型"]', 7300, 460, 260, 78, 1, '2023-09-28', 1, NOW(), NOW(), 0),
(11, 11, '计算机视觉目标检测', 'https://picsum.photos/seed/t11/800/450', 'CV 基础。', '# 目标检测\n\nYOLO 系列演进……（示例）', '教程组', '["CV"]', 6900, 430, 250, 73, 1, '2023-10-01', 1, NOW(), NOW(), 0),
(12, 13, '机器人机械结构设计', 'https://picsum.photos/seed/t12/800/450', '结构入门。', '# 机械结构\n\n连杆与关节设计基础……（示例）', '教程组', '["机械结构"]', 5400, 320, 180, 55, 1, '2023-10-03', 1, NOW(), NOW(), 0);

-- ---------------------------
-- 社区帖子（22 篇）
-- ---------------------------
INSERT INTO `community_post` (`id`, `circle_id`, `user_id`, `title`, `content`, `images`, `video_url`, `robot_id`, `brand_id`, `topic`, `like_count`, `comment_count`, `favorite_count`, `view_count`, `is_top`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 1, NULL, 'H1 到手一周体验', '用了宇树 H1 一周，运动能力真强，分享一些使用心得……（示例）', '["https://picsum.photos/seed/p1/400/300"]', NULL, 1, 1, '体验', 320, 45, 30, 1200, 0, 1, NOW(), NOW(), 0),
(2, 3, NULL, 'Go2 值得买吗', '预算有限，Go2 入门合适吗？求老哥们建议。', NULL, NULL, 9, 1, '求助', 210, 60, 20, 900, 0, 1, NOW(), NOW(), 0),
(3, 2, NULL, '宇树生态越来越丰富了', '从 Go 到 H1，宇树产品矩阵完整了。', NULL, NULL, NULL, 1, '讨论', 180, 30, 15, 700, 0, 1, NOW(), NOW(), 0),
(4, 4, NULL, 'ROS 学习路线图', '整理了一份 ROS 学习路线，供新手参考。', '["https://picsum.photos/seed/p4/400/300"]', NULL, NULL, NULL, 'ROS', 540, 80, 60, 2100, 1, 1, NOW(), NOW(), 0),
(5, 5, NULL, 'ROS2 比 ROS 好在哪', '实话说 DDS 通信确实更稳。', NULL, NULL, NULL, NULL, 'ROS2', 260, 40, 22, 1000, 0, 1, NOW(), NOW(), 0),
(6, 6, NULL, 'ESP32 做机器人主控够用吗', '想用 ESP32 做小型机器人，求建议。', NULL, NULL, NULL, NULL, 'ESP32', 150, 35, 12, 600, 0, 1, NOW(), NOW(), 0),
(7, 7, NULL, '大模型让机器人更聪明了', '最近体验了 LLM 控制的机器人，交互提升明显。', NULL, NULL, NULL, NULL, 'AI', 420, 55, 40, 1600, 0, 1, NOW(), NOW(), 0),
(8, 8, NULL, '从零做一台机械臂', '记录我的机械臂 DIY 过程。', '["https://picsum.photos/seed/p8/400/300"]', NULL, NULL, NULL, 'DIY', 380, 50, 35, 1400, 0, 1, NOW(), NOW(), 0),
(9, 9, NULL, '机器人创业哪些方向有机会', '聊聊机器人创业赛道。', NULL, NULL, NULL, NULL, '创业', 290, 42, 25, 1100, 0, 1, NOW(), NOW(), 0),
(10, 1, NULL, '人形机器人什么时候能家用', '期待真正能帮做家务的人形机器人。', NULL, NULL, NULL, NULL, '讨论', 350, 48, 28, 1300, 0, 1, NOW(), NOW(), 0),
(11, 3, NULL, '机器狗巡逻实测', '用 Go2 做了个巡逻小项目。', '["https://picsum.photos/seed/p11/400/300"]', NULL, 9, 1, '项目', 270, 38, 20, 950, 0, 1, NOW(), NOW(), 0),
(12, 4, NULL, 'ROS 仿真环境搭建', 'Gazebo + ROS 仿真踩坑记录。', NULL, NULL, NULL, NULL, 'ROS', 310, 44, 26, 1050, 0, 1, NOW(), NOW(), 0),
(13, 2, NULL, '宇树 G1 体验', 'G1 性价比不错，适合开发者。', NULL, NULL, 2, 1, '体验', 240, 33, 18, 850, 0, 1, NOW(), NOW(), 0),
(14, 7, NULL, '具身智能讨论帖', '具身智能到底是不是风口？', NULL, NULL, NULL, NULL, 'AI', 360, 50, 30, 1250, 0, 1, NOW(), NOW(), 0),
(15, 8, NULL, '第一台机器人怎么选', '预算 1w 内推荐什么？', NULL, NULL, NULL, NULL, '求助', 200, 55, 15, 800, 0, 1, NOW(), NOW(), 0),
(16, 5, NULL, 'ROS2 实时性调优', '分享 ROS2 实时性优化经验。', NULL, NULL, NULL, NULL, 'ROS2', 280, 36, 22, 1000, 0, 1, NOW(), NOW(), 0),
(17, 6, NULL, 'ESP32+舵机做机械臂', '小项目分享。', '["https://picsum.photos/seed/p17/400/300"]', NULL, NULL, NULL, 'DIY', 220, 30, 16, 750, 0, 1, NOW(), NOW(), 0),
(18, 9, NULL, '机器人融资怎么看', '聊聊近期融资事件。', NULL, NULL, NULL, NULL, '创业', 190, 28, 14, 700, 0, 1, NOW(), NOW(), 0),
(19, 1, NULL, '人形机器人运动控制原理', '简单科普一下。', NULL, NULL, NULL, NULL, '科普', 410, 52, 38, 1500, 0, 1, NOW(), NOW(), 0),
(20, 3, NULL, 'Go2 二次开发指南', '开放接口怎么用。', NULL, NULL, 9, 1, '开发', 300, 40, 24, 1100, 0, 1, NOW(), NOW(), 0),
(21, 8, NULL, '机器人竞赛经验', '参加 RoboMaster 的心得。', NULL, NULL, NULL, NULL, '竞赛', 260, 34, 19, 900, 0, 1, NOW(), NOW(), 0),
(22, 7, NULL, 'AI 语音交互实现', '用语音控制机器人。', NULL, NULL, NULL, NULL, 'AI', 330, 46, 27, 1200, 0, 1, NOW(), NOW(), 0);

-- ---------------------------
-- Banner
-- ---------------------------
INSERT INTO `banner` (`position`, `title`, `image`, `url`, `sort`, `status`, `start_time`, `end_time`, `create_time`, `update_time`, `deleted`) VALUES
('pc', '人形机器人元年', 'https://picsum.photos/seed/banner1/1920/500', '/robot/1', 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
('pc', '机器狗选购季', 'https://picsum.photos/seed/banner2/1920/500', '/robot/9', 2, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
('pc', '工业机器人专题', 'https://picsum.photos/seed/banner3/1920/500', '/category/5', 3, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
('app', '新人专享', 'https://picsum.photos/seed/banner4/800/400', '/robot/2', 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
('app', '社区热帖', 'https://picsum.photos/seed/banner5/800/400', '/community', 2, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0);

-- ---------------------------
-- 推荐项
-- ---------------------------
INSERT INTO `recommend_item` (`position_id`, `biz_type`, `biz_id`, `title`, `image`, `url`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'robot', 1, '宇树 H1', 'https://picsum.photos/seed/robot1/800/600', '/robot/1', 1, 1, NOW(), NOW(), 0),
(1, 'robot', 9, '宇树 Go2', 'https://picsum.photos/seed/robot9/800/600', '/robot/9', 2, 1, NOW(), NOW(), 0),
(1, 'robot', 15, '科沃斯 X1', 'https://picsum.photos/seed/robot15/800/600', '/robot/15', 3, 1, NOW(), NOW(), 0),
(1, 'robot', 2, '宇树 G1', 'https://picsum.photos/seed/robot2/800/600', '/robot/2', 4, 1, NOW(), NOW(), 0),
(2, 'robot', 2, '宇树 G1', 'https://picsum.photos/seed/robot2/800/600', '/robot/2', 1, 1, NOW(), NOW(), 0),
(2, 'robot', 6, '智元 A1', 'https://picsum.photos/seed/robot6/800/600', '/robot/6', 2, 1, NOW(), NOW(), 0),
(2, 'robot', 24, '傅利叶 EX', 'https://picsum.photos/seed/robot24/800/600', '/robot/24', 3, 1, NOW(), NOW(), 0),
(3, 'article', 1, '宇树 H1 发布', 'https://picsum.photos/seed/news1/800/450', '/article/1', 1, 1, NOW(), NOW(), 0),
(3, 'article', 4, '大模型重塑机器人', 'https://picsum.photos/seed/news4/800/450', '/article/4', 2, 1, NOW(), NOW(), 0),
(4, 'video', 1, '宇树 H1 演示', 'https://picsum.photos/seed/v1/800/450', '/video/1', 1, 1, NOW(), NOW(), 0),
(4, 'video', 12, 'Atlas 跑酷', 'https://picsum.photos/seed/v12/800/450', '/video/12', 2, 1, NOW(), NOW(), 0),
(5, 'company', 1, '宇树科技', 'https://picsum.photos/seed/unitree/200/200', '/company/1', 1, 1, NOW(), NOW(), 0),
(5, 'company', 9, '新松机器人', 'https://picsum.photos/seed/siasun/200/200', '/company/9', 2, 1, NOW(), NOW(), 0);

-- ---------------------------
-- 机器人标签（使用场景 / 开发能力 / AI 能力，用于机器人库多维筛选）
-- ---------------------------
INSERT INTO `robot_tag` (`robot_id`, `tag_type`, `tag_value`, `create_time`) VALUES
-- 宇树 H1
(1,'scene','科研教育',NOW()),(1,'scene','商业服务',NOW()),(1,'dev','SDK',NOW()),(1,'dev','ROS2',NOW()),(1,'dev','Python',NOW()),(1,'ai','大模型',NOW()),(1,'ai','视觉识别',NOW()),
-- 宇树 G1
(2,'scene','科研教育',NOW()),(2,'scene','家庭陪伴',NOW()),(2,'dev','SDK',NOW()),(2,'dev','ROS2',NOW()),(2,'dev','Python',NOW()),(2,'ai','大模型',NOW()),(2,'ai','语音交互',NOW()),
-- 优必选 Walker X
(3,'scene','商业服务',NOW()),(3,'scene','家庭陪伴',NOW()),(3,'dev','SDK',NOW()),(3,'dev','API',NOW()),(3,'ai','语音交互',NOW()),(3,'ai','人机交互',NOW()),
-- 优必选 Walker S
(4,'scene','工业制造',NOW()),(4,'scene','仓储物流',NOW()),(4,'dev','SDK',NOW()),(4,'dev','API',NOW()),(4,'ai','视觉识别',NOW()),
-- 傅利叶 GR-1
(5,'scene','科研教育',NOW()),(5,'scene','医疗康复',NOW()),(5,'dev','SDK',NOW()),(5,'dev','ROS',NOW()),(5,'ai','大模型',NOW()),
-- 智元 远征 A1
(6,'scene','工业制造',NOW()),(6,'scene','商业服务',NOW()),(6,'dev','SDK',NOW()),(6,'dev','ROS2',NOW()),(6,'ai','大模型',NOW()),(6,'ai','视觉识别',NOW()),
-- 波士顿动力 Atlas
(7,'scene','科研教育',NOW()),(7,'scene','特种作业',NOW()),(7,'dev','SDK',NOW()),(7,'ai','视觉识别',NOW()),(7,'ai','自主导航',NOW()),
-- 特斯拉 Optimus
(8,'scene','工业制造',NOW()),(8,'scene','家庭陪伴',NOW()),(8,'dev','SDK',NOW()),(8,'ai','大模型',NOW()),(8,'ai','视觉识别',NOW()),
-- 宇树 Go2
(9,'scene','家庭陪伴',NOW()),(9,'scene','教育教学',NOW()),(9,'scene','巡检安防',NOW()),(9,'dev','SDK',NOW()),(9,'dev','ROS2',NOW()),(9,'dev','Python',NOW()),(9,'dev','C++',NOW()),(9,'ai','语音交互',NOW()),(9,'ai','视觉识别',NOW()),
-- 宇树 B2
(10,'scene','巡检安防',NOW()),(10,'scene','特种作业',NOW()),(10,'scene','仓储物流',NOW()),(10,'dev','SDK',NOW()),(10,'dev','ROS2',NOW()),(10,'dev','C++',NOW()),(10,'ai','视觉识别',NOW()),(10,'ai','自主导航',NOW()),
-- 宇树 Aliengo
(11,'scene','科研教育',NOW()),(11,'dev','ROS',NOW()),(11,'dev','ROS2',NOW()),(11,'dev','Python',NOW()),(11,'dev','C++',NOW()),(11,'ai','视觉识别',NOW()),
-- 波士顿动力 Spot
(12,'scene','巡检安防',NOW()),(12,'scene','特种作业',NOW()),(12,'scene','科研教育',NOW()),(12,'dev','SDK',NOW()),(12,'dev','API',NOW()),(12,'dev','Python',NOW()),(12,'ai','视觉识别',NOW()),(12,'ai','自主导航',NOW()),
-- 达闼 送餐 T5
(13,'scene','商业服务',NOW()),(13,'dev','API',NOW()),(13,'ai','语音交互',NOW()),(13,'ai','自主导航',NOW()),
-- 达闼 酒店 H2
(14,'scene','商业服务',NOW()),(14,'dev','API',NOW()),(14,'ai','语音交互',NOW()),(14,'ai','自主导航',NOW()),
-- 科沃斯 X1 Omni
(15,'scene','家庭清洁',NOW()),(15,'dev','API',NOW()),(15,'ai','视觉识别',NOW()),(15,'ai','自主导航',NOW()),(15,'ai','语音交互',NOW()),
-- 新松 SR10
(16,'scene','工业制造',NOW()),(16,'dev','API',NOW()),(16,'ai','视觉识别',NOW()),
-- 埃斯顿 ER6
(17,'scene','工业制造',NOW()),(17,'scene','仓储物流',NOW()),(17,'dev','API',NOW()),
-- 埃斯顿 EC612 协作机器人
(18,'scene','工业制造',NOW()),(18,'scene','科研教育',NOW()),(18,'dev','API',NOW()),(18,'dev','ROS',NOW()),(18,'ai','视觉识别',NOW()),
-- 新松 焊接机器人
(19,'scene','工业制造',NOW()),(19,'dev','API',NOW()),
-- 小米 CyberDog
(20,'scene','家庭陪伴',NOW()),(20,'scene','教育教学',NOW()),(20,'scene','科研教育',NOW()),(20,'dev','Python',NOW()),(20,'dev','C++',NOW()),(20,'dev','ROS2',NOW()),(20,'ai','视觉识别',NOW()),(20,'ai','语音交互',NOW()),
-- 小米 扫地机器人
(21,'scene','家庭清洁',NOW()),(21,'dev','API',NOW()),(21,'ai','自主导航',NOW()),(21,'ai','视觉识别',NOW()),
-- 科沃斯 陪伴机器人
(22,'scene','家庭陪伴',NOW()),(22,'scene','教育教学',NOW()),(22,'dev','API',NOW()),(22,'ai','语音交互',NOW()),(22,'ai','大模型',NOW()),
-- 大疆 RoboMaster EP
(23,'scene','教育教学',NOW()),(23,'dev','Python',NOW()),(23,'dev','Scratch',NOW()),(23,'dev','ROS',NOW()),(23,'ai','视觉识别',NOW()),
-- 傅利叶 科研人形 EX
(24,'scene','科研教育',NOW()),(24,'dev','SDK',NOW()),(24,'dev','ROS',NOW()),(24,'dev','ROS2',NOW()),(24,'dev','Python',NOW()),(24,'dev','C++',NOW()),(24,'ai','大模型',NOW());

-- ---------------------------
-- 冗余统计字段维护（品牌机器人数 / 企业品牌数、产品数）
-- ---------------------------
UPDATE `brand` b SET `robot_count` = (SELECT COUNT(*) FROM `robot` r WHERE r.`brand_id` = b.`id` AND r.`deleted` = 0);
UPDATE `company` c SET `brand_count` = (SELECT COUNT(*) FROM `brand` b WHERE b.`company_id` = c.`id` AND b.`deleted` = 0);
UPDATE `company` c SET `product_count` = (
  SELECT COUNT(*) FROM `robot` r
  INNER JOIN `brand` b ON b.`id` = r.`brand_id`
  WHERE b.`company_id` = c.`id` AND r.`deleted` = 0
);
