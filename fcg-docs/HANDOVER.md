# FCG 项目交接文档

## 项目概况
- **项目名称**: Family Care Guardian（家庭健康管理系统）
- **技术栈**: Spring Boot 3 + Vue 3 + MySQL
- **后端端口**: 8080
- **前端端口**: 5173
- **数据库**: MySQL 8.0

---

## 认证体系（重要）

采用**家庭账号体系**，非传统用户独立登录：

1. 一个家庭共用一个账号（username/password 存在 `sys_family` 表）
2. 家庭成员无独立登录账号，通过"选人"切换身份
3. JWT 双 token：
   - **家庭级 token**（`tokenType=family`）：登录后获得，含 familyId，用于选人页
   - **成员级 token**（`tokenType=member`）：选人后获得，含 familyId+memberId+role，用于业务页
4. 管理员进入管理界面需二次验证家庭密码

### 登录流程
```
POST /api/family/login → 家庭级 token
GET  /api/family/members → 成员列表
POST /api/family/switch-member/{memberId} → 成员级 token
```

---

## 已完成功能

### 后端 API
- ✅ 家庭模块：注册、登录、获取成员列表、切换成员、验证管理员、添加成员
- ✅ 用户模块：获取信息、更新信息、切换关怀模式
- ✅ 药品模块：Medicine/MedicinePlan/MedicineRecord 增删改查
- ✅ 健康模块：Vital/HealthReport 增删改查、近一周体征查询
- ✅ JWT 双 token 认证拦截器
- ✅ 全局异常处理
- ✅ 药品图片智能识别接口（智谱AI GLM-4.6V-FlashX 多模态模型一步完成）
- ✅ OSS 上传接口（图片上传）

### 前端页面
- ✅ 登录页（/login）
- ✅ 注册页（/register）
- ✅ 选人页（/select-member）—— 需家庭级 token
- ✅ 首页（/home）—— 需成员级 token
- ✅ 管理员页（/admin）—— 需成员级 token + 管理员角色
- ✅ 家庭页（/family）骨架
- ✅ 药品页（/medicine）骨架
- ✅ 健康页（/health）骨架

---

## 数据库

7 张核心表：`sys_family`, `sys_user`, `med_medicine`, `med_plan`, `med_record`, `health_vital`, `health_report`

初始化脚本：`fcg-server/src/main/resources/sql/init.sql`

### 药品表设计（2025-04）
- 字段：name, specification, image_url, stock, stock_unit, expire_date, usage_notes
- OCR 提取：name, specification, expire_date, usage_notes

### 计划表设计（2025-04）
- 字段：user_id, medicine_id, dosage, remind_slots, start_date, end_date, take_days, plan_remark, status
- remind_slots 时段化（如 "早,中,晚"）

### 记录表设计（2025-04）
- 字段：plan_id, user_id, medicine_id, scheduled_date, slot_name, actual_time, status, record_remark
- slot_name 时段化（如 "早", "中", "晚", "睡前"）

测试账号：
- 家庭账号：`zhangfamily` / `test123`
- 家庭账号：`lifamily` / `test123`

---

## 代码规范
- 4 空格缩进
- 使用 `@Builder` 而非 `@Accessors`
- 异常用 `BusinessException`
- 响应用 `Result.success()` / `Result.error()`
- 静态方法工具类（PasswordEncoder、JwtUtils）
- 包名: `com.ghf.fcg`

---

## 快速启动

```bash
# 后端
cd fcg-server && mvn spring-boot:run

# 前端
cd fcg-client && npm install && npm run dev
```

Knife4j 接口文档：http://localhost:8080/doc.html

---

## 关键文件位置

| 文件 | 路径 |
|------|------|
| 后端入口 | `fcg-server/src/main/java/com/ghf/fcg/FcgApplication.java` |
| JWT 工具 | `fcg-server/.../common/utils/JwtUtils.java` |
| 认证拦截器 | `fcg-server/.../common/interceptor/JwtAuthInterceptor.java` |
| FamilyController | `fcg-server/.../modules/system/controller/FamilyController.java` |
| 前端 API 层 | `fcg-client/src/utils/api.js` |
| 前端路由 | `fcg-client/src/router/index.js` |
| 前端状态 | `fcg-client/src/stores/user.js` |

---

## 待完成（前端功能实现）
1. 药品页：药品列表、添加药品（含 OCR）、用药计划管理
2. 健康页：体征录入、趋势图表（推荐 ECharts）
3. 家庭页：成员管理、关怀模式切换
4. 关怀模式：大字体/简化 UI 适配
5. 健康周报：AI 摘要展示
