package com.liteware.service;

import com.liteware.model.entity.file.FileAttachment;
import com.liteware.repository.FileAttachmentRepository;
import com.liteware.service.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    
    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    
    @InjectMocks
    private FileService fileService;
    
    @TempDir
    Path tempDir;
    
    private FileAttachment fileAttachment;
    private MockMultipartFile mockFile;
    
    @BeforeEach
    void setUp() {
        // 업로드 디렉토리를 임시 디렉토리로 설정
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "maxFileSize", 10485760L); // 10MB
        ReflectionTestUtils.setField(fileService, "allowedExtensions", "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip");
        
        fileAttachment = FileAttachment.builder()
                .fileId(1L)
                .fileName("test.pdf")
                .storedName("20250107_abc123.pdf")
                .filePath(tempDir.resolve("test.pdf").toString())
                .fileSize(1024L)
                .fileType("application/pdf")
                .entityType("approval")
                .entityId(100L)
                .uploaderId(1L)
                .downloadCount(0L)
                .build();
        
        mockFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes()
        );
    }
    
    @Test
    @DisplayName("파일을 업로드할 수 있어야 한다")
    void uploadFile() {
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(invocation -> {
            FileAttachment saved = invocation.getArgument(0);
            saved.setFileId(1L);
            return saved;
        });
        
        FileAttachment result = fileService.uploadFile(mockFile, "approval", 100L, 1L);
        
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test.pdf");
        assertThat(result.getEntityType()).isEqualTo("approval");
        assertThat(result.getEntityId()).isEqualTo(100L);
        assertThat(result.getFileSize()).isEqualTo(mockFile.getSize());
        verify(fileAttachmentRepository).save(any(FileAttachment.class));
    }
    
    @Test
    @DisplayName("빈 파일은 업로드할 수 없어야 한다")
    void uploadFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);
        
        assertThatThrownBy(() -> fileService.uploadFile(emptyFile, "approval", 100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }
    
    @Test
    @DisplayName("파일 크기 제한을 초과하면 업로드할 수 없어야 한다")
    void uploadFile_ExceedsMaxSize_ThrowsException() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);
        
        assertThatThrownBy(() -> fileService.uploadFile(largeFile, "approval", 100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum limit");
    }
    
    @Test
    @DisplayName("허용되지 않은 확장자는 업로드할 수 없어야 한다")
    void uploadFile_InvalidExtension_ThrowsException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.exe", "application/octet-stream", "content".getBytes());
        
        assertThatThrownBy(() -> fileService.uploadFile(invalidFile, "approval", 100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File extension not allowed");
    }
    
    @Test
    @DisplayName("실행 파일은 업로드할 수 없어야 한다")
    void uploadFile_ExecutableFile_ThrowsException() {
        MockMultipartFile execFile = new MockMultipartFile(
                "file", "test.sh", "text/plain", "#!/bin/bash".getBytes());
        
        assertThatThrownBy(() -> fileService.uploadFile(execFile, "approval", 100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Executable files are not allowed");
    }
    
    @Test
    @DisplayName("Path traversal 공격을 방지해야 한다")
    void uploadFile_PathTraversal_ThrowsException() {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", "content".getBytes());
        
        assertThatThrownBy(() -> fileService.uploadFile(maliciousFile, "approval", 100L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file name");
    }
    
    @Test
    @DisplayName("여러 파일을 업로드할 수 있어야 한다")
    void uploadFiles() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "test1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "test2.pdf", "application/pdf", "content2".getBytes());
        
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(invocation -> {
            FileAttachment saved = invocation.getArgument(0);
            saved.setFileId(System.currentTimeMillis());
            return saved;
        });
        
        List<MultipartFile> files = Arrays.asList(file1, file2);
        List<FileAttachment> result = fileService.uploadFiles(files, "approval", 100L, 1L);
        
        assertThat(result).hasSize(2);
        verify(fileAttachmentRepository, times(2)).save(any(FileAttachment.class));
    }
    
    @Test
    @DisplayName("파일을 다운로드할 수 있어야 한다")
    void downloadFile() throws IOException {
        // 실제 파일 생성
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "Test content".getBytes());
        fileAttachment.setFilePath(testFile.toString());
        
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(fileAttachment));
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenReturn(fileAttachment);
        
        Resource result = fileService.downloadFile(1L);
        
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
        assertThat(fileAttachment.getDownloadCount()).isEqualTo(1L);
        verify(fileAttachmentRepository).save(fileAttachment);
    }
    
    @Test
    @DisplayName("존재하지 않는 파일 다운로드 시 예외가 발생해야 한다")
    void downloadFile_FileNotFound_ThrowsException() {
        when(fileAttachmentRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> fileService.downloadFile(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File not found");
    }
    
    @Test
    @DisplayName("파일을 삭제할 수 있어야 한다")
    void deleteFile() throws IOException {
        // 실제 파일 생성
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "Test content".getBytes());
        fileAttachment.setFilePath(testFile.toString());
        
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(fileAttachment));
        
        fileService.deleteFile(1L);
        
        assertThat(Files.exists(testFile)).isFalse();
        verify(fileAttachmentRepository).delete(fileAttachment);
    }
    
    @Test
    @DisplayName("엔티티의 모든 첨부파일을 조회할 수 있어야 한다")
    void getFilesByEntity() {
        List<FileAttachment> attachments = Arrays.asList(fileAttachment);
        when(fileAttachmentRepository.findByEntityTypeAndEntityId("approval", 100L))
                .thenReturn(attachments);
        
        List<FileAttachment> result = fileService.getFilesByEntity("approval", 100L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(fileAttachment);
    }
    
    @Test
    @DisplayName("엔티티의 모든 첨부파일을 삭제할 수 있어야 한다")
    void deleteFilesByEntity() throws IOException {
        // 실제 파일 생성
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "Test content".getBytes());
        fileAttachment.setFilePath(testFile.toString());
        
        List<FileAttachment> attachments = Arrays.asList(fileAttachment);
        when(fileAttachmentRepository.findByEntityTypeAndEntityId("approval", 100L))
                .thenReturn(attachments);
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(fileAttachment));
        
        fileService.deleteFilesByEntity("approval", 100L);
        
        assertThat(Files.exists(testFile)).isFalse();
        verify(fileAttachmentRepository).delete(fileAttachment);
    }
    
    @Test
    @DisplayName("파일 정보를 조회할 수 있어야 한다")
    void getFile() {
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(fileAttachment));
        
        FileAttachment result = fileService.getFile(1L);
        
        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo(1L);
        assertThat(result.getFileName()).isEqualTo("test.pdf");
    }
    
    @Test
    @DisplayName("파일 크기를 읽기 쉬운 형식으로 변환할 수 있어야 한다")
    void formatFileSize() {
        assertThat(fileService.formatFileSize(512)).isEqualTo("512 B");
        assertThat(fileService.formatFileSize(1024)).isEqualTo("1.0 KB");
        assertThat(fileService.formatFileSize(1536)).isEqualTo("1.5 KB");
        assertThat(fileService.formatFileSize(1048576)).isEqualTo("1.0 MB");
        assertThat(fileService.formatFileSize(1572864)).isEqualTo("1.5 MB");
        assertThat(fileService.formatFileSize(1073741824)).isEqualTo("1.0 GB");
    }
    
    @Test
    @DisplayName("IO 예외 발생 시 RuntimeException으로 래핑되어야 한다")
    void uploadFile_IOException_WrapsException() {
        MockMultipartFile errorFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes()) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("Test IO error");
            }
        };
        
        assertThatThrownBy(() -> fileService.uploadFile(errorFile, "approval", 100L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to store file")
                .hasCauseInstanceOf(IOException.class);
    }
}