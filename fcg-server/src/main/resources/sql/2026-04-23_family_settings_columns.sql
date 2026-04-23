-- 为 sys_family 增加系统设置字段（可重复执行）
ALTER TABLE `sys_family`
  ADD COLUMN IF NOT EXISTS `low_stock_threshold` int NOT NULL DEFAULT 5 COMMENT '低库存阈值（小于该值视为紧张）' AFTER `creator_id`,
  ADD COLUMN IF NOT EXISTS `expiring_days` int NOT NULL DEFAULT 30 COMMENT '临期提醒天数（距离过期<=该值）' AFTER `low_stock_threshold`;

