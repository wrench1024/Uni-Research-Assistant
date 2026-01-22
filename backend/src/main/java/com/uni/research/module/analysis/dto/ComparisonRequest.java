package com.uni.research.module.analysis.dto;

import lombok.Data;
import java.util.List;

@Data
public class ComparisonRequest {
    private List<String> docIds;
    private List<String> aspects;
}
