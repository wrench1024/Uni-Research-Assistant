package com.uni.research.module.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Note Entity
 */
@Data
@TableName("note")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Note ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Document ID
     */
    private Long docId;

    /**
     * Note Content
     */
    private String content;

    /**
     * Note Type: DOCUMENT (document-level) or PARAGRAPH (paragraph annotation)
     */
    private String noteType;

    /**
     * Position Information (JSON format for paragraph notes)
     */
    private String position;

    /**
     * Tags (comma-separated)
     */
    private String tags;

    /**
     * Logic Delete
     */
    @TableLogic
    private Integer deleted;

    /**
     * Create Time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * Update Time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
