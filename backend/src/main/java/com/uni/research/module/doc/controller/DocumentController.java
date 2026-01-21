package com.uni.research.module.doc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uni.research.common.result.Result;
import com.uni.research.module.doc.dto.DocumentQueryDto;
import com.uni.research.module.doc.dto.DocumentVo;
import com.uni.research.module.doc.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
@Tag(name = "Document Management")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload Document")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<DocumentVo> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(documentService.uploadDocument(file));
    }

    @Operation(summary = "List Documents")
    @GetMapping("/list")
    public Result<Page<DocumentVo>> list(DocumentQueryDto queryDto) {
        return Result.success(documentService.listDocuments(queryDto));
    }

    @Operation(summary = "Download Document")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        // 1. Check permission and get stream (Service throws exception if access
        // denied)
        try (InputStream inputStream = documentService.downloadDocument(id)) {

            // 2. Fetch metadata for filename (Access is already verified by step 1)
            com.uni.research.module.doc.entity.Document doc = documentService.getById(id);
            String filename = doc != null ? doc.getFileName() : "file_" + id;
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replaceAll("\\+",
                    "%20");

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error("Download failed", e);
            // If response is not committed, we can set status
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Or 403/404 based on exception type
                                                                                  // if we handled it
            }
        }
    }

    @Operation(summary = "Delete Document")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success(true);
    }
}
