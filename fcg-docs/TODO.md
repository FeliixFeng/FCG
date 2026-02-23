# FCG 待办清单 (TODO)

> 从 PROJECT_CONTEXT.md 恢复

## 里程碑 0：现状对齐（后端）
- [x] 数据库表结构设计（7 张核心表）
- [x] 统一返回结果封装（Result）
- [x] 全局异常处理
- [x] 实体/Mapper/Service（system/medicine/health）
- [x] 登录/注册基础接口
- [x] JWT/BCrypt 工具类
- [x] Swagger/Knife4j 基础配置

## 里程碑 1：认证闭环
- [x] JWT 认证拦截器（解析 token、设置当前用户上下文）
- [x] 统一处理未登录/无权限异常
- [x] CORS 配置
- [x] 登录/注册接口单测

## 里程碑 2：用户与家庭模块完善
- [x] UserController `/info`：读取当前用户信息
- [x] UserController `/update`：更新用户资料（昵称/头像/手机号等）
- [x] UserController `/care-mode`：切换关怀模式
- [x] 用户修改密码接口
- [x] Family Controller：创建家庭、加入家庭（邀请码）
- [x] Family DTO/VO：请求与返回模型
- [x] 用户与家庭关系逻辑校验（加入、退出、切换）

## 里程碑 3：药品模块基础 API
- [x] Medicine Controller：增删改查
- [x] Medicine DTO/VO
- [x] MedicinePlan Controller：增删改查
- [x] MedicinePlan DTO/VO
- [x] MedicineRecord Controller：增删改查
- [x] MedicineRecord DTO/VO
- [x] 简单联表查询：计划 + 记录

## 里程碑 4：健康模块基础 API
- [x] Vital Controller：增删改查
- [x] Vital DTO/VO
- [x] HealthReport Controller：增删改查
- [x] HealthReport DTO/VO
- [x] 近一周体征查询接口

## 里程碑 5：系统完善与稳定性
- [x] 参数校验覆盖（所有 DTO）
- [x] 统一异常码与消息常量化
- [x] 接口鉴权覆盖（除登录/注册外）
- [x] 数据库索引与慢查询检查
- [x] 最小集成测试（登录 -> 获取用户信息）
- [x] 文档与配置核对（启动说明、环境变量）
