# 开发运维文档

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

## 部署说明

- 推送到 `main` 可触发 GitHub Actions 部署流程（按仓库配置）。
- `dev` 分支用于日常开发联调。
