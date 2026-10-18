package com.limiti.employee.service;

import com.limiti.employee.dto.AttendanceSummaryDTO;
import com.limiti.employee.dto.SalaryAdjustmentDTO;
import com.limiti.employee.dto.SalaryReportDTO;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.entity.SalaryAdjustment;
import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.repository.PerformanceRepository;
import com.limiti.employee.repository.SalaryAdjustmentRepository;
import com.limiti.employee.utils.WorkdayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 月度工资报告服务。
 *
 * <p>工资构成（全部规则来自 {@link SalaryPolicy}，管理员可配）：</p>
 * <pre>
 * 应发 = 月薪 − 请假扣款 − 旷工扣款 − 迟到早退扣款 + 加班费 + 季度奖金 + 补发 − 扣款
 * 社保 = 月薪 × 社保比例
 * 公积金 = 月薪 × 公积金比例
 * 个税 = (应发 − 社保 − 公积金 − 起征点) 套用月度超额累进税率
 * 实发 = 应发 − 社保 − 公积金 − 个税
 * </pre>
 *
 * <p>请假扣款：事假按规则全额扣；病假按 sickLeaveRate（默认 50%）扣；年假/调休不扣。</p>
 *
 * @author Limiti
 */
@Slf4j
@Service
public class SalaryReportService {

    /** 季度奖金按月平摊，因此除以 3 */
    private static final BigDecimal MONTHS_PER_QUARTER = new BigDecimal("3");

    /** 每天标准工时 */
    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private SalaryAdjustmentRepository salaryAdjustmentRepository;

    @Autowired
    private SalaryPolicyService salaryPolicyService;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    /**
     * 生成指定年月的工资报告（管理员视图，全部在职员工）
     */
    @Transactional(readOnly = true)
    public List<SalaryReportDTO> buildReport(int year, int month) {
        SalaryPolicy policy = salaryPolicyService.get();
        List<Employee> employees = employeeRepository.findByIsDel(false);
        Map<Long, AttendanceSummaryDTO> stats =
                attendanceStatsService.buildForEmployees(employees, year, month);
        Map<Long, List<SalaryAdjustment>> adjustments = adjustmentsInMonth(year, month);

        List<SalaryReportDTO> rows = new ArrayList<>();
        for (Employee employee : employees) {
            rows.add(rowOf(employee, year, month, policy,
                    stats.get(employee.getId()), adjustments.get(employee.getId())));
        }
        rows.sort(Comparator.comparing(SalaryReportDTO::getEmployeeNumber,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    /**
     * 单个员工的工资明细（员工自查）
     */
    @Transactional(readOnly = true)
    public SalaryReportDTO buildEmployeeReport(Long employeeId, int year, int month) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || Boolean.TRUE.equals(employee.getIsDel())) {
            return null;
        }
        SalaryPolicy policy = salaryPolicyService.get();
        AttendanceSummaryDTO summary = attendanceStatsService.buildForEmployee(employee, year, month);
        List<SalaryAdjustment> list = salaryAdjustmentRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month);
        return rowOf(employee, year, month, policy, summary, groupByEmployee(list).get(employeeId));
    }

    private Map<Long, List<SalaryAdjustment>> adjustmentsInMonth(int year, int month) {
        return groupByEmployee(salaryAdjustmentRepository.findByYearAndMonth(year, month));
    }

    private Map<Long, List<SalaryAdjustment>> groupByEmployee(List<SalaryAdjustment> list) {
        Map<Long, List<SalaryAdjustment>> result = new HashMap<>();
        for (SalaryAdjustment adjustment : list) {
            result.computeIfAbsent(adjustment.getEmployeeId(), key -> new ArrayList<>()).add(adjustment);
        }
        return result;
    }

