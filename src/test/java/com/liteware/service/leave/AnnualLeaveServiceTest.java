package com.liteware.service.leave;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.entity.AnnualLeave;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.*;
import com.liteware.repository.AnnualLeaveRepository;
import com.liteware.service.BaseServiceTest;
import com.liteware.service.approval.ApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnualLeaveServiceTest extends BaseServiceTest {
    
    @Autowired
    private AnnualLeaveService annualLeaveService;
    
    @Autowired
    private AnnualLeaveRepository annualLeaveRepository;
    
    @Autowired
    private ApprovalService approvalService;
    
    private User employee;
    private User seniorEmployee;
    private Integer currentYear;
    
    @BeforeEach
    void setUp() {
        currentYear = LocalDate.now().getYear();
        
        // 일반 직원 (입사 2년차)
        employee = createUser("emp001", "김사원", "emp@example.com", department, position);
        employee.setHireDate(LocalDate.now().minusYears(2));
        employee.addRole(userRole);
        userRepository.save(employee);
        
        // 시니어 직원 (입사 10년차)
        seniorEmployee = createUser("senior001", "박부장", "senior@example.com", department, position);
        seniorEmployee.setHireDate(LocalDate.now().minusYears(10));
        seniorEmployee.addRole(userRole);
        userRepository.save(seniorEmployee);
    }
    
    @Test
    @DisplayName("연차 정보 조회 - 신규 생성")
    void getOrCreateAnnualLeave_NewCreate() {
        // when
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // then
        assertThat(annualLeave).isNotNull();
        assertThat(annualLeave.getUser().getUserId()).isEqualTo(employee.getUserId());
        assertThat(annualLeave.getYear()).isEqualTo(currentYear);
        assertThat(annualLeave.getTotalHours()).isEqualTo(15 * 8); // 2년차는 15일
        assertThat(annualLeave.getUsedHours()).isEqualTo(0.0);
        assertThat(annualLeave.getRemainingHours()).isEqualTo(120.0);
    }
    
    @Test
    @DisplayName("연차 정보 조회 - 기존 정보 반환")
    void getOrCreateAnnualLeave_ExistingReturn() {
        // given
        AnnualLeave created = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // when
        AnnualLeave retrieved = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // then
        assertThat(retrieved.getAnnualLeaveId()).isEqualTo(created.getAnnualLeaveId());
    }
    
    @Test
    @DisplayName("근속년수별 연차 계산 - 신입 (입사 첫해)")
    void calculateAnnualLeave_FirstYear() {
        // given
        User newEmployee = createUser("new001", "신입사원", "new@example.com", department, position);
        newEmployee.setHireDate(LocalDate.now().minusMonths(6)); // 6개월 근무
        newEmployee.addRole(userRole);
        userRepository.save(newEmployee);
        
        // when
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(newEmployee.getUserId(), currentYear);
        
        // then
        assertThat(annualLeave.getTotalHours()).isGreaterThanOrEqualTo(48.0); // 최소 6개월 * 8시간
    }
    
    @Test
    @DisplayName("근속년수별 연차 계산 - 10년차")
    void calculateAnnualLeave_TenYears() {
        // when
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(seniorEmployee.getUserId(), currentYear);
        
        // then
        assertThat(annualLeave.getTotalHours()).isEqualTo(19 * 8); // 10년차는 19일
    }
    
    @Test
    @DisplayName("연차 사용")
    void useAnnualLeave_Success() {
        // given
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        double initialRemaining = annualLeave.getRemainingHours();
        
        LeaveRequest leaveRequest = createLeaveRequest(employee, 2); // 2일 연차
        
        // when
        annualLeaveService.useAnnualLeave(leaveRequest);
        
        // then
        AnnualLeave updated = annualLeaveRepository.findById(annualLeave.getAnnualLeaveId()).orElseThrow();
        assertThat(updated.getUsedHours()).isEqualTo(16.0); // 2일 * 8시간
        assertThat(updated.getRemainingHours()).isEqualTo(initialRemaining - 16.0);
    }
    
    @Test
    @DisplayName("연차 사용 - 잔여 시간 부족 시 예외")
    void useAnnualLeave_InsufficientHours() {
        // given
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        LeaveRequest leaveRequest = createLeaveRequest(employee, 20); // 20일 연차 (초과)
        
        // when & then
        assertThatThrownBy(() -> annualLeaveService.useAnnualLeave(leaveRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("연차가 부족합니다");
    }
    
    @Test
    @DisplayName("연차 복원 (반려 시)")
    void restoreAnnualLeave_Success() {
        // given
        LeaveRequest leaveRequest = createLeaveRequest(employee, 3); // 3일 연차
        annualLeaveService.useAnnualLeave(leaveRequest);
        
        AnnualLeave afterUse = annualLeaveRepository.findByUserAndYear(employee, currentYear).orElseThrow();
        double usedHours = afterUse.getUsedHours();
        
        // when
        annualLeaveService.restoreAnnualLeave(leaveRequest);
        
        // then
        AnnualLeave restored = annualLeaveRepository.findByUserAndYear(employee, currentYear).orElseThrow();
        assertThat(restored.getUsedHours()).isEqualTo(usedHours - 24.0); // 3일 * 8시간 복원
        assertThat(restored.getRemainingHours()).isEqualTo(120.0); // 전체 복원
    }
    
    @Test
    @DisplayName("사용자 연차 현황 조회")
    void getUserAnnualLeaves_Success() {
        // given
        annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear - 1);
        
        // when
        List<AnnualLeave> leaves = annualLeaveService.getUserAnnualLeaves(employee.getUserId());
        
        // then
        assertThat(leaves).hasSize(2);
        assertThat(leaves.get(0).getYear()).isEqualTo(currentYear); // 최신년도 먼저
        assertThat(leaves.get(1).getYear()).isEqualTo(currentYear - 1);
    }
    
    @Test
    @DisplayName("부서별 연차 사용 통계")
    void getDepartmentLeaveStatistics_Success() {
        // given
        // 직원들 연차 사용
        LeaveRequest leaveRequest1 = createLeaveRequest(employee, 5);
        annualLeaveService.useAnnualLeave(leaveRequest1);
        
        LeaveRequest leaveRequest2 = createLeaveRequest(seniorEmployee, 10);
        annualLeaveService.useAnnualLeave(leaveRequest2);
        
        // when
        Map<String, Object> statistics = annualLeaveService.getDepartmentLeaveStatistics(currentYear);
        
        // then
        assertThat(statistics).isNotEmpty();
        // 부서별 통계가 Map 형태로 반환되는지 확인
    }
    
    @Test
    @DisplayName("만료 예정 연차 조회")
    void getExpiringLeaves_Success() {
        // given
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        annualLeave.setExpiryDate(LocalDate.now().plusDays(20)); // 20일 후 만료
        annualLeaveRepository.save(annualLeave);
        
        // when
        List<AnnualLeave> expiringLeaves = annualLeaveService.getExpiringLeaves();
        
        // then
        assertThat(expiringLeaves).isNotEmpty();
        assertThat(expiringLeaves).anyMatch(leave -> leave.getAnnualLeaveId().equals(annualLeave.getAnnualLeaveId()));
    }
    
    @Test
    @DisplayName("연차 잔여 확인")
    void hasEnoughLeave_Success() {
        // given
        annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // when & then
        assertThat(annualLeaveService.hasEnoughLeave(employee.getUserId(), currentYear, 40.0)).isTrue(); // 5일
        assertThat(annualLeaveService.hasEnoughLeave(employee.getUserId(), currentYear, 200.0)).isFalse(); // 25일
    }
    
    @Test
    @DisplayName("연차 수동 조정 - 추가")
    void adjustAnnualLeave_Add() {
        // given
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        double initialTotal = annualLeave.getTotalHours();
        
        // when
        annualLeaveService.adjustAnnualLeave(employee.getUserId(), currentYear, 16.0, "보상 연차");
        
        // then
        AnnualLeave adjusted = annualLeaveRepository.findById(annualLeave.getAnnualLeaveId()).orElseThrow();
        assertThat(adjusted.getTotalHours()).isEqualTo(initialTotal + 16.0);
        assertThat(adjusted.getRemainingHours()).isEqualTo(initialTotal + 16.0);
    }
    
    @Test
    @DisplayName("연차 수동 조정 - 차감")
    void adjustAnnualLeave_Subtract() {
        // given
        AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        double initialRemaining = annualLeave.getRemainingHours();
        
        // when
        annualLeaveService.adjustAnnualLeave(employee.getUserId(), currentYear, -8.0, "조정 차감");
        
        // then
        AnnualLeave adjusted = annualLeaveRepository.findById(annualLeave.getAnnualLeaveId()).orElseThrow();
        assertThat(adjusted.getUsedHours()).isEqualTo(8.0);
        assertThat(adjusted.getRemainingHours()).isEqualTo(initialRemaining - 8.0);
    }
    
    @Test
    @DisplayName("전체 사용자 연차 초기화")
    void initializeAnnualLeavesForYear_Success() {
        // given
        User newUser = createUser("init001", "초기화테스트", "init@example.com", department, position);
        newUser.setHireDate(LocalDate.now().minusYears(1));
        newUser.addRole(userRole);
        userRepository.save(newUser);
        
        // when
        annualLeaveService.initializeAnnualLeavesForYear(currentYear);
        
        // then
        List<AnnualLeave> allLeaves = annualLeaveRepository.findAll();
        assertThat(allLeaves).hasSizeGreaterThanOrEqualTo(3); // 최소 3명의 연차 정보
        
        // 신규 사용자의 연차가 생성되었는지 확인
        AnnualLeave newUserLeave = annualLeaveRepository.findByUserAndYear(newUser, currentYear).orElse(null);
        assertThat(newUserLeave).isNotNull();
    }
    
    @Test
    @DisplayName("이월 연차 적용")
    void applyCarriedOverLeave_Success() {
        // given
        // 작년 연차 생성 및 일부 남김
        AnnualLeave lastYear = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear - 1);
        lastYear.setRemainingHours(32.0); // 4일 남김
        annualLeaveRepository.save(lastYear);
        
        // when
        AnnualLeave thisYear = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // then
        assertThat(thisYear.getCarriedOverHours()).isEqualTo(32.0); // 4일 이월
        assertThat(thisYear.getRemainingHours()).isEqualTo(120.0 + 32.0); // 기본 + 이월
    }
    
    @Test
    @DisplayName("이월 연차 최대 제한 (5일)")
    void applyCarriedOverLeave_MaxLimit() {
        // given
        // 작년 연차 생성 및 많이 남김
        AnnualLeave lastYear = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear - 1);
        lastYear.setRemainingHours(80.0); // 10일 남김
        annualLeaveRepository.save(lastYear);
        
        // when
        AnnualLeave thisYear = annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        // then
        assertThat(thisYear.getCarriedOverHours()).isEqualTo(40.0); // 최대 5일(40시간)만 이월
    }
    
    @Test
    @DisplayName("시간 단위 연차 사용")
    void useAnnualLeave_HourlyLeave() {
        // given
        annualLeaveService.getOrCreateAnnualLeave(employee.getUserId(), currentYear);
        
        LeaveRequest hourlyLeave = createHourlyLeaveRequest(employee, 4.0); // 4시간 연차
        
        // when
        annualLeaveService.useAnnualLeave(hourlyLeave);
        
        // then
        AnnualLeave updated = annualLeaveRepository.findByUserAndYear(employee, currentYear).orElseThrow();
        assertThat(updated.getUsedHours()).isEqualTo(4.0);
        assertThat(updated.getRemainingHours()).isEqualTo(116.0);
    }
    
    // Helper methods
    private LeaveRequest createLeaveRequest(User drafter, int days) {
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle(days + "일 연차 신청");
        dto.setContent("연차 사용");
        dto.setDrafterId(drafter.getUserId());
        dto.setUrgency(UrgencyType.NORMAL);
        
        ApprovalDocument document = approvalService.draftDocument(dto);
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setDocument(document);
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(1));
        leaveRequest.setEndDate(LocalDate.now().plusDays(days));
        leaveRequest.setLeaveDays(days);
        leaveRequest.setLeaveHours((double) days * 8);
        leaveRequest.setReason("개인 사유");
        
        document.setLeaveRequest(leaveRequest);
        
        return leaveRequest;
    }
    
    private LeaveRequest createHourlyLeaveRequest(User drafter, double hours) {
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle(hours + "시간 연차 신청");
        dto.setContent("시간 연차 사용");
        dto.setDrafterId(drafter.getUserId());
        dto.setUrgency(UrgencyType.NORMAL);
        
        ApprovalDocument document = approvalService.draftDocument(dto);
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setDocument(document);
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.now().plusDays(1));
        leaveRequest.setEndDate(LocalDate.now().plusDays(1));
        leaveRequest.setIsHourlyLeave(true);
        leaveRequest.setLeaveHours(hours);
        leaveRequest.setLeaveDays(1);
        leaveRequest.setReason("개인 사유");
        
        document.setLeaveRequest(leaveRequest);
        
        return leaveRequest;
    }
}