-- ============================================
-- FCG 数据库初始化脚本
-- Family Care Guardian - 家庭健康管理系统
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 系统模块
-- ============================================

-- 1. 家庭表（家庭账号登录，一个家庭一个账号）
DROP TABLE IF EXISTS `sys_family`;
CREATE TABLE `sys_family` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
    `family_name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `username`    VARCHAR(50)  NOT NULL COMMENT '家庭账号（登录用）',
    `password`    VARCHAR(100) NOT NULL COMMENT '家庭密码（BCrypt加密）',
    `creator_id`  BIGINT       NOT NULL COMMENT '创建者成员ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭表';

-- 2. 成员表（家庭下的成员，无独立登录账号）
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称（必填，选人页展示用）',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`      MEDIUMTEXT   DEFAULT NULL COMMENT '头像URL（存OSS地址/Base64）',
    `role`        TINYINT      NOT NULL DEFAULT 1 COMMENT '角色 0-管理员 1-普通成员 2-关怀成员（老人）',
    `family_id`   BIGINT       NOT NULL COMMENT '所属家庭ID',
    `relation`    VARCHAR(20)  DEFAULT NULL COMMENT '家庭关系（父亲/母亲/爷爷等）',
    `care_mode`   TINYINT      NOT NULL DEFAULT 0 COMMENT '关怀模式 0-关闭 1-开启（关怀成员自动开启）',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭成员表';

