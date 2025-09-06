package com.liteware.model.dto;

import com.liteware.model.entity.approval.DocumentStatus;
import com.liteware.model.entity.approval.DocumentType;
import com.liteware.model.entity.approval.UrgencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDocumentDto {
    
    private Long docId;
    private String docNumber;
    
    @NotNull(message = "문서 유형은 필수입니다")
    private DocumentType docType;
    
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;
    
    private String content;
    private String formData;
    private DocumentStatus status;
    
    @NotNull(message = "기안자 ID는 필수입니다")
    private Long drafterId;
    private String drafterName;
    
    private Long currentApproverId;
    private String currentApproverName;
    
    private LocalDateTime draftedAt;
    private LocalDateTime completedAt;
    
    @Builder.Default
    private UrgencyType urgency = UrgencyType.NORMAL;
    
    @Builder.Default
    private List<ApprovalLineDto> approvalLines = new ArrayList<>();
    
    // Attachments are handled separately in the controller, not part of form binding
    
    private Boolean isDeleted;
    
    public static ApprovalDocumentDto from(com.liteware.model.entity.approval.ApprovalDocument document) {
        ApprovalDocumentDto dto = ApprovalDocumentDto.builder()
                .docId(document.getDocId())
                .docNumber(document.getDocNumber())
                .docType(document.getDocType())
                .title(document.getTitle())
                .content(document.getContent())
                .formData(document.getFormData())
                .status(document.getStatus())
                .drafterId(document.getDrafter().getUserId())
                .drafterName(document.getDrafter().getName())
                .draftedAt(document.getDraftedAt())
                .completedAt(document.getCompletedAt())
                .urgency(document.getUrgency())
                .isDeleted(document.getIsDeleted())
                .build();
        
        if (document.getCurrentApprover() != null) {
            dto.setCurrentApproverId(document.getCurrentApprover().getUserId());
            dto.setCurrentApproverName(document.getCurrentApprover().getName());
        }
        
        document.getApprovalLines().forEach(line -> 
            dto.getApprovalLines().add(ApprovalLineDto.from(line))
        );
        
        return dto;
    }
}