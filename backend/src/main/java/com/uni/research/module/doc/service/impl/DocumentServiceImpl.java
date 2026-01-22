package com.uni.research.module.doc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uni.research.common.exception.BizException;
import com.uni.research.common.service.MinioService;
import com.uni.research.common.service.RagService;
import com.uni.research.module.auth.entity.User;
import com.uni.research.module.auth.mapper.UserMapper;
import com.uni.research.module.doc.dto.DocumentQueryDto;
import com.uni.research.module.doc.dto.DocumentVo;
import com.uni.research.module.doc.entity.Document;
import com.uni.research.module.doc.mapper.DocumentMapper;
import com.uni.research.module.doc.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final MinioService minioService;
    private final UserMapper userMapper;
    private final RagService ragService;

    // 支持 RAG 索引的文件类型
    private static final Set<String> RAG_SUPPORTED_TYPES = Set.of("pdf", "md", "txt", "text", "doc", "docx");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVo uploadDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException("Cannot upload empty file");
        }

        // 1. Get Current User
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BizException("User not found");
        }

        // 2. Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.extName(originalFilename);
        String uuid = IdUtil.simpleUUID();
        String objectName = uuid + "." + suffix;

        // 3. Upload to MinIO
        minioService.uploadFile(file, objectName);

        // 4. Save Metadata
        Document doc = new Document();
        doc.setUserId(currentUser.getId());
        doc.setTitle(originalFilename); // Default title as filename
        doc.setFileName(originalFilename);
        doc.setFilePath(objectName);
        doc.setFileSize(file.getSize());
        doc.setFileType(suffix);
        doc.setStatus(0); // 0-Pending indexing
        doc.setDeleted(0);

        this.save(doc);

        // 5. 触发异步 RAG 索引（仅支持的文件类型）
        if (suffix != null && RAG_SUPPORTED_TYPES.contains(suffix.toLowerCase())) {
            triggerRagIndexing(file, doc.getId().toString(), originalFilename);
        }

        return BeanUtil.copyProperties(doc, DocumentVo.class);
    }

    /**
     * 异步触发 RAG 索引
     * 将上传的文件保存到临时目录，然后调用 Python 服务进行向量化
     */
    private void triggerRagIndexing(MultipartFile file, String docId, String fileName) {
        try {
            // 保存到临时文件
            String suffix = FileUtil.extName(fileName);
            Path tempFile = Files.createTempFile("rag_", "." + suffix);
            file.transferTo(tempFile.toFile());

            // 异步调用 RAG 服务
            ragService.triggerDocumentIngestion(tempFile.toAbsolutePath().toString(), docId);

            log.info("已触发 RAG 索引: docId={}, tempFile={}", docId, tempFile);
        } catch (Exception e) {
            log.error("触发 RAG 索引失败: docId={}, error={}", docId, e.getMessage(), e);
            // 不影响主流程，只记录日志
        }
    }

    @Override
    public Page<DocumentVo> listDocuments(DocumentQueryDto queryDto) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new BizException("User not found");
        }

        Page<Document> page = new Page<>(queryDto.getPage(), queryDto.getSize());
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getUserId, currentUser.getId())
                .eq(Document::getDeleted, 0)
                .like(StringUtils.hasText(queryDto.getKeyword()), Document::getTitle, queryDto.getKeyword())
                .orderByDesc(Document::getCreateTime);

        Page<Document> result = this.page(page, wrapper);

        Page<DocumentVo> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<DocumentVo> voList = result.getRecords().stream()
                .map(doc -> BeanUtil.copyProperties(doc, DocumentVo.class))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public InputStream downloadDocument(Long id) {
        Document doc = this.getById(id);
        if (doc == null || !doc.getUserId().equals(getCurrentUser().getId())) {
            throw new BizException("Document not found or access denied");
        }
        return minioService.getFile(doc.getFilePath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        Document doc = this.getById(id);
        if (doc == null || !doc.getUserId().equals(getCurrentUser().getId())) {
            throw new BizException("Document not found or access denied");
        }

        // 1. Delete from MinIO
        minioService.removeFile(doc.getFilePath());

        // 2. Delete from DB
        this.removeById(id);

        // 3. 异步删除向量数据库中的向量
        ragService.deleteDocumentVectors(id.toString());
        log.info("已触发向量删除: docId={}", id);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BizException("User not authenticated");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
