package com.liteware.model.entity.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UrgencyType {
    NORMAL("보통"),
    URGENT("긴급");
    
    private final String description;
}