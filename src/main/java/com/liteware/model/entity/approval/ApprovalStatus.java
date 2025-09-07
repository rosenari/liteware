package com.liteware.model.entity.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalStatus {
    DRAFT("임시저장"),
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("취소"),
    SKIPPED("건너뜀");
    
    private final String description;
}