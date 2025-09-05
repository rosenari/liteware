package com.liteware.model.dto;

import com.liteware.model.entity.approval.ApprovalStatus;
import com.liteware.model.entity.approval.ApprovalType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalLineDto {
    
    private Long lineId;
    private Long docId;
    
    @NotNull(message = "결재자 ID는 필수입니다")
    private Long approverId;
    private String approverName;
    private String approverPosition;
    private String approverDepartment;
    
    @NotNull(message = "결재 유형은 필수입니다")
    private ApprovalType approvalType;
    
    @NotNull(message = "순서는 필수입니다")
    private Integer orderSeq;
    
    private ApprovalStatus status;
    private String comment;
    private LocalDateTime approvedAt;
    private Boolean isOptional;
    
    private Long delegatedToId;
    private String delegatedToName;
    private LocalDateTime delegatedAt;
    
    public static ApprovalLineDto from(com.liteware.model.entity.approval.ApprovalLine line) {
        ApprovalLineDto dto = ApprovalLineDto.builder()
                .lineId(line.getLineId())
                .docId(line.getDocument().getDocId())
                .approverId(line.getApprover().getUserId())
                .approverName(line.getApprover().getName())
                .approvalType(line.getApprovalType())
                .orderSeq(line.getOrderSeq())
                .status(line.getStatus())
                .comment(line.getComment())
                .approvedAt(line.getApprovedAt())
                .isOptional(line.getIsOptional())
                .delegatedAt(line.getDelegatedAt())
                .build();
        
        if (line.getApprover().getPosition() != null) {
            dto.setApproverPosition(line.getApprover().getPosition().getPositionName());
        }
        
        if (line.getApprover().getDepartment() != null) {
            dto.setApproverDepartment(line.getApprover().getDepartment().getDeptName());
        }
        
        if (line.getDelegatedTo() != null) {
            dto.setDelegatedToId(line.getDelegatedTo().getUserId());
            dto.setDelegatedToName(line.getDelegatedTo().getName());
        }
        
        return dto;
    }
}