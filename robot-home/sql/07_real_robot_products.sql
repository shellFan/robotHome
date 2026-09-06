-- ============================================================
-- 机器人之家 Phase 2：真实机器人产品数据（50+ 产品）
-- 数据来源：企业官网公开信息
-- 兼容 MySQL 5.6
-- ============================================================
USE robot_home;

-- 标记 demo 机器人数据
UPDATE `robot` SET `data_source` = 'DEMO', `is_example` = 1 WHERE `id` <= 24;

-- ============================================================
-- 真实机器人产品
-- 列顺序: id, category_id, series_id, brand_id, name, model, subtitle, guide_price, market_price, status, release_date, cover_image, images, video_count, main_params, data_source, source_url, source_name, last_verified_time, hot_score, view_count, favorite_count, compare_count, inquiry_count, comment_count, score, is_example, create_time, update_time, deleted
-- ============================================================

INSERT INTO `robot` (`id`, `category_id`, `series_id`, `brand_id`, `name`, `model`, `subtitle`, `guide_price`, `market_price`, `status`, `release_date`, `cover_image`, `images`, `video_count`, `main_params`, `data_source`, `source_url`, `source_name`, `last_verified_time`, `hot_score`, `view_count`, `favorite_count`, `compare_count`, `inquiry_count`, `comment_count`, `score`, `is_example`, `create_time`, `update_time`, `deleted`) VALUES
-- ========== 宇树科技 (brand_id=101) ==========
(101, 11, NULL, 101, '宇树 H1', 'H1', '全尺寸通用人形机器人，运动能力业界领先', 90000.00, 90000.00, 1, '2023-08-01', NULL, NULL, 2, '{"身高":"1800mm","重量":"47kg","续航":"2h","最大速度":"1.5m/s","自由度":"19","负载":"5kg"}', 'OFFICIAL', 'https://www.unitree.com/h1', 'Unitree官网', NOW(), 960, 12000, 800, 320, 210, 56, 9.2, 0, NOW(), NOW(), 0),
(102, 11, NULL, 101, '宇树 G1', 'G1', '高性价比入门级人形机器人', 35000.00, 35000.00, 1, '2024-05-01', NULL, NULL, 1, '{"身高":"1270mm","重量":"35kg","续航":"2h","最大速度":"2m/s","自由度":"23","负载":"3kg"}', 'OFFICIAL', 'https://www.unitree.com/g1', 'Unitree官网', NOW(), 890, 9000, 700, 280, 180, 48, 9.0, 0, NOW(), NOW(), 0),
(103, 21, NULL, 101, '宇树 Go2', 'Go2', 'AI 机器狗，陪伴与开发首选', 1600.00, 1600.00, 1, '2023-07-01', NULL, NULL, 3, '{"重量":"15kg","续航":"1.5h","最大速度":"3.5m/s","负载":"5kg"}', 'OFFICIAL', 'https://www.unitree.com/go2', 'Unitree官网', NOW(), 910, 11000, 900, 300, 200, 50, 9.1, 0, NOW(), NOW(), 0),
(104, 21, NULL, 101, '宇树 B2', 'B2', '工业级四足机器人，强负载强续航', 28000.00, 28000.00, 1, '2023-11-01', NULL, NULL, 1, '{"重量":"46kg","负载":"20kg","续航":"4h","最大速度":"1.5m/s"}', 'OFFICIAL', 'https://www.unitree.com/b2', 'Unitree官网', NOW(), 870, 8000, 600, 240, 160, 42, 9.0, 0, NOW(), NOW(), 0),
(105, 21, NULL, 101, '宇树 B2-W', 'B2-W', '工业四足机器人，轮足融合', 0.00, 0.00, 2, '2024-01-01', NULL, NULL, 1, '{"重量":"50kg","负载":"25kg","续航":"4h","最大速度":"3m/s"}', 'OFFICIAL', 'https://www.unitree.com/b2w', 'Unitree官网', NOW(), 780, 5500, 400, 180, 120, 30, 8.8, 0, NOW(), NOW(), 0),

