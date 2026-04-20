-- 2026-04-20
-- 为 med_record 增加去重唯一索引，防止同一任务重复打卡记录

ALTER TABLE `med_record`
ADD UNIQUE KEY `uk_user_plan_date_slot` (`user_id`, `plan_id`, `scheduled_date`, `slot_name`);