    /**
     * 组装一名员工的工资明细
     */
    private SalaryReportDTO rowOf(Employee employee, int year, int month, SalaryPolicy policy,
                                  AttendanceSummaryDTO summary, List<SalaryAdjustment> adjustments) {
        SalaryReportDTO row = new SalaryReportDTO();
        row.setEmployeeId(employee.getId());
        row.setEmployeeNumber(employee.getEmployeeNumber());
        row.setName(employee.getName());
        row.setDepartment(employee.getDepartment());
        row.setPosition(employee.getPosition());
        row.setYearNum(year);
        row.setMonthNum(month);

        BigDecimal wage = employee.getWage() == null ? BigDecimal.ZERO : employee.getWage();
        int workDays = summary == null || summary.getWorkDays() == null
                ? WorkdayUtils.getWorkdaysInMonth(year, month) : summary.getWorkDays();
        row.setWorkDay(workDays);
        row.setWage(scale(wage));

        int casualDays = summary == null ? 0 : zero(summary.getCasualLeaveDays());
        int sickDays = summary == null ? 0 : zero(summary.getSickLeaveDays());
        int otherDays = summary == null ? 0 : zero(summary.getOtherLeaveDays());
        int lateCount = summary == null ? 0 : zero(summary.getLateCount());
        int earlyCount = summary == null ? 0 : zero(summary.getEarlyCount());
        int absentDays = summary == null ? 0 : zero(summary.getAbsentDays());

        row.setLeaveDays(casualDays + sickDays + otherDays);
        row.setCasualLeaveDays(casualDays);
        row.setSickLeaveDays(sickDays);
        row.setOtherLeaveDays(otherDays);
        row.setLateCount(lateCount);
        row.setEarlyCount(earlyCount);
        row.setAbsentDays(absentDays);
        row.setActualDays(summary == null ? 0 : zero(summary.getActualDays()));

        BigDecimal dailyWage = workDays > 0
                ? wage.divide(BigDecimal.valueOf(workDays), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal hourlyWage = workDays > 0
                ? wage.divide(BigDecimal.valueOf(workDays).multiply(HOURS_PER_DAY), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 请假扣款：事假全额、病假按比例、年假调休不扣
        BigDecimal leaveDeduct = leaveDeductOf(policy, dailyWage, casualDays, sickDays);
        // 旷工扣款：旷工天数 × 日工资 × 折算倍数
        BigDecimal absentDeduct = scale(dailyWage
                .multiply(BigDecimal.valueOf(absentDays))
                .multiply(nullSafe(policy.getAbsentDeductDays(), BigDecimal.ONE)));
        // 迟到早退扣款：每次固定金额
        BigDecimal lateDeduct = scale(nullSafe(policy.getLateDeductAmount(), BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf((long) lateCount + earlyCount)));

        BigDecimal weekdayHours = summary == null ? BigDecimal.ZERO : nullSafe(summary.getOvertimeWeekdayHours(), BigDecimal.ZERO);
        BigDecimal weekendHours = summary == null ? BigDecimal.ZERO : nullSafe(summary.getOvertimeWeekendHours(), BigDecimal.ZERO);
        BigDecimal holidayHours = summary == null ? BigDecimal.ZERO : nullSafe(summary.getOvertimeHolidayHours(), BigDecimal.ZERO);
        row.setOvertimeWeekdayHours(weekdayHours);
        row.setOvertimeWeekendHours(weekendHours);
        row.setOvertimeHolidayHours(holidayHours);
        BigDecimal overtimePay = scale(hourlyWage.multiply(
                weekdayHours.multiply(nullSafe(policy.getOvertimeWeekdayRate(), BigDecimal.ONE))
                        .add(weekendHours.multiply(nullSafe(policy.getOvertimeWeekendRate(), BigDecimal.ONE)))
                        .add(holidayHours.multiply(nullSafe(policy.getOvertimeHolidayRate(), BigDecimal.ONE)))));

        // 绩效：按季度评定，奖金 = 月薪 × 等级系数 ÷ 3，平摊到季度内每个月
        int quarter = quarterOf(month);
        Performance performance = performanceRepository
                .findByEmployee_IdAndYearAndQuarter(employee.getId(), year, quarter)
                .orElse(null);
        Double finalScore = null;
        String grade = null;
        BigDecimal bonus = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (performance != null) {
            // 出勤扣分按整个季度统计，保证审批/撤销后立刻反映到绩效与奖金
            AttendanceSummaryDTO quarterSummary =
                    attendanceStatsService.forQuarter(employee, year, quarter);
            finalScore = PerformanceService.finalScore(
                    performance.getScore(), performance.getAbilityScore(), performance.getAttitudeScore(),
                    PerformanceService.attendancePenalty(quarterSummary));
            grade = PerformanceService.gradeOf(finalScore);
            BigDecimal rate = PerformanceService.bonusRateOf(grade, policy);
            bonus = scale(wage.multiply(rate).divide(MONTHS_PER_QUARTER, 6, RoundingMode.HALF_UP));
            row.setPerformanceId(performance.getId());
        }
        row.setPerformanceScore(finalScore);
        row.setPerformanceGrade(grade);
        row.setBonus(bonus);

        BigDecimal adjustAdd = BigDecimal.ZERO;
        BigDecimal adjustDeduct = BigDecimal.ZERO;
        if (adjustments != null) {
            for (SalaryAdjustment adjustment : adjustments) {
                BigDecimal amount = nullSafe(adjustment.getAmount(), BigDecimal.ZERO);
                if (Integer.valueOf(SalaryAdjustment.TYPE_ADD).equals(adjustment.getType())) {
                    adjustAdd = adjustAdd.add(amount);
                } else {
                    adjustDeduct = adjustDeduct.add(amount);
                }
                row.getAdjustments().add(toAdjustmentDto(adjustment));
            }
        }
        row.setAdjustAdd(scale(adjustAdd));
        row.setAdjustDeduct(scale(adjustDeduct));

        BigDecimal gross = wage
                .subtract(leaveDeduct)
                .subtract(absentDeduct)
                .subtract(lateDeduct)
                .add(overtimePay)
                .add(bonus)
                .add(adjustAdd)
                .subtract(adjustDeduct);
        if (gross.signum() < 0) {
            gross = BigDecimal.ZERO;
        }
        gross = scale(gross);

        BigDecimal socialInsurance = scale(wage.multiply(nullSafe(policy.getSocialInsuranceRate(), BigDecimal.ZERO)));
        BigDecimal housingFund = scale(wage.multiply(nullSafe(policy.getHousingFundRate(), BigDecimal.ZERO)));
        BigDecimal tax = calcTax(gross.subtract(socialInsurance).subtract(housingFund),
                nullSafe(policy.getTaxThreshold(), new BigDecimal("5000")));

        BigDecimal net = gross.subtract(socialInsurance).subtract(housingFund).subtract(tax);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }
        net = scale(net);

        row.setLeaveDeduct(leaveDeduct);
        row.setAbsentDeduct(absentDeduct);
        row.setLateDeduct(lateDeduct);
        row.setOvertimePay(overtimePay);
        row.setSocialInsurance(socialInsurance);
        row.setHousingFund(housingFund);
        row.setTax(tax);
        row.setGrossPay(gross);
        row.setNetPay(net);
        row.setNowWage(net);
        return row;
    }

    /**
     * 请假扣款：事假全额 + 病假 × 比例
     */
    private BigDecimal leaveDeductOf(SalaryPolicy policy, BigDecimal dailyWage, int casualDays, int sickDays) {
        BigDecimal sickRate = nullSafe(policy.getSickLeaveRate(), BigDecimal.ZERO);
        if (policy.getLeaveDeductMode() != null && policy.getLeaveDeductMode() == 1) {
            BigDecimal perDay = nullSafe(policy.getLeaveDeductAmount(), BigDecimal.ZERO);
            return scale(perDay.multiply(BigDecimal.valueOf(casualDays))
                    .add(perDay.multiply(BigDecimal.valueOf(sickDays)).multiply(sickRate)));
        }
        return scale(dailyWage.multiply(BigDecimal.valueOf(casualDays))
                .add(dailyWage.multiply(BigDecimal.valueOf(sickDays)).multiply(sickRate)));
    }

    /**
     * 月度个税：超额累进，用速算扣除数实现
     */
    private BigDecimal calcTax(BigDecimal taxableBeforeThreshold, BigDecimal threshold) {
        BigDecimal taxable = taxableBeforeThreshold.subtract(threshold);
        if (taxable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double amount = taxable.doubleValue();
        double rate;
        double quickDeduction;
        if (amount <= 3000) {
            rate = 0.03;
            quickDeduction = 0;
        } else if (amount <= 12000) {
            rate = 0.10;
            quickDeduction = 210;
        } else if (amount <= 25000) {
            rate = 0.20;
            quickDeduction = 1410;
        } else if (amount <= 35000) {
            rate = 0.25;
            quickDeduction = 2660;
        } else if (amount <= 55000) {
            rate = 0.30;
            quickDeduction = 4410;
        } else if (amount <= 80000) {
            rate = 0.35;
            quickDeduction = 7160;
        } else {
            rate = 0.45;
            quickDeduction = 15160;
        }
        double tax = amount * rate - quickDeduction;
        if (tax < 0) {
            tax = 0;
        }
        return BigDecimal.valueOf(tax).setScale(2, RoundingMode.HALF_UP);
    }

    private SalaryAdjustmentDTO toAdjustmentDto(SalaryAdjustment adjustment) {
        SalaryAdjustmentDTO dto = new SalaryAdjustmentDTO();
        dto.setId(adjustment.getId());
        dto.setType(adjustment.getType());
        dto.setTypeName(Integer.valueOf(SalaryAdjustment.TYPE_ADD).equals(adjustment.getType()) ? "补发" : "扣款");
        dto.setAmount(adjustment.getAmount());
        dto.setReason(adjustment.getReason());
        dto.setOperatorName(adjustment.getOperatorName());
        dto.setCreateTime(adjustment.getCreateTime() == null
                ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(adjustment.getCreateTime()));
        return dto;
    }

    private int quarterOf(int month) {
        return (month - 1) / 3 + 1;
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nullSafe(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 当月工作日日期（供导出/调试使用）
     */
    public List<LocalDate> workdaysOf(int year, int month) {
        return WorkdayUtils.getWorkdayDates(year, month);
    }
}
