package com.liteware.service.leave;

import com.liteware.model.entity.AnnualLeave;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.LeaveRequest;
import com.liteware.repository.AnnualLeaveRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnnualLeaveService {
    
    private final AnnualLeaveRepository annualLeaveRepository;
    private final UserRepository userRepository;
    
    /**
     * 연차 정보 조회 (없으면 생성)
     */
    public AnnualLeave getOrCreateAnnualLeave(Long userId, Integer year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return annualLeaveRepository.findByUserAndYear(user, year)
                .orElseGet(() -> createAnnualLeave(user, year));
    }
    
    /**
     * 신규 연차 생성
     */
    private AnnualLeave createAnnualLeave(User user, Integer year) {
        double totalHours = calculateAnnualLeaveHours(user, year);
        
        AnnualLeave annualLeave = AnnualLeave.builder()
                .user(user)
                .year(year)
                .totalHours(totalHours)
                .usedHours(0.0)
                .remainingHours(totalHours)
                .carriedOverHours(0.0)
                .grantedDate(LocalDate.of(year, 1, 1))
                .expiryDate(LocalDate.of(year + 1, 12, 31))
                .build();
        
        // 이전 년도 이월 연차 적용
        applyCarriedOverLeave(annualLeave, user, year - 1);
        
        return annualLeaveRepository.save(annualLeave);
    }
    
    /**
     * 연차 시간 계산 (근속 연수에 따라)
     */
    private double calculateAnnualLeaveHours(User user, Integer year) {
        if (user.getHireDate() == null) {
            return 15 * 8; // 기본 15일 (120시간)
        }
        
        long yearsOfService = ChronoUnit.YEARS.between(user.getHireDate(), LocalDate.of(year, 12, 31));
        
        // 근속년수별 연차 일수 계산
        if (yearsOfService < 1) {
            // 입사 첫해: 월할 계산
            long monthsWorked = ChronoUnit.MONTHS.between(user.getHireDate(), LocalDate.of(year, 12, 31));
            return Math.max(monthsWorked * 8, 0); // 월 1일(8시간)
        } else if (yearsOfService < 3) {
            return 15 * 8; // 15일 (120시간)
        } else if (yearsOfService < 5) {
            return 16 * 8; // 16일 (128시간)
        } else if (yearsOfService < 10) {
            return 17 * 8; // 17일 (136시간)
        } else if (yearsOfService < 15) {
            return 19 * 8; // 19일 (152시간)
        } else if (yearsOfService < 20) {
            return 21 * 8; // 21일 (168시간)
        } else {
            return 25 * 8; // 25일 (200시간)
        }
    }
    
    /**
     * 이월 연차 적용
     */
    private void applyCarriedOverLeave(AnnualLeave currentYear, User user, Integer previousYear) {
        annualLeaveRepository.findByUserAndYear(user, previousYear)
                .ifPresent(previousAnnualLeave -> {
                    // 최대 5일(40시간)까지 이월 가능
                    double carriedOver = Math.min(previousAnnualLeave.getRemainingHours(), 40.0);
                    if (carriedOver > 0) {
                        currentYear.setCarriedOver(carriedOver);
                        currentYear.setRemainingHours(currentYear.getTotalHours() + carriedOver);
                    }
                });
    }
    
    /**
     * 연차 사용
     */
    public void useAnnualLeave(LeaveRequest leaveRequest) {
        AnnualLeave annualLeave = getOrCreateAnnualLeave(
                leaveRequest.getDocument().getDrafter().getUserId(),
                leaveRequest.getStartDate().getYear()
        );
        
        double hoursToUse = leaveRequest.getLeaveHours() != null ? 
                leaveRequest.getLeaveHours() : leaveRequest.getLeaveDays() * 8.0;
        
        annualLeave.useLeave(hoursToUse);
        annualLeaveRepository.save(annualLeave);
        
        log.info("Annual leave used: {} hours for user {}", hoursToUse, annualLeave.getUser().getName());
    }
    
    /**
     * 연차 복원 (반려 시)
     */
    public void restoreAnnualLeave(LeaveRequest leaveRequest) {
        AnnualLeave annualLeave = getOrCreateAnnualLeave(
                leaveRequest.getDocument().getDrafter().getUserId(),
                leaveRequest.getStartDate().getYear()
        );
        
        double hoursToRestore = leaveRequest.getLeaveHours() != null ? 
                leaveRequest.getLeaveHours() : leaveRequest.getLeaveDays() * 8.0;
        
        annualLeave.restoreLeave(hoursToRestore);
        annualLeaveRepository.save(annualLeave);
        
        log.info("Annual leave restored: {} hours for user {}", hoursToRestore, annualLeave.getUser().getName());
    }
    
    /**
     * 사용자 연차 현황 조회
     */
    @Transactional(readOnly = true)
    public List<AnnualLeave> getUserAnnualLeaves(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return annualLeaveRepository.findByUserOrderByYearDesc(user);
    }
    
    /**
     * 부서별 연차 사용 통계
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentLeaveStatistics(Integer year) {
        List<Object[]> results = annualLeaveRepository.getDepartmentLeaveStatistics(year);
        
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // departmentName
                        row -> Map.of(
                                "avgUsedHours", row[1],
                                "avgRemainingHours", row[2],
                                "avgUsedDays", ((Double) row[1]) / 8.0,
                                "avgRemainingDays", ((Double) row[2]) / 8.0
                        )
                ));
    }
    
    /**
     * 만료 예정 연차 조회
     */
    @Transactional(readOnly = true)
    public List<AnnualLeave> getExpiringLeaves() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        return annualLeaveRepository.findExpiringLeaves(targetDate);
    }
    
    /**
     * 연차 잔여 확인
     */
    @Transactional(readOnly = true)
    public boolean hasEnoughLeave(Long userId, Integer year, double requestedHours) {
        AnnualLeave annualLeave = getOrCreateAnnualLeave(userId, year);
        return annualLeave.getRemainingHours() >= requestedHours;
    }
    
    /**
     * 연차 수동 조정 (관리자용)
     */
    public void adjustAnnualLeave(Long userId, Integer year, double hours, String reason) {
        AnnualLeave annualLeave = getOrCreateAnnualLeave(userId, year);
        
        if (hours > 0) {
            annualLeave.addLeave(hours);
        } else {
            annualLeave.useLeave(Math.abs(hours));
        }
        
        annualLeaveRepository.save(annualLeave);
        
        log.info("Annual leave adjusted: {} hours for user {}, reason: {}", 
                hours, annualLeave.getUser().getName(), reason);
    }
    
    /**
     * 전체 사용자 연차 초기화 (연초)
     */
    public void initializeAnnualLeavesForYear(Integer year) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            if (user.getHireDate() != null && user.getHireDate().getYear() <= year) {
                getOrCreateAnnualLeave(user.getUserId(), year);
            }
        }
        
        log.info("Annual leaves initialized for year: {}", year);
    }
}