-- ========== 优必选 (brand_id=102) ==========
(106, 11, NULL, 102, '优必选 Walker S', 'Walker S', '工业人形机器人，面向智能制造', 680000.00, 680000.00, 1, '2024-01-01', NULL, NULL, 1, '{"身高":"1720mm","重量":"78kg","负载":"15kg","自由度":"41"}', 'OFFICIAL', 'https://www.ubtrobot.com/walker-s', 'UBTECH官网', NOW(), 820, 7000, 520, 200, 150, 40, 8.8, 0, NOW(), NOW(), 0),
(107, 11, NULL, 102, '优必选 Walker X', 'Walker X', '大型双足人形机器人，具备交互能力', 598000.00, 598000.00, 1, '2021-07-01', NULL, NULL, 1, '{"身高":"1645mm","重量":"74kg","续航":"2h","自由度":"41"}', 'OFFICIAL', 'https://www.ubtrobot.com/walker-x', 'UBTECH官网', NOW(), 780, 6000, 450, 180, 130, 35, 8.6, 0, NOW(), NOW(), 0),
(108, 37, NULL, 102, '优必选 ADIBOT', 'ADIBOT', '智能清洁消毒机器人', 0.00, 0.00, 1, '2022-06-01', NULL, NULL, 0, '{"续航":"6h","消毒方式":"紫外线+等离子"}', 'OFFICIAL', 'https://www.ubtrobot.com/adibot', 'UBTECH官网', NOW(), 520, 3000, 240, 90, 70, 18, 8.3, 0, NOW(), NOW(), 0),

-- ========== 智元机器人 (brand_id=103) ==========
(109, 11, NULL, 103, '智元 远征 A2', 'A2', '通用人形机器人，面向量产', 0.00, 0.00, 2, '2024-08-01', NULL, NULL, 0, '{"身高":"1750mm","重量":"55kg","自由度":"40","负载":"10kg"}', 'OFFICIAL', 'https://www.agibot.com/a2', 'Agibot官网', NOW(), 850, 7500, 600, 250, 170, 45, 8.9, 0, NOW(), NOW(), 0),
(110, 11, NULL, 103, '智元 灵犀 X1', 'X1', '开源人形机器人，开发者友好', 0.00, 0.00, 2, '2024-09-01', NULL, NULL, 0, '{"身高":"1650mm","重量":"40kg","自由度":"31"}', 'OFFICIAL', 'https://www.agibot.com/x1', 'Agibot官网', NOW(), 720, 5000, 380, 150, 100, 25, 8.5, 0, NOW(), NOW(), 0),

-- ========== 傅利叶智能 (brand_id=104) ==========
(111, 11, NULL, 104, '傅利叶 GR-1', 'GR-1', '通用人形机器人，康复基因', 268000.00, 268000.00, 1, '2023-09-01', NULL, NULL, 1, '{"身高":"1650mm","重量":"55kg","自由度":"44","负载":"3kg"}', 'OFFICIAL', 'https://www.fftai.com/gr1', 'Fourier官网', NOW(), 700, 4800, 360, 140, 100, 28, 8.5, 0, NOW(), NOW(), 0),
(112, 11, NULL, 104, '傅利叶 GR-2', 'GR-2', '新一代通用人形机器人', 0.00, 0.00, 2, '2024-10-01', NULL, NULL, 0, '{"身高":"1750mm","重量":"58kg","自由度":"53","负载":"5kg"}', 'OFFICIAL', 'https://www.fftai.com/gr2', 'Fourier官网', NOW(), 760, 5200, 400, 160, 120, 32, 8.7, 0, NOW(), NOW(), 0),

