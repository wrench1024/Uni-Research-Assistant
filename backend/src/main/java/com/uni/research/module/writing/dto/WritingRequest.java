package com.uni.research.module.writing.dto;

import lombok.Data;

@Data
public class WritingRequest {
    private String text;
    private String instruction; // polish, expand, continue, fix_grammar
    private String context;
}
