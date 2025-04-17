package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Attendance;
import com.limiti.employee.service.AttendanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤管理控制器
 * 处理员工考勤相关的请求，包括签到、签退和考勤记录查询
 * @author Limiti
 * @date 2024/11/20
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@Api(tags = "考勤管理接口")
public class AttendanceController {
    
    /** 考勤服务 */
    @Autowired
    private AttendanceService attendanceService;

    /**
     * 员工签到
     * @param employeeId 员工ID
     * @return 签到记录
     */
    @PostMapping("/check-in/{employeeId}")
    @ApiOperation("员工签到")
    public Result<Attendance> checkIn(@PathVariable Long employeeId) {
        try {
            log.info("员工签到请求, employeeId: {}", employeeId);
            Attendance attendance = attendanceService.checkIn(employeeId);
            log.info("签到成功: {}", attendance);
            return Result.success(attendance);
        } catch (Exception e) {
            log.error("签到失败, employeeId: {}", employeeId, e);
            return Result.error("签到失败：" + e.getMessage());
        }
    }

    /**
     * 员工签退
     * @param attendanceId 考勤记录ID
     * @return 更新后的考勤记录
     */
    @PostMapping("/check-out/{attendanceId}")
    @ApiOperation("员工签退")
    @ResponseBody
    public Result<Attendance> checkOut(@PathVariable Long attendanceId) {
        try {
            log.info("员工签退请求, attendanceId: {}", attendanceId);
            Attendance attendance = attendanceService.checkOut(attendanceId);
            log.info("签退成功: {}", attendance);
            return Result.success(attendance);
        } catch (Exception e) {
            log.error("签退失败, attendanceId: {}", attendanceId, e);
            return Result.error("签退失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工考勤记录
     * @param employeeId 员工ID
     * @return 考勤记录列表
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation("获取员工考勤记录")
    public Result<List<Attendance>> getEmployeeAttendance(@PathVariable Long employeeId) {
        try {
            log.info("获取员工考勤记录, employeeId: {}", employeeId);
            List<Attendance> attendances = attendanceService.findByEmployeeId(employeeId);
            return Result.success(attendances);
        } catch (Exception e) {
            log.error("获取考勤记录失败, employeeId: {}", employeeId, e);
            return Result.error("获取考勤记录失败：" + e.getMessage());
        }
    }
} 