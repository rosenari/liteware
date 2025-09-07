package com.liteware.service;

import com.liteware.model.entity.AnnualLeave;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.LeaveRequest;
import com.liteware.repository.AnnualLeaveRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.leave.AnnualLeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveServiceTest {
    
    @Mock
    private AnnualLeaveRepository annualLeaveRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AnnualLeaveService annualLeaveService;
    
    private User user;
    private AnnualLeave annualLeave;
    private LeaveRequest leaveRequest;
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .loginId("test001")
                .name("홍길동")
                .email("test@example.com")
                .hireDate(LocalDate.of(2020, 1, 1))
                .build();
        
        annualLeave = AnnualLeave.builder()
                .annualLeaveId(1L)
                .user(user)
                .year(2025)
                .totalHours(120.0) // 15일
                .usedHours(16.0) // 2일 사용
                .remainingHours(104.0) // 13일 남음
                .carriedOverHours(0.0)
                .grantedDate(LocalDate.of(2025, 1, 1))
                .expiryDate(LocalDate.of(2026, 12, 31))
                .build();
        
        ApprovalDocument document = ApprovalDocument.builder()
                .documentId(100L)
                .drafter(user)
                .build();
        
        leaveRequest = LeaveRequest.builder()
                .requestId(1L)
                .document(document)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 3))
                .leaveDays(3.0)
                .leaveType("연차")
                .build();
    }
    
    @Test
    @DisplayName("연차 정보를 조회할 수 있어야 한다")
    void getOrCreateAnnualLeave_ExistingLeave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        
        AnnualLeave result = annualLeaveService.getOrCreateAnnualLeave(1L, 2025);
        
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getTotalHours()).isEqualTo(120.0);
    }
    
    @Test
    @DisplayName("연차 정보가 없으면 새로 생성해야 한다")
    void getOrCreateAnnualLeave_CreateNew() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.empty());
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenAnswer(invocation -> {
            AnnualLeave saved = invocation.getArgument(0);
            saved.setAnnualLeaveId(1L);
            return saved;
        });
        
        AnnualLeave result = annualLeaveService.getOrCreateAnnualLeave(1L, 2025);
        
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getYear()).isEqualTo(2025);
        // 근속 5년차 = 17일 (136시간)
        assertThat(result.getTotalHours()).isEqualTo(136.0);
        verify(annualLeaveRepository).save(any(AnnualLeave.class));
    }
    
    @Test
    @DisplayName("신입사원의 연차는 월할 계산되어야 한다")
    void getOrCreateAnnualLeave_NewEmployee() {
        user.setHireDate(LocalDate.of(2025, 6, 1)); // 2025년 6월 입사
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.empty());
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        AnnualLeave result = annualLeaveService.getOrCreateAnnualLeave(1L, 2025);
        
        // 6월부터 12월까지 6개월 * 8시간 = 48시간
        assertThat(result.getTotalHours()).isEqualTo(48.0);
    }
    
    @Test
    @DisplayName("이전 년도 연차가 이월되어야 한다")
    void getOrCreateAnnualLeave_WithCarryOver() {
        AnnualLeave previousYear = AnnualLeave.builder()
                .annualLeaveId(2L)
                .user(user)
                .year(2024)
                .remainingHours(50.0) // 50시간 남음
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.empty());
        when(annualLeaveRepository.findByUserAndYear(user, 2024))
                .thenReturn(Optional.of(previousYear));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        AnnualLeave result = annualLeaveService.getOrCreateAnnualLeave(1L, 2025);
        
        // 최대 40시간까지 이월 가능
        assertThat(result.getCarriedOverHours()).isEqualTo(40.0);
        assertThat(result.getRemainingHours()).isEqualTo(136.0 + 40.0); // 기본 + 이월
    }
    
    @Test
    @DisplayName("연차를 사용할 수 있어야 한다")
    void useAnnualLeave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenReturn(annualLeave);
        
        annualLeaveService.useAnnualLeave(leaveRequest);
        
        assertThat(annualLeave.getUsedHours()).isEqualTo(40.0); // 16 + 24 (3일)
        assertThat(annualLeave.getRemainingHours()).isEqualTo(80.0); // 104 - 24
        verify(annualLeaveRepository).save(annualLeave);
    }
    
    @Test
    @DisplayName("연차를 복원할 수 있어야 한다")
    void restoreAnnualLeave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenReturn(annualLeave);
        
        annualLeaveService.restoreAnnualLeave(leaveRequest);
        
        assertThat(annualLeave.getUsedHours()).isEqualTo(-8.0); // 16 - 24 (3일 복원)
        assertThat(annualLeave.getRemainingHours()).isEqualTo(128.0); // 104 + 24
        verify(annualLeaveRepository).save(annualLeave);
    }
    
    @Test
    @DisplayName("사용자 연차 현황을 조회할 수 있어야 한다")
    void getUserAnnualLeaves() {
        List<AnnualLeave> leaves = Arrays.asList(annualLeave);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserOrderByYearDesc(user))
                .thenReturn(leaves);
        
        List<AnnualLeave> result = annualLeaveService.getUserAnnualLeaves(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(annualLeave);
    }
    
    @Test
    @DisplayName("부서별 연차 사용 통계를 조회할 수 있어야 한다")
    void getDepartmentLeaveStatistics() {
        Object[] stats = new Object[]{"개발팀", 80.0, 40.0};
        List<Object[]> results = Arrays.asList(stats);
        
        when(annualLeaveRepository.getDepartmentLeaveStatistics(2025))
                .thenReturn(results);
        
        Map<String, Object> result = annualLeaveService.getDepartmentLeaveStatistics(2025);
        
        assertThat(result).containsKey("개발팀");
        Map<String, Object> deptStats = (Map<String, Object>) result.get("개발팀");
        assertThat(deptStats.get("avgUsedHours")).isEqualTo(80.0);
        assertThat(deptStats.get("avgRemainingHours")).isEqualTo(40.0);
        assertThat(deptStats.get("avgUsedDays")).isEqualTo(10.0); // 80 / 8
        assertThat(deptStats.get("avgRemainingDays")).isEqualTo(5.0); // 40 / 8
    }
    
    @Test
    @DisplayName("만료 예정 연차를 조회할 수 있어야 한다")
    void getExpiringLeaves() {
        List<AnnualLeave> expiringLeaves = Arrays.asList(annualLeave);
        when(annualLeaveRepository.findExpiringLeaves(any(LocalDate.class)))
                .thenReturn(expiringLeaves);
        
        List<AnnualLeave> result = annualLeaveService.getExpiringLeaves();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(annualLeave);
    }
    
    @Test
    @DisplayName("연차 잔여를 확인할 수 있어야 한다")
    void hasEnoughLeave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        
        boolean hasEnough = annualLeaveService.hasEnoughLeave(1L, 2025, 80.0);
        boolean notEnough = annualLeaveService.hasEnoughLeave(1L, 2025, 120.0);
        
        assertThat(hasEnough).isTrue();
        assertThat(notEnough).isFalse();
    }
    
    @Test
    @DisplayName("연차를 수동으로 조정할 수 있어야 한다 - 추가")
    void adjustAnnualLeave_Add() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenReturn(annualLeave);
        
        annualLeaveService.adjustAnnualLeave(1L, 2025, 16.0, "보상 휴가");
        
        assertThat(annualLeave.getTotalHours()).isEqualTo(136.0); // 120 + 16
        assertThat(annualLeave.getRemainingHours()).isEqualTo(120.0); // 104 + 16
        verify(annualLeaveRepository).save(annualLeave);
    }
    
    @Test
    @DisplayName("연차를 수동으로 조정할 수 있어야 한다 - 차감")
    void adjustAnnualLeave_Subtract() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenReturn(annualLeave);
        
        annualLeaveService.adjustAnnualLeave(1L, 2025, -8.0, "무단 결근");
        
        assertThat(annualLeave.getUsedHours()).isEqualTo(24.0); // 16 + 8
        assertThat(annualLeave.getRemainingHours()).isEqualTo(96.0); // 104 - 8
        verify(annualLeaveRepository).save(annualLeave);
    }
    
    @Test
    @DisplayName("전체 사용자 연차를 초기화할 수 있어야 한다")
    void initializeAnnualLeavesForYear() {
        User user2 = User.builder()
                .userId(2L)
                .name("김철수")
                .hireDate(LocalDate.of(2021, 6, 1))
                .build();
        
        List<User> users = Arrays.asList(user, user2);
        when(userRepository.findAll()).thenReturn(users);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(annualLeaveRepository.findByUserAndYear(any(User.class), eq(2025)))
                .thenReturn(Optional.empty());
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        annualLeaveService.initializeAnnualLeavesForYear(2025);
        
        verify(annualLeaveRepository, times(2)).save(any(AnnualLeave.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자로 연차 조회 시 예외가 발생해야 한다")
    void getOrCreateAnnualLeave_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> annualLeaveService.getOrCreateAnnualLeave(999L, 2025))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("시간 단위 연차를 사용할 수 있어야 한다")
    void useAnnualLeave_WithHours() {
        leaveRequest.setLeaveHours(4.0); // 반차
        leaveRequest.setLeaveDays(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUserAndYear(user, 2025))
                .thenReturn(Optional.of(annualLeave));
        when(annualLeaveRepository.save(any(AnnualLeave.class))).thenReturn(annualLeave);
        
        annualLeaveService.useAnnualLeave(leaveRequest);
        
        assertThat(annualLeave.getUsedHours()).isEqualTo(20.0); // 16 + 4
        assertThat(annualLeave.getRemainingHours()).isEqualTo(100.0); // 104 - 4
    }
}