# FCG 待办清单 (TODO)

## 里程碑 0：现状对齐（后端）
- [x] 数据库表结构设计（7 张核心表）
- [x] 统一返回结果封装（Result）
- [x] 全局异常处理
- [x] 实体/Mapper/Service（system/medicine/health）
- [x] JWT/BCrypt 工具类
- [x] Swagger/Knife4j 基础配置

## 里程碑 1：认证闭环
- [x] JWT 认证拦截器（解析 token、设置当前用户上下文）
- [x] 统一处理未登录/无权限异常
- [x] CORS 配置

## 里程碑 2：家庭账号体系
- [x] 改造为家庭账号登录（sys_family 存 username/password）
- [x] 成员无独立登录账号，通过"选人"切换身份
- [x] JWT 双 token：家庭级（familyId）+ 成员级（familyId+memberId+role）
- [x] 管理员二次验证（用家庭密码）
- [x] FamilyController：register、login、members、switch-member、verify-admin、add-member
- [x] UserController：info、update、care-mode

## 里程碑 3：药品模块基础 API
- [x] Medicine Controller：增删改查
- [x] MedicinePlan Controller：增删改查
- [x] MedicineRecord Controller：增删改查
- [x] 简单联表查询：计划 + 记录
- [x] OCR 识别接口（药品图片识别，已升级为智谱AI GLM-4.6V-FlashX 多模态模型）
- [x] OSS 上传接口
- [x] 药品表字段优化：删除冗余字段，合并 usage_notes
- [x] 计划表优化：remind_slots 时段化，去 family_id
- [x] 记录表优化：slot_name 时段化，去 family_id

## 里程碑 4：健康模块基础 API
- [x] Vital Controller：增删改查
- [x] HealthReport Controller：增删改查
- [x] 近一周体征查询接口

## 里程碑 5：前端基础框架
- [x] Vue 3 + Vite 项目初始化
- [x] Vue Router（含路由守卫，区分家庭级/成员级 token）
- [x] Pinia 状态管理（user store）
- [x] Axios 封装 + API 层
- [x] 登录页 / 注册页 / 选人页
- [x] 首页（Home）/ 管理员页（AdminHome）骨架
- [x] 家庭页 / 药品页 / 健康页 骨架
- [x] Landing 落地页（Hero + 功能卡片 + Steps + CTA + Footer）
- [x] AuthDialog 登录/注册弹窗（两栏布局）

## 下一步（前端功能实现）

### 用户界面
- [ ] 首页（/dashboard）：待设计
- [ ] 药品页（/medicine）
  - 药品列表展示（家庭药箱）
  - 添加药品（OCR拍照识别，多图上传）
  - 用药计划管理（创建/修改计划）
- 核心设计：remind_slots 时段化（早/中/晚），slot_name 记录时段
- [ ] 健康页（/health）
  - 体征录入（血压、血糖等）
  - 近一周趋势图表（ECharts）
- [ ] 家庭页（/family）
  - 成员列表
  - 添加成员
  - 关怀模式切换
- [ ] 我的（/profile）
  - 个人信息展示
  - 进入管理界面入口（仅管理员）
  - **AI用药助手**（问答入口）

### 管理界面（仅管理员）
- [x] 成员管理（/admin/members）
  - [x] 成员列表展示
  - [x] 添加成员
  - [x] 查看成员详情（扩展信息）
  - [x] 编辑成员信息（含扩展字段 + 头像）
  - [x] 删除成员
  - [x] 修改成员角色
  - [x] 头像选择器（内置头像 + OSS上传）
- [ ] 药品管理（/admin/medicines）
- [ ] 系统设置（/admin/system）
- [ ] 数据统计（/admin/data）

### AI功能集成
- **OCR拍照识别**：支持多图上传，第一张做封面，其余参与识别
- **AI用药助手**：入口在"我的"页面，作为健康问答入口

### 后续功能
- [ ] 关怀模式：大字体/简化 UI 适配
- [ ] 健康周报：AI 生成摘要展示
