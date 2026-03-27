# AGENTS.md - FCG (Family Care Guardian)

> 家庭健康管理系统 - 毕业设计项目

**Generated:** 2026-03-08  
**Commit:** 5db3fe1  
**Branch:** dev

## Project Overview

- **Type**: Full-stack web application (Spring Boot + Vue 3)
- **Backend**: Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Maven
- **Frontend**: Vue 3.4.21, Vite 5.2.0, npm
- **Database**: MySQL 8.0

## Build Commands

### Backend (fcg-server/)

```bash
cd fcg-server

# Compile
mvn compile

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName

# Package (creates JAR)
mvn package

# Clean build
mvn clean package

# Run application
mvn spring-boot:run

# Or run main class directly
java -jar target/fcg-server-1.0.0.jar
```

### Frontend (fcg-client/)

```bash
cd fcg-client

# Install dependencies
npm install

# Development server
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

## Code Style Guidelines

### Java (Backend)

- **Package**: `com.ghf.fcg`
- **Naming**: PascalCase classes, camelCase methods/variables, UPPER_SNAKE_CASE constants
- **Imports**: Use explicit imports, no wildcard imports
- **Lombok**: Use `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` for entities
- **Annotations**: Place class annotations on separate lines
- **Braces**: Opening brace on same line (K&R style)
- **Indent**: 4 spaces, no tabs

### Vue/JavaScript (Frontend)

- **Style**: Composition API with `<script setup>`
- **Naming**: PascalCase components, camelCase functions/variables
- **Imports**: Use single quotes, no semicolons
- **Template**: 2-space indent, semantic HTML
- **Comments**: Use `//` for single line, `/* */` for blocks

### General

- **Encoding**: UTF-8
- **Line endings**: LF (Unix-style)
- **Line length**: 120 characters max
- **Trailing whitespace**: Remove before commit
- **EOF**: End files with a single newline

## Project Structure

```
fcg/
├── fcg-docs/                # 文档目录
│   ├── DATABASE_DESIGN.md  # 数据库设计
│   ├── REFERENCES.md       # 参考资料
│   └── TODO.md            # 任务清单
│
├── fcg-server/              # Spring Boot 后端
│   ├── pom.xml             # Maven 配置
│   └── src/
│       ├── main/java/com/ghf/fcg/
│       │   ├── modules/           # 业务模块
│       │   │   ├── system/        # 系统模块
│       │   │   │   ├── entity/    # 实体类
│       │   │   │   ├── mapper/    # 数据访问层
│       │   │   │   └── service/   # 业务逻辑层
│       │   │   ├── medicine/      # 药品模块
│       │   │   │   ├── entity/
│       │   │   │   ├── mapper/
│       │   │   │   └── service/
│       │   │   └── health/        # 健康模块
│       │   │       ├── entity/
│       │   │       ├── mapper/
│       │   │       └── service/
│       │   ├── common/            # 公共组件
│       │   │   ├── exception/     # 全局异常处理
│       │   │   └── result/        # 统一响应
│       │   └── config/            # 配置类
│       └── main/resources/
│           ├── application.yml    # 主配置
│           └── sql/
│               └── init.sql       # 数据库初始化脚本
│
└── fcg-client/              # Vue 3 前端
    ├── package.json
    └── src/
        ├── App.vue
        └── main.js
```

## Code Map

