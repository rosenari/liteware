package com.liteware.model.entity.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalType {
    APPROVAL("결재"),
    AGREEMENT("합의"),
    REFERENCE("참조"),
    NOTIFICATION("통보");
    
    private final String description;
}