package com.liteware.repository;

import com.liteware.model.entity.file.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    
    List<FileAttachment> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
    
    List<FileAttachment> findByUploaderId(Long uploaderId);
}