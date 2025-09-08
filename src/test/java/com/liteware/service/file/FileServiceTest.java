package com.liteware.service.file;

import com.liteware.model.entity.file.FileAttachment;
import com.liteware.repository.FileAttachmentRepository;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest extends BaseServiceTest {
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Set upload directory to temp directory for testing
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "maxFileSize", 10485760L); // 10MB
        ReflectionTestUtils.setField(fileService, "allowedExtensions", "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip");
    }
    
    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test file content".getBytes()
        );
        
        // when
        FileAttachment attachment = fileService.uploadFile(file, "board", 1L, testUser.getUserId());
        
        // then
        assertThat(attachment).isNotNull();
        assertThat(attachment.getFileName()).isEqualTo("test.pdf");
        assertThat(attachment.getFileType()).isEqualTo("application/pdf");
        assertThat(attachment.getFileSize()).isEqualTo(file.getSize());
        assertThat(attachment.getEntityType()).isEqualTo("board");
        assertThat(attachment.getEntityId()).isEqualTo(1L);
        assertThat(attachment.getUploaderId()).isEqualTo(testUser.getUserId());
        assertThat(attachment.getDownloadCount()).isEqualTo(0L);
        
        // 실제 파일이 저장되었는지 확인
        Path savedFile = Path.of(attachment.getFilePath());
        assertThat(Files.exists(savedFile)).isTrue();
    }
    
    @Test
    @DisplayName("빈 파일 업로드 시 예외 발생")
    void uploadFile_EmptyFile_ThrowsException() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );
        
        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(emptyFile, "board", 1L, testUser.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }
    
    @Test
    @DisplayName("파일 크기 초과 시 예외 발생")
    void uploadFile_ExceedsMaxSize_ThrowsException() {
        // given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );
        
        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(largeFile, "board", 1L, testUser.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum limit");
    }
    
    @Test
    @DisplayName("허용되지 않은 확장자 파일 업로드 시 예외 발생")
    void uploadFile_InvalidExtension_ThrowsException() {
        // given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "script.js",
                "application/javascript",
                "console.log('test');".getBytes()
        );
        
        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(invalidFile, "board", 1L, testUser.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File extension not allowed");
    }
    
    @Test
    @DisplayName("실행 파일 업로드 시 예외 발생")
    void uploadFile_ExecutableFile_ThrowsException() {
        // given
        MockMultipartFile execFile = new MockMultipartFile(
                "file",
                "program.exe",
                "application/octet-stream",
                "binary content".getBytes()
        );
        
        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(execFile, "board", 1L, testUser.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Executable files are not allowed");
    }
    
    @Test
    @DisplayName("Path traversal 공격 방지")
    void uploadFile_PathTraversal_ThrowsException() {
        // given
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "text/plain",
                "malicious content".getBytes()
        );
        
        // when & then
        assertThatThrownBy(() -> fileService.uploadFile(maliciousFile, "board", 1L, testUser.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file name");
    }
    
    @Test
    @DisplayName("여러 파일 업로드 성공")
    void uploadFiles_Success() {
        // given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "doc1.pdf",
                "application/pdf",
                "Content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "doc2.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Content 2".getBytes()
        );
        MockMultipartFile file3 = new MockMultipartFile(
                "files",
                "image.jpg",
                "image/jpeg",
                "Image content".getBytes()
        );
        
        List<MultipartFile> files = Arrays.asList(file1, file2, file3);
        
        // when
        List<FileAttachment> attachments = fileService.uploadFiles(files, "approval", 100L, testUser.getUserId());
        
        // then
        assertThat(attachments).hasSize(3);
        assertThat(attachments.get(0).getFileName()).isEqualTo("doc1.pdf");
        assertThat(attachments.get(1).getFileName()).isEqualTo("doc2.docx");
        assertThat(attachments.get(2).getFileName()).isEqualTo("image.jpg");
    }
    
    @Test
    @DisplayName("파일 다운로드 성공")
    void downloadFile_Success() throws IOException {
        // given
        MockMultipartFile uploadFile = new MockMultipartFile(
                "file",
                "download_test.txt",
                "text/plain",
                "Download test content".getBytes()
        );
        FileAttachment attachment = fileService.uploadFile(uploadFile, "board", 1L, testUser.getUserId());
        
        // when
        Resource resource = fileService.downloadFile(attachment.getFileId());
        
        // then
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        
        // 다운로드 횟수 증가 확인
        FileAttachment updated = fileAttachmentRepository.findById(attachment.getFileId()).orElseThrow();
        assertThat(updated.getDownloadCount()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("존재하지 않는 파일 다운로드 시 예외 발생")
    void downloadFile_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> fileService.downloadFile(999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File not found");
    }
    
    @Test
    @DisplayName("파일 삭제 성공")
    void deleteFile_Success() {
        // given
        MockMultipartFile uploadFile = new MockMultipartFile(
                "file",
                "delete_test.txt",
                "text/plain",
                "Delete test content".getBytes()
        );
        FileAttachment attachment = fileService.uploadFile(uploadFile, "board", 1L, testUser.getUserId());
        Path filePath = Path.of(attachment.getFilePath());
        
        // when
        fileService.deleteFile(attachment.getFileId());
        
        // then
        assertThat(fileAttachmentRepository.findById(attachment.getFileId())).isEmpty();
        assertThat(Files.exists(filePath)).isFalse();
    }
    
    @Test
    @DisplayName("엔티티의 모든 파일 조회")
    void getFilesByEntity_Success() {
        // given
        String entityType = "post";
        Long entityId = 10L;
        
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "file1.txt",
                "text/plain",
                "Content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "file2.pdf",
                "application/pdf",
                "Content 2".getBytes()
        );
        
        fileService.uploadFile(file1, entityType, entityId, testUser.getUserId());
        fileService.uploadFile(file2, entityType, entityId, testUser.getUserId());
        fileService.uploadFile(file1, "other", 20L, testUser.getUserId()); // 다른 엔티티
        
        // when
        List<FileAttachment> files = fileService.getFilesByEntity(entityType, entityId);
        
        // then
        assertThat(files).hasSize(2);
        assertThat(files).allMatch(f -> f.getEntityType().equals(entityType) && f.getEntityId().equals(entityId));
    }
    
    @Test
    @DisplayName("엔티티의 모든 파일 삭제")
    void deleteFilesByEntity_Success() {
        // given
        String entityType = "comment";
        Long entityId = 5L;
        
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "comment1.txt",
                "text/plain",
                "Comment 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "comment2.txt",
                "text/plain",
                "Comment 2".getBytes()
        );
        
        FileAttachment attachment1 = fileService.uploadFile(file1, entityType, entityId, testUser.getUserId());
        FileAttachment attachment2 = fileService.uploadFile(file2, entityType, entityId, testUser.getUserId());
        
        Path path1 = Path.of(attachment1.getFilePath());
        Path path2 = Path.of(attachment2.getFilePath());
        
        // when
        fileService.deleteFilesByEntity(entityType, entityId);
        
        // then
        assertThat(fileService.getFilesByEntity(entityType, entityId)).isEmpty();
        assertThat(Files.exists(path1)).isFalse();
        assertThat(Files.exists(path2)).isFalse();
    }
    
    @Test
    @DisplayName("파일 정보 조회")
    void getFile_Success() {
        // given
        MockMultipartFile uploadFile = new MockMultipartFile(
                "file",
                "info_test.txt",
                "text/plain",
                "Info test content".getBytes()
        );
        FileAttachment uploaded = fileService.uploadFile(uploadFile, "board", 1L, testUser.getUserId());
        
        // when
        FileAttachment file = fileService.getFile(uploaded.getFileId());
        
        // then
        assertThat(file).isNotNull();
        assertThat(file.getFileId()).isEqualTo(uploaded.getFileId());
        assertThat(file.getFileName()).isEqualTo("info_test.txt");
    }
    
    @Test
    @DisplayName("파일 크기 포맷팅")
    void formatFileSize_Success() {
        // when & then
        assertThat(fileService.formatFileSize(500)).isEqualTo("500 B");
        assertThat(fileService.formatFileSize(1024)).isEqualTo("1.0 KB");
        assertThat(fileService.formatFileSize(1024 * 1024)).isEqualTo("1.0 MB");
        assertThat(fileService.formatFileSize(5 * 1024 * 1024 + 512 * 1024)).isEqualTo("5.5 MB");
    }
    
    @Test
    @DisplayName("확장자 없는 파일 업로드")
    void uploadFile_NoExtension_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README",
                "text/plain",
                "Readme content".getBytes()
        );
        
        // when
        FileAttachment attachment = fileService.uploadFile(file, "project", 1L, testUser.getUserId());
        
        // then
        assertThat(attachment).isNotNull();
        assertThat(attachment.getFileName()).isEqualTo("README");
        assertThat(attachment.getStoredName()).doesNotContain(".");
    }
}