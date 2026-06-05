package com.soulmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soulmate.domain.entity.EmotionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmotionRecordMapper extends BaseMapper<EmotionRecord> {
}
