package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PerformanceDTO;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.repository.PerformanceRepository;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AuditLogService;
import com.limiti.employee.service.PerformanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 绩效管理控制器。
 *
 * <p>管理员录入业绩分/能力分/态度分，后端按 0.6/0.2/0.2 加权并扣减出勤分得到综合得分与等级，
 * 等级决定季度奖金系数（见薪酬规则）。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@RestController
@RequestMapping("/api/performances")
@Api(tags = "绩效管理接口")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping
    @ApiOperation("创建绩效评估")
    public Result<Performance> createPerformance(@RequestBody PerformanceDTO dto, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        Employee employee = employeeRepository.findById(dto.getEmployeeId() == null ? -1L : dto.getEmployeeId())
                .orElseThrow(() -> BusinessException.notFound("被评估的员工不存在"));
        validate(dto);
        if (performanceRepository.findByEmployee_IdAndYearAndQuarter(employee.getId(), dto.getYear(), dto.getQuarter())
                .isPresent()) {
            throw BusinessException.conflict(dto.getYear() + " 年第 " + dto.getQuarter()
                    + " 季度已录入过该员工的业绩，请直接修改");
        }
        Performance performance = new Performance();
        performance.setYear(dto.getYear());
        performance.setQuarter(dto.getQuarter());
        performance.setScore(dto.getScore());
        performance.setAbilityScore(dto.getAbilityScore());
        performance.setAttitudeScore(dto.getAttitudeScore());
        performance.setEvaluation(dto.getEvaluation());
        performance.setEmployee(employee);
        performance.setEvaluator(admin);
        performance.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        performance.setMonth(dto.getMonth());
        performance.setWorkScore(dto.getWorkScore());
        Performance created = performanceService.createPerformance(performance);
        auditLogService.record(AuditLogService.PERFORMANCE_SAVE, "录入绩效",
                "PERFORMANCE", created.getId(), employee.getName(),
                String.format("%d 年 Q%d：业绩 %.1f，能力 %s，态度 %s → 综合 %.1f（%s）",
                        created.getYear(), created.getQuarter(), created.getScore(),
                        text(created.getAbilityScore()), text(created.getAttitudeScore()),
                        created.getFinalScore(), created.getGrade()));
        return Result.success("录入成功", created);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新绩效评估")
    public Result<Performance> updatePerformance(@PathVariable Long id,
                                                 @RequestBody PerformanceDTO dto,
                                                 HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        Performance performance = performanceService.getPerformanceById(id);
        if (performance == null) {
            throw BusinessException.notFound("绩效记录不存在");
        }
        validate(dto);
        performance.setYear(dto.getYear());
        performance.setQuarter(dto.getQuarter());
        performance.setScore(dto.getScore());
        performance.setAbilityScore(dto.getAbilityScore());
        performance.setAttitudeScore(dto.getAttitudeScore());
        performance.setEvaluation(dto.getEvaluation());
        performance.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getMonth() != null) {
            performance.setMonth(dto.getMonth());
        }
        if (dto.getWorkScore() != null) {
            performance.setWorkScore(dto.getWorkScore());
        }
        Performance updated = performanceService.updatePerformance(performance);
        String employeeName = updated.getEmployee() == null ? "-" : updated.getEmployee().getName();
        auditLogService.record(AuditLogService.PERFORMANCE_SAVE, "修改绩效",
                "PERFORMANCE", updated.getId(), employeeName,
                String.format("%d 年 Q%d：业绩 %.1f，能力 %s，态度 %s → 综合 %.1f（%s）",
                        updated.getYear(), updated.getQuarter(), updated.getScore(),
                        text(updated.getAbilityScore()), text(updated.getAttitudeScore()),
                        updated.getFinalScore(), updated.getGrade()));
        return Result.success("修改成功", updated);
    }

    @GetMapping
    @ApiOperation("获取所有绩效记录")
    public Result<List<Performance>> getAllPerformances(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(performanceService.getAllPerformances());
    }

    @GetMapping("/{id}")
    @ApiOperation("根据 id 获取绩效记录")
    public Result<Performance> getPerformanceById(@PathVariable Long id, HttpServletRequest request) {
        Performance performance = performanceService.getPerformanceById(id);
        if (performance == null) {
            throw BusinessException.notFound("未找到对应的绩效记录");
        }
        Long ownerId = performance.getEmployee() == null ? null : performance.getEmployee().getId();
        SessionUser.requireAccessEmployee(request, ownerId);
        return Result.success(performance);
    }

    /**
     * 我的历史绩效（员工本人）
     */
    @GetMapping("/my")
    @ApiOperation("我的历史绩效")
    public Result<List<Performance>> myPerformances(HttpServletRequest request) {
        Employee employee = SessionUser.requireSelf(request);
        return Result.success(performanceRepository.findByEmployee_IdOrderByYearDescQuarterDesc(employee.getId()));
    }

    private void validate(PerformanceDTO dto) {
        if (dto.getYear() == null) {
            throw BusinessException.badRequest("请填写年份");
        }
        if (dto.getYear() < 2000 || dto.getYear() > 2100) {
            throw BusinessException.badRequest("年份必须在 2000 到 2100 之间");
        }
        if (dto.getQuarter() == null || dto.getQuarter() < 1 || dto.getQuarter() > 4) {
            throw BusinessException.badRequest("季度必须是 1 到 4");
        }
        if (dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 100) {
            throw BusinessException.badRequest("业绩评分必须在 0 到 100 之间");
        }
        if (dto.getAbilityScore() != null && (dto.getAbilityScore() < 0 || dto.getAbilityScore() > 100)) {
            throw BusinessException.badRequest("能力评分必须在 0 到 100 之间");
        }
        if (dto.getAttitudeScore() != null && (dto.getAttitudeScore() < 0 || dto.getAttitudeScore() > 100)) {
            throw BusinessException.badRequest("态度评分必须在 0 到 100 之间");
        }
    }

    private String text(Double value) {
        return value == null ? "默认" : String.format("%.1f", value);
    }
}
