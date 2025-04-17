package com.limiti.employee.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Controller
public class IndexController {
    
    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }

    @GetMapping("/pages/employee-center")
    public String employeeCenter() {
        return "pages/employee-center.html";
    }

    @GetMapping("/pages/leave")
    public String leave() {
        return "pages/leave.html";
    }

    @GetMapping("/pages/notice")
    public String notice() {
        return "pages/notice.html";
    }
} 