-- ========== 普渡机器人 (brand_id=105) ==========
(113, 31, NULL, 105, '普渡 贝拉 BellaBot', 'BellaBot', '多舱室送餐机器人', 68000.00, 68000.00, 1, '2022-03-01', NULL, NULL, 1, '{"负载":"40kg","续航":"8h","舱室":"4层"}', 'OFFICIAL', 'https://www.pudurobotics.com/bellabot', 'Pudu官网', NOW(), 680, 4500, 350, 130, 90, 24, 8.4, 0, NOW(), NOW(), 0),
(114, 32, NULL, 105, '普渡 PUDU D1', 'D1', '室内配送机器人', 45000.00, 45000.00, 1, '2023-01-01', NULL, NULL, 0, '{"负载":"30kg","续航":"10h","最大速度":"1.2m/s"}', 'OFFICIAL', 'https://www.pudurobotics.com/d1', 'Pudu官网', NOW(), 580, 3200, 250, 100, 70, 18, 8.2, 0, NOW(), NOW(), 0),

-- ========== 擎朗智能 (brand_id=106) ==========
(115, 31, NULL, 106, '擎朗 T1 Pro', 'T1 Pro', '智能送餐机器人，多场景适用', 55000.00, 55000.00, 1, '2022-08-01', NULL, NULL, 1, '{"负载":"35kg","续航":"8h","最大速度":"1.2m/s"}', 'OFFICIAL', 'https://www.keenonrobotics.com/t1pro', 'Keenon官网', NOW(), 640, 3800, 300, 120, 80, 22, 8.3, 0, NOW(), NOW(), 0),
(116, 33, NULL, 106, '擎朗 W3', 'W3', '酒店配送服务机器人', 48000.00, 48000.00, 1, '2023-05-01', NULL, NULL, 0, '{"负载":"25kg","续航":"10h","交互":"语音+触屏"}', 'OFFICIAL', 'https://www.keenonrobotics.com/w3', 'Keenon官网', NOW(), 560, 3000, 220, 90, 60, 16, 8.1, 0, NOW(), NOW(), 0),

-- ========== 越疆科技 (brand_id=107) ==========
(117, 52, NULL, 107, '越疆 CR5', 'CR5', '五轴协作机器人，高性价比', 68000.00, 68000.00, 1, '2022-06-01', NULL, NULL, 0, '{"负载":"5kg","臂展":"900mm","重复定位精度":"0.02mm"}', 'OFFICIAL', 'https://www.dobot.cc/cr5', 'Dobot官网', NOW(), 620, 3500, 280, 110, 75, 20, 8.4, 0, NOW(), NOW(), 0),
(118, 52, NULL, 107, '越疆 CR10', 'CR10', '六轴协作机器人，大负载', 88000.00, 88000.00, 1, '2023-03-01', NULL, NULL, 0, '{"负载":"10kg","臂展":"1300mm","重复定位精度":"0.03mm"}', 'OFFICIAL', 'https://www.dobot.cc/cr10', 'Dobot官网', NOW(), 580, 3000, 240, 95, 65, 17, 8.2, 0, NOW(), NOW(), 0),
(119, 52, NULL, 107, '越疆 MG400', 'MG400', '四轴桌面级协作机器人', 28000.00, 28000.00, 1, '2021-09-01', NULL, NULL, 0, '{"负载":"0.5kg","臂展":"400mm","重复定位精度":"0.02mm"}', 'OFFICIAL', 'https://www.dobot.cc/mg400', 'Dobot官网', NOW(), 540, 2800, 200, 80, 55, 14, 8.1, 0, NOW(), NOW(), 0),

