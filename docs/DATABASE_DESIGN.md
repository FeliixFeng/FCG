# 数据库设计文档

## 1. 概述

- **数据库**: MySQL 8.0
- **字符集**: utf8mb4
- **引擎**: InnoDB
- **逻辑删除**: 全表启用（deleted 字段）

## 2. 表结构设计

### 2.1 系统模块

#### sys_family（家庭表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 家庭ID（主键） |
| family_name | VARCHAR(100) | 家庭名称 |
| invite_code | VARCHAR(20) | 邀请码（唯一，用于家庭成员加入） |
| creator_id | BIGINT | 创建者用户ID |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(100) | 密码（加密） |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| role | TINYINT | 角色：0-管理员 1-普通成员 2-受控成员 |
| family_id | BIGINT | 所属家庭ID |
| care_mode | TINYINT | 关怀模式：0-关闭 1-开启 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.2 药品模块

#### med_medicine（药品表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 药品ID（主键） |
| family_id | BIGINT | 所属家庭ID |
| name | VARCHAR(100) | 药品名称 |
| specification | VARCHAR(100) | 规格（如 10mg*20片） |
| manufacturer | VARCHAR(100) | 生产厂家 |
| dosage_form | VARCHAR(50) | 剂型（片剂/胶囊/液体等） |
| image_url | VARCHAR(255) | 药品图片URL |
| instructions | TEXT | 药品说明书 |
| contraindications | TEXT | 禁忌事项 |
| side_effects | TEXT | 副作用 |
| stock | INT | 库存数量 |
| stock_unit | VARCHAR(20) | 库存单位（片/粒/瓶） |
| expire_date | DATE | 过期日期 |
| storage_location | VARCHAR(100) | 存放位置 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### med_plan（用药计划表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 计划ID（主键） |
| user_id | BIGINT | 使用者ID（受控成员） |
| family_id | BIGINT | 所属家庭ID |
| medicine_id | BIGINT | 药品ID |
| dosage | VARCHAR(50) | 单次剂量（如 1片） |
| frequency | VARCHAR(50) | 服药频率（如 每天3次） |
| remind_times | VARCHAR(100) | 提醒时间点（08:00,12:00,18:00） |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期（NULL表示长期） |
| take_days | VARCHAR(20) | 服药星期（1-7，逗号分隔） |
| notes | VARCHAR(255) | 备注（饭后服用等） |
| status | TINYINT | 状态：0-停用 1-启用 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### med_record（服药记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID（主键） |
| plan_id | BIGINT | 计划ID |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 所属家庭ID |
| medicine_id | BIGINT | 药品ID |
| scheduled_date | DATE | 应服药日期 |
| scheduled_time | TIME | 应服药时间 |
| actual_time | DATETIME | 实际服药时间 |
| status | TINYINT | 状态：0-未服 1-已服 2-跳过 |
| notes | VARCHAR(255) | 备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.3 健康模块

#### health_vital（体征数据表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID（主键） |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 所属家庭ID |
| type | TINYINT | 类型：1-血压 2-血糖 3-心率 4-体温 5-体重 |
| value_systolic | DECIMAL(5,1) | 收缩压（血压专用） |
| value_diastolic | DECIMAL(5,1) | 舒张压（血压专用） |
| value | DECIMAL(6,2) | 数值（血糖/心率/体温/体重） |
| unit | VARCHAR(20) | 单位 |
| measure_time | DATETIME | 测量时间 |
| measure_point | TINYINT | 测量时点（血糖专用：1-空腹 2-餐后） |
| notes | VARCHAR(255) | 备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### health_report（健康周报表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 报表ID（主键） |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 所属家庭ID |
| week_start | DATE | 周开始日期 |
| week_end | DATE | 周结束日期 |
| compliance_rate | DECIMAL(5,2) | 服药依从性（%） |
| vital_summary | TEXT | 体征摘要（JSON格式） |
| ai_summary | TEXT | AI健康建议 |
| risk_level | TINYINT | 风险等级：0-低 1-中 2-高 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 3. ER 关系

```
sys_family 1 ──── N sys_user
sys_user   1 ──── N med_plan
sys_user   1 ──── N med_record
sys_user   1 ──── N health_vital
sys_user   1 ──── N health_report

sys_family 1 ──── N med_medicine
med_medicine 1 ──── N med_plan
med_plan   1 ──── N med_record
```

## 4. 执行脚本

```bash
mysql -h HOST -u USER -p DATABASE < fcg-server/src/main/resources/sql/init.sql
```
