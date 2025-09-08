package com.liteware.service.file;

import com.liteware.model.entity.file.FileAttachment;
import com.liteware.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final FileAttachmentRepository fileAttachmentRepository;
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Value("${file.max-size:10485760}") // 10MB
    private long maxFileSize;
    
    @Value("${file.allowed-extensions:jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip}")
    private String allowedExtensions;
    
    /**
     * 파일 업로드
     */
    @Transactional
    public FileAttachment uploadFile(MultipartFile file, String entityType, Long entityId, Long uploaderId) {
        validateFile(file);
        
        try {
            // 파일명 생성
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = generateStoredFileName(fileExtension);
            
            // 저장 경로 생성
            Path uploadPath = createUploadPath(entityType, entityId);
            Path targetLocation = uploadPath.resolve(storedFileName);
            
            // 파일 저장
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // DB 저장
            FileAttachment attachment = FileAttachment.builder()
                    .fileName(originalFileName)
                    .storedName(storedFileName)
                    .filePath(targetLocation.toString())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .entityType(entityType)
                    .entityId(entityId)
                    .uploaderId(uploaderId)
                    .downloadCount(0L)
                    .build();
            
            return fileAttachmentRepository.save(attachment);
            
        } catch (IOException ex) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), ex);
            throw new RuntimeException("Failed to store file", ex);
        }
    }
    
    /**
     * 여러 파일 업로드
     */
    @Transactional
    public List<FileAttachment> uploadFiles(List<MultipartFile> files, String entityType, Long entityId, Long uploaderId) {
        List<FileAttachment> attachments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                attachments.add(uploadFile(file, entityType, entityId, uploaderId));
            }
        }
        
        return attachments;
    }
    
    /**
     * 파일 다운로드
     */
    @Transactional
    public Resource downloadFile(Long fileId) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // 다운로드 횟수 증가
                attachment.setDownloadCount(attachment.getDownloadCount() + 1);
                fileAttachmentRepository.save(attachment);
                
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + fileId);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + fileId, ex);
        }
    }
    
    /**
     * 파일 삭제
     */
    @Transactional
    public void deleteFile(Long fileId) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        
        try {
            // 실제 파일 삭제
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
            
            // DB에서 삭제
            fileAttachmentRepository.delete(attachment);
            
            log.info("File deleted: {}", attachment.getFileName());
        } catch (IOException ex) {
            log.error("Failed to delete file: {}", attachment.getFileName(), ex);
            throw new RuntimeException("Failed to delete file", ex);
        }
    }
    
    /**
     * 엔티티의 모든 첨부파일 조회
     */
    public List<FileAttachment> getFilesByEntity(String entityType, Long entityId) {
        return fileAttachmentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
    
    /**
     * 엔티티의 모든 첨부파일 삭제
     */
    @Transactional
    public void deleteFilesByEntity(String entityType, Long entityId) {
        List<FileAttachment> attachments = getFilesByEntity(entityType, entityId);
        
        for (FileAttachment attachment : attachments) {
            deleteFile(attachment.getFileId());
        }
    }
    
    /**
     * 파일 정보 조회
     */
    public FileAttachment getFile(Long fileId) {
        return fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }
    
    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // 파일 크기 검사
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit: " + maxFileSize);
        }
        
        // 파일 확장자 검사
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            // Path traversal 공격 방지
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("Invalid file name: " + fileName);
            }
            
            String extension = getFileExtension(fileName);
            
            // 실행 파일 차단 (먼저 체크)
            if (isExecutableFile(extension)) {
                throw new IllegalArgumentException("Executable files are not allowed");
            }
            
            // 확장자가 있는 경우만 검사 (확장자 없는 파일은 허용)
            if (!extension.isEmpty() && !isAllowedExtension(extension)) {
                throw new IllegalArgumentException("File extension not allowed: " + extension);
            }
        }
    }
    
    /**
     * 실행 파일 확인
     */
    private boolean isExecutableFile(String extension) {
        String[] executableExtensions = {"exe", "sh", "bat", "cmd", "com", "msi", "app", "deb", "rpm"};
        for (String ext : executableExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 허용된 확장자 확인
     */
    private boolean isAllowedExtension(String extension) {
        String[] allowed = allowedExtensions.split(",");
        for (String ext : allowed) {
            if (ext.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 저장 파일명 생성
     */
    private String generateStoredFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + (extension.isEmpty() ? "" : "." + extension);
    }
    
    /**
     * 업로드 경로 생성
     */
    private Path createUploadPath(String entityType, Long entityId) throws IOException {
        // 엔티티별 디렉토리 구조: uploads/entityType/entityId/yyyy/MM/
        String subPath = String.format("%s/%d/%s",
                entityType,
                entityId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM")));
        
        Path uploadPath = Paths.get(uploadDir).resolve(subPath);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        return uploadPath;
    }
    
    /**
     * 파일 크기를 읽기 쉬운 형식으로 변환
     */
    public String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }
}