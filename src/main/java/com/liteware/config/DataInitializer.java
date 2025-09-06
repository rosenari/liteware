package com.liteware.config;

import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.Role;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.BoardType;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.RoleRepository;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile({"docker", "dev", "test"})
public class DataInitializer {

    @Bean
    @Transactional
    public CommandLineRunner initData(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PositionRepository positionRepository,
            RoleRepository roleRepository,
            BoardRepository boardRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            log.info("Initializing test data...");
            
            // 권한 생성
            if (roleRepository.count() == 0) {
                Role adminRole = new Role();
                adminRole.setRoleName("ADMIN");
                adminRole.setRoleCode("ROLE_ADMIN");
                adminRole.setDescription("시스템 관리자");
                roleRepository.save(adminRole);
                
                Role managerRole = new Role();
                managerRole.setRoleName("MANAGER");
                managerRole.setRoleCode("ROLE_MANAGER");
                managerRole.setDescription("부서 관리자");
                roleRepository.save(managerRole);
                
                Role userRole = new Role();
                userRole.setRoleName("USER");
                userRole.setRoleCode("ROLE_USER");
                userRole.setDescription("일반 사용자");
                roleRepository.save(userRole);
                
                log.info("Created {} roles", 3);
            }
            
            // 부서 생성
            if (departmentRepository.count() == 0) {
                Department company = new Department();
                company.setDeptName("회사");
                company.setDeptCode("COMPANY");
                company.setDeptLevel(0);
                company.setSortOrder(0);
                company.setActive(true);
                departmentRepository.save(company);
                
                Department dev = new Department();
                dev.setDeptName("개발부");
                dev.setDeptCode("DEV");
                dev.setParentDepartment(company);
                dev.setDeptLevel(1);
                dev.setSortOrder(1);
                dev.setActive(true);
                departmentRepository.save(dev);
                
                Department sales = new Department();
                sales.setDeptName("영업부");
                sales.setDeptCode("SALES");
                sales.setParentDepartment(company);
                sales.setDeptLevel(1);
                sales.setSortOrder(2);
                sales.setActive(true);
                departmentRepository.save(sales);
                
                Department hr = new Department();
                hr.setDeptName("인사부");
                hr.setDeptCode("HR");
                hr.setParentDepartment(company);
                hr.setDeptLevel(1);
                hr.setSortOrder(3);
                hr.setActive(true);
                departmentRepository.save(hr);
                
                log.info("Created {} departments", 4);
            }
            
            // 직급 생성
            if (positionRepository.count() == 0) {
                Position ceo = new Position();
                ceo.setPositionName("대표이사");
                ceo.setPositionCode("CEO");
                ceo.setPositionLevel(1);
                ceo.setSortOrder(1);
                positionRepository.save(ceo);
                
                Position director = new Position();
                director.setPositionName("이사");
                director.setPositionCode("DIRECTOR");
                director.setPositionLevel(2);
                director.setSortOrder(2);
                positionRepository.save(director);
                
                Position manager = new Position();
                manager.setPositionName("부장");
                manager.setPositionCode("MANAGER");
                manager.setPositionLevel(3);
                manager.setSortOrder(3);
                positionRepository.save(manager);
                
                Position assistant = new Position();
                assistant.setPositionName("과장");
                assistant.setPositionCode("ASSISTANT");
                assistant.setPositionLevel(4);
                assistant.setSortOrder(4);
                positionRepository.save(assistant);
                
                Position staff = new Position();
                staff.setPositionName("대리");
                staff.setPositionCode("STAFF");
                staff.setPositionLevel(5);
                staff.setSortOrder(5);
                positionRepository.save(staff);
                
                Position junior = new Position();
                junior.setPositionName("사원");
                junior.setPositionCode("JUNIOR");
                junior.setPositionLevel(6);
                junior.setSortOrder(6);
                positionRepository.save(junior);
                
                log.info("Created {} positions", 6);
            }
            
            // 사용자 생성
            if (userRepository.count() == 0) {
                Department dev = departmentRepository.findByDeptCode("DEV").orElse(null);
                Department sales = departmentRepository.findByDeptCode("SALES").orElse(null);
                Department hr = departmentRepository.findByDeptCode("HR").orElse(null);
                
                Position ceo = positionRepository.findByPositionCode("CEO").orElse(null);
                Position manager = positionRepository.findByPositionCode("MANAGER").orElse(null);
                Position assistant = positionRepository.findByPositionCode("ASSISTANT").orElse(null);
                Position staff = positionRepository.findByPositionCode("STAFF").orElse(null);
                Position junior = positionRepository.findByPositionCode("JUNIOR").orElse(null);
                
                Role adminRole = roleRepository.findByRoleCode("ROLE_ADMIN").orElse(null);
                Role managerRole = roleRepository.findByRoleCode("ROLE_MANAGER").orElse(null);
                Role userRole = roleRepository.findByRoleCode("ROLE_USER").orElse(null);
                
                // 관리자
                User admin = new User();
                admin.setLoginId("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setName("관리자");
                admin.setEmail("admin@liteware.com");
                admin.setPhone("010-1234-5678");
                admin.setDepartment(dev);
                admin.setPosition(ceo);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setHireDate(LocalDate.of(2020, 1, 1));
                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(adminRole);
                admin.setRoles(adminRoles);
                userRepository.save(admin);
                
                // 부서장 (개발부)
                User devManager = new User();
                devManager.setLoginId("devmanager");
                devManager.setPassword(passwordEncoder.encode("password123"));
                devManager.setName("김개발");
                devManager.setEmail("devmanager@liteware.com");
                devManager.setPhone("010-2222-3333");
                devManager.setDepartment(dev);
                devManager.setPosition(manager);
                devManager.setStatus(UserStatus.ACTIVE);
                devManager.setHireDate(LocalDate.of(2021, 3, 15));
                Set<Role> managerRoles = new HashSet<>();
                managerRoles.add(managerRole);
                devManager.setRoles(managerRoles);
                userRepository.save(devManager);
                
                // 일반 사용자 (개발부)
                User devUser = new User();
                devUser.setLoginId("devuser");
                devUser.setPassword(passwordEncoder.encode("password123"));
                devUser.setName("이개발");
                devUser.setEmail("devuser@liteware.com");
                devUser.setPhone("010-3333-4444");
                devUser.setDepartment(dev);
                devUser.setPosition(staff);
                devUser.setStatus(UserStatus.ACTIVE);
                devUser.setHireDate(LocalDate.of(2022, 7, 1));
                Set<Role> userRoles1 = new HashSet<>();
                userRoles1.add(userRole);
                devUser.setRoles(userRoles1);
                userRepository.save(devUser);
                
                // 일반 사용자 (영업부)
                User salesUser = new User();
                salesUser.setLoginId("salesuser");
                salesUser.setPassword(passwordEncoder.encode("password123"));
                salesUser.setName("박영업");
                salesUser.setEmail("salesuser@liteware.com");
                salesUser.setPhone("010-4444-5555");
                salesUser.setDepartment(sales);
                salesUser.setPosition(assistant);
                salesUser.setStatus(UserStatus.ACTIVE);
                salesUser.setHireDate(LocalDate.of(2023, 1, 10));
                Set<Role> userRoles2 = new HashSet<>();
                userRoles2.add(userRole);
                salesUser.setRoles(userRoles2);
                userRepository.save(salesUser);
                
                // 일반 사용자 (인사부)
                User hrUser = new User();
                hrUser.setLoginId("hruser");
                hrUser.setPassword(passwordEncoder.encode("password123"));
                hrUser.setName("최인사");
                hrUser.setEmail("hruser@liteware.com");
                hrUser.setPhone("010-5555-6666");
                hrUser.setDepartment(hr);
                hrUser.setPosition(junior);
                hrUser.setStatus(UserStatus.ACTIVE);
                hrUser.setHireDate(LocalDate.of(2024, 2, 1));
                Set<Role> userRoles3 = new HashSet<>();
                userRoles3.add(userRole);
                hrUser.setRoles(userRoles3);
                userRepository.save(hrUser);
                
                log.info("Created {} users", 5);
            }
            
            // 게시판 생성
            if (boardRepository.count() == 0) {
                Board noticeBoard = Board.builder()
                    .boardName("공지사항")
                    .boardCode("NOTICE")
                    .boardType(BoardType.NOTICE)
                    .description("회사 공지사항 게시판")
                    .sortOrder(1)
                    .useYn(true)
                    .noticeYn(true)
                    .secretYn(false)
                    .attachmentYn(true)
                    .writeAuthLevel(3)  // ADMIN level
                    .readAuthLevel(1)   // USER level
                    .commentAuthLevel(1) // USER level
                    .build();
                boardRepository.save(noticeBoard);
                
                Board freeBoard = Board.builder()
                    .boardName("자유게시판")
                    .boardCode("FREE")
                    .boardType(BoardType.GENERAL)
                    .description("자유롭게 소통하는 게시판")
                    .sortOrder(2)
                    .useYn(true)
                    .noticeYn(false)
                    .secretYn(false)
                    .attachmentYn(true)
                    .writeAuthLevel(1)   // USER level
                    .readAuthLevel(1)    // USER level
                    .commentAuthLevel(1) // USER level
                    .build();
                boardRepository.save(freeBoard);
                
                Board resourceBoard = Board.builder()
                    .boardName("자료실")
                    .boardCode("RESOURCE")
                    .boardType(BoardType.DATA)
                    .description("업무 자료 공유 게시판")
                    .sortOrder(3)
                    .useYn(true)
                    .noticeYn(false)
                    .secretYn(false)
                    .attachmentYn(true)
                    .writeAuthLevel(1)   // USER level
                    .readAuthLevel(1)    // USER level
                    .commentAuthLevel(1) // USER level
                    .build();
                boardRepository.save(resourceBoard);
                
                Board qnaBoard = Board.builder()
                    .boardName("Q&A")
                    .boardCode("QNA")
                    .boardType(BoardType.QNA)
                    .description("질문과 답변 게시판")
                    .sortOrder(4)
                    .useYn(true)
                    .noticeYn(false)
                    .secretYn(false)
                    .attachmentYn(true)
                    .writeAuthLevel(1)   // USER level
                    .readAuthLevel(1)    // USER level
                    .commentAuthLevel(1) // USER level
                    .build();
                boardRepository.save(qnaBoard);
                
                log.info("Created {} boards", 4);
            }
            
            log.info("Test data initialization completed!");
        };
    }
}