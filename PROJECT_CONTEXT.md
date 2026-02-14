# FCG 项目上下文

> 保存项目关键信息，方便后续恢复上下文

## 📋 项目基本信息

- **项目名称**: Family Care Guardian (FCG)
- **课题**: 基于Spring Boot的家庭健康管理系统设计与实现
- **学生**: 管海峰 (2204240115)
- **指导教师**: 王帮超 (副教授)
- **学校**: 武汉纺织大学 计算机与人工智能学院
- **时间**: 2025年12月20日 - 2026年5月20日

## 🔗 项目地址

- **GitHub**: https://github.com/FeliixFeng/FCG (私有仓库)
- **本地路径**: `/Users/haifeng/coding/IdeaProjects/fcg/`

## 🏗️ 项目结构

```
fcg/
├── README.md                     # 项目说明
├── .gitignore                    # Git忽略配置
├── fcg-server/                   # 后端 (Spring Boot)
│   ├── pom.xml                   # Maven配置
│   ├── .gitignore                # 后端Git忽略
│   └── src/main/
│       ├── java/com/ghf/fcg/     # Java源码
│       │   └── FcgApplication.java
│       └── resources/
│           ├── application.yml              # 公共配置(提交)
│           ├── application-local.yml        # 本地配置(忽略)
│           └── application-local.yml.example # 配置模板
└── fcg-client/                   # 前端 (Vue 3)
    ├── package.json              # npm配置
    ├── index.html
    ├── .gitignore
    ├── README.md
    └── src/
        ├── App.vue
        └── main.js
```

## 🛠️ 技术栈

### 后端
- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.6
- Maven
- MySQL 8.0

### 前端
- Vue 3.4.21
- Vite 5.2.0
- npm

### 第三方服务
- 百度 OCR SDK (药盒识别)
- 通义千问 API (AI用药咨询)

## 🗄️ 数据库信息

- **Host**: 47.121.181.198:3306
- **Database**: `fcg_db` (已创建，空库)
- **Username**: root
- **Password**: `d!MLe?R87+`
- **字符集**: utf8mb4_unicode_ci

## 🚀 启动方式

### 后端
```bash
cd fcg-server
# 使用 IDEA 打开，运行 FcgApplication.java
# 端口: 8080
```

### 前端
```bash
cd fcg-client
npm install
npm run dev
```

## 📝 待办清单

### 高优先级（基础架构）
- [ ] 数据库表结构设计（用户、家庭、药品、用药计划、体征等）
- [ ] 后端统一返回结果封装（R/Result）
- [ ] 后端全局异常处理
- [ ] JWT认证拦截器
- [ ] 跨域配置（CORS）
- [ ] 前端路由配置（admin/family区分）
- [ ] Axios封装（统一拦截、错误处理）
- [ ] Pinia状态管理
- [ ] 用户认证模块（注册/登录接口 + 登录页）

### 中优先级（开发效率）
- [ ] MyBatis-Plus Generator 代码生成器
- [ ] 管理员后台基础布局（Element Plus）
- [ ] 家庭端"标准/关怀"双模式框架（Tailwind CSS）

### 功能模块
- [ ] 平台运营管理模块（药品字典、监控看板）
- [ ] 家庭与档案管理模块（1+N家庭组、关怀模式）
- [ ] 智慧药箱与用药辅助模块（OCR、库存预警、LLM配伍禁忌）
- [ ] 智能计划与执行模块（服药规则、多端打卡）
- [ ] 健康体征监测模块（血压/血糖/心率、ECharts可视化、AI周报）

## 📚 参考文档

- **任务书**: `/Users/haifeng/Documents/毕业论文/报告/毕业设计（论文）任务书_管海峰.docx`
- **开题报告**: `/Users/haifeng/Documents/毕业论文/报告/毕业设计(论文)开题报告_2204240115_管海峰.docx`

## 🔐 安全注意事项

- `application-local.yml` 已添加到 `.gitignore`，包含数据库密码，不会推送到GitHub
- 生产环境需修改数据库密码和JWT密钥

---

**最后更新**: 2026年2月14日
