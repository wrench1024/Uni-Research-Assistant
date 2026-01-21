package com.uni.research.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uni.research.module.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * 
 * 对应 408 考点：数据库 - SQL 查询与 ORM 映射
 * 
 * @author wrench1024
 * @since 2026-01-02
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