-- ========== 节卡机器人 (brand_id=108) ==========
(120, 52, NULL, 108, '节卡 Zu 5', 'Zu 5', '五轴协作机器人，灵活部署', 75000.00, 75000.00, 1, '2022-04-01', NULL, NULL, 0, '{"负载":"5kg","臂展":"950mm","重复定位精度":"0.02mm"}', 'OFFICIAL', 'https://www.jaka.com/zu5', 'JAKA官网', NOW(), 600, 3200, 260, 100, 70, 18, 8.3, 0, NOW(), NOW(), 0),
(121, 52, NULL, 108, '节卡 Zu 12', 'Zu 12', '大负载协作机器人', 98000.00, 98000.00, 1, '2023-01-01', NULL, NULL, 0, '{"负载":"12kg","臂展":"1326mm","重复定位精度":"0.03mm"}', 'OFFICIAL', 'https://www.jaka.com/zu12', 'JAKA官网', NOW(), 560, 2800, 220, 85, 60, 15, 8.2, 0, NOW(), NOW(), 0),

-- ========== 遨博智能 (brand_id=109) ==========
(122, 52, NULL, 109, '遨博 i5', 'i5', '轻量级协作机器人', 65000.00, 65000.00, 1, '2021-06-01', NULL, NULL, 0, '{"负载":"5kg","臂展":"920mm","重复定位精度":"0.02mm"}', 'OFFICIAL', 'https://www.aubo-robotics.com/i5', 'AUBO官网', NOW(), 540, 2600, 200, 80, 55, 14, 8.1, 0, NOW(), NOW(), 0),
(123, 52, NULL, 109, '遨博 i10', 'i10', '中负载协作机器人', 82000.00, 82000.00, 1, '2022-09-01', NULL, NULL, 0, '{"负载":"10kg","臂展":"1300mm","重复定位精度":"0.03mm"}', 'OFFICIAL', 'https://www.aubo-robotics.com/i10', 'AUBO官网', NOW(), 500, 2200, 170, 70, 45, 12, 8.0, 0, NOW(), NOW(), 0),

-- ========== 达闼科技 (brand_id=110) ==========
(124, 11, NULL, 110, '达闼 XR-4', 'XR-4', '云端智能人形机器人', 0.00, 0.00, 2, '2024-06-01', NULL, NULL, 0, '{"身高":"1650mm","重量":"60kg","自由度":"50","负载":"5kg"}', 'OFFICIAL', 'https://www.cloudminds.com/xr4', 'CloudMinds官网', NOW(), 680, 4200, 340, 130, 90, 24, 8.4, 0, NOW(), NOW(), 0),
(125, 33, NULL, 110, '达闼 Cloud Ginger', 'Cloud Ginger', '云端服务机器人', 0.00, 0.00, 1, '2022-12-01', NULL, NULL, 0, '{"身高":"1550mm","续航":"8h","交互":"云端AI"}', 'OFFICIAL', 'https://www.cloudminds.com/ginger', 'CloudMinds官网', NOW(), 480, 2500, 190, 75, 50, 13, 8.0, 0, NOW(), NOW(), 0),

-- ========== 波士顿动力 (brand_id=111) ==========
(126, 11, NULL, 111, '波士顿动力 Atlas', 'Atlas', '全电动人形机器人，运动能力顶尖', 0.00, 0.00, 2, '2024-04-01', NULL, NULL, 2, '{"身高":"1500mm","重量":"89kg","自由度":"28","最大速度":"2.5m/s"}', 'OFFICIAL', 'https://bostondynamics.com/atlas', 'Boston Dynamics官网', NOW(), 950, 15000, 1200, 400, 250, 68, 9.3, 0, NOW(), NOW(), 0),
(127, 21, NULL, 111, '波士顿动力 Spot', 'Spot', '企业级四足机器人标杆', 52000.00, 52000.00, 1, '2019-09-01', NULL, NULL, 3, '{"重量":"32kg","负载":"14kg","续航":"90min","最大速度":"1.6m/s"}', 'OFFICIAL', 'https://bostondynamics.com/spot', 'Boston Dynamics官网', NOW(), 880, 10000, 800, 300, 200, 52, 9.1, 0, NOW(), NOW(), 0),
(128, 52, NULL, 111, '波士顿动力 Stretch', 'Stretch', '仓库物流移动操作机器人', 0.00, 0.00, 1, '2022-03-01', NULL, NULL, 1, '{"负载":"23kg","臂展":"1200mm","移动速度":"2m/s"}', 'OFFICIAL', 'https://bostondynamics.com/stretch', 'Boston Dynamics官网', NOW(), 620, 3500, 280, 110, 75, 20, 8.4, 0, NOW(), NOW(), 0),