### Backend Entry Points

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `FcgApplication` | Class | `fcg-server/.../FcgApplication.java` | Spring Boot 启动类 |
| `Knife4jConfig` | Class | `fcg-server/.../config/Knife4jConfig.java` | Swagger/OpenAPI 文档配置 |
| `MybatisPlusConfig` | Class | `fcg-server/.../config/MybatisPlusConfig.java` | MyBatis-Plus 配置 |
| `GlobalExceptionHandler` | Class | `fcg-server/.../common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `R` | Class | `fcg-server/.../common/result/R.java` | 统一响应封装 |

### Domain Entities (系统模块)

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `Family` | Entity | `.../modules/system/entity/Family.java` | 家庭实体 |
| `User` | Entity | `.../modules/system/entity/User.java` | 用户实体 |

### Domain Entities (药品模块)

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `Medicine` | Entity | `.../modules/medicine/entity/Medicine.java` | 药品实体 |
| `MedicinePlan` | Entity | `.../modules/medicine/entity/MedicinePlan.java` | 用药计划实体 |
| `MedicineRecord` | Entity | `.../modules/medicine/entity/MedicineRecord.java` | 服药记录实体 |

### Domain Entities (健康模块)

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `Vital` | Entity | `.../modules/health/entity/Vital.java` | 体征数据实体 |
| `HealthReport` | Entity | `.../modules/health/entity/HealthReport.java` | 健康周报实体 |

### Data Access Layer

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `FamilyMapper` | Mapper | `.../modules/system/mapper/FamilyMapper.java` | 家庭数据访问 |
| `UserMapper` | Mapper | `.../modules/system/mapper/UserMapper.java` | 用户数据访问 |
| `MedicineMapper` | Mapper | `.../modules/medicine/mapper/MedicineMapper.java` | 药品数据访问 |
| `MedicinePlanMapper` | Mapper | `.../modules/medicine/mapper/MedicinePlanMapper.java` | 用药计划数据访问 |
| `MedicineRecordMapper` | Mapper | `.../modules/medicine/mapper/MedicineRecordMapper.java` | 服药记录数据访问 |
| `VitalMapper` | Mapper | `.../modules/health/mapper/VitalMapper.java` | 体征数据访问 |
| `HealthReportMapper` | Mapper | `.../modules/health/mapper/HealthReportMapper.java` | 健康报告数据访问 |

### Service Layer

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `IFamilyService` | Interface | `.../modules/system/service/IFamilyService.java` | 家庭服务接口 |
| `FamilyServiceImpl` | Class | `.../modules/system/service/impl/FamilyServiceImpl.java` | 家庭服务实现 |
| `IUserService` | Interface | `.../modules/system/service/IUserService.java` | 用户服务接口 |
| `UserServiceImpl` | Class | `.../modules/system/service/impl/UserServiceImpl.java` | 用户服务实现 |
| `IMedicineService` | Interface | `.../modules/medicine/service/IMedicineService.java` | 药品服务接口 |
| `MedicineServiceImpl` | Class | `.../modules/medicine/service/impl/MedicineServiceImpl.java` | 药品服务实现 |
| `IMedicinePlanService` | Interface | `.../modules/medicine/service/IMedicinePlanService.java` | 用药计划服务接口 |
| `MedicinePlanServiceImpl` | Class | `.../modules/medicine/service/impl/MedicinePlanServiceImpl.java` | 用药计划服务实现 |
| `IMedicineRecordService` | Interface | `.../modules/medicine/service/IMedicineRecordService.java` | 服药记录服务接口 |
| `MedicineRecordServiceImpl` | Class | `.../modules/medicine/service/impl/MedicineRecordServiceImpl.java` | 服药记录服务实现 |
| `IVitalService` | Interface | `.../modules/health/service/IVitalService.java` | 体征服务接口 |
| `VitalServiceImpl` | Class | `.../modules/health/service/impl/VitalServiceImpl.java` | 体征服务实现 |
| `IHealthReportService` | Interface | `.../modules/health/service/IHealthReportService.java` | 健康报告服务接口 |
| `HealthReportServiceImpl` | Class | `.../modules/health/service/impl/HealthReportServiceImpl.java` | 健康报告服务实现 |

### Frontend Entry Points

| Symbol | Type | Location | Purpose |
|--------|------|----------|---------|
| `App` | Component | `fcg-client/src/App.vue` | 根组件 |
| `main` | Script | `fcg-client/src/main.js` | 应用入口 |
| `Landing` | View | `fcg-client/src/views/Landing.vue` | 落地页（Hero + 功能介绍 + CTA）|
| `AuthDialog` | Component | `fcg-client/src/components/auth/AuthDialog.vue` | 登录/注册弹窗 |
| `Home` | View | `fcg-client/src/views/Home.vue` | 首页（成员选择后主页）|
| `SelectMember` | View | `fcg-client/src/views/SelectMember.vue` | 成员选择页 |

## Key Configuration Files

- **Backend**: `fcg-server/src/main/resources/application.yml`
- **Local Config**: `application-local.yml` (gitignored, contains secrets)
- **Frontend**: `fcg-client/package.json`

## Testing

### Backend Tests
- Use JUnit 5 (included with Spring Boot)
- Test classes: `src/test/java/...`
- Naming: `*Test.java` or `*Tests.java`

### Running Single Test
```bash
# Maven single test
mvn test -Dtest=UserServiceTest

