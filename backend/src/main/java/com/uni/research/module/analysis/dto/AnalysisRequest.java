package com.uni.research.module.analysis.dto;

import lombok.Data;

@Data
public class AnalysisRequest {
    private String docId;
    private String type; // "summary" or other types
}