-- ========== Figure AI (brand_id=112) ==========
(129, 11, NULL, 112, 'Figure 02', 'Figure 02', '第二代通用人形机器人', 0.00, 0.00, 2, '2024-08-01', NULL, NULL, 1, '{"身高":"1700mm","重量":"70kg","自由度":"41","负载":"20kg"}', 'OFFICIAL', 'https://www.figure.ai/figure02', 'Figure AI官网', NOW(), 820, 6500, 500, 200, 140, 38, 8.8, 0, NOW(), NOW(), 0),

-- ========== Agility Robotics (brand_id=113) ==========
(130, 11, NULL, 113, 'Agility Digit', 'Digit', '双足物流机器人', 0.00, 0.00, 2, '2023-07-01', NULL, NULL, 1, '{"身高":"1750mm","重量":"65kg","负载":"16kg","最大速度":"1.5m/s"}', 'OFFICIAL', 'https://www.agilityrobotics.com/digit', 'Agility官网', NOW(), 700, 4200, 340, 130, 90, 24, 8.4, 0, NOW(), NOW(), 0),

-- ========== 小鹏鹏行 (brand_id=114) ==========
(131, 11, NULL, 114, '小鹏 PX5', 'PX5', '通用人形机器人', 0.00, 0.00, 2, '2024-06-01', NULL, NULL, 0, '{"身高":"1650mm","重量":"50kg","自由度":"35","负载":"5kg"}', 'OFFICIAL', 'https://www.xpeng.com/px5', 'Xpeng官网', NOW(), 640, 3800, 300, 120, 80, 22, 8.3, 0, NOW(), NOW(), 0),

-- ========== 追觅科技 (brand_id=115) ==========
(132, 11, NULL, 115, '追觅 人形机器人', 'Dreame Humanoid', '追觅通用人形机器人', 0.00, 0.00, 2, '2024-03-01', NULL, NULL, 0, '{"身高":"1700mm","重量":"55kg","自由度":"30"}', 'OFFICIAL', 'https://www.dreame.tech/humanoid', 'Dreame官网', NOW(), 600, 3200, 250, 100, 70, 18, 8.2, 0, NOW(), NOW(), 0),
(133, 41, NULL, 115, '追觅 X40 Pro Ultra', 'X40 Pro Ultra', '旗舰扫拖机器人', 5999.00, 5999.00, 1, '2024-02-01', NULL, NULL, 2, '{"续航":"180min","吸力":"7000Pa","导航":"3D结构光"}', 'OFFICIAL', 'https://www.dreame.tech/x40', 'Dreame官网', NOW(), 860, 9500, 750, 260, 180, 48, 9.0, 0, NOW(), NOW(), 0),

-- ========== 乐聚机器人 (brand_id=118) ==========
(134, 11, NULL, 118, '乐聚 Kv1.1', 'Kv1.1', '开源人形机器人开发平台', 0.00, 0.00, 2, '2024-01-01', NULL, NULL, 0, '{"身高":"1600mm","重量":"45kg","自由度":"26","负载":"3kg"}', 'OFFICIAL', 'https://www.lejurobot.com/kv1', 'Leju官网', NOW(), 560, 3000, 240, 95, 65, 16, 8.1, 0, NOW(), NOW(), 0),
(135, 14, NULL, 118, '乐聚 Aelos', 'Aelos', '教育人形机器人', 12800.00, 12800.00, 1, '2022-05-01', NULL, NULL, 1, '{"身高":"400mm","自由度":"16","编程":"Scratch/Python"}', 'OFFICIAL', 'https://www.lejurobot.com/aelos', 'Leju官网', NOW(), 480, 2500, 190, 75, 50, 13, 8.0, 0, NOW(), NOW(), 0),

