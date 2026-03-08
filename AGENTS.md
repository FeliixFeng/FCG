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

## Development Status

### Completed (2026-03-08)
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

### Next Steps
- 🔄 药品页：药品列表、添加药品（含 OCR 识别）、用药计划
- 🔄 健康页：体征录入、近一周趋势图表
- 🔄 家庭页：成员列表、添加成员、关怀模式切换
- 🔄 管理员页：成员管理完善
- 🔄 关怀模式：大字体/简化 UI 适配
- 🔄 健康周报：AI 生成摘要展示

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
