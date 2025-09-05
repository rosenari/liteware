package com.liteware.model.entity.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentStatus {
    DRAFT("임시저장"),
    PENDING("진행중"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("회수");
    
    private final String description;
}