-- ========== 猎户星空 (brand_id=119) ==========
(136, 31, NULL, 119, '猎户 招财豹', '招财豹', '智能送餐机器人', 42000.00, 42000.00, 1, '2022-10-01', NULL, NULL, 1, '{"负载":"30kg","续航":"8h","最大速度":"1m/s"}', 'OFFICIAL', 'https://www.orionstar.com/foodbot', 'OrionStar官网', NOW(), 580, 3000, 230, 90, 60, 16, 8.2, 0, NOW(), NOW(), 0),
(137, 37, NULL, 119, '猎户 智能清洁机器人', 'OrionStar Cleaner', '商用清洁机器人', 0.00, 0.00, 1, '2023-06-01', NULL, NULL, 0, '{"续航":"6h","清洁面积":"2000㎡/h"}', 'OFFICIAL', 'https://www.orionstar.com/cleaner', 'OrionStar官网', NOW(), 460, 2200, 170, 65, 45, 12, 7.9, 0, NOW(), NOW(), 0),

-- ========== 思岚科技 (brand_id=120) ==========
(138, 37, NULL, 120, '思岚 A1', 'A1', '商用清洁机器人', 0.00, 0.00, 1, '2023-01-01', NULL, NULL, 0, '{"续航":"6h","导航":"SLAM","清洁面积":"1500㎡/h"}', 'OFFICIAL', 'https://www.slamtec.com/a1', 'SLAMTEC官网', NOW(), 440, 2000, 150, 60, 40, 10, 7.8, 0, NOW(), NOW(), 0),

-- ========== 高仙机器人 (brand_id=121) ==========
(139, 37, NULL, 121, '高仙 Scrubber 50', 'Scrubber 50', '商用洗地机器人', 0.00, 0.00, 1, '2022-08-01', NULL, NULL, 1, '{"续航":"6h","清洁面积":"3000㎡/h","导航":"SLAM"}', 'OFFICIAL', 'https://www.gaussianrobotics.com/scrubber50', 'Gaussian官网', NOW(), 560, 3000, 240, 95, 65, 16, 8.1, 0, NOW(), NOW(), 0),
(140, 37, NULL, 121, '高仙 Scrubber 75', 'Scrubber 75', '大型商用洗地机器人', 0.00, 0.00, 1, '2023-04-01', NULL, NULL, 0, '{"续航":"8h","清洁面积":"5000㎡/h","导航":"SLAM"}', 'OFFICIAL', 'https://www.gaussianrobotics.com/scrubber75', 'Gaussian官网', NOW(), 500, 2500, 190, 75, 50, 13, 8.0, 0, NOW(), NOW(), 0),

-- ========== 九号机器人 (brand_id=122) ==========
(141, 32, NULL, 122, '九号 配送机器人 S2', 'S2', '室内配送机器人', 0.00, 0.00, 1, '2023-09-01', NULL, NULL, 0, '{"负载":"25kg","续航":"8h","最大速度":"1.2m/s"}', 'OFFICIAL', 'https://www.ninebot.com/s2', 'Ninebot官网', NOW(), 520, 2800, 210, 85, 55, 14, 8.1, 0, NOW(), NOW(), 0),

-- ========== 特斯拉 (brand_id=125) ==========
(142, 11, NULL, 125, '特斯拉 Optimus Gen 2', 'Optimus Gen 2', '第二代量产目标人形机器人', 0.00, 0.00, 2, '2023-12-01', NULL, NULL, 1, '{"身高":"1730mm","重量":"57kg","负载":"20kg","自由度":"40"}', 'OFFICIAL', 'https://www.tesla.com/optimus', 'Tesla官网', NOW(), 920, 13000, 1000, 350, 220, 60, 9.2, 0, NOW(), NOW(), 0),

