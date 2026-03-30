package com.indolyn.rill.app.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indolyn.rill.app.persistence.entity.AppJwtSessionEntity;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppJwtSessionMapper extends BaseMapper<AppJwtSessionEntity> {
}
