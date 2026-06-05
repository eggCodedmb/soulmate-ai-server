package com.soulmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soulmate.domain.entity.Memory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryMapper extends BaseMapper<Memory> {
}
