package com.ghf.fcg.modules.medicine.service;

import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.modules.medicine.vo.MedicinePlanRecordVO;

import java.time.LocalDate;

public interface IMedicinePlanTaskService {

    PageResult<MedicinePlanRecordVO> buildDailyTasks(Long targetUserId, LocalDate scheduledDate, Integer status, PageQuery query);
}
