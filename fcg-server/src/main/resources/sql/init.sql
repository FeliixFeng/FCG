-- ============================================
-- FCG 数据库初始化脚本
-- Family Care Guardian - 家庭健康管理系统
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 系统模块
-- ============================================

-- 1. 家庭表
DROP TABLE IF EXISTS `sys_family`;
CREATE TABLE `sys_family` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
    `family_name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `invite_code` VARCHAR(20)  NOT NULL COMMENT '邀请码（用于家庭成员加入）',
    `creator_id`  BIGINT       NOT NULL COMMENT '创建者用户ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭表';

-- 2. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role`        TINYINT      NOT NULL DEFAULT 2 COMMENT '角色 0-管理员 1-普通成员 2-受控成员',
    `family_id`   BIGINT       DEFAULT NULL COMMENT '所属家庭ID',
    `relation`    VARCHAR(20)  DEFAULT NULL COMMENT '家庭关系（如：本人/父亲/母亲/儿子/女儿）',
    `care_mode`   TINYINT      NOT NULL DEFAULT 0 COMMENT '关怀模式 0-关闭 1-开启',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 药品模块
-- ============================================

-- 3. 药品表（每个家庭独立）
DROP TABLE IF EXISTS `med_medicine`;
CREATE TABLE `med_medicine` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '药品ID',
    `family_id`       BIGINT       NOT NULL COMMENT '所属家庭ID',
    `name`            VARCHAR(100) NOT NULL COMMENT '药品名称',
    `specification`   VARCHAR(100) DEFAULT NULL COMMENT '规格（如 10mg*20片）',
    `manufacturer`    VARCHAR(100) DEFAULT NULL COMMENT '生产厂家',
    `dosage_form`     VARCHAR(50)  DEFAULT NULL COMMENT '剂型（片剂/胶囊/液体等）',
    `image_url`       VARCHAR(255) DEFAULT NULL COMMENT '药品图片URL',
    `instructions`    TEXT         DEFAULT NULL COMMENT '药品说明书',
    `contraindications` TEXT       DEFAULT NULL COMMENT '禁忌事项',
    `side_effects`    TEXT         DEFAULT NULL COMMENT '副作用',
    `stock`           INT          DEFAULT 0 COMMENT '库存数量',
    `stock_unit`      VARCHAR(20)  DEFAULT NULL COMMENT '库存单位（片/粒/瓶）',
    `expire_date`     DATE         DEFAULT NULL COMMENT '过期日期',
    `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '存放位置',
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
    `user_id`       BIGINT       NOT NULL COMMENT '使用者ID（受控成员）',
    `family_id`     BIGINT       NOT NULL COMMENT '所属家庭ID',
    `medicine_id`   BIGINT       NOT NULL COMMENT '药品ID',
    `dosage`        VARCHAR(50)  NOT NULL COMMENT '单次剂量（如 1片）',
    `frequency`     VARCHAR(50)  NOT NULL COMMENT '服药频率（如 每天3次）',
    `remind_times`  VARCHAR(100) NOT NULL COMMENT '提醒时间点（08:00,12:00,18:00）',
    `start_date`    DATE         NOT NULL COMMENT '开始日期',
    `end_date`      DATE         DEFAULT NULL COMMENT '结束日期（NULL表示长期）',
    `take_days`     VARCHAR(20)  DEFAULT '1,2,3,4,5,6,7' COMMENT '服药星期（1-7，逗号分隔）',
    `notes`         VARCHAR(255) DEFAULT NULL COMMENT '备注（饭后服用等）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_status` (`status`),
    KEY `idx_family_user` (`family_id`, `user_id`),
    KEY `idx_family_status` (`family_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- 5. 服药记录表
DROP TABLE IF EXISTS `med_record`;
CREATE TABLE `med_record` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `plan_id`        BIGINT   NOT NULL COMMENT '计划ID',
    `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
    `family_id`      BIGINT   NOT NULL COMMENT '所属家庭ID',
    `medicine_id`    BIGINT   NOT NULL COMMENT '药品ID',
    `scheduled_date` DATE     NOT NULL COMMENT '应服药日期',
    `scheduled_time` TIME     NOT NULL COMMENT '应服药时间',
    `actual_time`    DATETIME DEFAULT NULL COMMENT '实际服药时间',
    `status`         TINYINT  NOT NULL DEFAULT 0 COMMENT '状态 0-未服 1-已服 2-跳过',
    `notes`          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_family_id` (`family_id`),
    KEY `idx_scheduled_date` (`scheduled_date`),
    KEY `idx_family_user_date` (`family_id`, `user_id`, `scheduled_date`)
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
    `type`           TINYINT      NOT NULL COMMENT '类型 1-血压 2-血糖 3-心率 4-体温 5-体重',
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
-- 初始化数据
-- ============================================

-- 插入默认管理员（密码: admin123，实际项目需加密）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `care_mode`) 
VALUES ('admin', 'admin123', '系统管理员', 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
