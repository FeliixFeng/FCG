-- ============================================
-- FCG 数据库初始化脚本（与当前线上库结构对齐）
-- Database: fcg_db
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------
-- 1. 家庭表
-- --------------------------------------------
DROP TABLE IF EXISTS `sys_family`;
CREATE TABLE `sys_family` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
  `family_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '家庭名称',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '家庭账号',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '家庭密码（BCrypt加密）',
  `creator_id` bigint NOT NULL COMMENT '创建者用户ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭表';

-- --------------------------------------------
-- 2. 成员表
-- --------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '头像URL',
  `role` tinyint NOT NULL DEFAULT '2' COMMENT '角色 0-管理员 1-普通成员 2-受控成员',
  `family_id` bigint DEFAULT NULL COMMENT '所属家庭ID',
  `relation` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '家庭关系',
  `care_mode` tinyint NOT NULL DEFAULT '0' COMMENT '关怀模式 0-关闭 1-开启',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- --------------------------------------------
-- 3. 成员扩展信息表
-- --------------------------------------------
DROP TABLE IF EXISTS `sys_user_profile`;
CREATE TABLE `sys_user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `birthday` date DEFAULT NULL,
  `height` decimal(5,1) DEFAULT NULL,
  `weight` decimal(5,1) DEFAULT NULL,
  `disease` varchar(255) DEFAULT NULL,
  `allergy` varchar(255) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------
-- 4. 药品表
-- --------------------------------------------
DROP TABLE IF EXISTS `med_medicine`;
CREATE TABLE `med_medicine` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `family_id` bigint NOT NULL COMMENT '所属家庭ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '药品名称',
  `specification` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格（如 10mg*20片）',
  `image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '药品图片URL',
  `stock` int DEFAULT '0' COMMENT '库存数量',
  `stock_unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库存单位（片/粒/瓶）',
  `expire_date` date DEFAULT NULL COMMENT '过期日期',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `usage_notes` text COLLATE utf8mb4_unicode_ci COMMENT '用药注意',
  `indication` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品表';

-- --------------------------------------------
-- 5. 用药计划表
-- --------------------------------------------
DROP TABLE IF EXISTS `med_plan`;
CREATE TABLE `med_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '使用者ID（受控成员）',
  `medicine_id` bigint NOT NULL COMMENT '药品ID',
  `dosage` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单次剂量',
  `remind_slots` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒时段',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期（NULL表示长期）',
  `take_days` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '1,2,3,4,5,6,7' COMMENT '服药星期（1-7，逗号分隔）',
  `plan_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用药注意',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- --------------------------------------------
-- 6. 服药记录表
-- --------------------------------------------
DROP TABLE IF EXISTS `med_record`;
CREATE TABLE `med_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `medicine_id` bigint NOT NULL COMMENT '药品ID',
  `scheduled_date` date NOT NULL COMMENT '应服药日期',
  `slot_name` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应服时段（早/中/晚/睡前）',
  `actual_time` datetime DEFAULT NULL COMMENT '实际服药时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-未服 1-已服 2-跳过',
  `record_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记录备注',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_plan_date_slot` (`user_id`,`plan_id`,`scheduled_date`,`slot_name`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_scheduled_date` (`scheduled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服药记录表';

-- --------------------------------------------
-- 7. 体征数据表
-- --------------------------------------------
DROP TABLE IF EXISTS `health_vital`;
CREATE TABLE `health_vital` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `family_id` bigint NOT NULL COMMENT '所属家庭ID',
  `type` tinyint NOT NULL COMMENT '类型 1-血压 2-血糖 3-心率 4-体温 5-体重',
  `value_systolic` decimal(5,1) DEFAULT NULL COMMENT '收缩压（血压专用）',
  `value_diastolic` decimal(5,1) DEFAULT NULL COMMENT '舒张压（血压专用）',
  `value` decimal(6,2) DEFAULT NULL COMMENT '数值（血糖/心率/体温/体重）',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位（mmHg/mmol/L/bpm/°C/kg）',
  `measure_time` datetime NOT NULL COMMENT '测量时间',
  `measure_point` tinyint DEFAULT NULL COMMENT '测量时点（血糖专用 1-空腹 2-餐后）',
  `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_type` (`type`),
  KEY `idx_measure_time` (`measure_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体征数据表';

-- --------------------------------------------
-- 8. 健康周报表
-- --------------------------------------------
DROP TABLE IF EXISTS `health_report`;
CREATE TABLE `health_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报表ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `family_id` bigint NOT NULL COMMENT '所属家庭ID',
  `week_start` date NOT NULL COMMENT '周开始日期',
  `week_end` date NOT NULL COMMENT '周结束日期',
  `compliance_rate` decimal(5,2) DEFAULT NULL COMMENT '服药依从性（%）',
  `vital_summary` text COLLATE utf8mb4_unicode_ci COMMENT '体征摘要（JSON格式）',
  `ai_summary` text COLLATE utf8mb4_unicode_ci COMMENT 'AI健康建议',
  `risk_level` tinyint DEFAULT NULL COMMENT '风险等级 0-低 1-中 2-高',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_week` (`user_id`,`week_start`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康周报表';

SET FOREIGN_KEY_CHECKS = 1;
