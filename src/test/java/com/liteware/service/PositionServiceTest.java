package com.liteware.service;

import com.liteware.model.dto.PositionDto;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.organization.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {
    
    @Mock
    private PositionRepository positionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PositionService positionService;
    
    private Position position;
    private PositionDto positionDto;
    
    @BeforeEach
    void setUp() {
        position = Position.builder()
                .positionId(1L)
                .positionName("대리")
                .positionCode("ASST_MGR")
                .positionLevel(3)
                .sortOrder(3)
                .description("Assistant Manager")
                .isActive(true)
                .build();
        
        positionDto = PositionDto.builder()
                .positionName("과장")
                .positionCode("MGR")
                .positionLevel(4)
                .sortOrder(4)
                .description("Manager")
                .build();
    }
    
    @Test
    @DisplayName("직급을 생성할 수 있어야 한다")
    void createPosition() {
        when(positionRepository.existsByPositionCode("MGR")).thenReturn(false);
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position pos = invocation.getArgument(0);
            pos.setPositionId(2L);
            return pos;
        });
        
        Position created = positionService.createPosition(positionDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getPositionId()).isEqualTo(2L);
        assertThat(created.getPositionName()).isEqualTo("과장");
        assertThat(created.getPositionCode()).isEqualTo("MGR");
        assertThat(created.getPositionLevel()).isEqualTo(4);
        
        verify(positionRepository).existsByPositionCode("MGR");
        verify(positionRepository).save(any(Position.class));
    }
    
    @Test
    @DisplayName("중복된 직급 코드로 생성 시 예외가 발생해야 한다")
    void createPositionWithDuplicateCode() {
        when(positionRepository.existsByPositionCode("MGR")).thenReturn(true);
        
        assertThatThrownBy(() -> positionService.createPosition(positionDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 직급 코드입니다");
    }
    
    @Test
    @DisplayName("직급 정보를 수정할 수 있어야 한다")
    void updatePosition() {
        PositionDto updateDto = PositionDto.builder()
                .positionName("차장")
                .description("Deputy General Manager")
                .sortOrder(5)
                .build();
        
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);
        
        Position updated = positionService.updatePosition(1L, updateDto);
        
        assertThat(updated.getPositionName()).isEqualTo("차장");
        assertThat(updated.getDescription()).isEqualTo("Deputy General Manager");
        assertThat(updated.getSortOrder()).isEqualTo(5);
        
        verify(positionRepository).findById(1L);
        verify(positionRepository).save(position);
    }
    
    @Test
    @DisplayName("직급을 삭제(비활성화)할 수 있어야 한다")
    void deletePosition() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.countUsersByPositionId(1L)).thenReturn(0L);
        
        positionService.deletePosition(1L);
        
        assertThat(position.getIsActive()).isFalse();
        verify(positionRepository).save(position);
    }
    
    @Test
    @DisplayName("사용자가 있는 직급은 삭제할 수 없어야 한다")
    void cannotDeletePositionWithUsers() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.countUsersByPositionId(1L)).thenReturn(10L);
        
        assertThatThrownBy(() -> positionService.deletePosition(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 직급을 가진 사용자가 있어 삭제할 수 없습니다");
    }
    
    @Test
    @DisplayName("모든 활성 직급 목록을 조회할 수 있어야 한다")
    void getAllActivePositions() {
        Position position2 = Position.builder()
                .positionId(2L)
                .positionName("과장")
                .positionCode("MGR")
                .positionLevel(4)
                .sortOrder(4)
                .isActive(true)
                .build();
        
        List<Position> positions = Arrays.asList(position, position2);
        when(positionRepository.findByIsActiveTrueOrderBySortOrder()).thenReturn(positions);
        
        List<Position> result = positionService.getAllActivePositions();
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting("positionName").containsExactly("대리", "과장");
    }
    
    @Test
    @DisplayName("직급 레벨별로 목록을 조회할 수 있어야 한다")
    void getPositionsByLevel() {
        List<Position> positions = Arrays.asList(position);
        when(positionRepository.findByPositionLevel(3)).thenReturn(positions);
        
        List<Position> result = positionService.getPositionsByLevel(3);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPositionLevel()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("직급별 사용자 수를 조회할 수 있어야 한다")
    void getUserCountByPosition() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.countUsersByPositionId(1L)).thenReturn(25L);
        
        Long count = positionService.getUserCountByPosition(1L);
        
        assertThat(count).isEqualTo(25L);
    }
    
    @Test
    @DisplayName("직급별 사용자 목록을 조회할 수 있어야 한다")
    void getUsersByPosition() {
        List<User> users = Arrays.asList(
                User.builder().userId(1L).name("김대리").position(position).build(),
                User.builder().userId(2L).name("박대리").position(position).build()
        );
        
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(userRepository.findByPosition(position)).thenReturn(users);
        
        List<User> result = positionService.getUsersByPosition(1L);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("김대리", "박대리");
    }
    
    @Test
    @DisplayName("직급 검색을 할 수 있어야 한다")
    void searchPositions() {
        List<Position> positions = Arrays.asList(position);
        when(positionRepository.searchByPositionName("대리")).thenReturn(positions);
        
        List<Position> result = positionService.searchPositions("대리");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPositionName()).isEqualTo("대리");
    }
    
    @Test
    @DisplayName("직급 순서를 변경할 수 있어야 한다")
    void updatePositionOrder() {
        Position position2 = Position.builder()
                .positionId(2L)
                .positionName("과장")
                .positionCode("MGR")
                .sortOrder(4)
                .isActive(true)
                .build();
        
        List<Long> newOrder = Arrays.asList(2L, 1L);
        
        when(positionRepository.findById(2L)).thenReturn(Optional.of(position2));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.saveAll(anyList())).thenReturn(Arrays.asList(position2, position));
        
        positionService.updatePositionOrder(newOrder);
        
        verify(positionRepository).saveAll(anyList());
    }
}