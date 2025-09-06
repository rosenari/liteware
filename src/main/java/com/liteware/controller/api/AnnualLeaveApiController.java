package com.liteware.controller.api;

import com.liteware.model.entity.AnnualLeave;
import com.liteware.service.leave.AnnualLeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/annual-leave")
@RequiredArgsConstructor
public class AnnualLeaveApiController {
    
    private final AnnualLeaveService annualLeaveService;
    
    /**
     * 사용자 연차 현황 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAnnualLeaveStatus(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // userId가 없으면 현재 사용자 ID 사용 (TODO: UserDetails에서 추출)
            Long targetUserId = userId != null ? userId : 1L;
            Integer targetYear = year != null ? year : LocalDate.now().getYear();
            
            AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(targetUserId, targetYear);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("annualLeave", annualLeave);
            response.put("totalDays", annualLeave.getTotalDays());
            response.put("usedDays", annualLeave.getUsedDays());
            response.put("remainingDays", annualLeave.getRemainingDays());
            response.put("usageRate", annualLeave.getUsageRate());
            response.put("leaveStatus", annualLeave.getLeaveStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get annual leave status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 사용자 연차 이력 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getAnnualLeaveHistory(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // TODO: UserDetails에서 추출
            
            List<AnnualLeave> history = annualLeaveService.getUserAnnualLeaves(targetUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", history);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get annual leave history", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 연차 잔여 확인
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAnnualLeave(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Double requestedHours) {
        
        try {
            boolean hasEnough = annualLeaveService.hasEnoughLeave(userId, year, requestedHours);
            AnnualLeave annualLeave = annualLeaveService.getOrCreateAnnualLeave(userId, year);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasEnoughLeave", hasEnough);
            response.put("remainingHours", annualLeave.getRemainingHours());
            response.put("requestedHours", requestedHours);
            response.put("shortfall", hasEnough ? 0 : requestedHours - annualLeave.getRemainingHours());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to check annual leave", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 부서별 연차 사용 통계
     */
    @GetMapping("/department-stats")
    public ResponseEntity<Map<String, Object>> getDepartmentStatistics(
            @RequestParam(required = false) Integer year) {
        
        try {
            Integer targetYear = year != null ? year : LocalDate.now().getYear();
            Map<String, Object> stats = annualLeaveService.getDepartmentLeaveStatistics(targetYear);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("year", targetYear);
            response.put("departmentStats", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get department statistics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 만료 예정 연차 조회
     */
    @GetMapping("/expiring")
    public ResponseEntity<Map<String, Object>> getExpiringLeaves() {
        try {
            List<AnnualLeave> expiringLeaves = annualLeaveService.getExpiringLeaves();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expiringLeaves", expiringLeaves);
            response.put("count", expiringLeaves.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get expiring leaves", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 연차 수동 조정 (관리자용)
     */
    @PostMapping("/adjust")
    public ResponseEntity<Map<String, Object>> adjustAnnualLeave(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Double hours,
            @RequestParam String reason) {
        
        try {
            annualLeaveService.adjustAnnualLeave(userId, year, hours, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "연차가 조정되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to adjust annual leave", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 연도별 연차 초기화 (관리자용)
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeAnnualLeaves(
            @RequestParam Integer year) {
        
        try {
            annualLeaveService.initializeAnnualLeavesForYear(year);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", year + "년도 연차가 초기화되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to initialize annual leaves", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}