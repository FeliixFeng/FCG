# Family Care Guardian (FCG) - 家庭健康管理系统

> 武汉纺织大学 计算机与人工智能学院 毕业设计项目

## 项目简介

基于 Spring Boot 3 与 Vue 3 的家庭健康管理系统，深度立足于居家养老场景。针对高龄群体在慢性病管理中面临的药品繁杂、服药易忘、数字鸿沟等痛点，通过数字化手段实现高效的用药管理、家庭协同与智能咨询。

### 核心价值
- **降低看护负担**：数字化管理复杂用药方案，闭环提醒降低安全风险。
- **跨越数字鸿沟**：深度适老化设计，支持“标准/关怀”双模式一键切换。
- **强化家庭协同**：子女远程掌握父母用药情况，打破数据孤岛。
- **技术赋能安全**：集成 OCR 识别与 LLM 大模型，提供通俗易懂的用药建议。

## 核心功能

- **RBAC 家庭多级权限体系**
  - 预设超级管理员、普通成员、受控成员（长辈）三种角色。
  - 受控成员强制进入“关怀模式”，简化交互，防止误操作。
- **智能药箱与 AI 咨询**
  - **OCR 识别**：拍照自动采集药品信息，告别繁琐手动录入。
  - **AI 助手**：利用大语言模型（LLM）转译晦涩说明书，提供用药禁忌预警。
- **闭环服药提醒与打卡**
  - 定时任务触发服药提醒，支持一键打卡反馈。
  - 自动生成健康周报，同步至家庭成员。
- **适老化双模式界面**
  - **标准模式**：功能全面，适合子女/管理员使用。
  - **关怀模式**：大字体、高对比度、扁平化路径，专为长辈设计。

## 当前进度

### 后端
- ✅ 认证闭环（JWT + 拦截器 + CORS）
- ✅ 用户与家庭模块（登录/注册、用户信息、关怀模式、家庭创建/加入/成员）
- ✅ 药品模块基础 API（药品/计划/记录增删改查 + 计划记录联表）
- ✅ 健康模块基础 API（体征/周报增删改查 + 近一周体征查询）

### 前端
- 🔄 基础脚手架已完成，页面与状态管理待完善

## 技术栈

### 后端 (fcg-server)
- **核心框架**：Java 17, Spring Boot 3.2.5
- **持久层**：MyBatis-Plus 3.5.6, MySQL 8.0
- **缓存/任务**：Redis (提醒任务加速), Spring Task
- **安全认证**：JWT, RBAC 权限模型
- **部署运维**：Docker, GitHub Actions (CI/CD)

### 前端 (fcg-client)
- **核心框架**：Vue 3.4.21 (Composition API)
- **构建工具**：Vite 5.2.0
- **状态管理**：Pinia
- **UI 组件**：Element Plus (适老化定制)
- **样式处理**：Tailwind CSS

### 智能能力
- **OCR**：药品包装/说明书文字识别
- **LLM**：大语言模型 API (用药建议与禁忌分析)

## 项目结构

```
fcg/
├── fcg-server/              # Spring Boot 后端
│   ├── src/main/java/       # 业务逻辑
│   └── src/main/resources/  # 配置文件
├── fcg-client/              # Vue 3 前端
│   ├── src/components/      # 公共组件
│   └── src/views/           # 页面视图 (标准/关怀模式)
└── docs/                    # 项目文档 (数据库设计等)
```

## 快速开始

### 后端启动
1. 配置 `application-local.yml` 中的数据库与 Redis 连接。
2. 运行 `mvn spring-boot:run` 或启动 `FcgApplication.java`。

### 前端启动
1. `cd fcg-client`
2. `npm install`
3. `npm run dev`

## 配置说明

### 本地配置文件
后端默认启用 `local` profile，请在 `fcg-server/src/main/resources/application-local.yml` 中配置数据库连接。
示例见：`fcg-server/src/main/resources/application-local.yml.example`。

### 常用环境变量（可选）
Spring Boot 支持用环境变量覆盖配置，常用项如下：
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `SERVER_PORT`

## 作者与致谢
- **作者**：管海峰 (2204240115)
- **指导教师**：王帮超 (副教授)
- **学校**：武汉纺织大学 计算机与人工智能学院

## 开发备注
最近一次本地测试：2026-02-23