-- 2.1 成员扩展信息表（1:1 关联 sys_user）
DROP TABLE IF EXISTS `sys_user_profile`;
CREATE TABLE `sys_user_profile` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`     BIGINT       NOT NULL COMMENT '成员ID（关联 sys_user）',
    `birthday`    DATE         DEFAULT NULL COMMENT '出生日期',
    `height`     DECIMAL(5,1) DEFAULT NULL COMMENT '身高（cm）',
    `weight`     DECIMAL(5,1) DEFAULT NULL COMMENT '体重（kg）',
    `disease`    VARCHAR(255) DEFAULT NULL COMMENT '病史（高血压/糖尿病等，逗号分隔）',
    `allergy`    VARCHAR(255) DEFAULT NULL COMMENT '过敏史',
    `remark`     VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成员扩展信息表';

-- ============================================
-- 药品模块
-- ============================================

-- 3. 药品表（每个家庭独立）
DROP TABLE IF EXISTS `med_medicine`;
CREATE TABLE `med_medicine` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '药品ID',
    `family_id`       BIGINT       NOT NULL COMMENT '所属家庭ID',
    `name`            VARCHAR(100) NOT NULL COMMENT '药品名称',
    `specification`   VARCHAR(100) DEFAULT NULL COMMENT '规格（如 0.3g*20粒）',
    `image_url`       VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `stock`           INT          DEFAULT 0 COMMENT '库存数量',
    `stock_unit`      VARCHAR(20)  DEFAULT NULL COMMENT '库存单位（片/粒/ml）',
    `expire_date`     DATE         DEFAULT NULL COMMENT '有效期',
    `usage_notes`    TEXT         DEFAULT NULL COMMENT '用药注意',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- 4. 用药计划表
DROP TABLE IF EXISTS `med_plan`;
CREATE TABLE `med_plan` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
    `user_id`       BIGINT       NOT NULL COMMENT '使用者ID',
    `medicine_id`   BIGINT       NOT NULL COMMENT '药品ID',
    `dosage`        DECIMAL(5,2) NOT NULL COMMENT '单次剂量（如 1 或 0.5）',
    `remind_slots`  VARCHAR(50)  NOT NULL COMMENT '提醒时段（早,中,晚）',
    `take_days`    VARCHAR(20)  DEFAULT '1,2,3,4,5,6,7' COMMENT '服药星期（1-7，逗号分隔）',
    `start_date`   DATE         NOT NULL COMMENT '开始日期',
    `end_date`     DATE         DEFAULT NULL COMMENT '结束日期（可空=长期）',
    `plan_remark` VARCHAR(255) DEFAULT NULL COMMENT '用药注意',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_medicine_id` (`medicine_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- 5. 服药记录表
DROP TABLE IF EXISTS `med_record`;
CREATE TABLE `med_record` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `plan_id`         BIGINT   NOT NULL COMMENT '计划ID',
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `medicine_id`     BIGINT   NOT NULL COMMENT '药品ID',
    `scheduled_date`  DATE     NOT NULL COMMENT '应服日期',
    `slot_name`       VARCHAR(20) NOT NULL COMMENT '应服时段（早/中/晚/睡前）',
    `actual_time`     DATETIME DEFAULT NULL COMMENT '实际打卡时间',
    `status`          TINYINT  NOT NULL DEFAULT 0 COMMENT '状态 0-未服 1-已服 2-跳过',
    `record_remark`   VARCHAR(255) DEFAULT NULL COMMENT '记录备注',
    `deleted`         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_scheduled_date` (`scheduled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服药记录表';

-- ============================================
-- 健康模块
-- ============================================

-- 6. 体征数据表
DROP TABLE IF EXISTS `health_vital`;
CREATE TABLE `health_vital` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `family_id`      BIGINT       NOT NULL COMMENT '所属家庭ID',
    `type`           TINYINT      NOT NULL COMMENT '类型 1-血压 2-血糖 3-体重',
    `value_systolic` DECIMAL(5,1) DEFAULT NULL COMMENT '收缩压（血压专用）',
    `value_diastolic` DECIMAL(5,1) DEFAULT NULL COMMENT '舒张压（血压专用）',
    `value`          DECIMAL(6,2) DEFAULT NULL COMMENT '数值（血糖/心率/体温/体重）',
    `unit`           VARCHAR(20)  DEFAULT NULL COMMENT '单位（mmHg/mmol/L/bpm/°C/kg）',
    `measure_time`   DATETIME     NOT NULL COMMENT '测量时间',
    `measure_point`  TINYINT      DEFAULT NULL COMMENT '测量时点（血糖专用 1-空腹 2-餐后）',
    `notes`          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_type` (`type`),
    KEY `idx_measure_time` (`measure_time`),
    KEY `idx_family_user_type_time` (`family_id`, `user_id`, `type`, `measure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体征数据表';

-- 7. 健康周报表
DROP TABLE IF EXISTS `health_report`;
CREATE TABLE `health_report` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '报表ID',
    `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
    `family_id`        BIGINT       NOT NULL COMMENT '所属家庭ID',
    `week_start`       DATE         NOT NULL COMMENT '周开始日期',
    `week_end`         DATE         NOT NULL COMMENT '周结束日期',
    `compliance_rate`  DECIMAL(5,2) DEFAULT NULL COMMENT '服药依从性（%）',
    `vital_summary`    TEXT         DEFAULT NULL COMMENT '体征摘要（JSON格式）',
    `ai_summary`       TEXT         DEFAULT NULL COMMENT 'AI健康建议',
    `risk_level`       TINYINT      DEFAULT NULL COMMENT '风险等级 0-低 1-中 2-高',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_family_user_week` (`family_id`, `user_id`, `week_start`),
    UNIQUE KEY `uk_user_week` (`user_id`, `week_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康周报表';

-- ============================================
-- 测试数据（密码均为 test123）
-- ============================================

-- 测试家庭（密码 test123 的BCrypt hash）
INSERT INTO `sys_family` (`family_name`, `username`, `password`, `creator_id`) VALUES
('张家', 'zhangfamily', '$2a$12$Lmi6OtRgo6Po5fk7w.gWYOg3vZLLxZG5Y5DaQ.rtN3pIZHC5W8fUq', 1),
('李家', 'lifamily',   '$2a$12$Lmi6OtRgo6Po5fk7w.gWYOg3vZLLxZG5Y5DaQ.rtN3pIZHC5W8fUq', 4);

-- 张家成员
INSERT INTO `sys_user` (`nickname`, `phone`, `role`, `family_id`, `relation`, `care_mode`) VALUES
('张爸爸', '13800000001', 0, 1, '父亲', 0),  -- 管理员
('张妈妈', '13800000002', 1, 1, '母亲', 0),  -- 普通成员
('张爷爷', '13800000003', 2, 1, '爷爷', 1);  -- 关怀成员

-- 李家成员
INSERT INTO `sys_user` (`nickname`, `phone`, `role`, `family_id`, `relation`, `care_mode`) VALUES
('李爸爸', '13800000004', 0, 2, '父亲', 0);  -- 管理员

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 测试数据：药品模块
-- ============================================

-- 张家药品（family_id=1）
-- 张爷爷（user id=3）是关怀成员，主要服药对象
-- 张爸爸（user id=1）偶尔服药
INSERT INTO `med_medicine` (`family_id`, `name`, `specification`, `stock`, `stock_unit`, `expire_date`, `usage_notes`) VALUES
(1, '苯磺酸氨氯地平片', '5mg*7片', 42, '片', '2026-12-31', '用于高血压及冠心病。每日一次，每次一片，餐后服用。'),
(1, '阿卡波糖片', '50mg*30片', 90, '片', '2027-03-15', '用于2型糖尿病。进餐时与第一口食物一起嚼服，每次50mg。'),
(1, '阿司匹林肠溶片', '100mg*30片', 60, '片', '2027-06-30', '抗血栓，预防心肌梗死。每日一次，每次100mg，餐后服用。'),
(1, '盐酸二甲双胍片', '500mg*60片', 120, '片', '2026-09-20', '用于2型糖尿病辅助治疗。每次500mg，随餐服用。'),
(1, '酒石酸美托洛尔片', '25mg*20片', 40, '片', '2026-08-10', '用于高血压、心绞痛。每日两次，每次25mg。');

-- ============================================
-- 用药计划（张爷爷 user_id=3，family_id=1）
-- ============================================
-- med_medicine id：苯磺酸氨氯地平=1, 阿卡波糖=2, 阿司匹林=3, 二甲双胍=4, 美托洛尔=5
INSERT INTO `med_plan` (`user_id`, `family_id`, `medicine_id`, `dosage`, `frequency`, `remind_times`, `start_date`, `end_date`, `take_days`, `notes`, `status`) VALUES
(3, 1, 1, '1片', '每日1次', '08:00', '2026-01-01', NULL, '1,2,3,4,5,6,7', '餐后服用，监测血压', 1),
(3, 1, 2, '1片', '每日3次', '08:00,12:00,18:00', '2026-01-01', NULL, '1,2,3,4,5,6,7', '随餐嚼服', 1),
(3, 1, 3, '1片', '每日1次', '08:00', '2026-01-01', NULL, '1,2,3,4,5,6,7', '餐后服用', 1),
(3, 1, 5, '1片', '每日2次', '08:00,20:00', '2026-01-01', NULL, '1,2,3,4,5,6,7', '血压控制辅助用药', 1);

-- 张爸爸用药计划（user_id=1）
INSERT INTO `med_plan` (`user_id`, `family_id`, `medicine_id`, `dosage`, `frequency`, `remind_times`, `start_date`, `end_date`, `take_days`, `notes`, `status`) VALUES
(1, 1, 4, '1片', '每日2次', '08:00,18:00', '2026-02-01', NULL, '1,2,3,4,5,6,7', '随餐服用，监测血糖', 1);

-- ============================================
-- 服药记录（近一周，今天=2026-03-11）
-- plan_id: 爷爷氨氯地平=1, 爷爷阿卡波糖=2, 爷爷阿司匹林=3, 爷爷美托洛尔=4
-- plan_id: 爸爸二甲双胍=5
-- ============================================

-- 03-05 周三（全部已服）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-05', '08:00:00', '2026-03-05 08:12:00', 1),
(2, 3, 1, 2, '2026-03-05', '08:00:00', '2026-03-05 08:13:00', 1),
(2, 3, 1, 2, '2026-03-05', '12:00:00', '2026-03-05 12:05:00', 1),
(2, 3, 1, 2, '2026-03-05', '18:00:00', '2026-03-05 18:20:00', 1),
(3, 3, 1, 3, '2026-03-05', '08:00:00', '2026-03-05 08:14:00', 1),
(4, 3, 1, 5, '2026-03-05', '08:00:00', '2026-03-05 08:15:00', 1),
(4, 3, 1, 5, '2026-03-05', '20:00:00', '2026-03-05 20:08:00', 1),
(5, 1, 1, 4, '2026-03-05', '08:00:00', '2026-03-05 08:30:00', 1),
(5, 1, 1, 4, '2026-03-05', '18:00:00', '2026-03-05 18:35:00', 1);

-- 03-06 周四（爷爷漏服一次午间阿卡波糖）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-06', '08:00:00', '2026-03-06 08:05:00', 1),
(2, 3, 1, 2, '2026-03-06', '08:00:00', '2026-03-06 08:06:00', 1),
(2, 3, 1, 2, '2026-03-06', '12:00:00', NULL, 0),
-- 漏服
(2, 3, 1, 2, '2026-03-06', '18:00:00', '2026-03-06 18:22:00', 1),
(3, 3, 1, 3, '2026-03-06', '08:00:00', '2026-03-06 08:07:00', 1),
(4, 3, 1, 5, '2026-03-06', '08:00:00', '2026-03-06 08:08:00', 1),
(4, 3, 1, 5, '2026-03-06', '20:00:00', '2026-03-06 20:15:00', 1),
(5, 1, 1, 4, '2026-03-06', '08:00:00', '2026-03-06 08:45:00', 1),
(5, 1, 1, 4, '2026-03-06', '18:00:00', '2026-03-06 18:50:00', 1);

-- 03-07 周五（全部正常）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-07', '08:00:00', '2026-03-07 08:02:00', 1),
(2, 3, 1, 2, '2026-03-07', '08:00:00', '2026-03-07 08:03:00', 1),
(2, 3, 1, 2, '2026-03-07', '12:00:00', '2026-03-07 11:58:00', 1),
(2, 3, 1, 2, '2026-03-07', '18:00:00', '2026-03-07 18:10:00', 1),
(3, 3, 1, 3, '2026-03-07', '08:00:00', '2026-03-07 08:04:00', 1),
(4, 3, 1, 5, '2026-03-07', '08:00:00', '2026-03-07 08:05:00', 1),
(4, 3, 1, 5, '2026-03-07', '20:00:00', '2026-03-07 20:03:00', 1),
(5, 1, 1, 4, '2026-03-07', '08:00:00', '2026-03-07 08:20:00', 1),
(5, 1, 1, 4, '2026-03-07', '18:00:00', '2026-03-07 18:25:00', 1);

-- 03-08 周六（爷爷晚间美托洛尔跳过）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`, `notes`) VALUES
(1, 3, 1, 1, '2026-03-08', '08:00:00', '2026-03-08 08:10:00', 1, NULL),
(2, 3, 1, 2, '2026-03-08', '08:00:00', '2026-03-08 08:11:00', 1, NULL),
(2, 3, 1, 2, '2026-03-08', '12:00:00', '2026-03-08 12:02:00', 1, NULL),
(2, 3, 1, 2, '2026-03-08', '18:00:00', '2026-03-08 18:15:00', 1, NULL),
(3, 3, 1, 3, '2026-03-08', '08:00:00', '2026-03-08 08:12:00', 1, NULL),
(4, 3, 1, 5, '2026-03-08', '08:00:00', '2026-03-08 08:13:00', 1, NULL),
(4, 3, 1, 5, '2026-03-08', '20:00:00', NULL, 2, '身体不适，医嘱暂停'),
-- 跳过
(5, 1, 1, 4, '2026-03-08', '08:00:00', '2026-03-08 09:00:00', 1, NULL),
(5, 1, 1, 4, '2026-03-08', '18:00:00', '2026-03-08 18:40:00', 1, NULL);

-- 03-09 周日（全部已服）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-09', '08:00:00', '2026-03-09 08:08:00', 1),
(2, 3, 1, 2, '2026-03-09', '08:00:00', '2026-03-09 08:09:00', 1),
(2, 3, 1, 2, '2026-03-09', '12:00:00', '2026-03-09 12:00:00', 1),
(2, 3, 1, 2, '2026-03-09', '18:00:00', '2026-03-09 18:05:00', 1),
(3, 3, 1, 3, '2026-03-09', '08:00:00', '2026-03-09 08:10:00', 1),
(4, 3, 1, 5, '2026-03-09', '08:00:00', '2026-03-09 08:11:00', 1),
(4, 3, 1, 5, '2026-03-09', '20:00:00', '2026-03-09 20:00:00', 1),
(5, 1, 1, 4, '2026-03-09', '08:00:00', '2026-03-09 08:22:00', 1),
(5, 1, 1, 4, '2026-03-09', '18:00:00', '2026-03-09 18:30:00', 1);

-- 03-10 周一（爷爷全部已服，爸爸早间已服，晚间未服）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-10', '08:00:00', '2026-03-10 08:06:00', 1),
(2, 3, 1, 2, '2026-03-10', '08:00:00', '2026-03-10 08:07:00', 1),
(2, 3, 1, 2, '2026-03-10', '12:00:00', '2026-03-10 12:10:00', 1),
(2, 3, 1, 2, '2026-03-10', '18:00:00', '2026-03-10 18:08:00', 1),
(3, 3, 1, 3, '2026-03-10', '08:00:00', '2026-03-10 08:08:00', 1),
(4, 3, 1, 5, '2026-03-10', '08:00:00', '2026-03-10 08:09:00', 1),
(4, 3, 1, 5, '2026-03-10', '20:00:00', '2026-03-10 20:20:00', 1),
(5, 1, 1, 4, '2026-03-10', '08:00:00', '2026-03-10 08:35:00', 1),
(5, 1, 1, 4, '2026-03-10', '18:00:00', NULL, 0);
-- 未服

-- 03-11 今天（部分已服，部分待服）
INSERT INTO `med_record` (`plan_id`, `user_id`, `family_id`, `medicine_id`, `scheduled_date`, `scheduled_time`, `actual_time`, `status`) VALUES
(1, 3, 1, 1, '2026-03-11', '08:00:00', '2026-03-11 08:05:00', 1),
-- 已服
(2, 3, 1, 2, '2026-03-11', '08:00:00', '2026-03-11 08:06:00', 1),
-- 已服
(2, 3, 1, 2, '2026-03-11', '12:00:00', '2026-03-11 12:08:00', 1),
-- 已服
(2, 3, 1, 2, '2026-03-11', '18:00:00', NULL, 0),
-- 待服（晚间）
(3, 3, 1, 3, '2026-03-11', '08:00:00', '2026-03-11 08:07:00', 1),
-- 已服
(4, 3, 1, 5, '2026-03-11', '08:00:00', '2026-03-11 08:08:00', 1),
-- 已服
(4, 3, 1, 5, '2026-03-11', '20:00:00', NULL, 0),
-- 待服（晚间）
(5, 1, 1, 4, '2026-03-11', '08:00:00', '2026-03-11 08:40:00', 1),
-- 已服
(5, 1, 1, 4, '2026-03-11', '18:00:00', NULL, 0);
-- 待服（晚间）

-- ============================================
-- 体征数据（张爷爷 user_id=3，近一周血压+血糖）
-- type: 1=血压 2=血糖 3=心率 4=体温 5=体重
-- unit: mmHg / mmol/L
-- ============================================

-- 血压记录（近7天，每天早晨一次）
INSERT INTO `health_vital` (`user_id`, `family_id`, `type`, `value_systolic`, `value_diastolic`, `unit`, `measure_time`, `notes`) VALUES
(3, 1, 1, 148.0, 92.0, 'mmHg', '2026-03-05 07:30:00', '清晨空腹'),
(3, 1, 1, 145.0, 90.0, 'mmHg', '2026-03-06 07:28:00', '清晨空腹'),
(3, 1, 1, 152.0, 94.0, 'mmHg', '2026-03-07 07:35:00', '昨晚睡眠较差'),
(3, 1, 1, 143.0, 88.0, 'mmHg', '2026-03-08 07:32:00', '清晨空腹'),
(3, 1, 1, 139.0, 86.0, 'mmHg', '2026-03-09 07:30:00', '感觉良好'),
(3, 1, 1, 141.0, 87.0, 'mmHg', '2026-03-10 07:33:00', '清晨空腹'),
(3, 1, 1, 138.0, 85.0, 'mmHg', '2026-03-11 07:30:00', '今日状态好');

-- 血糖记录（近7天，每天空腹+餐后）
-- measure_point: 1=空腹 2=餐后
INSERT INTO `health_vital` (`user_id`, `family_id`, `type`, `value`, `unit`, `measure_time`, `measure_point`, `notes`) VALUES
(3, 1, 2, 7.2, 'mmol/L', '2026-03-05 07:00:00', 1, '空腹'),
(3, 1, 2, 9.8, 'mmol/L', '2026-03-05 10:00:00', 2, '早餐后2小时'),
(3, 1, 2, 7.0, 'mmol/L', '2026-03-06 07:05:00', 1, '空腹'),
(3, 1, 2, 10.2, 'mmol/L', '2026-03-06 10:10:00', 2, '早餐后2小时'),
(3, 1, 2, 6.8, 'mmol/L', '2026-03-07 07:00:00', 1, '空腹'),
(3, 1, 2, 9.5, 'mmol/L', '2026-03-07 10:05:00', 2, '早餐后2小时'),
(3, 1, 2, 7.4, 'mmol/L', '2026-03-08 07:10:00', 1, '空腹'),
(3, 1, 2, 10.5, 'mmol/L', '2026-03-08 10:15:00', 2, '早餐后2小时'),
(3, 1, 2, 6.9, 'mmol/L', '2026-03-09 07:00:00', 1, '空腹'),
(3, 1, 2, 9.3, 'mmol/L', '2026-03-09 10:00:00', 2, '早餐后2小时'),
(3, 1, 2, 7.1, 'mmol/L', '2026-03-10 07:08:00', 1, '空腹'),
(3, 1, 2, 9.6, 'mmol/L', '2026-03-10 10:08:00', 2, '早餐后2小时'),
(3, 1, 2, 6.7, 'mmol/L', '2026-03-11 07:00:00', 1, '今日空腹');

-- 体重记录（近7天）
INSERT INTO `health_vital` (`user_id`, `family_id`, `type`, `value`, `unit`, `measure_time`) VALUES
(3, 1, 3, 75.5, 'kg', '2026-03-05 07:00:00'),
(3, 1, 3, 75.3, 'kg', '2026-03-06 07:00:00'),
(3, 1, 3, 75.2, 'kg', '2026-03-07 07:00:00'),
(3, 1, 3, 75.0, 'kg', '2026-03-08 07:00:00'),
(3, 1, 3, 74.8, 'kg', '2026-03-09 07:00:00'),
(3, 1, 3, 74.6, 'kg', '2026-03-10 07:00:00'),
(3, 1, 3, 74.5, 'kg', '2026-03-11 07:00:00');

SET FOREIGN_KEY_CHECKS = 1;
