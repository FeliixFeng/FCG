# 数据库设计文档 (Preliminary)

## 1. 实体关系图 (ERD) 概述
系统核心围绕 **家庭 (Family)** 展开，连接 **用户 (User)** 与 **健康数据 (Medicine/Record)**。

## 2. 表结构设计

### 2.1 用户表 (sys_user)
存储用户信息及角色。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| username | varchar(50) | 用户名 | 唯一 |
| password | varchar(100) | 加密密码 | |
| nickname | varchar(50) | 昵称 | |
| role | varchar(20) | 角色 | ADMIN/MEMBER/CONTROLLED |
| family_id | bigint | 所属家庭 ID | 外键 |
| phone | varchar(20) | 手机号 | |
| create_time | datetime | 创建时间 | |

### 2.2 家庭表 (sys_family)
管理家庭组。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| family_name | varchar(100) | 家庭名称 | |
| creator_id | bigint | 创建者 ID | |
| create_time | datetime | 创建时间 | |

### 2.3 药品信息表 (med_medicine)
存储药品基础数据（支持 OCR 录入）。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| name | varchar(100) | 药品名称 | |
| description | text | 药品描述/说明书 | |
| dosage_form | varchar(50) | 剂型 | 片剂/胶囊/液体等 |
| manufacturer | varchar(100) | 生产厂家 | |
| image_url | varchar(255) | 药品图片 | |

### 2.4 用药计划表 (med_plan)
定义服药周期与剂量。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| user_id | bigint | 使用者 ID | 受控成员 |
| medicine_id | bigint | 药品 ID | |
| dosage | varchar(50) | 单次剂量 | |
| frequency | varchar(50) | 频率 | 如 "每天3次" |
| cron_expression | varchar(100) | 定时表达式 | 用于任务调度 |
| start_date | date | 开始日期 | |
| end_date | date | 结束日期 | |
| status | tinyint | 状态 | 0:禁用, 1:启用 |

### 2.5 服药记录表 (med_record)
记录实际服药打卡情况。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| plan_id | bigint | 计划 ID | |
| user_id | bigint | 用户 ID | |
| scheduled_time | datetime | 应服药时间 | |
| actual_time | datetime | 实际服药时间 | |
| status | tinyint | 状态 | 0:未服, 1:已服, 2:跳过 |

### 2.6 健康周报表 (health_report)
由系统自动生成的汇总数据。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| id | bigint | 主键 ID | |
| user_id | bigint | 用户 ID | |
| family_id | bigint | 家庭 ID | |
| week_range | varchar(50) | 周区间 | 如 "2024-W12" |
| compliance_rate | decimal(5,2) | 依公性(打卡率) | |
| ai_summary | text | AI 总结建议 | |
| create_time | datetime | 生成时间 | |

## 3. 索引设计
- `sys_user`: `username` (Unique), `family_id`
- `med_plan`: `user_id`, `status`
- `med_record`: `user_id`, `scheduled_time`
