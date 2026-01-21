package com.uni.research.module.doc.dto;

import lombok.Data;

@Data
public class DocumentQueryDto {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
}
