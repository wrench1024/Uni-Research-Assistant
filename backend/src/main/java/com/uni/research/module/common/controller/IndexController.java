package com.uni.research.module.common.controller;

import com.uni.research.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 * 
 * @author wrench1024
 * @since 2026-01-03
 */
@Tag(name = "系统信息", description = "系统基础信息接口")
@RestController
@RequestMapping("/")
public class IndexController {

    @Operation(summary = "系统欢迎页")
    @GetMapping
    public Result<Map<String, Object>> index() {
        Map<String, Object> data = new HashMap<>();
        data.put("project", "Uni-Research-Assistant");
        data.put("version", "1.0.0");
        data.put("description", "基于大模型的文章编研系统");
        data.put("apiDoc", "http://localhost:8080/api/doc.html");
        data.put("author", "wrench1024");
        return Result.success(data);
    }
}