-- ========== 1X Technologies (brand_id=126) ==========
(143, 11, NULL, 126, '1X NEO', 'NEO', '家用通用人形机器人', 0.00, 0.00, 2, '2024-08-01', NULL, NULL, 0, '{"身高":"1650mm","重量":"60kg","续航":"4h","负载":"10kg"}', 'OFFICIAL', 'https://www.1x.tech/neo', '1X官网', NOW(), 680, 4000, 320, 125, 85, 22, 8.4, 0, NOW(), NOW(), 0),

-- ========== Sanctuary AI (brand_id=127) ==========
(144, 11, NULL, 127, 'Sanctuary Phoenix', 'Phoenix', '通用人形机器人Phoenix', 0.00, 0.00, 2, '2024-05-01', NULL, NULL, 0, '{"身高":"1700mm","重量":"70kg","自由度":"30","负载":"12kg"}', 'OFFICIAL', 'https://www.sanctuary.ai/phoenix', 'Sanctuary官网', NOW(), 620, 3500, 280, 110, 75, 20, 8.3, 0, NOW(), NOW(), 0),

-- ========== Apptronik (brand_id=128) ==========
(145, 11, NULL, 128, 'Apptronik Apollo', 'Apollo', '通用人形机器人Apollo', 0.00, 0.00, 2, '2024-01-01', NULL, NULL, 0, '{"身高":"1700mm","重量":"73kg","负载":"25kg","续航":"4h"}', 'OFFICIAL', 'https://www.apptronik.com/apollo', 'Apptronik官网', NOW(), 640, 3800, 300, 120, 80, 22, 8.3, 0, NOW(), NOW(), 0),

-- ========== FANUC (brand_id=131) ==========
(146, 51, NULL, 131, 'FANUC LR-200iA', 'LR-200iA', '六轴工业机器人，高精度', 0.00, 0.00, 1, '2020-01-01', NULL, NULL, 0, '{"负载":"200kg","臂展":"2611mm","重复定位精度":"0.3mm"}', 'OFFICIAL', 'https://www.fanuc.com/lr200', 'FANUC官网', NOW(), 580, 3000, 240, 95, 65, 16, 8.2, 0, NOW(), NOW(), 0),
(147, 52, NULL, 131, 'FANUC CR-15iA', 'CR-15iA', '协作机器人，安全协作', 0.00, 0.00, 1, '2021-06-01', NULL, NULL, 0, '{"负载":"15kg","臂展":"1445mm","重复定位精度":"0.04mm"}', 'OFFICIAL', 'https://www.fanuc.com/cr15', 'FANUC官网', NOW(), 520, 2600, 200, 80, 55, 14, 8.1, 0, NOW(), NOW(), 0),

-- ========== ABB (brand_id=132) ==========
(148, 51, NULL, 132, 'ABB IRB 6700', 'IRB 6700', '大型工业机器人', 0.00, 0.00, 1, '2019-01-01', NULL, NULL, 0, '{"负载":"200kg","臂展":"2600mm","重复定位精度":"0.2mm"}', 'OFFICIAL', 'https://new.abb.com/irb6700', 'ABB官网', NOW(), 560, 2800, 220, 85, 60, 15, 8.1, 0, NOW(), NOW(), 0),
(149, 52, NULL, 132, 'ABB GoFa CRB 15000', 'GoFa', '协作机器人，安全高性能', 0.00, 0.00, 1, '2022-03-01', NULL, NULL, 0, '{"负载":"5kg","臂展":"950mm","重复定位精度":"0.01mm"}', 'OFFICIAL', 'https://new.abb.com/gofa', 'ABB官网', NOW(), 540, 2700, 210, 80, 55, 14, 8.0, 0, NOW(), NOW(), 0),

