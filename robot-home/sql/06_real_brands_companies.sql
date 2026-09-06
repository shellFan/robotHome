-- ============================================================
-- 机器人之家 Phase 2：真实品牌 + 企业 + 品牌别名数据
-- 数据来源：企业官网公开信息
-- 兼容 MySQL 5.6
-- ============================================================
USE robot_home;

-- 标记 demo 数据
UPDATE `brand` SET `data_source` = 'DEMO' WHERE `data_source` = 'MANUAL' AND `id` <= 20;

-- ============================================================
-- 品牌数据（35 个真实机器人品牌）
-- 列顺序: id, name, brand_name_en, short_name, logo, website, company_id, country, intro, found_year, data_source, source_url, last_verified_time, create_time, update_time, deleted, status
-- ============================================================

INSERT INTO `brand` (`id`, `name`, `brand_name_en`, `short_name`, `logo`, `website`, `company_id`, `country`, `intro`, `found_year`, `data_source`, `source_url`, `last_verified_time`, `create_time`, `update_time`, `deleted`, `status`) VALUES
(101, '宇树科技', 'Unitree Robotics', '宇树', NULL, 'https://www.unitree.com', NULL, '中国', '宇树科技成立于2017年，专注于高性能四足机器人和人形机器人的研发与制造。', 2017, 'OFFICIAL', 'https://www.unitree.com', NOW(), NOW(), NOW(), 0, 1),
(102, '优必选', 'UBTECH Robotics', '优必选', NULL, 'https://www.ubtrobot.com', NULL, '中国', '优必选科技成立于2012年，是全球领先的人工智能和人形机器人企业。', 2012, 'OFFICIAL', 'https://www.ubtrobot.com', NOW(), NOW(), NOW(), 0, 1),
(103, '智元机器人', 'Agibot', '智元', NULL, 'https://www.agibot.com', NULL, '中国', '智元机器人成立于2023年，致力于通用人形机器人的研发。', 2023, 'OFFICIAL', 'https://www.agibot.com', NOW(), NOW(), NOW(), 0, 1),
(104, '傅利叶智能', 'Fourier Intelligence', '傅利叶', NULL, 'https://www.fftai.com', NULL, '中国', '傅利叶智能成立于2015年，专注于康复机器人和通用人形机器人GR-1。', 2015, 'OFFICIAL', 'https://www.fftai.com', NOW(), NOW(), NOW(), 0, 1),
(105, '普渡机器人', 'Pudu Robotics', '普渡', NULL, 'https://www.pudurobotics.com', NULL, '中国', '普渡科技成立于2016年，是全球领先的商用服务机器人企业。', 2016, 'OFFICIAL', 'https://www.pudurobotics.com', NOW(), NOW(), NOW(), 0, 1),
(106, '擎朗智能', 'Keenon Robotics', '擎朗', NULL, 'https://www.keenonrobotics.com', NULL, '中国', '擎朗智能成立于2010年，专注于室内服务机器人。', 2010, 'OFFICIAL', 'https://www.keenonrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(107, '越疆科技', 'Dobot', '越疆', NULL, 'https://www.dobot.cc', NULL, '中国', '越疆科技成立于2015年，是全球领先的轻量型智能机械臂企业。', 2015, 'OFFICIAL', 'https://www.dobot.cc', NOW(), NOW(), NOW(), 0, 1),
(108, '节卡机器人', 'JAKA Robotics', '节卡', NULL, 'https://www.jaka.com', NULL, '中国', '节卡机器人成立于2014年，专注于协作机器人研发。', 2014, 'OFFICIAL', 'https://www.jaka.com', NOW(), NOW(), NOW(), 0, 1),
(109, '遨博智能', 'AUBO Robotics', '遨博', NULL, 'https://www.aubo-robotics.com', NULL, '中国', '遨博智能成立于2015年，专注于轻量级协作机器人。', 2015, 'OFFICIAL', 'https://www.aubo-robotics.com', NOW(), NOW(), NOW(), 0, 1),
(110, '达闼科技', 'CloudMinds', '达闼', NULL, 'https://www.cloudminds.com', NULL, '中国', '达闼科技成立于2015年，是全球首家云端智能机器人运营商。', 2015, 'OFFICIAL', 'https://www.cloudminds.com', NOW(), NOW(), NOW(), 0, 1),
(111, '波士顿动力', 'Boston Dynamics', 'BD', NULL, 'https://www.bostondynamics.com', NULL, '美国', 'Boston Dynamics成立于1992年，以Spot四足机器人和Atlas人形机器人闻名。', 1992, 'OFFICIAL', 'https://www.bostondynamics.com', NOW(), NOW(), NOW(), 0, 1),
(112, 'Figure AI', 'Figure AI', 'Figure', NULL, 'https://www.figure.ai', NULL, '美国', 'Figure AI成立于2022年，专注于开发通用人形机器人Figure 01/02。', 2022, 'OFFICIAL', 'https://www.figure.ai', NOW(), NOW(), NOW(), 0, 1),
(113, 'Agility Robotics', 'Agility Robotics', 'Agility', NULL, 'https://www.agilityrobotics.com', NULL, '美国', 'Agility Robotics成立于2015年，开发双足机器人Digit。', 2015, 'OFFICIAL', 'https://www.agilityrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(114, '小鹏鹏行', 'Xpeng Robotics', '鹏行', NULL, 'https://www.xpeng.com', NULL, '中国', '小鹏鹏行是小鹏汽车旗下机器人业务板块。', 2020, 'OFFICIAL', 'https://www.xpeng.com', NOW(), NOW(), NOW(), 0, 1),
(115, '追觅科技', 'Dreame Technology', '追觅', NULL, 'https://www.dreame.tech', NULL, '中国', '追觅科技成立于2017年，以智能清洁机器人起家，已扩展至人形机器人领域。', 2017, 'OFFICIAL', 'https://www.dreame.tech', NOW(), NOW(), NOW(), 0, 1),
(116, '开普勒机器人', 'Kepler Robotics', '开普勒', NULL, 'https://www.keplerbot.com', NULL, '中国', '开普勒机器人专注于通用人形机器人研发。', 2023, 'OFFICIAL', 'https://www.keplerbot.com', NOW(), NOW(), NOW(), 0, 1),
(117, '星动纪元', 'Star Era Robotics', '星动', NULL, 'https://www.starera.com', NULL, '中国', '星动纪元成立于2023年，专注于人形机器人研发。', 2023, 'OFFICIAL', 'https://www.starera.com', NOW(), NOW(), NOW(), 0, 1),
(118, '乐聚机器人', 'Leju Robotics', '乐聚', NULL, 'https://www.lejurobot.com', NULL, '中国', '乐聚机器人成立于2016年，专注于人形机器人和AI教育。', 2016, 'OFFICIAL', 'https://www.lejurobot.com', NOW(), NOW(), NOW(), 0, 1),
(119, '猎户星空', 'OrionStar', '猎户', NULL, 'https://www.orionstar.com', NULL, '中国', '猎户星空成立于2016年，是猎豹移动旗下智能服务机器人公司。', 2016, 'OFFICIAL', 'https://www.orionstar.com', NOW(), NOW(), NOW(), 0, 1),
(120, '思岚科技', 'SLAMTEC', '思岚', NULL, 'https://www.slamtec.com', NULL, '中国', '思岚科技成立于2013年，专注于机器人自主定位导航技术。', 2013, 'OFFICIAL', 'https://www.slamtec.com', NOW(), NOW(), NOW(), 0, 1),
(121, '高仙机器人', 'Gaussian Robotics', '高仙', NULL, 'https://www.gaussianrobotics.com', NULL, '中国', '高仙机器人成立于2013年，专注于商用清洁机器人。', 2013, 'OFFICIAL', 'https://www.gaussianrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(122, '九号机器人', 'Ninebot', '九号', NULL, 'https://www.ninebot.com', NULL, '中国', '九号机器人成立于2012年，专注于智能短交通和配送机器人。', 2012, 'OFFICIAL', 'https://www.ninebot.com', NOW(), NOW(), NOW(), 0, 1),
(123, '丰田机器人', 'Toyota Robotics', '丰田机器人', NULL, 'https://robotics.toyota', NULL, '日本', '丰田机器人部门专注于人形机器人和辅助机器人研发。', 2017, 'OFFICIAL', 'https://robotics.toyota', NOW(), NOW(), NOW(), 0, 1),
(124, '本田机器人', 'Honda Robotics', '本田机器人', NULL, 'https://global.honda/robotics', NULL, '日本', '本田自1986年起研发人形机器人ASIMO。', 1986, 'OFFICIAL', 'https://global.honda/robotics', NOW(), NOW(), NOW(), 0, 1),
(125, '特斯拉机器人', 'Tesla Optimus', 'Optimus', NULL, 'https://www.tesla.com/optimus', NULL, '美国', 'Tesla Optimus是特斯拉开发的人形机器人项目。', 2021, 'OFFICIAL', 'https://www.tesla.com/optimus', NOW(), NOW(), NOW(), 0, 1),
(126, '1X Technologies', '1X Technologies', '1X', NULL, 'https://www.1x.tech', NULL, '挪威', '1X Technologies成立于2014年，开发人形机器人NEO和Eve。', 2014, 'OFFICIAL', 'https://www.1x.tech', NOW(), NOW(), NOW(), 0, 1),
(127, 'Sanctuary AI', 'Sanctuary AI', 'Sanctuary', NULL, 'https://www.sanctuary.ai', NULL, '加拿大', 'Sanctuary AI成立于2018年，专注于通用人形机器人Phoenix。', 2018, 'OFFICIAL', 'https://www.sanctuary.ai', NOW(), NOW(), NOW(), 0, 1),
(128, 'Apptronik', 'Apptronik', 'Apptronik', NULL, 'https://www.apptronik.com', NULL, '美国', 'Apptronik成立于2016年，推出Apollo通用人形机器人。', 2016, 'OFFICIAL', 'https://www.apptronik.com', NOW(), NOW(), NOW(), 0, 1),
(129, '帕西尼感知', 'PaXini Technology', '帕西尼', NULL, 'https://www.paxini.com', NULL, '中国', '帕西尼感知专注于多维触觉感知技术与人形机器人研发。', 2023, 'OFFICIAL', 'https://www.paxini.com', NOW(), NOW(), NOW(), 0, 1),
(130, '穹彻智能', 'Qiongche Intelligence', '穹彻', NULL, 'https://www.qiongche.com', NULL, '中国', '穹彻智能专注于通用人形机器人研发，聚焦具身智能技术。', 2023, 'OFFICIAL', 'https://www.qiongche.com', NOW(), NOW(), NOW(), 0, 1),
(131, '发那科', 'FANUC', 'FANUC', NULL, 'https://www.fanuc.com', NULL, '日本', 'FANUC是全球最大的工业机器人制造商之一。', 1972, 'OFFICIAL', 'https://www.fanuc.com', NOW(), NOW(), NOW(), 0, 1),
(132, 'ABB机器人', 'ABB Robotics', 'ABB', NULL, 'https://new.abb.com/products/robotics', NULL, '瑞士', 'ABB是全球领先的工业机器人制造商。', 1974, 'OFFICIAL', 'https://new.abb.com/products/robotics', NOW(), NOW(), NOW(), 0, 1),
(133, '库卡', 'KUKA Robotics', 'KUKA', NULL, 'https://www.kuka.com', NULL, '德国', 'KUKA是全球领先的工业机器人制造商之一。', 1898, 'OFFICIAL', 'https://www.kuka.com', NOW(), NOW(), NOW(), 0, 1),
(134, '安川电机', 'Yaskawa Electric', '安川', NULL, 'https://www.yaskawa.com', NULL, '日本', '安川电机是全球四大工业机器人巨头之一。', 1915, 'OFFICIAL', 'https://www.yaskawa.com', NOW(), NOW(), NOW(), 0, 1),
(135, '大疆', 'DJI', '大疆', NULL, 'https://www.dji.com', NULL, '中国', '大疆创新成立于2006年，是全球领先的无人机制造商。', 2006, 'OFFICIAL', 'https://www.dji.com', NOW(), NOW(), NOW(), 0, 1);

-- ============================================================
-- 企业数据（30 个真实机器人企业）
-- 列顺序: id, name, company_name_en, short_name, logo, website, country, province, city, intro, found_year, business_scope, data_source, source_url, last_verified_time, create_time, update_time, deleted, status
-- ============================================================

INSERT INTO `company` (`id`, `name`, `company_name_en`, `short_name`, `logo`, `website`, `country`, `province`, `city`, `intro`, `found_year`, `business_scope`, `data_source`, `source_url`, `last_verified_time`, `create_time`, `update_time`, `deleted`, `status`) VALUES
(101, '宇树科技有限公司', 'Unitree Robotics Co., Ltd.', '宇树科技', NULL, 'https://www.unitree.com', '中国', '浙江省', '杭州市', '宇树科技专注于高性能四足机器人和人形机器人的研发与制造。', 2017, '机器人研发制造', 'OFFICIAL', 'https://www.unitree.com', NOW(), NOW(), NOW(), 0, 1),
(102, '深圳市优必选科技股份有限公司', 'UBTECH Robotics Corporation', '优必选', NULL, 'https://www.ubtrobot.com', '中国', '广东省', '深圳市', '优必选科技是全球领先的人工智能和人形机器人企业。', 2012, 'AI与机器人', 'OFFICIAL', 'https://www.ubtrobot.com', NOW(), NOW(), NOW(), 0, 1),
(103, '上海智元机器人有限公司', 'Agibot Inc.', '智元机器人', NULL, 'https://www.agibot.com', '中国', '上海市', '上海市', '智元机器人致力于通用人形机器人研发。', 2023, '人形机器人', 'OFFICIAL', 'https://www.agibot.com', NOW(), NOW(), NOW(), 0, 1),
(104, '上海傅利叶智能科技有限公司', 'Fourier Intelligence Co., Ltd.', '傅利叶智能', NULL, 'https://www.fftai.com', '中国', '上海市', '上海市', '傅利叶智能专注于康复机器人和通用人形机器人。', 2015, '康复与人形机器人', 'OFFICIAL', 'https://www.fftai.com', NOW(), NOW(), NOW(), 0, 1),
(105, '深圳市普渡科技有限公司', 'Pudu Technology Co., Ltd.', '普渡科技', NULL, 'https://www.pudurobotics.com', '中国', '广东省', '深圳市', '普渡科技是全球领先的商用服务机器人企业。', 2016, '商用服务机器人', 'OFFICIAL', 'https://www.pudurobotics.com', NOW(), NOW(), NOW(), 0, 1),
(106, '上海擎朗智能科技有限公司', 'Keenon Robotics Co., Ltd.', '擎朗智能', NULL, 'https://www.keenonrobotics.com', '中国', '上海市', '上海市', '擎朗智能专注于室内服务机器人。', 2010, '室内服务机器人', 'OFFICIAL', 'https://www.keenonrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(107, '深圳市越疆科技有限公司', 'Dobot Inc.', '越疆科技', NULL, 'https://www.dobot.cc', '中国', '广东省', '深圳市', '越疆科技是全球领先的轻量型智能机械臂企业。', 2015, '协作机器人', 'OFFICIAL', 'https://www.dobot.cc', NOW(), NOW(), NOW(), 0, 1),
(108, '上海节卡机器人科技有限公司', 'JAKA Robotics Co., Ltd.', '节卡机器人', NULL, 'https://www.jaka.com', '中国', '上海市', '上海市', '节卡机器人专注于协作机器人研发。', 2014, '协作机器人', 'OFFICIAL', 'https://www.jaka.com', NOW(), NOW(), NOW(), 0, 1),
(109, '遨博（北京）智能科技有限公司', 'AUBO Robotics Co., Ltd.', '遨博智能', NULL, 'https://www.aubo-robotics.com', '中国', '北京市', '北京市', '遨博智能专注于轻量级协作机器人。', 2015, '协作机器人', 'OFFICIAL', 'https://www.aubo-robotics.com', NOW(), NOW(), NOW(), 0, 1),
(110, '达闼科技有限公司', 'CloudMinds Technology Inc.', '达闼科技', NULL, 'https://www.cloudminds.com', '中国', '北京市', '北京市', '达闼科技是全球首家云端智能机器人运营商。', 2015, '云端智能机器人', 'OFFICIAL', 'https://www.cloudminds.com', NOW(), NOW(), NOW(), 0, 1),
(111, 'Boston Dynamics Inc.', 'Boston Dynamics Inc.', 'Boston Dynamics', NULL, 'https://www.bostondynamics.com', '美国', NULL, 'Waltham, MA', 'Boston Dynamics是全球最知名的机器人公司之一。', 1992, '机器人研发制造', 'OFFICIAL', 'https://www.bostondynamics.com', NOW(), NOW(), NOW(), 0, 1),
(112, 'Figure AI Inc.', 'Figure AI Inc.', 'Figure AI', NULL, 'https://www.figure.ai', '美国', NULL, 'Sunnyvale, CA', 'Figure AI专注于通用人形机器人。', 2022, '人形机器人', 'OFFICIAL', 'https://www.figure.ai', NOW(), NOW(), NOW(), 0, 1),
(113, 'Agility Robotics Inc.', 'Agility Robotics Inc.', 'Agility', NULL, 'https://www.agilityrobotics.com', '美国', NULL, 'Pittsburgh, PA', 'Agility Robotics开发双足机器人Digit。', 2015, '物流机器人', 'OFFICIAL', 'https://www.agilityrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(114, '广州小鹏汽车科技有限公司', 'Xpeng Motors', '小鹏鹏行', NULL, 'https://www.xpeng.com', '中国', '广东省', '广州市', '小鹏鹏行是小鹏汽车旗下机器人业务。', 2020, '汽车与机器人', 'OFFICIAL', 'https://www.xpeng.com', NOW(), NOW(), NOW(), 0, 1),
(115, '追觅科技（苏州）有限公司', 'Dreame Technology Co., Ltd.', '追觅科技', NULL, 'https://www.dreame.tech', '中国', '江苏省', '苏州市', '追觅科技以智能清洁机器人起家，扩展至人形机器人。', 2017, '智能清洁与人形机器人', 'OFFICIAL', 'https://www.dreame.tech', NOW(), NOW(), NOW(), 0, 1),
(116, '上海开普勒机器人有限公司', 'Kepler Robotics', '开普勒机器人', NULL, 'https://www.keplerbot.com', '中国', '上海市', '上海市', '开普勒机器人专注于通用人形机器人。', 2023, '人形机器人', 'OFFICIAL', 'https://www.keplerbot.com', NOW(), NOW(), NOW(), 0, 1),
(117, '深圳星动纪元科技有限公司', 'Star Era Robotics', '星动纪元', NULL, 'https://www.starera.com', '中国', '广东省', '深圳市', '星动纪元专注于人形机器人研发。', 2023, '人形机器人', 'OFFICIAL', 'https://www.starera.com', NOW(), NOW(), NOW(), 0, 1),
(118, '深圳乐聚机器人技术有限公司', 'Leju Robotics Co., Ltd.', '乐聚机器人', NULL, 'https://www.lejurobot.com', '中国', '广东省', '深圳市', '乐聚机器人专注于人形机器人和AI教育。', 2016, '人形机器人与教育', 'OFFICIAL', 'https://www.lejurobot.com', NOW(), NOW(), NOW(), 0, 1),
(119, '猎户星空科技有限公司', 'OrionStar Technology Co., Ltd.', '猎户星空', NULL, 'https://www.orionstar.com', '中国', '北京市', '北京市', '猎户星空是猎豹移动旗下智能服务机器人公司。', 2016, '服务机器人', 'OFFICIAL', 'https://www.orionstar.com', NOW(), NOW(), NOW(), 0, 1),
(120, '上海思岚科技有限公司', 'SLAMTEC Co., Ltd.', '思岚科技', NULL, 'https://www.slamtec.com', '中国', '上海市', '上海市', '思岚科技专注于机器人自主定位导航技术。', 2013, 'SLAM导航', 'OFFICIAL', 'https://www.slamtec.com', NOW(), NOW(), NOW(), 0, 1),
(121, '上海高仙自动化科技发展有限公司', 'Gaussian Robotics Co., Ltd.', '高仙机器人', NULL, 'https://www.gaussianrobotics.com', '中国', '上海市', '上海市', '高仙机器人专注于商用清洁机器人。', 2013, '清洁机器人', 'OFFICIAL', 'https://www.gaussianrobotics.com', NOW(), NOW(), NOW(), 0, 1),
(122, '九号机器人有限公司', 'Ninebot Co., Ltd.', '九号机器人', NULL, 'https://www.ninebot.com', '中国', '北京市', '北京市', '九号机器人专注于智能短交通和配送机器人。', 2012, '短交通与机器人', 'OFFICIAL', 'https://www.ninebot.com', NOW(), NOW(), NOW(), 0, 1),
(123, '丰田汽车株式会社', 'Toyota Motor Corporation', '丰田', NULL, 'https://www.toyota-global.com', '日本', NULL, '丰田市', '丰田机器人部门专注于人形和辅助机器人。', 1937, '汽车与机器人', 'OFFICIAL', 'https://robotics.toyota', NOW(), NOW(), NOW(), 0, 1),
(124, '本田技研工业株式会社', 'Honda Motor Co., Ltd.', '本田', NULL, 'https://www.global.honda', '日本', NULL, '东京', '本田自1986年起研发人形机器人ASIMO。', 1948, '汽车与机器人', 'OFFICIAL', 'https://global.honda/robotics', NOW(), NOW(), NOW(), 0, 1),
(125, 'Tesla Inc.', 'Tesla Inc.', 'Tesla', NULL, 'https://www.tesla.com', '美国', NULL, 'Austin, TX', 'Tesla Optimus人形机器人项目。', 2003, '电动汽车与机器人', 'OFFICIAL', 'https://www.tesla.com/optimus', NOW(), NOW(), NOW(), 0, 1),
(126, '1X Technologies AS', '1X Technologies AS', '1X', NULL, 'https://www.1x.tech', '挪威', NULL, 'Sandnes', '1X Technologies开发人形机器人NEO和Eve。', 2014, '人形机器人', 'OFFICIAL', 'https://www.1x.tech', NOW(), NOW(), NOW(), 0, 1),
(127, 'Sanctuary AI Inc.', 'Sanctuary AI Inc.', 'Sanctuary', NULL, 'https://www.sanctuary.ai', '加拿大', NULL, 'Vancouver, BC', 'Sanctuary AI专注于通用人形机器人Phoenix。', 2018, '人形机器人', 'OFFICIAL', 'https://www.sanctuary.ai', NOW(), NOW(), NOW(), 0, 1),
(128, 'Apptronik Inc.', 'Apptronik Inc.', 'Apptronik', NULL, 'https://www.apptronik.com', '美国', NULL, 'Austin, TX', 'Apptronik开发Apollo通用人形机器人。', 2016, '人形机器人', 'OFFICIAL', 'https://www.apptronik.com', NOW(), NOW(), NOW(), 0, 1),
(131, 'FANUC株式会社', 'FANUC Corporation', 'FANUC', NULL, 'https://www.fanuc.com', '日本', NULL, '山梨县', 'FANUC是全球最大的工业机器人制造商之一。', 1972, '工业机器人', 'OFFICIAL', 'https://www.fanuc.com', NOW(), NOW(), NOW(), 0, 1),
(132, 'ABB有限公司', 'ABB Ltd.', 'ABB', NULL, 'https://new.abb.com', '瑞士', NULL, '苏黎世', 'ABB是全球领先的工业机器人制造商。', 1988, '工业自动化与机器人', 'OFFICIAL', 'https://new.abb.com/products/robotics', NOW(), NOW(), NOW(), 0, 1);

-- 更新品牌关联企业ID
UPDATE `brand` SET `company_id` = 101 WHERE `id` = 101;
UPDATE `brand` SET `company_id` = 102 WHERE `id` = 102;
UPDATE `brand` SET `company_id` = 103 WHERE `id` = 103;
UPDATE `brand` SET `company_id` = 104 WHERE `id` = 104;
UPDATE `brand` SET `company_id` = 105 WHERE `id` = 105;
UPDATE `brand` SET `company_id` = 106 WHERE `id` = 106;
UPDATE `brand` SET `company_id` = 107 WHERE `id` = 107;
UPDATE `brand` SET `company_id` = 108 WHERE `id` = 108;
UPDATE `brand` SET `company_id` = 109 WHERE `id` = 109;
UPDATE `brand` SET `company_id` = 110 WHERE `id` = 110;
UPDATE `brand` SET `company_id` = 111 WHERE `id` = 111;
UPDATE `brand` SET `company_id` = 112 WHERE `id` = 112;
UPDATE `brand` SET `company_id` = 113 WHERE `id` = 113;
UPDATE `brand` SET `company_id` = 114 WHERE `id` = 114;
UPDATE `brand` SET `company_id` = 115 WHERE `id` = 115;
UPDATE `brand` SET `company_id` = 116 WHERE `id` = 116;
UPDATE `brand` SET `company_id` = 117 WHERE `id` = 117;
UPDATE `brand` SET `company_id` = 118 WHERE `id` = 118;
UPDATE `brand` SET `company_id` = 119 WHERE `id` = 119;
UPDATE `brand` SET `company_id` = 120 WHERE `id` = 120;
UPDATE `brand` SET `company_id` = 121 WHERE `id` = 121;
UPDATE `brand` SET `company_id` = 122 WHERE `id` = 122;
UPDATE `brand` SET `company_id` = 123 WHERE `id` = 123;
UPDATE `brand` SET `company_id` = 124 WHERE `id` = 124;
UPDATE `brand` SET `company_id` = 125 WHERE `id` = 125;
UPDATE `brand` SET `company_id` = 126 WHERE `id` = 126;
UPDATE `brand` SET `company_id` = 127 WHERE `id` = 127;
UPDATE `brand` SET `company_id` = 128 WHERE `id` = 128;
UPDATE `brand` SET `company_id` = 131 WHERE `id` = 131;
UPDATE `brand` SET `company_id` = 132 WHERE `id` = 132;

-- ============================================================
-- 品牌别名数据（50+ 条，用于采集匹配）
-- ============================================================

INSERT INTO `brand_alias` (`brand_id`, `alias_name`, `alias_type`, `brand_name`, `data_source`, `create_time`, `update_time`) VALUES
(101, 'Unitree', 'ENGLISH', '宇树科技', 'OFFICIAL', NOW(), NOW()),
(101, '宇树', 'ABBREVIATION', '宇树科技', 'OFFICIAL', NOW(), NOW()),
(101, 'Unitree Robotics', 'ENGLISH', '宇树科技', 'OFFICIAL', NOW(), NOW()),
(102, 'UBTECH', 'ENGLISH', '优必选', 'OFFICIAL', NOW(), NOW()),
(102, '优必选科技', 'OTHER', '优必选', 'OFFICIAL', NOW(), NOW()),
(102, 'UBTECH Robotics', 'ENGLISH', '优必选', 'OFFICIAL', NOW(), NOW()),
(103, 'Agibot', 'ENGLISH', '智元机器人', 'OFFICIAL', NOW(), NOW()),
(103, '智元', 'ABBREVIATION', '智元机器人', 'OFFICIAL', NOW(), NOW()),
(104, 'Fourier Intelligence', 'ENGLISH', '傅利叶智能', 'OFFICIAL', NOW(), NOW()),
(104, '傅利叶', 'ABBREVIATION', '傅利叶智能', 'OFFICIAL', NOW(), NOW()),
(104, 'GR-1', 'OTHER', '傅利叶智能', 'OFFICIAL', NOW(), NOW()),
(105, 'Pudu', 'ENGLISH', '普渡机器人', 'OFFICIAL', NOW(), NOW()),
(105, '普渡', 'ABBREVIATION', '普渡机器人', 'OFFICIAL', NOW(), NOW()),
(105, 'Pudu Robotics', 'ENGLISH', '普渡机器人', 'OFFICIAL', NOW(), NOW()),
(106, 'Keenon', 'ENGLISH', '擎朗智能', 'OFFICIAL', NOW(), NOW()),
(106, '擎朗', 'ABBREVIATION', '擎朗智能', 'OFFICIAL', NOW(), NOW()),
(107, 'Dobot', 'ENGLISH', '越疆科技', 'OFFICIAL', NOW(), NOW()),
(107, '越疆', 'ABBREVIATION', '越疆科技', 'OFFICIAL', NOW(), NOW()),
(108, 'JAKA', 'ENGLISH', '节卡机器人', 'OFFICIAL', NOW(), NOW()),
(108, '节卡', 'ABBREVIATION', '节卡机器人', 'OFFICIAL', NOW(), NOW()),
(109, 'AUBO', 'ENGLISH', '遨博智能', 'OFFICIAL', NOW(), NOW()),
(109, '遨博', 'ABBREVIATION', '遨博智能', 'OFFICIAL', NOW(), NOW()),
(110, 'CloudMinds', 'ENGLISH', '达闼科技', 'OFFICIAL', NOW(), NOW()),
(110, '达闼', 'ABBREVIATION', '达闼科技', 'OFFICIAL', NOW(), NOW()),
(111, 'Boston Dynamics', 'ENGLISH', '波士顿动力', 'OFFICIAL', NOW(), NOW()),
(111, 'BD', 'ABBREVIATION', '波士顿动力', 'OFFICIAL', NOW(), NOW()),
(111, 'Spot', 'OTHER', '波士顿动力', 'OFFICIAL', NOW(), NOW()),
(111, 'Atlas', 'OTHER', '波士顿动力', 'OFFICIAL', NOW(), NOW()),
(112, 'Figure', 'ENGLISH', 'Figure AI', 'OFFICIAL', NOW(), NOW()),
(112, 'Figure 01', 'OTHER', 'Figure AI', 'OFFICIAL', NOW(), NOW()),
(112, 'Figure 02', 'OTHER', 'Figure AI', 'OFFICIAL', NOW(), NOW()),
(113, 'Agility', 'ENGLISH', 'Agility Robotics', 'OFFICIAL', NOW(), NOW()),
(113, 'Digit', 'OTHER', 'Agility Robotics', 'OFFICIAL', NOW(), NOW()),
(114, 'Xpeng Robotics', 'ENGLISH', '小鹏鹏行', 'OFFICIAL', NOW(), NOW()),
(114, '鹏行智能', 'OTHER', '小鹏鹏行', 'OFFICIAL', NOW(), NOW()),
(115, 'Dreame', 'ENGLISH', '追觅科技', 'OFFICIAL', NOW(), NOW()),
(115, '追觅', 'ABBREVIATION', '追觅科技', 'OFFICIAL', NOW(), NOW()),
(116, 'Kepler', 'ENGLISH', '开普勒机器人', 'OFFICIAL', NOW(), NOW()),
(116, '开普勒', 'ABBREVIATION', '开普勒机器人', 'OFFICIAL', NOW(), NOW()),
(117, 'Star Era', 'ENGLISH', '星动纪元', 'OFFICIAL', NOW(), NOW()),
(117, '星动', 'ABBREVIATION', '星动纪元', 'OFFICIAL', NOW(), NOW()),
(118, 'Leju', 'ENGLISH', '乐聚机器人', 'OFFICIAL', NOW(), NOW()),
(118, '乐聚', 'ABBREVIATION', '乐聚机器人', 'OFFICIAL', NOW(), NOW()),
(119, 'OrionStar', 'ENGLISH', '猎户星空', 'OFFICIAL', NOW(), NOW()),
(119, '猎户', 'ABBREVIATION', '猎户星空', 'OFFICIAL', NOW(), NOW()),
(120, 'SLAMTEC', 'ENGLISH', '思岚科技', 'OFFICIAL', NOW(), NOW()),
(120, '思岚', 'ABBREVIATION', '思岚科技', 'OFFICIAL', NOW(), NOW()),
(121, 'Gaussian', 'ENGLISH', '高仙机器人', 'OFFICIAL', NOW(), NOW()),
(121, '高仙', 'ABBREVIATION', '高仙机器人', 'OFFICIAL', NOW(), NOW()),
(122, 'Ninebot', 'ENGLISH', '九号机器人', 'OFFICIAL', NOW(), NOW()),
(122, '九号', 'ABBREVIATION', '九号机器人', 'OFFICIAL', NOW(), NOW()),
(125, 'Optimus', 'OTHER', '特斯拉机器人', 'OFFICIAL', NOW(), NOW()),
(125, 'Tesla Bot', 'OTHER', '特斯拉机器人', 'OFFICIAL', NOW(), NOW()),
(131, 'FANUC', 'ENGLISH', '发那科', 'OFFICIAL', NOW(), NOW()),
(131, '发那科', 'ABBREVIATION', '发那科', 'OFFICIAL', NOW(), NOW()),
(132, 'ABB', 'ENGLISH', 'ABB机器人', 'OFFICIAL', NOW(), NOW()),
(133, 'KUKA', 'ENGLISH', '库卡', 'OFFICIAL', NOW(), NOW()),
(133, '库卡', 'ABBREVIATION', '库卡', 'OFFICIAL', NOW(), NOW()),
(134, 'Yaskawa', 'ENGLISH', '安川电机', 'OFFICIAL', NOW(), NOW()),
(134, '安川', 'ABBREVIATION', '安川电机', 'OFFICIAL', NOW(), NOW()),
(135, 'DJI', 'ENGLISH', '大疆', 'OFFICIAL', NOW(), NOW()),
(135, '大疆创新', 'OTHER', '大疆', 'OFFICIAL', NOW(), NOW());

-- ============================================================
-- 参数标准化映射（20+ 条）
-- ============================================================

INSERT INTO `param_mapping` (`raw_name`, `normalized_name`, `category`, `unit`, `aliases`, `data_source`, `create_time`, `update_time`) VALUES
('重量', 'weight', 'physical', 'kg', '自重,机体重量,净重,body weight,mass', 'MANUAL', NOW(), NOW()),
('身高', 'height', 'physical', 'cm', '高度,站立高度,standing height,stature', 'MANUAL', NOW(), NOW()),
('续航时间', 'battery_life', 'power', 'h', '续航,电池续航,工作时长,battery life,operation time', 'MANUAL', NOW(), NOW()),
('行走速度', 'walking_speed', 'mobility', 'km/h', '步行速度,移动速度,行走步速,walking speed,locomotion speed', 'MANUAL', NOW(), NOW()),
('负载能力', 'payload', 'performance', 'kg', '负载,载重,有效载荷,payload capacity,load capacity', 'MANUAL', NOW(), NOW()),
('自由度', 'dof', 'performance', 'DOF', 'DOF,degrees of freedom,关节数,关节数量', 'MANUAL', NOW(), NOW()),
('电池容量', 'battery_capacity', 'power', 'mAh', '电池容量,battery capacity,电量', 'MANUAL', NOW(), NOW()),
('臂展', 'arm_reach', 'physical', 'cm', '臂展长度,手臂伸展,arm reach,reach radius', 'MANUAL', NOW(), NOW()),
('重复定位精度', 'repeatability', 'performance', 'mm', '定位精度,重复精度,repeatability,positioning accuracy', 'MANUAL', NOW(), NOW()),
('最大速度', 'max_speed', 'mobility', 'km/h', '最高速度,最大移动速度,max speed,maximum speed', 'MANUAL', NOW(), NOW()),
('工作温度', 'operating_temp', 'environment', '°C', '工作温度范围,使用温度,operating temperature,working temperature', 'MANUAL', NOW(), NOW()),
('防护等级', 'ip_rating', 'environment', NULL, 'IP等级,防护级别,IP rating,protection level', 'MANUAL', NOW(), NOW()),
('控制方式', 'control_mode', 'control', NULL, '控制模式,操控方式,control mode,control type', 'MANUAL', NOW(), NOW()),
('通信方式', 'communication', 'control', NULL, '通讯方式,通信接口,communication,connectivity', 'MANUAL', NOW(), NOW()),
('传感器', 'sensors', 'hardware', NULL, '传感器配置,sensor,sensor suite', 'MANUAL', NOW(), NOW()),
('处理器', 'processor', 'hardware', NULL, 'CPU,计算平台,processor,compute unit', 'MANUAL', NOW(), NOW()),
('视觉系统', 'vision_system', 'hardware', NULL, '视觉,视觉传感器,vision,camera system', 'MANUAL', NOW(), NOW()),
('语音交互', 'voice_interaction', 'feature', NULL, '语音,语音识别,voice,speech recognition', 'MANUAL', NOW(), NOW()),
('导航方式', 'navigation', 'mobility', NULL, '导航,自主导航,navigation,SLAM', 'MANUAL', NOW(), NOW()),
('应用场景', 'application', 'general', NULL, '应用领域,使用场景,application,use case', 'MANUAL', NOW(), NOW());