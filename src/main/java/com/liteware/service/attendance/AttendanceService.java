package com.liteware.service.attendance;

import com.liteware.model.entity.Attendance;
import com.liteware.model.entity.User;
import com.liteware.repository.AttendanceRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    
    /**
     * 출근 처리
     */
    @Transactional
    public void checkIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        LocalDate today = LocalDate.now();
        
        // 이미 오늘 출근했는지 확인
        attendanceRepository.findByUserAndWorkDate(user, today)
                .ifPresentOrElse(
                    attendance -> {
                        if (attendance.getCheckInTime() != null) {
                            throw new RuntimeException("이미 출근 처리되었습니다.");
                        }
                        // 출근 시간만 업데이트
                        attendance.setCheckInTime(LocalDateTime.now());
                        attendanceRepository.save(attendance);
                    },
                    () -> {
                        // 새로운 근태 기록 생성
                        Attendance attendance = Attendance.builder()
                                .user(user)
                                .workDate(today)
                                .checkInTime(LocalDateTime.now())
                                .build();
                        attendanceRepository.save(attendance);
                    }
                );
    }
    
    /**
     * 퇴근 처리
     */
    @Transactional
    public void checkOut(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserAndWorkDate(user, today)
                .orElseThrow(() -> new RuntimeException("출근 기록이 없습니다. 먼저 출근 처리해주세요."));
        
        if (attendance.getCheckInTime() == null) {
            throw new RuntimeException("출근 기록이 없습니다.");
        }
        
        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("이미 퇴근 처리되었습니다.");
        }
        
        attendance.setCheckOutTime(LocalDateTime.now());
        attendanceRepository.save(attendance);
    }
    
    /**
     * 오늘의 근태 기록 조회
     */
    public Attendance getTodayAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return attendanceRepository.findTodayAttendance(user, LocalDate.now())
                .orElse(null);
    }
    
    /**
     * 특정 월의 근태 기록 조회
     */
    public List<Attendance> getMonthlyAttendance(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return attendanceRepository.findByUserAndYearMonth(user, year, month);
    }
    
    /**
     * 이번달 총 근무일수 조회
     */
    public long getMonthlyWorkingDays(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        YearMonth currentMonth = YearMonth.now();
        return attendanceRepository.countWorkingDaysByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue());
    }
    
    /**
     * 이번달 총 근무시간 조회 (분 단위)
     */
    public long getMonthlyWorkingMinutes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        YearMonth currentMonth = YearMonth.now();
        Long minutes = attendanceRepository.getTotalWorkingMinutesByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue());
        
        return minutes != null ? minutes : 0L;
    }
    
    /**
     * 이번달 총 근무시간을 "X시간 Y분" 형태로 반환
     */
    public String getFormattedMonthlyWorkingHours(Long userId) {
        long totalMinutes = getMonthlyWorkingMinutes(userId);
        if (totalMinutes == 0) {
            return "0시간 0분";
        }
        
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        return String.format("%d시간 %d분", hours, minutes);
    }
    
    /**
     * 최근 일주일 근태 기록 조회
     */
    public List<Attendance> getWeeklyAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // 오늘 포함 7일
        
        List<Attendance> weeklyData = new java.util.ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            Attendance attendance = attendanceRepository.findByUserAndWorkDate(user, date)
                    .orElse(null);
            
            if (attendance == null && !date.isAfter(today)) {
                // 과거 날짜인데 기록이 없으면 결근으로 처리
                attendance = Attendance.builder()
                        .user(user)
                        .workDate(date)
                        .build();
            }
            
            if (attendance != null) {
                weeklyData.add(attendance);
            }
        }
        
        return weeklyData;
    }
    
    /**
     * 근태 상태 반환 (출근/근무중 또는 빈값)
     */
    public String getAttendanceStatus(Attendance attendance) {
        if (attendance == null || attendance.getCheckInTime() == null) {
            return "";  // 빈 상태
        }
        
        if (attendance.getCheckOutTime() == null) {
            return "근무중";
        }
        
        return "출근";
    }
    
    /**
     * 근태 상태에 따른 뱃지 클래스 반환
     */
    public String getStatusBadgeClass(String status) {
        switch (status) {
            case "출근":
                return "badge-success";
            case "근무중":
                return "badge-info";
            default:
                return "badge-secondary";  // 빈 상태일 때
        }
    }
    
    /**
     * 이번달 출근일수 조회
     */
    public long getMonthlyAttendanceDays(Long userId) {
        return getMonthlyWorkingDays(userId);
    }
    
    /**
     * 이번달 결근일수 조회
     */
    public long getMonthlyAbsentDays(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = today.isBefore(currentMonth.atEndOfMonth()) ? today : currentMonth.atEndOfMonth();
        
        long totalWorkdays = 0;
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            // 주말 제외 (토요일=6, 일요일=7)
            if (date.getDayOfWeek().getValue() < 6) {
                totalWorkdays++;
            }
        }
        
        long attendanceDays = getMonthlyWorkingDays(userId);
        return Math.max(0, totalWorkdays - attendanceDays);
    }
}