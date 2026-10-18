package com.limiti.employee.service;

import com.limiti.employee.dto.SalaryReportDTO;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.repository.LeaveRepository;
import com.limiti.employee.repository.PerformanceRepository;
import com.limiti.employee.utils.WorkdayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 月度工资报告服务
 *
 * <p>计算规则：</p>
 * <ul>
 *   <li>当月工作日：{@link WorkdayUtils#getWorkdaysInMonth(int, int)}（排除周末与内置节假日）</li>
 *   <li>请假天数：当月所有“已通过”请假与该月重叠的自然日天数（整段都在本月时用登记的 leaves_day）</li>
 *   <li>奖金：月薪 × 绩效系数，系数由该季度绩效评分决定：
 *       评分 ≥90 → 20%，≥80 → 15%，≥70 → 10%，≥60 → 5%，其余或无业绩记录 → 0</li>
 *   <li>实际工资：月薪 − 月薪÷当月工作日×请假天数 + 奖金（不小于 0，四舍五入保留两位）</li>
 * </ul>
 *
 * @author Limiti
 */
@Service
public class SalaryReportService {

    /** 已通过的请假状态 */
    private static final int LEAVE_STATUS_APPROVED = 1;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    /**
     * 生成指定年月的工资报告（管理员视图，全部在职员工）
     *
     * @param year  年份
     * @param month 月份（1-12）
     * @return 员工维度的工资明细，按工号升序
     */
    public List<SalaryReportDTO> buildReport(int year, int month) {
        int quarter = quarterOf(month);
        int workDays = WorkdayUtils.getWorkdaysInMonth(year, month);
        Map<Long, Integer> leavesByEmployee = leavesInMonth(year, month);

        List<SalaryReportDTO> rows = new ArrayList<>();
        for (Employee employee : employeeRepository.findByIsDel(false)) {
            rows.add(rowOf(employee, year, month, quarter, workDays, leavesByEmployee));
        }
        rows.sort(Comparator.comparing(SalaryReportDTO::getEmployeeNumber,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    /**
     * 生成单个员工指定年月的工资明细（员工自查视图）
     *
     * @param employeeId 员工 ID
     * @param year       年份
     * @param month      月份（1-12）
     * @return 工资明细；员工不存在或已离职时返回 null
     */
    public SalaryReportDTO buildEmployeeReport(Long employeeId, int year, int month) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || Boolean.TRUE.equals(employee.getIsDel())) {
            return null;
        }
        return rowOf(employee, year, month, quarterOf(month),
                WorkdayUtils.getWorkdaysInMonth(year, month), leavesInMonth(year, month));
    }

    private int quarterOf(int month) {
        return (month - 1) / 3 + 1;
    }

    /**
     * 统计当月各员工「已通过」的请假天数
     */
    private Map<Long, Integer> leavesInMonth(int year, int month) {
        Map<Long, Integer> result = new HashMap<>();
        for (Leave leave : leaveRepository.findByStatus(LEAVE_STATUS_APPROVED)) {
            if (leave.getEmployee() == null || leave.getEmployee().getId() == null) {
                continue;
            }
            int days = overlapDays(leave, year, month);
            if (days > 0) {
                result.merge(leave.getEmployee().getId(), days, Integer::sum);
            }
        }
        return result;
    }

    /**
     * 组装一名员工在指定年月的工资明细
     */
    private SalaryReportDTO rowOf(Employee employee, int year, int month, int quarter,
                                  int workDays, Map<Long, Integer> leavesByEmployee) {
        BigDecimal wage = employee.getWage() == null ? BigDecimal.ZERO : employee.getWage();
        int leavesDay = leavesByEmployee.getOrDefault(employee.getId(), 0);

        Performance performance = performanceRepository
                .findByEmployee_IdAndYearAndQuarter(employee.getId(), year, quarter)
                .orElse(null);
        Double score = performance == null ? null : performance.getScore();
        BigDecimal bonus = bonusOf(wage, score);

        SalaryReportDTO row = new SalaryReportDTO();
        row.setEmployeeId(employee.getId());
        row.setEmployeeNumber(employee.getEmployeeNumber());
        row.setName(employee.getName());
        row.setDepartment(employee.getDepartment());
        row.setPosition(employee.getPosition());
        row.setYearNum(year);
        row.setMonthNum(month);
        row.setWorkDay(workDays);
        row.setWage(wage.setScale(2, RoundingMode.HALF_UP));
        row.setLeavesDay(leavesDay);
        row.setPerformanceScore(score);
        row.setPerformanceId(performance == null ? null : performance.getId());
        row.setBonus(bonus);
        row.setNowWage(nowWageOf(wage, workDays, leavesDay, bonus));
        return row;
    }

    /**
     * 按绩效评分换算奖金比例
     */
    private BigDecimal bonusRate(Double score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        if (score >= 90) {
            return new BigDecimal("0.20");
        }
        if (score >= 80) {
            return new BigDecimal("0.15");
        }
        if (score >= 70) {
            return new BigDecimal("0.10");
        }
        if (score >= 60) {
            return new BigDecimal("0.05");
        }
        return BigDecimal.ZERO;
    }

    /**
     * 奖金 = 月薪 × 绩效系数
     */
    private BigDecimal bonusOf(BigDecimal wage, Double score) {
        BigDecimal rate = bonusRate(score);
        if (rate.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return wage.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 实际工资 = 月薪 − 月薪÷工作日×请假天数 + 奖金
     */
    private BigDecimal nowWageOf(BigDecimal wage, int workDays, int leavesDay, BigDecimal bonus) {
        BigDecimal daily = workDays > 0
                ? wage.divide(BigDecimal.valueOf(workDays), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal deduction = daily.multiply(BigDecimal.valueOf(leavesDay));
        BigDecimal nowWage = wage.subtract(deduction).add(bonus);
        if (nowWage.signum() < 0) {
            nowWage = BigDecimal.ZERO;
        }
        return nowWage.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 请假记录与指定年月重叠的自然日天数
     */
    private int overlapDays(Leave leave, int year, int month) {
        LocalDate start = toLocalDate(leave.getStartTime());
        LocalDate end = toLocalDate(leave.getEndTime());
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        LocalDate from = start.isBefore(first) ? first : start;
        LocalDate to = end.isAfter(last) ? last : end;
        if (from.isAfter(to)) {
            return 0;
        }
        boolean wholeInMonth = !start.isBefore(first) && !end.isAfter(last);
        if (wholeInMonth && leave.getLeavesDay() != null && leave.getLeavesDay() > 0) {
            return leave.getLeavesDay();
        }
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
