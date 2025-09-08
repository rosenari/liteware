package com.liteware.service.organization;

import com.liteware.model.dto.PositionDto;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.repository.PositionRepository;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionServiceTest extends BaseServiceTest {
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private PositionRepository positionRepository;
    
    @BeforeEach
    void setUp() {
        // BaseServiceTest에서 이미 position이 생성됨
    }
    
    @Test
    @DisplayName("직급 생성 성공")
    void createPosition_Success() {
        // given
        PositionDto dto = new PositionDto();
        dto.setPositionCode("MANAGER");
        dto.setPositionName("과장");
        dto.setPositionLevel(3);
        dto.setDescription("중간 관리자");
        dto.setSortOrder(3);
        
        // when
        Position created = positionService.createPosition(dto);
        
        // then
        assertThat(created).isNotNull();
        assertThat(created.getPositionCode()).isEqualTo("MANAGER");
        assertThat(created.getPositionName()).isEqualTo("과장");
        assertThat(created.getPositionLevel()).isEqualTo(3);
        assertThat(created.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("중복 직급 코드로 생성 시 예외 발생")
    void createPosition_DuplicateCode_ThrowsException() {
        // given
        PositionDto dto1 = new PositionDto();
        dto1.setPositionCode("DUP_CODE");
        dto1.setPositionName("직급1");
        dto1.setPositionLevel(1);
        dto1.setSortOrder(1);
        positionService.createPosition(dto1);
        
        // when & then
        PositionDto dto2 = new PositionDto();
        dto2.setPositionCode("DUP_CODE");
        dto2.setPositionName("직급2");
        dto2.setPositionLevel(2);
        dto2.setSortOrder(2);
        
        assertThatThrownBy(() -> positionService.createPosition(dto2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 직급 코드입니다");
    }
    
    @Test
    @DisplayName("직급 정보 수정 성공")
    void updatePosition_Success() {
        // given
        Position pos = createAndSavePosition("UPDATE_TEST", "수정전", 1);
        
        PositionDto updateDto = new PositionDto();
        updateDto.setPositionName("수정후");
        updateDto.setPositionLevel(5);
        updateDto.setDescription("수정된 설명");
        updateDto.setSortOrder(10);
        updateDto.setIsActive(true);
        
        // when
        Position updated = positionService.updatePosition(pos.getPositionId(), updateDto);
        
        // then
        assertThat(updated.getPositionName()).isEqualTo("수정후");
        assertThat(updated.getPositionLevel()).isEqualTo(5);
        assertThat(updated.getDescription()).isEqualTo("수정된 설명");
        assertThat(updated.getSortOrder()).isEqualTo(10);
        assertThat(updated.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("존재하지 않는 직급 수정 시 예외 발생")
    void updatePosition_NotFound_ThrowsException() {
        // given
        PositionDto dto = new PositionDto();
        dto.setPositionName("수정");
        
        // when & then
        assertThatThrownBy(() -> positionService.updatePosition(999999L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("직급을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("직급 삭제(비활성화) 성공")
    void deletePosition_Success() {
        // given
        Position pos = createAndSavePosition("DELETE_TEST", "삭제테스트", 1);
        
        // when
        positionService.deletePosition(pos.getPositionId());
        
        // then
        Position deleted = positionRepository.findById(pos.getPositionId()).orElseThrow();
        assertThat(deleted.getIsActive()).isFalse();
    }
    
    @Test
    @DisplayName("사용자가 있는 직급 삭제 시 예외 발생")
    void deletePosition_HasUsers_ThrowsException() {
        // given
        Position pos = createAndSavePosition("POS_WITH_USER", "사용자있는직급", 1);
        User user = createUser("pos_user", "사용자", "posuser@example.com", department, pos);
        user.addRole(userRole);
        userRepository.save(user);
        
        // when & then
        assertThatThrownBy(() -> positionService.deletePosition(pos.getPositionId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 직급을 가진 사용자가 있어 삭제할 수 없습니다");
    }
    
    @Test
    @DisplayName("직급 조회 성공")
    void getPosition_Success() {
        // given
        Position pos = createAndSavePosition("GET_TEST", "조회테스트", 1);
        
        // when
        Position found = positionService.getPosition(pos.getPositionId());
        
        // then
        assertThat(found).isNotNull();
        assertThat(found.getPositionId()).isEqualTo(pos.getPositionId());
        assertThat(found.getPositionCode()).isEqualTo("GET_TEST");
    }
    
    @Test
    @DisplayName("존재하지 않는 직급 조회 시 예외 발생")
    void getPosition_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> positionService.getPosition(999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("직급을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("활성 직급 목록 조회")
    void getAllActivePositions_Success() {
        // given
        createAndSavePosition("POS1", "직급1", 1);
        createAndSavePosition("POS2", "직급2", 2);
        
        Position inactive = createAndSavePosition("INACTIVE", "비활성직급", 3);
        inactive.setIsActive(false);
        positionRepository.save(inactive);
        
        // when
        List<Position> positions = positionService.getAllActivePositions();
        
        // then
        assertThat(positions).hasSizeGreaterThanOrEqualTo(2);
        assertThat(positions).allMatch(Position::getIsActive);
        assertThat(positions).noneMatch(p -> p.getPositionCode().equals("INACTIVE"));
    }
    
    @Test
    @DisplayName("레벨별 직급 조회")
    void getPositionsByLevel_Success() {
        // given
        createAndSavePosition("LEVEL1_A", "레벨1-A", 1);
        createAndSavePosition("LEVEL1_B", "레벨1-B", 1);
        createAndSavePosition("LEVEL2", "레벨2", 2);
        
        // when
        List<Position> level1Positions = positionService.getPositionsByLevel(1);
        
        // then
        assertThat(level1Positions).hasSizeGreaterThanOrEqualTo(2);
        assertThat(level1Positions).allMatch(p -> p.getPositionLevel().equals(1));
    }
    
    @Test
    @DisplayName("직급별 사용자 수 조회")
    void getUserCountByPosition_Success() {
        // given
        Position pos = createAndSavePosition("COUNT_TEST", "카운트테스트", 1);
        User user1 = createUser("count_user1", "사용자1", "cu1@example.com", department, pos);
        User user2 = createUser("count_user2", "사용자2", "cu2@example.com", department, pos);
        user1.addRole(userRole);
        user2.addRole(userRole);
        userRepository.save(user1);
        userRepository.save(user2);
        
        // when
        Long count = positionService.getUserCountByPosition(pos.getPositionId());
        
        // then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    @DisplayName("직급별 사용자 목록 조회")
    void getUsersByPosition_Success() {
        // given
        Position pos = createAndSavePosition("USER_POS", "사용자직급", 1);
        User user1 = createUser("pos_user1", "사용자1", "pu1@example.com", department, pos);
        User user2 = createUser("pos_user2", "사용자2", "pu2@example.com", department, pos);
        user1.addRole(userRole);
        user2.addRole(userRole);
        userRepository.save(user1);
        userRepository.save(user2);
        
        // when
        List<User> users = positionService.getUsersByPosition(pos.getPositionId());
        
        // then
        assertThat(users).hasSize(2);
        assertThat(users).anyMatch(u -> u.getLoginId().equals("pos_user1"));
        assertThat(users).anyMatch(u -> u.getLoginId().equals("pos_user2"));
    }
    
    @Test
    @DisplayName("직급명으로 검색")
    void searchPositions_Success() {
        // given
        createAndSavePosition("SEARCH1", "개발팀장", 5);
        createAndSavePosition("SEARCH2", "개발매니저", 4);
        createAndSavePosition("SEARCH3", "영업팀장", 5);
        
        // when
        List<Position> results = positionService.searchPositions("개발");
        
        // then
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results).allMatch(p -> p.getPositionName().contains("개발"));
    }
    
    @Test
    @DisplayName("직급 순서 변경")
    void updatePositionOrder_Success() {
        // given
        Position pos1 = createAndSavePosition("ORDER1", "순서1", 1);
        Position pos2 = createAndSavePosition("ORDER2", "순서2", 1);
        Position pos3 = createAndSavePosition("ORDER3", "순서3", 1);
        
        List<Long> newOrder = Arrays.asList(
                pos3.getPositionId(),
                pos1.getPositionId(),
                pos2.getPositionId()
        );
        
        // when
        positionService.updatePositionOrder(newOrder);
        
        // then
        Position updated1 = positionRepository.findById(pos1.getPositionId()).orElseThrow();
        Position updated2 = positionRepository.findById(pos2.getPositionId()).orElseThrow();
        Position updated3 = positionRepository.findById(pos3.getPositionId()).orElseThrow();
        
        assertThat(updated3.getSortOrder()).isEqualTo(1);
        assertThat(updated1.getSortOrder()).isEqualTo(2);
        assertThat(updated2.getSortOrder()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("사용자 수 포함 직급 목록 조회")
    void getAllPositionsWithUserCount_Success() {
        // given
        Position pos = createAndSavePosition("WITH_COUNT", "카운트포함", 1);
        User user = createUser("wc_user", "사용자", "wc@example.com", department, pos);
        user.addRole(userRole);
        userRepository.save(user);
        
        // when
        List<PositionDto> positionsWithCount = positionService.getAllPositionsWithUserCount();
        
        // then
        assertThat(positionsWithCount).isNotEmpty();
        PositionDto foundDto = positionsWithCount.stream()
                .filter(dto -> dto.getPositionCode().equals("WITH_COUNT"))
                .findFirst()
                .orElse(null);
        
        assertThat(foundDto).isNotNull();
        assertThat(foundDto.getUserCount()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("직급 상태 변경 - 활성화")
    void changePositionStatus_Activate_Success() {
        // given
        Position pos = createAndSavePosition("STATUS_TEST", "상태테스트", 1);
        pos.setIsActive(false);
        positionRepository.save(pos);
        
        // when
        Position changed = positionService.changePositionStatus(pos.getPositionId(), true);
        
        // then
        assertThat(changed.getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("직급 상태 변경 - 비활성화")
    void changePositionStatus_Deactivate_Success() {
        // given
        Position pos = createAndSavePosition("DEACT_TEST", "비활성테스트", 1);
        
        // when
        Position changed = positionService.changePositionStatus(pos.getPositionId(), false);
        
        // then
        assertThat(changed.getIsActive()).isFalse();
    }
    
    @Test
    @DisplayName("사용자가 있는 직급 비활성화 시 예외 발생")
    void changePositionStatus_HasUsers_ThrowsException() {
        // given
        Position pos = createAndSavePosition("STATUS_USER", "사용자직급", 1);
        User user = createUser("su_user", "사용자", "su@example.com", department, pos);
        user.addRole(userRole);
        userRepository.save(user);
        
        // when & then
        assertThatThrownBy(() -> positionService.changePositionStatus(pos.getPositionId(), false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자가 배정된 직급은 비활성화할 수 없습니다");
    }
    
    // Helper methods
    private Position createAndSavePosition(String code, String name, int level) {
        Position pos = Position.builder()
                .positionCode(code)
                .positionName(name)
                .positionLevel(level)
                .isActive(true)
                .sortOrder(1)
                .build();
        return positionRepository.save(pos);
    }
}