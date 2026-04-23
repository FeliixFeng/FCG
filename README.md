# Family Care Guardian (FCG) - 家庭健康管理系统

> 武汉纺织大学毕业设计项目（Spring Boot 3 + Vue 3）

## 项目简介

FCG 面向家庭健康管理场景，围绕“谁在什么时候该吃什么药”构建完整链路：药品管理、用药计划、首页打卡、健康数据、周报查看。

## 当前核心能力（2026-04-23）

- 家庭账号 + 成员切换 + 角色权限（管理员/普通成员/受控成员）
- 药品页：药箱管理、OCR 识别、创建用药计划
- 首页：今日任务中心（主任务卡 + 待处理任务 + 今日已处理）
- 打卡链路：
  - 首页按“计划表 + 记录表”联表展示今日任务
  - 当天未打卡任务可补打到 23:59
  - 打卡状态变为“已服”时自动扣减库存
- 健康页：体征录入、趋势图、周报查看
- 管理员可切换查看家庭其他成员

## 关键实现说明

### 用药任务展示逻辑

- 首页不再只依赖 `med_record`。
- `/api/medicine/plan/records` 在传入 `scheduledDate` 时会：
  1. 从 `med_plan` 生成当天应执行任务；
  2. 再叠加 `med_record` 的实际状态；
  3. 无记录时返回 `待服`（可直接打卡/跳过）。

### 打卡与库存

- 首次打卡可直接创建记录（不要求先有 recordId）。
- 状态从“非已服”变为“已服”时自动扣库存。
- 扣减量按计划剂量（最少 1）计算。

### 数据一致性

- `med_record` 已增加唯一索引：
  - `(user_id, plan_id, scheduled_date, slot_name)`
- 防止同用户同计划同日期同时间段出现重复记录。

## 技术栈

- 后端：Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL 8.0
- 前端：Vue 3.4, Vite 5, Pinia, Element Plus
- 智能能力：GLM 多模态 OCR/解析（药品识别）

## 项目结构

```text
fcg/
├── fcg-server/              # Spring Boot 后端
├── fcg-client/              # Vue 前端
└── fcg-docs/                # 项目文档
```

## 本地启动

### 后端

```bash
cd fcg-server
mvn spring-boot:run
```

### 前端

```bash
cd fcg-client
npm install
npm run dev
```

## 文档导航

- 开发运维：`fcg-docs/DEVELOPMENT.md`
- 数据库设计：`fcg-docs/DATABASE_DESIGN.md`

## 作者

- 管海峰（2204240115）