# Single test method
mvn test -Dtest=UserServiceTest#shouldCreateUser
```

## UI/UX Design Guidelines

### Role-Based Permission System

#### Role Definitions

| 角色 | role 值 | 定位 | 典型用户 | 创建方式 |
|------|---------|------|----------|----------|
| **超级管理员** | 0 | 家庭管理者 | 子女、创建者 | 注册时第一个用户 |
| **普通成员** | 1 | 自主使用者 | 中青年家庭成员 | 管理员添加 |
| **受控成员** | 2 | 被照护对象 | 老人、需关怀人群 | 管理员添加，强制关怀模式 |

#### Page Access Matrix

| 页面 | 超级管理员 | 普通成员 | 受控成员 | 说明 |
|------|-----------|---------|---------|------|
| **首页** | ✅ 完整版 | ✅ 完整版 | ✅ 简化版 | 所有人都有 |
| **药品页** | ✅ 增删改查 + 为所有人创建计划 | ✅ 增删改查 + 仅为自己创建计划 | ✅ 仅查看（关怀模式） | 药品库共享，计划个人化 |
| **健康页** | ✅ 可切换查看所有人 | ✅ 仅查看自己 | ✅ 仅录入/查看自己（关怀模式） | 管理员可查看家庭所有人体征 |
| **家庭页** | ✅ 成员列表 + 基础信息 + 编辑家庭名 | ✅ 成员列表 + 基础信息 | ❌ 不显示 | 添加/删除成员功能在管理员页 |
| **管理员页** | ✅ 成员管理 + 权限分配 | ❌ 不显示 | ❌ 不显示 | 仅管理员可见 |

**核心逻辑**：
- **药品库是共享的**（家庭药箱概念）→ 管理员和普通成员都可以增删改查
- **用药计划是个人的**（谁吃什么药）→ 管理员可为所有人创建，普通成员只能管理自己的
- **受控成员只能打卡和查看**（老人只需要按提醒吃药）

### Dual-Layer Interface Architecture

**设计理念**：为了解决移动端导航栏 Tab 过多（6+个）的问题，同时保持清晰的功能分层，FCG 采用 **"用户界面 + 管理界面"** 双层架构设计。

#### User Interface (用户界面)

**适用角色**：所有成员（超级管理员、普通成员、受控成员）  
**功能定位**：日常健康管理功能（用药打卡、体征录入、家庭协作）  
**Layout Component**: `BaseLayout.vue`  
**Route Prefix**: `/`

##### Desktop Navigation (≥ 768px)

- **超级管理员**：`[Logo] FCG | 张家庭  [首页] [药品] [健康] [家庭] [管理]  张爸爸▼`
- **普通成员**：`[Logo] FCG | 张家庭  [首页] [药品] [健康] [家庭]  张妈妈▼`
- **受控成员**：`[Logo] FCG | 张家庭  [首页] [药品] [健康]  张奶奶▼`

桌面端保持原有设计，顶部横向导航，管理员有"管理"Tab（后期可能调整为"我的"页面入口）。

##### Mobile Navigation (< 768px)

- **超级管理员 / 普通成员**：`[首页] [药品] [健康] [家庭] [我的]` **5个Tab**
- **受控成员**：`[首页] [药品] [健康] [我的]` **4个Tab**（无"家庭"）

**关键改进**：
- ✅ 移动端所有角色统一增加 **"我的"** 导航项（个人中心）
- ✅ 解决移动端缺少切换成员/退出登录入口的问题
- ✅ 符合 iOS/Android 设计规范（≤5个 Tab）
- ✅ 管理员通过"我的"页面进入独立的**管理界面**

**关怀模式特殊样式**（受控成员，移动端）：
- 底部导航栏高度：80px（常规 60px）
- 图标尺寸：32px（常规 24px）
- 文字尺寸：18px（常规 12px）
- 居中布局（4个Tab均匀分布）

#### Admin Interface (管理界面)

**适用角色**：仅超级管理员（role=0）  
**功能定位**：家庭管理、成员权限分配、系统设置、数据统计  
**Layout Component**: `AdminLayout.vue` **（新建）**  
**Route Prefix**: `/admin`  
**Entry Point**: `ProfileHome.vue` → "进入管理界面" 按钮

##### Desktop Navigation (≥ 768px)

- **顶部导航栏** / **左侧边栏**：`[成员管理] [药品管理] [系统设置] [数据统计] [返回]`
- 样式风格与用户界面一致（主色调 `#2d5f5d`），但标题显示"管理中心"
- 支持侧边栏布局（可选）

##### Mobile Navigation (< 768px)

- **底部导航栏**：`[成员管理] [药品管理] [系统设置] [数据统计] [返回]` **5个Tab**
- "返回" Tab 点击后回到用户界面的"我的"页面

##### Admin Interface Features

