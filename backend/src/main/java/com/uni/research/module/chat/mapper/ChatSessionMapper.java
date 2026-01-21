package com.uni.research.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uni.research.module.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
