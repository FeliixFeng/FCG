# Family Care Guardian (FCG)

> 武汉纺织大学毕业设计项目  
> 家庭健康管理系统（Spring Boot 3 + Vue 3）

## 项目定位

FCG 面向家庭场景，围绕“计划制定 -> 当日提醒 -> 打卡记录 -> 健康追踪 -> 周报总结”形成闭环。  
系统采用家庭账号 + 成员身份切换，覆盖管理员、普通成员、受控成员三类角色。

## 核心功能（当前版本）

### 1) 用户侧（全成员可用）
- 首页任务中心：下一条任务、待处理列表、已处理列表、一键打卡/跳过/切换
- 药品管理：家庭药箱增删改查、OCR 识别录入、库存与临期提示、计划创建
- 健康管理：血压/血糖/体重录入、7天趋势图、周报生成与查看
- AI 助手：流式对话（SSE）、基于成员上下文的问答（档案+任务+体征+周报）
- 我的页面：成员资料、头像、档案信息、常用操作

### 2) 关怀模式（受控成员 role=2）
- 保留并放大核心能力：`首页 / 健康 / 我的`
- 简化操作路径：更少入口、更大按钮、更强对比、更低误触成本
- 药品与计划维护职责由管理员承担

### 3) 管理侧（仅管理员）
- 管理首页：家庭概览与快捷入口
- 成员管理：成员新增/编辑、角色与关系维护、头像管理
- 计划管理：家庭当日计划总览与筛选
- 系统设置：家庭名称、低库存阈值、临期提醒天数、密码修改

## 关键业务逻辑

### 今日任务生成
- 首页任务不是单看记录表，而是由 `med_plan` 生成当日应执行任务，再叠加 `med_record` 状态。
- 没有历史记录时，任务也能正常显示为“待服”，可直接打卡。

### 打卡与库存联动
- 首次打卡可直接创建记录（无需先有 recordId）。
- 状态从“非已服”变更为“已服”时自动扣减库存。
- 扣减量按计划剂量计算，最小扣减 1。

### 数据一致性保障
- `med_record` 唯一索引：`(user_id, plan_id, scheduled_date, slot_name)`。
- 防止同成员同计划同日期同时间段重复记录。

## 技术栈

- 后端：Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.6、MySQL 8.0
- 前端：Vue 3.4、Vite 5、Pinia、Element Plus
- AI 能力：GLM（OCR + 对话）
- 部署：Docker + GitHub Actions + Alibaba Cloud ACR

## 项目结构

```text
fcg/
├── fcg-server/                    # Spring Boot 后端
│   └── src/main/resources/sql/
│       └── init.sql              # 基线建表脚本
├── fcg-client/                    # Vue 前端
│   └── src/
│       ├── views/public          # Landing / SelectMember
│       ├── views/entry           # HomeEntry / HealthEntry / ProfileEntry（角色分流）
│       ├── views/user            # 普通用户页面
│       ├── views/care            # 关怀模式页面
│       └── views/admin           # 管理界面页面
└── fcg-docs/                      # 文档
```

## 本地开发

### 启动后端
```bash
cd fcg-server
mvn spring-boot:run
```

### 启动前端
```bash
cd fcg-client
npm install
npm run dev
```

### 构建检查
```bash
cd fcg-server && mvn -q compile
cd ../fcg-client && npm run build
```

## 配置说明

- 后端主配置：`fcg-server/src/main/resources/application.yml`
- 本地私密配置：`application-local.yml`（不入库）
- 前端接口配置：`fcg-client/src/utils/http.js`

## 分支与发布约定

- `dev`：日常开发联调分支
- `main`：稳定发布分支（推送后触发部署）

## 文档导航

- 开发运维文档：`fcg-docs/DEVELOPMENT.md`
- 数据库设计文档：`fcg-docs/DATABASE_DESIGN.md`

## 作者

- 管海峰（2204240115）