| 导航项 | 功能说明 | 实现优先级 |
|--------|---------|-----------|
| 👥 成员管理 | 增删改查家庭成员、权限分配、关系设置 | ✅ **高**（V1 核心功能） |
| 💊 药品管理 | 批量管理药品库、查看所有人用药计划、过期药品清理 | ⏸️ 中（待定义需求） |
| ⚙️ 系统设置 | 家庭名称编辑、通知设置、数据导出/导入 | ⏸️ 中（待定义需求） |
| 📊 数据统计 | 家庭健康趋势图表、用药依从率、ECharts 可视化 | ⏸️ 低（后期迭代） |
| ← 返回 | 退出管理界面，返回用户界面 | ✅ 必需 |

**当前实现策略**（2026-03-27 确认）：
- **V1.0**: 优先实现"成员管理"功能，其他3个导航项暂时留空或显示"敬请期待"
- **V2.0**: 根据用户反馈逐步补充其他管理功能

#### Navigation Design Summary

**用户界面 vs 管理界面对比**：

| 对比项 | 用户界面 | 管理界面 |
|--------|---------|---------|
| **功能定位** | 日常健康管理 | 家庭管理后台 |
| **适用角色** | 所有成员 | 仅超级管理员 |
| **布局组件** | `BaseLayout.vue` | `AdminLayout.vue` |
| **路由前缀** | `/` | `/admin` |
| **导航数量** | 4-5个 | 5个 |
| **入口方式** | 登录后默认进入 | 通过"我的"页面进入 |
| **桌面端导航** | 顶部横向 | 顶部/侧边栏可选 |
| **移动端导航** | 底部固定 | 底部固定 |

#### Role Navigation Matrix (Updated)

**用户界面导航**：

| 导航项 | 超级管理员 | 普通成员 | 受控成员 | 说明 |
|--------|-----------|---------|---------|------|
| 🏠 首页 | ✅ | ✅ | ✅ | 所有人可见 |
| 💊 药品 | ✅ | ✅ | ✅ | 所有人可见 |
| ❤️ 健康 | ✅ | ✅ | ✅ | 所有人可见 |
| 👨‍👩‍👧‍👦 家庭 | ✅ | ✅ | ❌ | 受控成员不显示 |
| 👤 我的 | ✅ | ✅ | ✅ | **新增**，所有人可见 |
| ⚙️ 管理 | ⚠️ 桌面端可见 | ❌ | ❌ | 桌面端暂保留，移动端移除 |

**管理界面导航**（仅管理员）：

| 导航项 | 功能覆盖 | 备注 |
|--------|---------|------|
| 👥 成员管理 | 增删改查成员、权限分配 | ✅ V1 核心功能 |
| 💊 药品管理 | 批量药品管理、过期清理 | ⏸️ 待定义 |
| ⚙️ 系统设置 | 家庭设置、通知设置 | ⏸️ 待定义 |
| 📊 数据统计 | 健康趋势、ECharts 图表 | ⏸️ 后期迭代 |
| ← 返回 | 退出管理界面 | ✅ 必需 |

### Layout Strategy

#### Device Priorities
- **主要适配**：桌面网页端（≥ 768px）— 优先开发
- **兼容**：移动端响应式（< 768px）— 同步适配

#### Navigation Placement
- **桌面端**（≥ 768px）：顶部横向导航
- **移动端**（< 768px）：底部固定导航栏

### Component Selection

#### Medicine Page
- **Layout**：卡片布局（温馨，适合家庭）
- **Plan Management**：右侧抽屉（Drawer）
- **Forms**：弹窗（Dialog）

#### Health Page
- **Charts**：ECharts 5（折线图 + 参考线）
- **Data Entry**：弹窗表单
- **Member Switcher**（管理员）：顶部下拉选择

#### Family Page
- **Member List**：卡片布局
- **Quick Actions**：切换成员、退出登录

#### Admin Page
- **Member Management**：表格（Table）
- **Forms**：弹窗（Dialog）

### Care Mode Enhancement

**适配对象**：受控成员（role = 2）强制启用

#### Visual Enhancements
- **Font Size**：1.5rem+ （常规 0.9-1rem）
- **Icon Size**：32px（常规 22-24px）
- **Button Height**：52px（常规 40px）
- **Color Contrast**：高对比度配色
- **Line Height**：1.8（常规 1.4-1.5）

#### Interaction Simplification
- 隐藏次要功能（编辑/删除按钮）
- 减少步骤（一键打卡，无确认弹窗）
- 简化时间线（仅显示今日任务）

### Color Palette

| 用途 | 颜色 | 用法 |
|------|------|------|
| 主色调 | `#2d5f5d` | 导航栏、按钮、强调文字 |
| 超时高亮 | `#e74c3c` | 未打卡提醒、过期药品 |
| 背景渐变 | `linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%)` | 页面背景 |
| 卡片背景 | `rgba(255, 255, 255, 0.9)` | 卡片、弹窗 |

