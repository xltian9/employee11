package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PerformanceDTO;
//import com.limiti.employee.dto.PerformanceInfoDTO;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.service.PerformanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import javax.persistence.Id;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

/**
 * 业绩管理控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/performances")
@Api(tags = "业绩管理接口")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HttpServletRequest request;

    /**
     * 创建业绩评估
     */
    @PostMapping
    @ApiOperation("创建业绩评估")
    public Result<Performance> createPerformance(@RequestBody PerformanceDTO performanceDTO) {
        try {
            // 从 session 获取当前管理员信息
            Admin admin = getCurrentAdmin();
            System.out.println("管理员信息：" + admin);
            Employee employee = getEmployee(performanceDTO.getEmployeeId());

            Performance performance = new Performance();
            performance.setYear(performanceDTO.getYear());
            performance.setQuarter(performanceDTO.getQuarter());
            performance.setScore(performanceDTO.getScore());
            performance.setEvaluation(performanceDTO.getEvaluation());
            performance.setEmployee(employee);
            performance.setEvaluator(admin);
            performance.setStatus(performanceDTO.getStatus());
            System.err.println("收到的参数"+performance);
            System.err.println("管理员信息：" + admin);
            Performance created = performanceService.createPerformance(performance);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建业绩评估失败", e);
            return Result.error("创建业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 更新业绩评估
     */
    @PutMapping("/{id}")
    @ApiOperation("更新业绩评估")
    public Result<Performance> updatePerformance(@PathVariable Long id, @RequestBody PerformanceDTO performanceDTO) {
        try {
            Performance performance = performanceService.getPerformanceById(id);
            if (performance == null) {
                return Result.error("业绩记录不存在");
            }

            performance.setYear(performanceDTO.getYear());
            performance.setQuarter(performanceDTO.getQuarter());
            performance.setScore(performanceDTO.getScore());
            performance.setEvaluation(performanceDTO.getEvaluation());
            performance.setStatus(performanceDTO.getStatus());

            Performance updated = performanceService.updatePerformance(performance);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新业绩评估失败", e);
            return Result.error("更新业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有业绩记录
     */
    @GetMapping
    @ApiOperation("获取所有业绩记录")
    public Result<List<Performance>> getAllPerformances() {
        try {
            List<Performance> performances = performanceService.getAllPerformances();
            System.out.println("performanceService.getAllPerformances()");
            return Result.success(performances);
        } catch (Exception e) {
            log.error("获取所有业绩记录失败", e);
            return Result.error("获取所有业绩记录失败：" + e.getMessage());
        }

    }

    @GetMapping("/{id}")
    @ApiOperation("根据 id 获取业绩记录")
    public Result<Performance> getPerformanceById(@PathVariable Long id) {
        try {
            Performance performance = performanceService.getPerformanceById(id);
            if (performance == null) {
                return Result.error("未找到对应的业绩记录");
            }
            return Result.success(performance);
        } catch (Exception e) {
            log.error("获取业绩记录get失败", e);
            return Result.error("获取业绩记录get失败：" + e.getMessage());
        }
    }


    // // 获取当前管理员和员工的方法
     private Admin getCurrentAdmin() {
          HttpSession session = request.getSession(false);
        if (session != null) {
            // 根据登录逻辑，session 中存储管理员信息的键为 "admin"
            return (Admin) session.getAttribute("admin");
        }
        return null;
     }

    private Employee getEmployee(Long employeeId) {
        // 实现根据员工 ID 获取员工信息的逻辑
        return employeeRepository.findById(employeeId).orElse(null);
    }

//    @GetMapping("/list")
//    @ApiOperation("获取所有业绩记录新")
//    public Result<List<PerformanceInfoDTO>> list(@RequestParam Map<String,Object> params) {
//        List<PerformanceInfoDTO> list = performanceService.list(params);
//        return Result.success(list);
//    }
} 