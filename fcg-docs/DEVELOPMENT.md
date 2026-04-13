# 开发运维文档

## SSH 连接

### 服务器列表

| 名称 | IP | 用户 | 用途 |
|-----|-----|-----|------|
| ali1 | 47.121.181.198 | root | 数据库服务器 |
| ali2 | 8.148.25.67 | root | 应用服务器 |

### 连接方式

```bash
ssh ali1   # 连接数据库服务器
ssh ali2   # 连接应用服务器
```

SSH 配置已保存在 `~/.ssh/config`，支持免密登录。

---

## 数据库操作

### 查看数据

```bash
ssh ali1 "mysql -u root -p'你的密码' -e 'SELECT * FROM fcg_db.medicine;'"
```

### 删除数据（示例：删除药品表）

```bash
ssh ali1 "mysql -u root -p'你的密码' -e 'DELETE FROM fcg_db.medicine WHERE id=1;'"
```

---

## 本地启动

### 方式一：命令行

```bash
# 后端
cd fcg-server && mvn spring-boot:run

# 前端（开新终端）
cd fcg-client && npm run dev
```

### 方式二：VSCode

- 按 `Ctrl+Shift+P` → "Tasks: Run Task" → 选择任务
- 或按 `F5` 使用 Debug 配置

### 方式三：一键启动

```bash
./run.sh
```

---

## 云端部署

项目已配置 GitHub Actions CI/CD，推送到 main 分支自动部署。