## Development Status

### Completed (2026-03-27)
- ✅ Database schema design (7 tables)
- ✅ MyBatis-Plus Generator configuration
- ✅ Entity classes (7 entities)
- ✅ Mapper interfaces (7 mappers)
- ✅ Service layer (7 interfaces + implementations)
- ✅ Project structure documentation
- ✅ dev branch created and synced
- ✅ Landing page（Hero + Feature Cards + Steps + CTA + Footer）
- ✅ AuthDialog（两栏登录/注册弹窗，支持注册）
- ✅ 路由守卫（家庭级/成员级 token 双重校验）
- ✅ 前端基础页面骨架（Home/SelectMember/FamilyHome/MedicineHome/AdminHome）
- ✅ SelectMember 成员按关系词权重排序（老人 > 父母 > 子女）
- ✅ SelectMember 移动端适配（3列/2列/单列响应式）
- ✅ SelectMember + user.js 家庭名称并行加载修复
- ✅ 页面切换 fade 过渡动画（App.vue Transition，纯 opacity，无位移）
- ✅ 滚动条跳动修复（html scrollbar-gutter: stable，消除 overflow-x:hidden 干扰）
- ✅ 测试数据：zhangfamily 8 名成员覆盖所有头像类型
- ✅ "访客"Bug 修复（BaseLayout onMounted 自动加载 member）
- ✅ 权限体系与 UI/UX 设计规范确认
- ✅ 双层界面架构设计（用户界面 + 管理界面）

### In Progress
- 🔄 ProfileHome.vue（"我的"页面）实现
- 🔄 AdminLayout.vue（管理界面布局）实现

### Next Steps
- 🔄 药品页：药品列表、添加药品（暂跳过 OCR）、用药计划
- 🔄 健康页：体征录入、近一周趋势图表
- 🔄 家庭页：成员列表、基础信息展示
- 🔄 管理员页：成员管理完善
- 🔄 关怀模式：大字体/简化 UI 统一适配
- ⏸️ OCR/AI 功能：方案待定（服务器不可用）
- ⏸️ 健康周报：AI 生成摘要展示

## CI/CD (GitHub Actions + Alibaba Cloud ACR)

### 流程概述

```
推送 main 分支 → GitHub Actions 构建 → 推送到阿里云 ACR → 服务器拉取镜像 → 部署
```

### GitHub Secrets

需要在仓库 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 | 示例值 |
|--------|------|--------|
| `ACR_REGISTRY` | ACR 地址 | `crpi-xxxxx.cn-hangzhou.personal.cr.aliyuncs.com` |
| `ACR_USERNAME` | 阿里云账号 | 你的阿里云用户名 |
| `ACR_PASSWORD` | 阿里云密码 | 你的阿里云密码 |
| `SSH_HOST` | 服务器 IP | 你的服务器地址 |
| `SSH_PORT` | SSH 端口 | 22 |
| `SSH_USER` | SSH 用户名 | root |
| `SSH_PRIVATE_KEY` | SSH 私钥 | 你的私钥内容 |

### 触发条件

- 推送到 `main` 分支触发部署
- 推送到 `dev` 分支不触发（开发测试用）

### ACR 镜像

- **后端**: `crpi-xxxxx.cn-hangzhou.personal.cr.aliyuncs.com/feliixfeng/fcg-server:latest`
- **前端**: `crpi-xxxxx.cn-hangzhou.personal.cr.aliyuncs.com/feliixfeng/fcg-client:latest`

只保留 `latest` 标签，每次推送覆盖。

### 回滚

如果新版本有问题，需要重新推送旧代码到 main 分支触发重新部署。

## Notes

- This is a graduation project (毕业设计) from Wuhan Textile University
- Backend runs on port 8080 by default
- Frontend dev server typically runs on port 5173
- Database credentials stored in `application-local.yml` (not in git)
- No existing linter configs (ESLint/Prettier) or Cursor/Copilot rules found
- Working branch: `dev` (main for stable releases)

## Dependencies to Know

### Backend
- Spring Boot Web, Validation
- MyBatis-Plus (ORM)
- MySQL Connector
- Lombok
- Knife4j (Swagger/OpenAPI 文档)

### Frontend
- Vue 3 (Composition API)
- Vite (build tool)
- Installed: Element Plus, Axios, Pinia, Tailwind CSS

## Common Tasks

```bash
# Start backend
cd fcg-server && mvn spring-boot:run

# Start frontend (in new terminal)
cd fcg-client && npm run dev

# Full build
cd fcg-server && mvn clean package && cd ../fcg-client && npm run build
```
