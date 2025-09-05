package com.liteware.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    
    private Long attachmentId;
    private Long docId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    
    public static AttachmentDto from(com.liteware.model.entity.approval.ApprovalAttachment attachment) {
        return AttachmentDto.builder()
                .attachmentId(attachment.getAttachmentId())
                .docId(attachment.getDocument().getDocId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .filePath(attachment.getFilePath())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .uploadedById(attachment.getUploadedBy().getUserId())
                .uploadedByName(attachment.getUploadedBy().getName())
                .uploadedAt(attachment.getCreatedAt())
                .build();
    }
}