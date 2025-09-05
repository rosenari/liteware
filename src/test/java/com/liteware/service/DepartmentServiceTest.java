package com.liteware.service;

import com.liteware.model.dto.DepartmentDto;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.User;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.organization.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private DepartmentService departmentService;
    
    private Department department;
    private Department parentDepartment;
    private DepartmentDto departmentDto;
    
    @BeforeEach
    void setUp() {
        parentDepartment = Department.builder()
                .deptId(1L)
                .deptName("본사")
                .deptCode("HQ")
                .deptLevel(1)
                .isActive(true)
                .build();
        
        department = Department.builder()
                .deptId(2L)
                .deptName("개발부")
                .deptCode("DEV")
                .parentDepartment(parentDepartment)
                .deptLevel(2)
                .sortOrder(1)
                .isActive(true)
                .build();
        
        departmentDto = DepartmentDto.builder()
                .deptName("신규부서")
                .deptCode("NEW")
                .parentDeptId(1L)
                .sortOrder(2)
                .description("신규 부서 설명")
                .build();
    }
    
    @Test
    @DisplayName("부서를 생성할 수 있어야 한다")
    void createDepartment() {
        when(departmentRepository.existsByDeptCode("NEW")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setDeptId(3L);
            return dept;
        });
        
        Department created = departmentService.createDepartment(departmentDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getDeptId()).isEqualTo(3L);
        assertThat(created.getDeptName()).isEqualTo("신규부서");
        assertThat(created.getDeptCode()).isEqualTo("NEW");
        assertThat(created.getParentDepartment().getDeptId()).isEqualTo(1L);
        
        verify(departmentRepository).existsByDeptCode("NEW");
        verify(departmentRepository).save(any(Department.class));
    }
    
    @Test
    @DisplayName("중복된 부서 코드로 생성 시 예외가 발생해야 한다")
    void createDepartmentWithDuplicateCode() {
        when(departmentRepository.existsByDeptCode("NEW")).thenReturn(true);
        
        assertThatThrownBy(() -> departmentService.createDepartment(departmentDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 부서 코드입니다");
    }
    
    @Test
    @DisplayName("부서 정보를 수정할 수 있어야 한다")
    void updateDepartment() {
        DepartmentDto updateDto = DepartmentDto.builder()
                .deptName("개발팀")
                .description("개발팀 설명")
                .sortOrder(2)
                .build();
        
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        
        Department updated = departmentService.updateDepartment(2L, updateDto);
        
        assertThat(updated.getDeptName()).isEqualTo("개발팀");
        assertThat(updated.getDescription()).isEqualTo("개발팀 설명");
        assertThat(updated.getSortOrder()).isEqualTo(2);
        
        verify(departmentRepository).findById(2L);
        verify(departmentRepository).save(department);
    }
    
    @Test
    @DisplayName("부서를 삭제(비활성화)할 수 있어야 한다")
    void deleteDepartment() {
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.countActiveUsersByDepartmentId(2L)).thenReturn(0L);
        when(departmentRepository.countSubDepartments(2L)).thenReturn(0L);
        
        departmentService.deleteDepartment(2L);
        
        assertThat(department.getIsActive()).isFalse();
        verify(departmentRepository).save(department);
    }
    
    @Test
    @DisplayName("사용자가 있는 부서는 삭제할 수 없어야 한다")
    void cannotDeleteDepartmentWithUsers() {
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.countActiveUsersByDepartmentId(2L)).thenReturn(5L);
        
        assertThatThrownBy(() -> departmentService.deleteDepartment(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("소속된 사용자가 있는 부서는 삭제할 수 없습니다");
    }
    
    @Test
    @DisplayName("하위 부서가 있는 부서는 삭제할 수 없어야 한다")
    void cannotDeleteDepartmentWithSubDepartments() {
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.countActiveUsersByDepartmentId(2L)).thenReturn(0L);
        when(departmentRepository.countSubDepartments(2L)).thenReturn(3L);
        
        assertThatThrownBy(() -> departmentService.deleteDepartment(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("하위 부서가 있는 부서는 삭제할 수 없습니다");
    }
    
    @Test
    @DisplayName("부서 트리 구조를 조회할 수 있어야 한다")
    void getDepartmentTree() {
        Department subDept = Department.builder()
                .deptId(3L)
                .deptName("백엔드팀")
                .deptCode("BACKEND")
                .parentDepartment(department)
                .deptLevel(3)
                .isActive(true)
                .build();
        
        parentDepartment.getSubDepartments().add(department);
        department.getSubDepartments().add(subDept);
        
        when(departmentRepository.findByParentDepartmentIsNull()).thenReturn(Arrays.asList(parentDepartment));
        
        List<Department> tree = departmentService.getDepartmentTree();
        
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getDeptName()).isEqualTo("본사");
        assertThat(tree.get(0).getSubDepartments()).hasSize(1);
        assertThat(tree.get(0).getSubDepartments().get(0).getDeptName()).isEqualTo("개발부");
    }
    
    @Test
    @DisplayName("부서별 사용자 목록을 조회할 수 있어야 한다")
    void getUsersByDepartment() {
        List<User> users = Arrays.asList(
                User.builder().userId(1L).name("홍길동").build(),
                User.builder().userId(2L).name("김철수").build()
        );
        
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.findByDepartment(department)).thenReturn(users);
        
        List<User> result = departmentService.getUsersByDepartment(2L);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("홍길동", "김철수");
    }
    
    @Test
    @DisplayName("부서 검색을 할 수 있어야 한다")
    void searchDepartments() {
        List<Department> departments = Arrays.asList(department);
        when(departmentRepository.searchByDeptName("개발")).thenReturn(departments);
        
        List<Department> result = departmentService.searchDepartments("개발");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptName()).isEqualTo("개발부");
    }
    
    @Test
    @DisplayName("부서장을 지정할 수 있어야 한다")
    void assignDepartmentManager() {
        User manager = User.builder()
                .userId(1L)
                .name("팀장")
                .department(department)
                .build();
        
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        
        Department updated = departmentService.assignManager(2L, 1L);
        
        assertThat(updated.getManager()).isEqualTo(manager);
        verify(departmentRepository).save(department);
    }
    
    @Test
    @DisplayName("다른 부서 소속 사용자는 부서장이 될 수 없어야 한다")
    void cannotAssignManagerFromOtherDepartment() {
        Department otherDept = Department.builder()
                .deptId(99L)
                .deptName("다른부서")
                .build();
        
        User manager = User.builder()
                .userId(1L)
                .name("외부인")
                .department(otherDept)
                .build();
        
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        
        assertThatThrownBy(() -> departmentService.assignManager(2L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 부서 소속이 아닌 사용자는 부서장이 될 수 없습니다");
    }
    
    @Test
    @DisplayName("부서를 이동할 수 있어야 한다")
    void moveDepartment() {
        Department newParent = Department.builder()
                .deptId(10L)
                .deptName("신규본부")
                .deptLevel(1)
                .isActive(true)
                .build();
        
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(newParent));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        
        Department moved = departmentService.moveDepartment(2L, 10L);
        
        assertThat(moved.getParentDepartment()).isEqualTo(newParent);
        verify(departmentRepository).save(department);
    }
}