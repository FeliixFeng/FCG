package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.system.entity.Family;
import com.ghf.fcg.modules.system.mapper.FamilyMapper;
import com.ghf.fcg.modules.system.service.IFamilyService;
import org.springframework.stereotype.Service;

@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {
}