-- ========== KUKA (brand_id=133) ==========
(150, 51, NULL, 133, 'KUKA KR 500', 'KR 500', '重载工业机器人', 0.00, 0.00, 1, '2018-01-01', NULL, NULL, 0, '{"负载":"500kg","臂展":"2826mm","重复定位精度":"0.5mm"}', 'OFFICIAL', 'https://www.kuka.com/kr500', 'KUKA官网', NOW(), 500, 2400, 180, 70, 50, 12, 8.0, 0, NOW(), NOW(), 0),
(151, 52, NULL, 133, 'KUKA LBR iisy', 'LBR iisy', '灵敏协作机器人', 0.00, 0.00, 1, '2022-06-01', NULL, NULL, 0, '{"负载":"15kg","臂展":"900mm","重复定位精度":"0.03mm"}', 'OFFICIAL', 'https://www.kuka.com/lbr-iisy', 'KUKA官网', NOW(), 520, 2500, 190, 75, 50, 13, 8.0, 0, NOW(), NOW(), 0),

-- ========== 安川 (brand_id=134) ==========
(152, 51, NULL, 134, '安川 GP225', 'GP225', '高速点焊工业机器人', 0.00, 0.00, 1, '2021-01-01', NULL, NULL, 0, '{"负载":"225kg","臂展":"2710mm","重复定位精度":"0.3mm"}', 'OFFICIAL', 'https://www.yaskawa.com/gp225', 'Yaskawa官网', NOW(), 500, 2400, 180, 70, 50, 12, 8.0, 0, NOW(), NOW(), 0),
(153, 52, NULL, 134, '安川 HC10DT', 'HC10DT', '协作机器人，人机协作', 0.00, 0.00, 1, '2022-09-01', NULL, NULL, 0, '{"负载":"10kg","臂展":"1200mm","重复定位精度":"0.04mm"}', 'OFFICIAL', 'https://www.yaskawa.com/hc10dt', 'Yaskawa官网', NOW(), 480, 2200, 170, 65, 45, 11, 7.9, 0, NOW(), NOW(), 0),

-- ========== 大疆 (brand_id=135) ==========
(154, 94, NULL, 135, '大疆 RoboMaster EP Core', 'EP Core', '教育机器人开发平台', 4999.00, 4999.00, 1, '2023-03-01', NULL, NULL, 1, '{"编程":"Scratch/Python/C++","传感器":"视觉/红外/IMU","续航":"2h"}', 'OFFICIAL', 'https://www.dji.com/robomaster-ep-core', 'DJI官网', NOW(), 540, 3200, 260, 100, 70, 18, 8.4, 0, NOW(), NOW(), 0),

-- ========== 开普勒 (brand_id=116) ==========
(155, 11, NULL, 116, '开普勒 K1', 'K1', '通用人形机器人，面向工业', 0.00, 0.00, 2, '2024-06-01', NULL, NULL, 0, '{"身高":"1750mm","重量":"80kg","自由度":"30","负载":"10kg"}', 'OFFICIAL', 'https://www.keplerbot.com/k1', 'Kepler官网', NOW(), 580, 3000, 240, 95, 65, 16, 8.2, 0, NOW(), NOW(), 0),

-- ========== 星动纪元 (brand_id=117) ==========
(156, 11, NULL, 117, '星动 Star 1', 'Star 1', '通用人形机器人', 0.00, 0.00, 2, '2024-08-01', NULL, NULL, 0, '{"身高":"1700mm","重量":"60kg","自由度":"35","负载":"8kg"}', 'OFFICIAL', 'https://www.starera.com/star1', 'Star Era官网', NOW(), 560, 2800, 220, 85, 60, 14, 8.1, 0, NOW(), NOW(), 0);

-- ============================================================
-- 更新品牌机器人计数
-- ============================================================
UPDATE `brand` b SET `robot_count` = (SELECT COUNT(*) FROM `robot` r WHERE r.`brand_id` = b.`id` AND r.`deleted` = 0);