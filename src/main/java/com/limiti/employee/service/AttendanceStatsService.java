package com.limiti.employee.service;

import com.limiti.employee.dto.AttendanceSummaryDTO;
import com.limiti.employee.dto.DailyAttendanceDTO;
import com.limiti.employee.entity.Attendance;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.entity.OvertimeRecord;
import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.repository.AttendanceRepository;
import com.limiti.employee.repository.LeaveRepository;
import com.limiti.employee.repository.OvertimeRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤统计服务：把「打卡记录 + 已批准请假 + 加班记录」合成为月度考勤汇总。
 *
 * <p>核心思路是**实时比对**而不是预生成：把当月每个工作日逐个过一遍，有请假算请假、
 * 有打卡判迟到早退、两者都没有算旷工。好处是请假审批/撤销后立刻生效，
 * 也不会出现"定时任务跑失败导致考勤数据错一天"的问题。</p>
 *
 * @author Limiti
 */
@Slf4j
@Service
public class AttendanceStatsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** 请假类型：1-事假，2-病假，3-年假，4-调休 */
    private static final int LEAVE_TYPE_CASUAL = 1;
    private static final int LEAVE_TYPE_SICK = 2;

    /** 已通过 */
    private static final int LEAVE_STATUS_APPROVED = 1;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private OvertimeRecordRepository overtimeRecordRepository;

    @Autowired
    private SalaryPolicyService salaryPolicyService;

    /**
     * 单个员工的月度考勤汇总
     */
    public AttendanceSummaryDTO buildForEmployee(Employee employee, int year, int month) {
        return buildForEmployees(Collections.singletonList(employee), year, month).get(employee.getId());
    }

    /**
     * 批量统计（工资报告用，避免 N+1 查询）
     */
    @Transactional(readOnly = true)
    public Map<Long, AttendanceSummaryDTO> buildForEmployees(List<Employee> employees, int year, int month) {
        SalaryPolicy policy = salaryPolicyService.get();
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        LocalDate today = LocalDate.now();

        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = last.plusDays(1).atStartOfDay();

        Map<Long, List<Attendance>> attendancesByEmployee = new HashMap<>();
        for (Attendance attendance : attendanceRepository.findByCheckInTimeBetween(from, to)) {
            if (attendance.getEmployee() == null || attendance.getEmployee().getId() == null
                    || attendance.getCheckInTime() == null) {
                continue;
            }
            attendancesByEmployee
                    .computeIfAbsent(attendance.getEmployee().getId(), key -> new ArrayList<>())
                    .add(attendance);
        }

        List<Leave> approvedLeaves = leaveRepository.findByStatus(LEAVE_STATUS_APPROVED);

        Map<Long, List<OvertimeRecord>> overtimeByEmployee = new HashMap<>();
        for (OvertimeRecord record : overtimeRecordRepository.findByWorkDateBetween(first, last)) {
            overtimeByEmployee
                    .computeIfAbsent(record.getEmployeeId(), key -> new ArrayList<>())
                    .add(record);
        }

        List<LocalDate> workdays = com.limiti.employee.utils.WorkdayUtils.getWorkdayDates(year, month);
        LocalTime workStart = parseTime(policy.getWorkStart(), LocalTime.of(9, 0));
        LocalTime workEnd = parseTime(policy.getWorkEnd(), LocalTime.of(18, 0));

        Map<Long, AttendanceSummaryDTO> result = new HashMap<>();
        for (Employee employee : employees) {
            AttendanceSummaryDTO summary = new AttendanceSummaryDTO();
            summary.setEmployeeId(employee.getId());
            summary.setEmployeeNumber(employee.getEmployeeNumber());
            summary.setName(employee.getName());
            summary.setYearNum(year);
            summary.setMonthNum(month);
            summary.setWorkDays(workdays.size());

            LocalDate entryDate = toLocalDate(employee.getEntryDate());
            List<Attendance> ownAttendances =
                    attendancesByEmployee.getOrDefault(employee.getId(), Collections.emptyList());
            List<OvertimeRecord> ownOvertimes =
                    overtimeByEmployee.getOrDefault(employee.getId(), Collections.emptyList());

            for (LocalDate date : workdays) {
                if (entryDate != null && date.isBefore(entryDate)) {
                    continue;
                }
                Leave leave = findLeaveCovering(approvedLeaves, employee.getId(), date);
                Attendance attendance = findAttendance(ownAttendances, date);
                // 已批准的请假即使日期未到也计入（工资按批准结果预扣），
                // 但"未到且无请假"的未来日期既不算旷工也不算出勤。
                if (leave != null) {
                    summary.setLeaveDays(summary.getLeaveDays() + 1);
                    if (leave.getType() != null && leave.getType() == LEAVE_TYPE_CASUAL) {
                        summary.setCasualLeaveDays(summary.getCasualLeaveDays() + 1);
                    } else if (leave.getType() != null && leave.getType() == LEAVE_TYPE_SICK) {
                        summary.setSickLeaveDays(summary.getSickLeaveDays() + 1);
                    } else {
                        summary.setOtherLeaveDays(summary.getOtherLeaveDays() + 1);
                    }
                    continue;
                }
                if (date.isAfter(today)) {
                    continue;
                }
                if (attendance == null) {
                    summary.setAbsentDays(summary.getAbsentDays() + 1);
                    continue;
                }
                summary.setActualDays(summary.getActualDays() + 1);
                boolean late = attendance.getCheckInTime() != null
                        && attendance.getCheckInTime().toLocalTime().isAfter(workStart);
                boolean early = attendance.getCheckOutTime() != null
                        && attendance.getCheckOutTime().toLocalTime().isBefore(workEnd);
                if (late) {
                    summary.setLateCount(summary.getLateCount() + 1);
                }
                if (early) {
                    summary.setEarlyCount(summary.getEarlyCount() + 1);
                }
                if (!late && !early) {
                    summary.setNormalDays(summary.getNormalDays() + 1);
                }
            }

            BigDecimal weekdayHours = BigDecimal.ZERO;
            BigDecimal weekendHours = BigDecimal.ZERO;
            BigDecimal holidayHours = BigDecimal.ZERO;
            for (OvertimeRecord record : ownOvertimes) {
                BigDecimal hours = record.getHours() == null ? BigDecimal.ZERO : record.getHours();
                if (record.getOtType() != null && record.getOtType() == OvertimeRecord.TYPE_WEEKEND) {
                    weekendHours = weekendHours.add(hours);
                } else if (record.getOtType() != null && record.getOtType() == OvertimeRecord.TYPE_HOLIDAY) {
                    holidayHours = holidayHours.add(hours);
                } else {
                    weekdayHours = weekdayHours.add(hours);
                }
            }
            summary.setOvertimeWeekdayHours(weekdayHours);
            summary.setOvertimeWeekendHours(weekendHours);
            summary.setOvertimeHolidayHours(holidayHours);
            summary.setOvertimeHours(weekdayHours.add(weekendHours).add(holidayHours));

            result.put(employee.getId(), summary);
        }
        return result;
    }

    /**
     * 季度考勤汇总（绩效出勤系数用）
     */
    public AttendanceSummaryDTO forQuarter(Employee employee, int year, int quarter) {
        AttendanceSummaryDTO total = null;
        for (int i = 0; i < 3; i++) {
            int month = (quarter - 1) * 3 + i + 1;
            AttendanceSummaryDTO part = buildForEmployee(employee, year, month);
            if (total == null) {
                total = part;
                total.setMonthNum(null);
            } else {
                total.setWorkDays(total.getWorkDays() + part.getWorkDays());
                total.setActualDays(total.getActualDays() + part.getActualDays());
                total.setNormalDays(total.getNormalDays() + part.getNormalDays());
                total.setLateCount(total.getLateCount() + part.getLateCount());
                total.setEarlyCount(total.getEarlyCount() + part.getEarlyCount());
                total.setAbsentDays(total.getAbsentDays() + part.getAbsentDays());
                total.setLeaveDays(total.getLeaveDays() + part.getLeaveDays());
                total.setCasualLeaveDays(total.getCasualLeaveDays() + part.getCasualLeaveDays());
                total.setSickLeaveDays(total.getSickLeaveDays() + part.getSickLeaveDays());
                total.setOtherLeaveDays(total.getOtherLeaveDays() + part.getOtherLeaveDays());
                total.setOvertimeWeekdayHours(total.getOvertimeWeekdayHours().add(part.getOvertimeWeekdayHours()));
                total.setOvertimeWeekendHours(total.getOvertimeWeekendHours().add(part.getOvertimeWeekendHours()));
                total.setOvertimeHolidayHours(total.getOvertimeHolidayHours().add(part.getOvertimeHolidayHours()));
                total.setOvertimeHours(total.getOvertimeHours().add(part.getOvertimeHours()));
            }
        }
        return total;
    }

    /**
     * 月度考勤明细（每一天的状态），用于考勤页面
     */
    @Transactional(readOnly = true)
    public List<DailyAttendanceDTO> dailyDetail(Employee employee, int year, int month) {
        SalaryPolicy policy = salaryPolicyService.get();
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = last.plusDays(1).atStartOfDay();
        LocalDate today = LocalDate.now();
        LocalDate entryDate = toLocalDate(employee.getEntryDate());

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeAndCheckInTimeBetween(employee, from, to);
        List<Leave> leaves = leaveRepository.findByStatus(LEAVE_STATUS_APPROVED);
        LocalTime workStart = parseTime(policy.getWorkStart(), LocalTime.of(9, 0));
        LocalTime workEnd = parseTime(policy.getWorkEnd(), LocalTime.of(18, 0));

        List<DailyAttendanceDTO> details = new ArrayList<>();
        for (LocalDate date : com.limiti.employee.utils.WorkdayUtils.getWorkdayDates(year, month)) {
            DailyAttendanceDTO item = new DailyAttendanceDTO();
            item.setDate(date.format(DATE_FORMAT));
            Leave leave = findLeaveCovering(leaves, employee.getId(), date);
            Attendance attendance = findAttendance(attendances, date);
            if (attendance != null) {
                item.setCheckIn(attendance.getCheckInTime() == null
                        ? null : attendance.getCheckInTime().format(TIME_FORMAT));
                item.setCheckOut(attendance.getCheckOutTime() == null
                        ? null : attendance.getCheckOutTime().format(TIME_FORMAT));
            }
            if (entryDate != null && date.isBefore(entryDate)) {
                item.setStatus("入职前");
            } else if (leave != null) {
                item.setStatus("请假");
                item.setLeaveType(leaveTypeName(leave.getType()));
                item.setNote(leave.getReason());
            } else if (date.isAfter(today)) {
                item.setStatus("未到");
            } else if (attendance == null) {
                item.setStatus("旷工");
            } else {
                boolean late = attendance.getCheckInTime() != null
                        && attendance.getCheckInTime().toLocalTime().isAfter(workStart);
                boolean early = attendance.getCheckOutTime() != null
                        && attendance.getCheckOutTime().toLocalTime().isBefore(workEnd);
                if (late && early) {
                    item.setStatus("迟到+早退");
                } else if (late) {
                    item.setStatus("迟到");
                } else if (early) {
                    item.setStatus("早退");
                } else {
                    item.setStatus("正常");
                }
            }
            details.add(item);
        }
        return details;
    }

    /**
     * 当月已批准请假与指定日期是否重叠
     */
    private Leave findLeaveCovering(List<Leave> leaves, Long employeeId, LocalDate date) {
        for (Leave leave : leaves) {
            if (leave.getEmployee() == null || !employeeId.equals(leave.getEmployee().getId())) {
                continue;
            }
            LocalDate start = toLocalDate(leave.getStartTime());
            LocalDate end = toLocalDate(leave.getEndTime());
            if (start == null || end == null) {
                continue;
            }
            if (!date.isBefore(start) && !date.isAfter(end)) {
                return leave;
            }
        }
        return null;
    }

    private Attendance findAttendance(List<Attendance> attendances, LocalDate date) {
        for (Attendance attendance : attendances) {
            if (attendance.getCheckInTime() != null
                    && attendance.getCheckInTime().toLocalDate().equals(date)) {
                return attendance;
            }
        }
        return null;
    }

    private String leaveTypeName(Integer type) {
        if (type == null) {
            return "请假";
        }
        switch (type) {
            case 1:
                return "事假";
            case 2:
                return "病假";
            case 3:
                return "年假";
            case 4:
                return "调休";
            default:
                return "请假";
        }
    }

    private LocalTime parseTime(String text, LocalTime fallback) {
        try {
            return text == null ? fallback : LocalTime.parse(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }
}
