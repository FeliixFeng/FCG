# 数据库设计文档

## 1. 概述

- **数据库**: MySQL 8.0
- **字符集**: utf8mb4
- **引擎**: InnoDB
- **逻辑删除**: 全表启用（deleted 字段）

## 2. 表结构设计

### 2.1 系统模块

#### sys_family（家庭表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 家庭ID（主键） |
| family_name | VARCHAR(100) | 家庭名称 |
| invite_code | VARCHAR(20) | 邀请码（唯一，用于家庭成员加入） |
| creator_id | BIGINT | 创建者用户ID |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(100) | 密码（加密） |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| avatar | MEDIUMTEXT | 头像URL（OSS地址） |
| role | TINYINT | 角色：0-管理员 1-普通成员 2-受控成员 |
| family_id | BIGINT | 所属家庭ID |
| care_mode | TINYINT | 关怀模式：0-关闭 1-开启 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.2 药品模块

#### med_medicine（药品表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 药品ID |
| family_id | BIGINT | 所属家庭ID |
| name | VARCHAR(100) | 药品名称 |
| specification | VARCHAR(100) | 规格 |
| image_url | VARCHAR(500) | 封面图片URL |
| stock | INT | 库存数量 |
| stock_unit | VARCHAR(20) | 库存单位 |
| expire_date | DATE | 有效期 |
| usage_notes | TEXT | 用法用量 |
| indication | VARCHAR(500) | 适应症 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

```sql
CREATE TABLE med_medicine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    specification VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    stock INT,
    stock_unit VARCHAR(20),
    expire_date DATE,
    usage_notes TEXT,
    indication VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_family_id (family_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### med_plan（用药计划表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 计划ID |
| user_id | BIGINT | 使用者ID |
| medicine_id | BIGINT | 药品ID |
| dosage | VARCHAR(50) | 单次剂量 |
| remind_slots | VARCHAR(50) | 提醒时段（早,中,晚） |
| start_date | DATE | 开始日期 |
| end_date | DATE | 结束日期（可空=长期） |
| take_days | VARCHAR(20) | 服药星期（1-7，逗号分隔） |
| plan_remark | VARCHAR(255) | 用药注意 |
| status | TINYINT | 状态 0-停用 1-启用 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### med_record（服药记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID |
| plan_id | BIGINT | 计划ID |
| user_id | BIGINT | 用户ID |
| medicine_id | BIGINT | 药品ID |
| scheduled_date | DATE | 应服日期 |
| scheduled_time | TIME | 应服时间 |
| actual_time | DATETIME | 实际打卡时间 |
| status | TINYINT | 状态 0-未服 1-已服 2-跳过 |
| record_remark | VARCHAR(255) | 记录备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.3 健康模块

#### health_vital（体征数据表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID（主键） |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 所属家庭ID |
| type | TINYINT | 类型：1-血压 2-血糖 3-心率 4-体温 5-体重 |
| value_systolic | DECIMAL(5,1) | 收缩压（血压专用） |
| value_diastolic | DECIMAL(5,1) | 舒张压（血压专用） |
| value | DECIMAL(6,2) | 数值（血糖/心率/体温/体重） |
| unit | VARCHAR(20) | 单位 |
| measure_time | DATETIME | 测量时间 |
| measure_point | TINYINT | 测量时点（血糖专用：1-空腹 2-餐后） |
| notes | VARCHAR(255) | 备注 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### health_report（健康周报表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 报表ID（主键） |
| user_id | BIGINT | 用户ID |
| family_id | BIGINT | 所属家庭ID |
| week_start | DATE | 周开始日期 |
| week_end | DATE | 周结束日期 |
| compliance_rate | DECIMAL(5,2) | 服药依从性（%） |
| vital_summary | TEXT | 体征摘要（JSON格式） |
| ai_summary | TEXT | AI健康建议 |
| risk_level | TINYINT | 风险等级：0-低 1-中 2-高 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 3. ER 关系

```
sys_family 1 ──── N sys_user
sys_user   1 ──── N med_plan
sys_user   1 ──── N med_record
sys_user   1 ──── N health_vital
sys_user   1 ──── N health_report

sys_family 1 ──── N med_medicine
med_medicine 1 ──── N med_plan
med_plan   1 ──── N med_record
```

## 4. 执行脚本

```bash
mysql -h HOST -u USER -p DATABASE < fcg-server/src/main/resources/sql/init.sql
```

---

## 3. 药品模块实施规范

### 3.1 OCR识药流程

```
前端                                    后端
───────────                          ───────────
1. 用户拍照/上传多张图                  │
2. 压缩每张图 (1024px, 80%)            │
3. POST /api/medicine/ocr ←───────── MultipartFile[] files
4. 接收识别结果                      │
   {name, specification, expireDate, usageNotes}
5. 第一张图显示为封面预览（本地base64）
6. 用户可点击预览图替换
7. 确认提交 → 上传封面到OSS → 保存数据
```

### 3.2 API接口

| 功能 | 接口 | 方法 | 参数 |
|------|------|------|------|
| OCR识别 | `/api/medicine/ocr` | POST | MultipartFile[] files |
| 药品列表 | `/api/medicine/list` | GET | page, size |
| 新增药品 | `/api/medicine` | POST | {name, specification, imageUrl, stock, stockUnit, expireDate, usageNotes, indication} |
| 更新药品 | `/api/medicine/{id}` | PUT | 同上 |
| 删除药品 | `/api/medicine/{id}` | DELETE | - |
| OSS上传 | `/api/oss/upload?dir=medicine` | POST | MultipartFile file |

### 3.3 前端工具函数

```javascript
// 图片压缩 utils/image.js
import { compressImage, fileToBase64 } from '@/utils/image'

// OCR识别 api.js
import { recognizeMedicine, uploadFile, createMedicine } from '@/utils/api'

// 流程示例
const onOcrSelect = async (files) => {
  // 1. 压缩所有图
  const compressed = await Promise.all(files.map(f => compressImage(f)))
  
  // 2. OCR识别
  const res = await recognizeMedicine(compressed)
  const parsed = res.data.parsed
  
   // 3. 填充表单
   form.name = parsed.name
   form.specification = parsed.specification
   form.expireDate = parsed.expireDate
   form.usageNotes = parsed.usageNotes
   form.indication = parsed.indication
  
  // 4. 第一张图预览
  previewUrl.value = await fileToBase64(compressed[0])
}

const onSubmit = async () => {
  // 1. 上传封面到OSS
  const imageUrl = await uploadFile(previewFile.value, 'medicine')
  
  // 2. 保存药品
  await createMedicine({ ...form, imageUrl })
}
```

### 3.4 添加药品弹窗交互

```
┌─────────────────────────────────────┐
│  📷 智能OCR            [上传/拍照]    │ ← 点击触发多图选择
│  ┌─────────────────────────────┐    │
│  │     封面预览图        │    │ ← 可点击替换
│  │     (本地base64)     │    │
│  └─────────────────────────────┘    │
│                                     │
│  名称: [自动填充/手动输入]          │
│  规格: [自动填充/手动输入]        │
│  适应症: [自动填充/手动输入]       │ ← 新增
│  库存: [手动输入]                │
│  单位: [片/粒/ml] 手动选择      │
│  有效期: [自动填充/手动选择]      │
│  用法用量: [自动填充/手动输入]    │
│                                     │
│  [取消]              [确认添加]  │
└─────────────────────────────────────┘
```
