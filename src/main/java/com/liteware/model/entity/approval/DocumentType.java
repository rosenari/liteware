package com.liteware.model.entity.approval;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    LEAVE_REQUEST("휴가신청"),
    OVERTIME_REQUEST("연장근무신청"),
    EXPENSE_REQUEST("경비청구"),
    PURCHASE_REQUEST("구매요청"),
    GENERAL_APPROVAL("일반결재"),
    BUSINESS_TRIP("출장신청"),
    WORK_FROM_HOME("재택근무신청"),
    RESIGNATION("퇴직신청");
    
    private final String description;
}