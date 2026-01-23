package com.uni.research.module.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Document Entity
 */
@Data
@TableName("doc_document")
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Document ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Document Title
     */
    private String title;

    /**
     * Original File Name
     */
    private String fileName;

    /**
     * File Path in Storage
     */
    private String filePath;

    /**
     * File Size (bytes)
     */
    private Long fileSize;

    /**
     * File Type (pdf, txt, etc.)
     */
    private String fileType;

    /**
     * Status: 0-Pending, 1-Vectorizing, 2-Completed, 3-Failed
     */
    private Integer status;

    /**
     * Authors (comma-separated)
     */
    private String authors;

    /**
     * Publication Year
     */
    private Integer publicationYear;

    /**
     * Journal Name
     */
    private String journal;

    /**
     * Volume
     */
    private String volume;

    /**
     * Page Range
     */
    private String pages;

    /**
     * DOI
     */
    private String doi;

    /**
     * Publisher
     */
    private String publisher;

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
