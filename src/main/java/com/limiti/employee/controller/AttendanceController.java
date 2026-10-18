package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.AttendanceSummaryDTO;
import com.limiti.employee.dto.DailyAttendanceDTO;
import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.repository.AttendanceRepository;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AttendanceService;
import com.limiti.employee.service.AttendanceStatsService;
import com.limiti.employee.exception.BusinessException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理控制器：签到、签退、考勤记录、月度统计与每日明细。
 *
 * <p>统计不落库，每次按「工作日 × 打卡记录 × 已批准请假」实时计算，
 * 所以管理员撤销请假后，考勤与工资立刻同步变化。</p>
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/attendance")
@Api(tags = "考勤管理接口")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/check-in/{employeeId}")
    @ApiOperation("员工签到")
    public Result<Attendance> checkIn(@PathVariable Long employeeId, HttpServletRequest request) {
        SessionUser.requireAccessEmployee(request, employeeId);
        return Result.success(attendanceService.checkIn(employeeId));
    }

    @PostMapping("/check-out/{attendanceId}")
    @ApiOperation("员工签退")
    public Result<Attendance> checkOut(@PathVariable Long attendanceId, HttpServletRequest request) {
        Attendance target = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> BusinessException.notFound("考勤记录不存在"));
        Long ownerId = target.getEmployee() == null ? null : target.getEmployee().getId();
        SessionUser.requireAccessEmployee(request, ownerId);
        return Result.success(attendanceService.checkOut(attendanceId));
    }

    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取员工考勤记录")
    public Result<List<Attendance>> getEmployeeAttendance(@PathVariable Long employeeId,
                                                          HttpServletRequest request) {
        SessionUser.requireAccessEmployee(request, employeeId);
        return Result.success(attendanceService.findByEmployeeId(employeeId));
    }

    /**
     * 月度考勤汇总：应出勤、实际出勤、迟到、早退、旷工、各类请假、加班小时
     */
    @GetMapping("/summary")
    @ApiOperation("月度考勤汇总")
    public Result<AttendanceSummaryDTO> summary(@RequestParam(required = false) Long employeeId,
                                                @RequestParam(required = false) Integer year,
                                                @RequestParam(required = false) Integer month,
                                                HttpServletRequest request) {
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        Long targetId = employeeId;
        if (targetId == null) {
            targetId = SessionUser.currentEmployeeId(request);
        }
        if (targetId == null) {
            throw BusinessException.badRequest("请指定员工");
        }
        SessionUser.requireAccessEmployee(request, targetId);
        Employee employee = employeeRepository.findById(targetId)
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        return Result.success(attendanceStatsService.buildForEmployee(employee, y, m));
    }

    /**
     * 全员月度考勤汇总（管理员）
     */
    @GetMapping("/summary/all")
    @ApiOperation("全员月度考勤汇总")
    public Result<List<AttendanceSummaryDTO>> summaryAll(@RequestParam(required = false) Integer year,
                                                         @RequestParam(required = false) Integer month,
                                                         HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        List<Employee> employees = employeeRepository.findByIsDel(false);
        Map<Long, AttendanceSummaryDTO> map = attendanceStatsService.buildForEmployees(employees, y, m);
        List<AttendanceSummaryDTO> result = new ArrayList<>();
        for (Employee employee : employees) {
            AttendanceSummaryDTO summary = map.get(employee.getId());
            if (summary != null) {
                result.add(summary);
            }
        }
        return Result.success(result);
    }

    /**
     * 每日明细：这个月每一天是正常 / 迟到 / 早退 / 旷工 / 请假
     */
    @GetMapping("/daily")
    @ApiOperation("月度每日考勤明细")
    public Result<List<DailyAttendanceDTO>> daily(@RequestParam(required = false) Long employeeId,
                                                  @RequestParam(required = false) Integer year,
                                                  @RequestParam(required = false) Integer month,
                                                  HttpServletRequest request) {
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        Long targetId = employeeId == null ? SessionUser.currentEmployeeId(request) : employeeId;
        if (targetId == null) {
            throw BusinessException.badRequest("请指定员工");
        }
        SessionUser.requireAccessEmployee(request, targetId);
        Employee employee = employeeRepository.findById(targetId)
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        return Result.success(attendanceStatsService.dailyDetail(employee, y, m));
    }
}
