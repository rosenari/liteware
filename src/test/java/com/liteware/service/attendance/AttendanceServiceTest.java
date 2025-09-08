package com.liteware.service.attendance;

import com.liteware.model.entity.Attendance;
import com.liteware.model.entity.User;
import com.liteware.repository.AttendanceRepository;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceServiceTest extends BaseServiceTest {
    
    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    private User employee;
    
    @BeforeEach
    void setUp() {
        employee = createUser("emp001", "김사원", "emp@example.com", department, position);
        employee.addRole(userRole);
        userRepository.save(employee);
    }
    
    @Test
    @DisplayName("출근 처리 성공")
    void checkIn_Success() {
        // when
        attendanceService.checkIn(employee.getUserId());
        
        // then
        Attendance attendance = attendanceRepository.findByUserAndWorkDate(employee, LocalDate.now())
                .orElse(null);
        assertThat(attendance).isNotNull();
        assertThat(attendance.getCheckInTime()).isNotNull();
        assertThat(attendance.getCheckOutTime()).isNull();
    }
    
    @Test
    @DisplayName("중복 출근 시 예외 발생")
    void checkIn_Duplicate_ThrowsException() {
        // given
        attendanceService.checkIn(employee.getUserId());
        
        // when & then
        assertThatThrownBy(() -> attendanceService.checkIn(employee.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 출근 처리되었습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 출근 시 예외 발생")
    void checkIn_UserNotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> attendanceService.checkIn(999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("퇴근 처리 성공")
    void checkOut_Success() {
        // given
        attendanceService.checkIn(employee.getUserId());
        
        // when
        attendanceService.checkOut(employee.getUserId());
        
        // then
        Attendance attendance = attendanceRepository.findByUserAndWorkDate(employee, LocalDate.now())
                .orElse(null);
        assertThat(attendance).isNotNull();
        assertThat(attendance.getCheckInTime()).isNotNull();
        assertThat(attendance.getCheckOutTime()).isNotNull();
    }
    
    @Test
    @DisplayName("출근 없이 퇴근 시 예외 발생")
    void checkOut_NoCheckIn_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> attendanceService.checkOut(employee.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("출근 기록이 없습니다");
    }
    
    @Test
    @DisplayName("중복 퇴근 시 예외 발생")
    void checkOut_Duplicate_ThrowsException() {
        // given
        attendanceService.checkIn(employee.getUserId());
        attendanceService.checkOut(employee.getUserId());
        
        // when & then
        assertThatThrownBy(() -> attendanceService.checkOut(employee.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 퇴근 처리되었습니다");
    }
    
    @Test
    @DisplayName("오늘의 근태 기록 조회")
    void getTodayAttendance_Success() {
        // given
        attendanceService.checkIn(employee.getUserId());
        
        // when
        Attendance todayAttendance = attendanceService.getTodayAttendance(employee.getUserId());
        
        // then
        assertThat(todayAttendance).isNotNull();
        assertThat(todayAttendance.getWorkDate()).isEqualTo(LocalDate.now());
        assertThat(todayAttendance.getCheckInTime()).isNotNull();
    }
    
    @Test
    @DisplayName("오늘 근태 기록이 없는 경우")
    void getTodayAttendance_NoRecord() {
        // when
        Attendance todayAttendance = attendanceService.getTodayAttendance(employee.getUserId());
        
        // then
        assertThat(todayAttendance).isNull();
    }
    
    @Test
    @DisplayName("월별 근태 기록 조회")
    void getMonthlyAttendance_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 이번 달 여러 날짜에 출퇴근 기록 생성
        for (int i = 0; i < 5; i++) {
            LocalDate workDate = today.minusDays(i);
            if (workDate.getMonth() == today.getMonth()) {
                Attendance attendance = Attendance.builder()
                        .user(employee)
                        .workDate(workDate)
                        .checkInTime(workDate.atTime(9, 0))
                        .checkOutTime(workDate.atTime(18, 0))
                        .build();
                attendanceRepository.save(attendance);
            }
        }
        
        // when
        List<Attendance> monthlyAttendance = attendanceService.getMonthlyAttendance(
                employee.getUserId(), today.getYear(), today.getMonthValue());
        
        // then
        assertThat(monthlyAttendance).hasSizeGreaterThanOrEqualTo(5);
        assertThat(monthlyAttendance).allMatch(a -> 
                a.getWorkDate().getMonth() == today.getMonth() &&
                a.getWorkDate().getYear() == today.getYear());
    }
    
    @Test
    @DisplayName("월간 근무일수 조회")
    void getMonthlyWorkingDays_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 3일간 출근 기록 생성
        for (int i = 0; i < 3; i++) {
            LocalDate workDate = today.minusDays(i);
            if (workDate.getMonth() == today.getMonth()) {
                Attendance attendance = Attendance.builder()
                        .user(employee)
                        .workDate(workDate)
                        .checkInTime(workDate.atTime(9, 0))
                        .build();
                attendanceRepository.save(attendance);
            }
        }
        
        // when
        long workingDays = attendanceService.getMonthlyWorkingDays(employee.getUserId());
        
        // then
        assertThat(workingDays).isGreaterThanOrEqualTo(3);
    }
    
    @Test
    @DisplayName("월간 총 근무시간 조회")
    void getMonthlyWorkingMinutes_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 8시간 근무 기록 생성
        Attendance attendance = Attendance.builder()
                .user(employee)
                .workDate(today)
                .checkInTime(today.atTime(9, 0))
                .checkOutTime(today.atTime(17, 0))
                .build();
        attendanceRepository.save(attendance);
        
        // when
        long workingMinutes = attendanceService.getMonthlyWorkingMinutes(employee.getUserId());
        
        // then
        assertThat(workingMinutes).isEqualTo(480); // 8시간 = 480분
    }
    
    @Test
    @DisplayName("월간 근무시간 포맷팅")
    void getFormattedMonthlyWorkingHours_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 8시간 30분 근무 기록 생성
        Attendance attendance = Attendance.builder()
                .user(employee)
                .workDate(today)
                .checkInTime(today.atTime(9, 0))
                .checkOutTime(today.atTime(17, 30))
                .build();
        attendanceRepository.save(attendance);
        
        // when
        String formatted = attendanceService.getFormattedMonthlyWorkingHours(employee.getUserId());
        
        // then
        assertThat(formatted).isEqualTo("8시간 30분");
    }
    
    @Test
    @DisplayName("근무 기록이 없을 때 포맷팅")
    void getFormattedMonthlyWorkingHours_NoRecord() {
        // when
        String formatted = attendanceService.getFormattedMonthlyWorkingHours(employee.getUserId());
        
        // then
        assertThat(formatted).isEqualTo("0시간 0분");
    }
    
    @Test
    @DisplayName("주간 근태 기록 조회")
    void getWeeklyAttendance_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 최근 3일간 출근 기록 생성
        for (int i = 0; i < 3; i++) {
            LocalDate workDate = today.minusDays(i);
            Attendance attendance = Attendance.builder()
                    .user(employee)
                    .workDate(workDate)
                    .checkInTime(workDate.atTime(9, 0))
                    .checkOutTime(workDate.atTime(18, 0))
                    .build();
            attendanceRepository.save(attendance);
        }
        
        // when
        List<Attendance> weeklyAttendance = attendanceService.getWeeklyAttendance(employee.getUserId());
        
        // then
        assertThat(weeklyAttendance).hasSize(7); // 일주일치 데이터
        assertThat(weeklyAttendance).filteredOn(a -> a.getCheckInTime() != null).hasSize(3);
    }
    
    @Test
    @DisplayName("근태 상태 확인 - 근무중")
    void getAttendanceStatus_Working() {
        // given
        Attendance attendance = Attendance.builder()
                .user(employee)
                .workDate(LocalDate.now())
                .checkInTime(LocalDateTime.now())
                .build();
        
        // when
        String status = attendanceService.getAttendanceStatus(attendance);
        
        // then
        assertThat(status).isEqualTo("근무중");
    }
    
    @Test
    @DisplayName("근태 상태 확인 - 출근")
    void getAttendanceStatus_Completed() {
        // given
        Attendance attendance = Attendance.builder()
                .user(employee)
                .workDate(LocalDate.now())
                .checkInTime(LocalDateTime.now().minusHours(8))
                .checkOutTime(LocalDateTime.now())
                .build();
        
        // when
        String status = attendanceService.getAttendanceStatus(attendance);
        
        // then
        assertThat(status).isEqualTo("출근");
    }
    
    @Test
    @DisplayName("근태 상태 확인 - 빈 상태")
    void getAttendanceStatus_Empty() {
        // given
        Attendance attendance = Attendance.builder()
                .user(employee)
                .workDate(LocalDate.now())
                .build();
        
        // when
        String status = attendanceService.getAttendanceStatus(attendance);
        
        // then
        assertThat(status).isEqualTo("");
    }
    
    @Test
    @DisplayName("상태별 뱃지 클래스 반환")
    void getStatusBadgeClass_Success() {
        // when & then
        assertThat(attendanceService.getStatusBadgeClass("출근")).isEqualTo("badge-success");
        assertThat(attendanceService.getStatusBadgeClass("근무중")).isEqualTo("badge-info");
        assertThat(attendanceService.getStatusBadgeClass("")).isEqualTo("badge-secondary");
    }
    
    @Test
    @DisplayName("월간 결근일수 조회")
    void getMonthlyAbsentDays_Success() {
        // given
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        
        // 이번달 평일 중 일부만 출근
        int attendanceDays = 0;
        for (int i = 1; i <= today.getDayOfMonth(); i++) {
            LocalDate date = currentMonth.atDay(i);
            // 평일이고 3일에 한번만 출근
            if (date.getDayOfWeek().getValue() < 6 && i % 3 == 0) {
                Attendance attendance = Attendance.builder()
                        .user(employee)
                        .workDate(date)
                        .checkInTime(date.atTime(9, 0))
                        .checkOutTime(date.atTime(18, 0))
                        .build();
                attendanceRepository.save(attendance);
                attendanceDays++;
            }
        }
        
        // when
        long absentDays = attendanceService.getMonthlyAbsentDays(employee.getUserId());
        
        // then
        assertThat(absentDays).isGreaterThanOrEqualTo(0);
        
        // 전체 평일 수 계산
        long totalWorkdays = 0;
        for (int i = 1; i <= today.getDayOfMonth(); i++) {
            LocalDate date = currentMonth.atDay(i);
            if (date.getDayOfWeek().getValue() < 6) {
                totalWorkdays++;
            }
        }
        
        assertThat(absentDays).isEqualTo(totalWorkdays - attendanceDays);
    }
    
    @Test
    @DisplayName("월간 출근일수 조회")
    void getMonthlyAttendanceDays_Success() {
        // given
        LocalDate today = LocalDate.now();
        
        // 5일간 출근 기록 생성
        int expectedDays = 0;
        for (int i = 0; i < 5; i++) {
            LocalDate workDate = today.minusDays(i);
            if (workDate.getMonth() == today.getMonth()) {
                Attendance attendance = Attendance.builder()
                        .user(employee)
                        .workDate(workDate)
                        .checkInTime(workDate.atTime(9, 0))
                        .build();
                attendanceRepository.save(attendance);
                expectedDays++;
            }
        }
        
        // when
        long attendanceDays = attendanceService.getMonthlyAttendanceDays(employee.getUserId());
        
        // then
        assertThat(attendanceDays).isEqualTo(expectedDays);
    }
}