package com.soulmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soulmate.domain.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
