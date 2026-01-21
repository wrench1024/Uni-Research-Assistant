package com.uni.research.module.doc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uni.research.module.doc.dto.DocumentQueryDto;
import com.uni.research.module.doc.dto.DocumentVo;
import com.uni.research.module.doc.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentService extends IService<Document> {

    /**
     * Upload Document
     *
     * @param file Multipart File
     * @return Document VO
     */
    DocumentVo uploadDocument(MultipartFile file);

    /**
     * List Documents
     *
     * @param queryDto Query Parameters
     * @return Page of Document VOs
     */
    Page<DocumentVo> listDocuments(DocumentQueryDto queryDto);

    /**
     * Download Document
     *
     * @param id Document ID
     * @return File Input Stream
     */
    InputStream downloadDocument(Long id);

    /**
     * Delete Document
     *
     * @param id Document ID
     */
    void deleteDocument(Long id);
}
