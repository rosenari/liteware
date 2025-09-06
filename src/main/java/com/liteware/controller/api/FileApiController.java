package com.liteware.controller.api;

import com.liteware.model.entity.file.FileAttachment;
import com.liteware.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileApiController {
    
    private final FileService fileService;
    
    /**
     * 단일 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 사용자 ID 추출 (실제 구현에서는 UserDetails에서 추출)
            Long uploaderId = extractUserId(userDetails);
            
            FileAttachment attachment = fileService.uploadFile(file, entityType, entityId, uploaderId);
            
            response.put("success", true);
            response.put("fileId", attachment.getFileId());
            response.put("fileName", attachment.getFileName());
            response.put("fileSize", fileService.formatFileSize(attachment.getFileSize()));
            response.put("message", "파일이 성공적으로 업로드되었습니다.");
            
            log.info("File uploaded successfully: {} by user {}", attachment.getFileName(), uploaderId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("File upload failed", e);
            response.put("success", false);
            response.put("message", "파일 업로드 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 다중 파일 업로드
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long uploaderId = extractUserId(userDetails);
            
            List<FileAttachment> attachments = fileService.uploadFiles(files, entityType, entityId, uploaderId);
            
            response.put("success", true);
            response.put("count", attachments.size());
            response.put("files", attachments.stream().map(a -> Map.of(
                "fileId", a.getFileId(),
                "fileName", a.getFileName(),
                "fileSize", fileService.formatFileSize(a.getFileSize())
            )).toList());
            response.put("message", attachments.size() + "개의 파일이 업로드되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Multiple file upload failed", e);
            response.put("success", false);
            response.put("message", "파일 업로드 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 파일 다운로드
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        
        try {
            Resource resource = fileService.downloadFile(fileId);
            FileAttachment attachment = fileService.getFile(fileId);
            
            // Content-Type 결정
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                log.debug("Could not determine file type.");
            }
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 파일명 인코딩 (한글 파일명 처리)
            String encodedFileName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("File download failed for fileId: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 파일 미리보기 (이미지, PDF 등)
     */
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        
        try {
            Resource resource = fileService.downloadFile(fileId);
            FileAttachment attachment = fileService.getFile(fileId);
            
            // 미리보기 가능한 파일 타입 확인
            String fileType = attachment.getFileType();
            if (fileType == null || (!fileType.startsWith("image/") && !fileType.equals("application/pdf"))) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("File preview failed for fileId: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 파일 삭제
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 권한 확인 (파일 업로더 또는 관리자만 삭제 가능)
            FileAttachment attachment = fileService.getFile(fileId);
            Long userId = extractUserId(userDetails);
            
            if (!attachment.getUploaderId().equals(userId) && !hasAdminRole(userDetails)) {
                response.put("success", false);
                response.put("message", "파일 삭제 권한이 없습니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            fileService.deleteFile(fileId);
            
            response.put("success", true);
            response.put("message", "파일이 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("File deletion failed for fileId: {}", fileId, e);
            response.put("success", false);
            response.put("message", "파일 삭제 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 엔티티의 파일 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileAttachment>> getFilesByEntity(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId) {
        
        try {
            List<FileAttachment> files = fileService.getFilesByEntity(entityType, entityId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Failed to get files for entity: {} {}", entityType, entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 파일 정보 조회
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileAttachment> getFile(@PathVariable Long fileId) {
        try {
            FileAttachment file = fileService.getFile(fileId);
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            log.error("File not found: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 ID 추출 (실제 구현에서는 UserDetails 구현체에 따라 수정 필요)
     */
    private Long extractUserId(UserDetails userDetails) {
        // CustomUserDetails에서 userId를 추출하는 로직
        // 예: return ((CustomUserDetails) userDetails).getUserId();
        return 1L; // 임시 반환값
    }
    
    /**
     * 관리자 권한 확인
     */
    private boolean hasAdminRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}