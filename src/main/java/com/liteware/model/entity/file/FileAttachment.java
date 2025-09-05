package com.liteware.model.entity.file;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;
    
    @Column(nullable = false)
    private String fileName;        // 원본 파일명
    
    @Column(nullable = false)
    private String storedName;      // 저장된 파일명
    
    @Column(nullable = false)
    private String filePath;        // 파일 경로
    
    @Column(nullable = false)
    private Long fileSize;          // 파일 크기 (bytes)
    
    private String fileType;        // MIME 타입
    
    @Column(nullable = false)
    private String entityType;      // 연결된 엔티티 타입 (board, approval, etc.)
    
    @Column(nullable = false)
    private Long entityId;          // 연결된 엔티티 ID
    
    private Long uploaderId;        // 업로드한 사용자 ID
    
    @Builder.Default
    private Long downloadCount = 0L; // 다운로드 횟수
    
    @CreationTimestamp
    private LocalDateTime uploadedAt;
}