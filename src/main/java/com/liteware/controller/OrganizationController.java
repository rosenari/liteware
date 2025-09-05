package com.liteware.controller;

import com.liteware.service.user.UserService;
import com.liteware.service.department.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrganizationController {
    
    private final UserService userService;
    private final DepartmentService departmentService;
    
    @GetMapping
    public String organizationChart(Model model) {
        try {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("users", userService.getAllUsers());
        } catch (Exception e) {
            log.error("Error loading organization data", e);
        }
        
        return "organization/chart";
    }
}