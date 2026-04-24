# 开发运维文档

> 文档定位：保留长期有效的开发、运维、排障信息。  
> 当前状态：第一阶段核心功能开发已收尾（2026-04-24），项目进入论文撰写优先阶段。

## 项目结构说明（当前版）

### 后端目录

`fcg-server/src/main/java/com/ghf/fcg`

- `config`：全局配置（Web、MyBatis、AI、OSS）
- `common`：统一返回、异常、上下文、拦截器、通用工具
- `modules/system`：家庭、成员、档案与鉴权基础
- `modules/medicine`：药品、计划、打卡记录
- `modules/health`：体征与周报
- `modules/ai`：AI 对话、上下文聚合
- `modules/admin`：管理端聚合查询

模块内通用分层约定：
- 入参：`dto`
- 出参：`vo`
- 领域对象：`entity`
- 数据访问：`mapper`
- 业务接口：`I*Service`
- 业务实现：`service/impl/*ServiceImpl`

### 前端目录

`fcg-client/src`

- `views/public`：Landing、成员选择
- `views/entry`：角色分流入口页
- `views/user`：普通成员页面
- `views/care`：关怀模式页面
- `views/admin`：管理端页面
- `components`：组件库（通用/业务组件）
- `stores`：Pinia 状态管理
- `router`：路由与守卫
- `utils`：请求、缓存、图片处理等工具

## 当前阶段建议

- 主策略：冻结大功能开发，优先论文初稿
- 代码侧仅做：
  1. 小范围体验优化
  2. 风险可控的结构规范化
  3. 演示稳定性修复
- 测试文档与测试执行可在论文阶段统一补齐

## 本地开发

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

### 本地编译检查

```bash
# 后端
cd fcg-server && mvn -q compile

# 前端
cd fcg-client && npm run build
```

## 服务器与数据库

### 常用 SSH

```bash
ssh ali1   # 数据库服务器
ssh ali2   # 应用服务器
```

### 数据库连通性快速检查

```bash
# 端口连通
nc -vz <db_host> 3306

# 从服务器侧执行 SQL
ssh ali1 "mysql -uroot -p'***' -D fcg_db -e \"SELECT NOW();\""
```

## 首页打卡链路（当前实现）

1. 药品页创建 `med_plan`
2. 首页请求 `/api/medicine/plan/records?scheduledDate=...`
3. 后端按计划生成当天任务，并叠加 `med_record` 状态
4. 首页打卡/跳过：
   - 有 `recordId` 则更新记录
   - 无 `recordId` 则创建记录
5. 打卡变“已服”时自动扣库存

## 关怀模式（role=2）实现约束（已定稿）

- 导航仅保留：`首页 / 健康 / 我的`
- 隐藏入口：`药品 / AI / 周报 / 管理界面`
- 受控成员路由收口：
  - 访问受限路由时统一重定向至首页
  - 仅做前端权限可见性与路由守卫改造，不新增后端表
- 计划维护边界：
  - 受控成员不维护药品与计划
  - 管理员负责为受控成员创建与维护计划
- 数据可见性边界：
  - 受控成员只执行任务与录入体征
  - 管理员仍可查看该成员图表、周报并执行周报生成

## 药品 OCR（当前实现）

- 药品录入支持多图（当前 UI 最多 4 张）
- 图片在前端先压缩，再预览、再调用 OCR
- OCR 为手动触发（点击“AI 智能解析”）
- 移动端支持：
  - `拍照`（调用手机摄像头，`capture=environment`）
  - `相册` 选择

## 健康周报链路（当前实现）

1. 前端点击“生成周报”
2. 后端聚合本周体征与依从率
3. 生成结构化体征摘要（最新值/均值/异常次数）
4. 调用 AI 生成 Markdown 周报
5. 同周覆盖写入（每周每人保留一条）

## AI 助手链路（当前实现）

1. 前端发送问题到 `/api/ai/chat/stream`（SSE）
2. 发送前先请求 `/api/ai/context` 获取聚合上下文（支持短缓存）
3. 后端聚合数据：
  - 成员健康档案（`sys_user_profile`）
  - 今日用药任务（计划+记录叠加）
  - 今日体征
  - 近 7 天体征趋势
  - 最新健康周报
4. 后端将聚合文本与用户问题一起传入大模型，流式回传
5. 前端逐步渲染流式内容，完成后按 Markdown 展示

## 常见故障排查

### 1) `Communications link failure`

现象：后端日志出现 `CannotGetJdbcConnectionException`。  
排查顺序：

1. 确认数据库服务可连通（`nc`/`mysql`）
2. 检查 `application-local.yml` 的数据库地址与账号
3. 重启后端（使连接池参数生效）
4. 观察连接池与数据库连接数

### 2) 首页显示“暂无计划”但药品页有计划

检查点：

1. `scheduledDate` 是否为当天
2. 计划状态是否启用
3. `takeDays` 是否包含当天
4. 计划起止日期是否覆盖当天

### 3) 周报生成失败

前端已做错误类型弹窗，常见原因：

1. API Key 失效/过期（认证失败）
2. AI 调用额度不足或限流（429）
3. 网络超时或连接失败
4. 后端 AI 地址/模型配置错误

建议排查：

1. 检查 `application-local.yml` 里的 AI 配置（baseUrl / apiKey / model）
2. 在服务器上测试网络连通
3. 查看后端日志关键字：`AI服务调用失败`、`Unauthorized`、`429`

## 部署说明

- 推送到 `main` 可触发 GitHub Actions 部署流程（按仓库配置）。
- `dev` 分支用于日常开发联调。
