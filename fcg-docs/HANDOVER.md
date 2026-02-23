# FCG 项目交接 Prompt

## 项目概况
- **项目名称**: Family Care Guardian (家庭健康管理系统)
- **技术栈**: Spring Boot 3 + Vue 3 + MySQL
- **后端端口**: 8080
- **数据库**: MySQL 8.0

## 已完成功能

### 后端 API
- ✅ 用户模块：注册、登录、获取信息、更新信息、切换关怀模式
- ✅ 家庭模块：创建家庭、加入家庭（邀请码）、获取家庭信息、获取家庭成员
- ✅ 药品模块：Medicine/MedicinePlan/MedicineRecord 增删改查
- ✅ 健康模块：Vital/HealthReport 增删改查、近一周体征查询
- ✅ JWT 认证拦截器
- ✅ 全局异常处理

### 数据库
- 7 张核心表：sys_user, sys_family, medicine, medicine_plan, medicine_record, vital, health_report

## 代码规范
- 4 空格缩进
- 使用 `@Builder` 而非 `@Accessors`
- 异常用 `BusinessException`
- 响应用 `Result.success()` / `Result.error()`
- 静态方法工具类（PasswordEncoder、JwtUtils）
- 包名: `com.ghf.fcg`

## 待完成

### 前端搭建（紧急）
1. 初始化 Vue 3 项目 (`npm create vite@latest fcg-client -- --template vue`)
2. 安装依赖：Element Plus, Axios, Pinia, Vue Router
3. 搭建基础框架：路由、状态管理、Axios 封装
4. 登录页 + 注册页
5. 首页布局

### 后端可优化
- 家庭成员退出逻辑
- 药品库存预警
- 健康报告 AI 生成（对接 LLM）

## 快速启动

```bash
# 后端
cd fcg-server && mvn spring-boot:run

# 前端
cd fcg-client && npm install && npm run dev
```

## 关键文件位置
- 后端入口: `fcg-server/src/main/java/com/ghf/fcg/FcgApplication.java`
- UserController: `fcg-server/src/main/java/com/ghf/fcg/modules/system/controller/UserController.java`
- FamilyController: `fcg-server/src/main/java/com/ghf/fcg/modules/system/controller/FamilyController.java`
- Knife4j 文档: http://localhost:8080/doc.html

## 下一步任务建议
1. **搭建前端基础框架**（高优先级）
2. 实现登录页 + 注册页
3. 实现首页（显示家庭成员、药品计划等）

---

交接完成，祝开发顺利！
