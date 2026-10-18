package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PerformanceDTO;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.PerformanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    private com.limiti.employee.repository.PerformanceRepository performanceRepository;

    /**
     * 创建业绩评估（仅管理员）
     */
    @PostMapping
    @ApiOperation("创建业绩评估")
    public Result<Performance> createPerformance(@RequestBody PerformanceDTO performanceDTO,
                                                 HttpServletRequest request) {
        Admin admin = SessionUser.admin(request);
        if (admin == null) {
            return Result.error(403, "只有管理员才能录入绩效");
        }
        try {
            Employee employee = getEmployee(performanceDTO.getEmployeeId());
            if (employee == null) {
                return Result.error(404, "被评估的员工不存在");
            }
            String invalid = validate(performanceDTO);
            if (invalid != null) {
                return Result.error(invalid);
            }
            if (performanceRepository
                    .findByEmployee_IdAndYearAndQuarter(employee.getId(),
                            performanceDTO.getYear(), performanceDTO.getQuarter())
                    .isPresent()) {
                return Result.error(performanceDTO.getYear() + " 年第 " + performanceDTO.getQuarter()
                        + " 季度已录入过该员工的业绩，请直接修改");
            }

            Performance performance = new Performance();
            performance.setYear(performanceDTO.getYear());
            performance.setQuarter(performanceDTO.getQuarter());
            performance.setScore(performanceDTO.getScore());
            performance.setEvaluation(performanceDTO.getEvaluation());
            performance.setEmployee(employee);
            performance.setEvaluator(admin);
            performance.setStatus(performanceDTO.getStatus() == null ? 1 : performanceDTO.getStatus());
            performance.setMonth(performanceDTO.getMonth());
            performance.setWorkScore(performanceDTO.getWorkScore());
            Performance created = performanceService.createPerformance(performance);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建业绩评估失败", e);
            return Result.error("创建业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 更新业绩评估（仅管理员）
     */
    @PutMapping("/{id}")
    @ApiOperation("更新业绩评估")
    public Result<Performance> updatePerformance(@PathVariable Long id,
                                                 @RequestBody PerformanceDTO performanceDTO,
                                                 HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "只有管理员才能修改绩效");
        }
        try {
            Performance performance = performanceService.getPerformanceById(id);
            if (performance == null) {
                return Result.error("业绩记录不存在");
            }
            String invalid = validate(performanceDTO);
            if (invalid != null) {
                return Result.error(invalid);
            }

            performance.setYear(performanceDTO.getYear());
            performance.setQuarter(performanceDTO.getQuarter());
            performance.setScore(performanceDTO.getScore());
            performance.setEvaluation(performanceDTO.getEvaluation());
            performance.setStatus(performanceDTO.getStatus() == null ? 1 : performanceDTO.getStatus());
            performance.setMonth(performanceDTO.getMonth());
            performance.setWorkScore(performanceDTO.getWorkScore());

            Performance updated = performanceService.updatePerformance(performance);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新业绩评估失败", e);
            return Result.error("更新业绩评估失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有业绩记录（仅管理员）
     */
    @GetMapping
    @ApiOperation("获取所有业绩记录")
    public Result<List<Performance>> getAllPerformances(HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权查看全部业绩记录");
        }
        try {
            List<Performance> performances = performanceService.getAllPerformances();
            return Result.success(performances);
        } catch (Exception e) {
            log.error("获取所有业绩记录失败", e);
            return Result.error("获取所有业绩记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据 id 获取业绩记录（管理员，或被评估的员工本人）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据 id 获取业绩记录")
    public Result<Performance> getPerformanceById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Performance performance = performanceService.getPerformanceById(id);
            if (performance == null) {
                return Result.error(404, "未找到对应的业绩记录");
            }
            Long ownerId = performance.getEmployee() == null ? null : performance.getEmployee().getId();
            if (!SessionUser.canAccessEmployee(request, ownerId)) {
                return Result.error(403, "无权查看他人的业绩记录");
            }
            return Result.success(performance);
        } catch (Exception e) {
            log.error("获取业绩记录失败", e);
            return Result.error("获取业绩记录get失败：" + e.getMessage());
        }
    }

    /**
     * 校验业绩录入参数，返回错误信息；通过时返回 null
     */
    private String validate(PerformanceDTO dto) {
        if (dto.getYear() == null) {
            return "请填写年份";
        }
        if (dto.getQuarter() == null || dto.getQuarter() < 1 || dto.getQuarter() > 4) {
            return "季度必须是 1 到 4";
        }
        if (dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 100) {
            return "绩效评分必须在 0 到 100 之间";
        }
        return null;
    }

    private Employee getEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId).orElse(null);
    }
}
