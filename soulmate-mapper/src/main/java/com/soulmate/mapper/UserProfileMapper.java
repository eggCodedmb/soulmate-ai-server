package com.soulmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soulmate.domain.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
