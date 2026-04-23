# 数据库设计文档（当前基线）

> 基线来源：`fcg-server/src/main/resources/sql/init.sql`  
> 版本日期：2026-04-23

## 1. 概述

- 数据库：MySQL 8.0
- 库名：`fcg_db`
- 字符集：`utf8mb4`
- 存储引擎：InnoDB
- 逻辑删除字段：`deleted`（0=未删除，1=已删除）

## 2. 表清单

当前共 8 张核心业务表：
- `sys_family`（家庭）
- `sys_user`（成员）
- `sys_user_profile`（成员扩展信息）
- `med_medicine`（药品）
- `med_plan`（用药计划）
- `med_record`（服药记录）
- `health_vital`（体征数据）
- `health_report`（健康周报）

## 3. 关键表结构

### 3.1 `sys_family`（家庭表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| family_name | VARCHAR(100) | 家庭名称 |
| username | VARCHAR(50) | 家庭登录账号（唯一） |
| password | VARCHAR(100) | 家庭登录密码（BCrypt） |
| creator_id | BIGINT | 创建者成员ID |
| low_stock_threshold | INT | 低库存阈值（库存 < 该值触发提醒） |
| expiring_days | INT | 临期提醒天数（距离过期 <= 该值触发提醒） |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `UNIQUE uk_username(username)`

### 3.2 `sys_user`（成员表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| avatar | MEDIUMTEXT | 头像 URL/Base64 |
| role | TINYINT | 0-管理员 1-普通成员 2-受控成员 |
| family_id | BIGINT | 家庭ID |
| relation | VARCHAR(20) | 家庭关系 |
| care_mode | TINYINT | 关怀模式开关 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `KEY idx_family_id(family_id)`

### 3.3 `sys_user_profile`（成员扩展信息）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 成员ID（唯一） |
| birthday | DATE | 生日 |
| height | DECIMAL(5,1) | 身高(cm) |
| weight | DECIMAL(5,1) | 体重(kg) |
| disease | VARCHAR(255) | 病史 |
| allergy | VARCHAR(255) | 过敏史 |
| remark | VARCHAR(500) | 备注 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `UNIQUE uk_user_id(user_id)`

### 3.4 `med_medicine`（药品表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| family_id | BIGINT | 家庭ID（共享药箱） |
| name | VARCHAR(100) | 药品名称 |
| specification | VARCHAR(100) | 规格 |
| image_url | VARCHAR(255) | 图片 URL |
| stock | INT | 库存数量 |
| stock_unit | VARCHAR(20) | 库存单位（片/粒/瓶） |
| expire_date | DATE | 过期日期 |
| usage_notes | TEXT | 用药注意 |
| indication | VARCHAR(500) | 适应症 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `KEY idx_family_id(family_id)`

### 3.5 `med_plan`（用药计划表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 使用者ID |
| medicine_id | BIGINT | 药品ID |
| dosage | VARCHAR(50) | 单次剂量（字符串） |
| remind_slots | VARCHAR(50) | 服药时段（逗号分隔） |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期（NULL=长期） |
| take_days | VARCHAR(20) | 星期（1-7，逗号分隔） |
| plan_remark | VARCHAR(255) | 计划备注 |
| status | TINYINT | 0-停用 1-启用 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `KEY idx_user_id(user_id)`
- `KEY idx_status(status)`

### 3.6 `med_record`（服药记录表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| plan_id | BIGINT | 计划ID |
| user_id | BIGINT | 用户ID |
| medicine_id | BIGINT | 药品ID |
| scheduled_date | DATE | 应服日期 |
| slot_name | VARCHAR(20) | 时段（早/中/晚/睡前） |
| actual_time | DATETIME | 实际服药时间 |
| status | TINYINT | 0-未服 1-已服 2-跳过 |
| record_remark | VARCHAR(255) | 记录备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `KEY idx_user_id(user_id)`
- `KEY idx_scheduled_date(scheduled_date)`
- `UNIQUE uk_user_plan_date_slot(user_id, plan_id, scheduled_date, slot_name)`  
  说明：防止同一成员同一计划同一日期同一时段写入重复记录。

### 3.7 `health_vital`（体征数据表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 家庭ID |
| type | TINYINT | 体征类型 |
| value_systolic | DECIMAL(5,1) | 收缩压（血压专用） |
| value_diastolic | DECIMAL(5,1) | 舒张压（血压专用） |
| value | DECIMAL(6,2) | 数值（血糖/体重等） |
| unit | VARCHAR(20) | 单位 |
| measure_time | DATETIME | 测量时间 |
| measure_point | TINYINT | 血糖时点（1-空腹 2-餐后） |
| notes | VARCHAR(255) | 备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `idx_user_id`
- `idx_family_id`
- `idx_type`
- `idx_measure_time`

业务约定（当前后端常量）：
- `1`：血压
- `2`：血糖
- `3`：体重

### 3.8 `health_report`（健康周报表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 家庭ID |
| week_start | DATE | 周开始日期 |
| week_end | DATE | 周结束日期 |
| compliance_rate | DECIMAL(5,2) | 依从率（%） |
| vital_summary | TEXT | 体征摘要（文本/JSON片段） |
| ai_summary | TEXT | AI 总结 |
| risk_level | TINYINT | 0-低 1-中 2-高 |
| deleted | TINYINT | 逻辑删除 |
| create_time/update_time | DATETIME | 创建/更新时间 |

索引：
- `PRIMARY(id)`
- `UNIQUE uk_user_week(user_id, week_start)`
- `KEY idx_user_id(user_id)`
- `KEY idx_family_id(family_id)`

## 4. 关系说明（逻辑）

- `sys_family` 1 : N `sys_user`
- `sys_user` 1 : 1 `sys_user_profile`
- `sys_family` 1 : N `med_medicine`
- `sys_user` 1 : N `med_plan`
- `med_plan` 1 : N `med_record`
- `sys_user` 1 : N `health_vital`
- `sys_user` 1 : N `health_report`

## 5. 初始化脚本

项目初始化脚本：
- `fcg-server/src/main/resources/sql/init.sql`

执行方式：

```bash
mysql -h HOST -u USER -p fcg_db < fcg-server/src/main/resources/sql/init.sql
```
