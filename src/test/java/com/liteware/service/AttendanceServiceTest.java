package com.liteware.service;

import com.liteware.model.entity.Attendance;
import com.liteware.model.entity.User;
import com.liteware.repository.AttendanceRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.attendance.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    
    @Mock
    private AttendanceRepository attendanceRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AttendanceService attendanceService;
    
    private User user;
    private Attendance attendance;
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .loginId("test001")
                .name("홍길동")
                .email("test@example.com")
                .build();
        
        attendance = Attendance.builder()
                .attendanceId(1L)
                .user(user)
                .workDate(LocalDate.now())
                .checkInTime(LocalDateTime.now().withHour(9).withMinute(0))
                .build();
    }
    
    @Test
    @DisplayName("출근 처리를 할 수 있어야 한다")
    void checkIn_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndWorkDate(user, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        attendanceService.checkIn(1L);
        
        verify(attendanceRepository).save(any(Attendance.class));
    }
    
    @Test
    @DisplayName("이미 출근한 경우 예외가 발생해야 한다")
    void checkIn_AlreadyCheckedIn_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndWorkDate(user, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        
        assertThatThrownBy(() -> attendanceService.checkIn(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 출근 처리되었습니다");
    }
    
    @Test
    @DisplayName("퇴근 처리를 할 수 있어야 한다")
    void checkOut_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndWorkDate(user, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        attendanceService.checkOut(1L);
        
        assertThat(attendance.getCheckOutTime()).isNotNull();
        verify(attendanceRepository).save(attendance);
    }
    
    @Test
    @DisplayName("출근 기록이 없으면 퇴근 처리 시 예외가 발생해야 한다")
    void checkOut_NoCheckIn_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndWorkDate(user, LocalDate.now()))
                .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> attendanceService.checkOut(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("출근 기록이 없습니다");
    }
    
    @Test
    @DisplayName("이미 퇴근한 경우 예외가 발생해야 한다")
    void checkOut_AlreadyCheckedOut_ThrowsException() {
        attendance.setCheckOutTime(LocalDateTime.now().withHour(18).withMinute(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndWorkDate(user, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        
        assertThatThrownBy(() -> attendanceService.checkOut(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 퇴근 처리되었습니다");
    }
    
    @Test
    @DisplayName("오늘의 근태 기록을 조회할 수 있어야 한다")
    void getTodayAttendance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findTodayAttendance(user, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        
        Attendance result = attendanceService.getTodayAttendance(1L);
        
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getWorkDate()).isEqualTo(LocalDate.now());
    }
    
    @Test
    @DisplayName("월별 근태 기록을 조회할 수 있어야 한다")
    void getMonthlyAttendance() {
        List<Attendance> attendances = Arrays.asList(attendance);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserAndYearMonth(user, 2025, 1))
                .thenReturn(attendances);
        
        List<Attendance> result = attendanceService.getMonthlyAttendance(1L, 2025, 1);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(attendance);
    }
    
    @Test
    @DisplayName("이번달 총 근무일수를 조회할 수 있어야 한다")
    void getMonthlyWorkingDays() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.countWorkingDaysByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(20L);
        
        long result = attendanceService.getMonthlyWorkingDays(1L);
        
        assertThat(result).isEqualTo(20L);
    }
    
    @Test
    @DisplayName("이번달 총 근무시간을 분 단위로 조회할 수 있어야 한다")
    void getMonthlyWorkingMinutes() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.getTotalWorkingMinutesByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(9600L); // 160시간 = 9600분
        
        long result = attendanceService.getMonthlyWorkingMinutes(1L);
        
        assertThat(result).isEqualTo(9600L);
    }
    
    @Test
    @DisplayName("근무시간이 없으면 0을 반환해야 한다")
    void getMonthlyWorkingMinutes_NoData_ReturnsZero() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.getTotalWorkingMinutesByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(null);
        
        long result = attendanceService.getMonthlyWorkingMinutes(1L);
        
        assertThat(result).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("포맷된 월간 근무시간을 반환할 수 있어야 한다")
    void getFormattedMonthlyWorkingHours() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.getTotalWorkingMinutesByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(545L); // 9시간 5분
        
        String result = attendanceService.getFormattedMonthlyWorkingHours(1L);
        
        assertThat(result).isEqualTo("9시간 5분");
    }
    
    @Test
    @DisplayName("근무시간이 없으면 0시간 0분을 반환해야 한다")
    void getFormattedMonthlyWorkingHours_NoData() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.getTotalWorkingMinutesByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(null);
        
        String result = attendanceService.getFormattedMonthlyWorkingHours(1L);
        
        assertThat(result).isEqualTo("0시간 0분");
    }
    
    @Test
    @DisplayName("최근 일주일 근태 기록을 조회할 수 있어야 한다")
    void getWeeklyAttendance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(6).plusDays(i);
            if (i == 6) { // 오늘
                when(attendanceRepository.findByUserAndWorkDate(user, date))
                        .thenReturn(Optional.of(attendance));
            } else {
                when(attendanceRepository.findByUserAndWorkDate(user, date))
                        .thenReturn(Optional.empty());
            }
        }
        
        List<Attendance> result = attendanceService.getWeeklyAttendance(1L);
        
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("근태 상태를 반환할 수 있어야 한다 - 근무중")
    void getAttendanceStatus_Working() {
        String result = attendanceService.getAttendanceStatus(attendance);
        
        assertThat(result).isEqualTo("근무중");
    }
    
    @Test
    @DisplayName("근태 상태를 반환할 수 있어야 한다 - 출근")
    void getAttendanceStatus_Completed() {
        attendance.setCheckOutTime(LocalDateTime.now().withHour(18).withMinute(0));
        
        String result = attendanceService.getAttendanceStatus(attendance);
        
        assertThat(result).isEqualTo("출근");
    }
    
    @Test
    @DisplayName("근태 상태를 반환할 수 있어야 한다 - 빈값")
    void getAttendanceStatus_Empty() {
        String result = attendanceService.getAttendanceStatus(null);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("상태에 따른 뱃지 클래스를 반환할 수 있어야 한다")
    void getStatusBadgeClass() {
        assertThat(attendanceService.getStatusBadgeClass("출근")).isEqualTo("badge-success");
        assertThat(attendanceService.getStatusBadgeClass("근무중")).isEqualTo("badge-info");
        assertThat(attendanceService.getStatusBadgeClass("")).isEqualTo("badge-secondary");
    }
    
    @Test
    @DisplayName("이번달 출근일수를 조회할 수 있어야 한다")
    void getMonthlyAttendanceDays() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.countWorkingDaysByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(15L);
        
        long result = attendanceService.getMonthlyAttendanceDays(1L);
        
        assertThat(result).isEqualTo(15L);
    }
    
    @Test
    @DisplayName("이번달 결근일수를 조회할 수 있어야 한다")
    void getMonthlyAbsentDays() {
        YearMonth currentMonth = YearMonth.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.countWorkingDaysByUserAndYearMonth(
                user, currentMonth.getYear(), currentMonth.getMonthValue()))
                .thenReturn(10L); // 10일 출근
        
        long result = attendanceService.getMonthlyAbsentDays(1L);
        
        // 결과는 현재 날짜의 평일 수에서 출근일수를 뺀 값
        assertThat(result).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자로 출근 시 예외가 발생해야 한다")
    void checkIn_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> attendanceService.checkIn(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}