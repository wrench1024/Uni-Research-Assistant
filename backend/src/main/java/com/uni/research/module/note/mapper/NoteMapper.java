package com.uni.research.module.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uni.research.module.note.entity.Note;
import org.apache.ibatis.annotations.Mapper;

/**
 * Note Mapper
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
