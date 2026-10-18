package com.limiti.employee.service;

import com.limiti.employee.dto.AttendanceSummaryDTO;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.repository.PerformanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 绩效服务。
 *
 * <p>评分模型（管理员录入业绩分/能力分/态度分，系统算出综合得分与等级）：</p>
 * <pre>
 * 综合得分 = 0.6 × 业绩分 + 0.2 × 能力分 + 0.2 × 态度分 − 出勤扣分
 * 出勤扣分 = 旷工天数 × 2 + 迟到次数 × 0.5（上限 20 分）
 * 等级：S ≥ 90，A ≥ 80，B ≥ 70，C ≥ 60，D &lt; 60
 * </pre>
 *
 * <p>等级系数决定季度奖金：S/A/B/C/D 分别对应 {@link SalaryPolicy} 里的
 * bonusRateS/A/B/C/D，奖金按月平摊（季度奖金 ÷ 3）。</p>
 *
 * @author Limiti
 */
@Service
public class PerformanceService {

    /** 业绩分权重 */
    public static final double WEIGHT_WORK = 0.6;
    /** 能力分权重 */
    public static final double WEIGHT_ABILITY = 0.2;
    /** 态度分权重 */
    public static final double WEIGHT_ATTITUDE = 0.2;
    /** 出勤扣分上限 */
    public static final double MAX_ATTENDANCE_PENALTY = 20.0;
    /** 每旷工一天扣分 */
    public static final double PENALTY_PER_ABSENT_DAY = 2.0;
    /** 每迟到一次扣分 */
    public static final double PENALTY_PER_LATE = 0.5;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private AttendanceStatsService attendanceStatsService;

    @Autowired
    private SalaryPolicyService salaryPolicyService;

    /**
     * 计算综合得分（不含出勤扣分）
     */
    public static double finalScore(Double work, Double ability, Double attitude) {
        return finalScore(work, ability, attitude, 0D);
    }

    /**
     * 计算综合得分
     *
     * @param work     业绩分，为空时视为 0
     * @param ability  能力分，为空时取业绩分
     * @param attitude 态度分，为空时取业绩分
     * @param penalty  出勤扣分，超过 {@link #MAX_ATTENDANCE_PENALTY} 按上限计
     */
    public static double finalScore(Double work, Double ability, Double attitude, double penalty) {
        double workScore = work == null ? 0D : work;
        double abilityScore = ability == null ? workScore : ability;
        double attitudeScore = attitude == null ? workScore : attitude;
        double raw = WEIGHT_WORK * workScore
                + WEIGHT_ABILITY * abilityScore
                + WEIGHT_ATTITUDE * attitudeScore;
        double deduction = Math.min(Math.max(penalty, 0D), MAX_ATTENDANCE_PENALTY);
        double result = Math.max(raw - deduction, 0D);
        return Math.round(result * 10D) / 10D;
    }

    /**
     * 根据综合得分评级
     */
    public static String gradeOf(Double score) {
        if (score == null) {
            return null;
        }
        if (score >= 90D) {
            return "S";
        }
        if (score >= 80D) {
            return "A";
        }
        if (score >= 70D) {
            return "B";
        }
        if (score >= 60D) {
            return "C";
        }
        return "D";
    }

    /**
     * 等级对应的奖金系数（季薪的比例）
     */
    public static BigDecimal bonusRateOf(String grade, SalaryPolicy policy) {
        if (grade == null || policy == null) {
            return BigDecimal.ZERO;
        }
        switch (grade) {
            case "S":
                return nullSafe(policy.getBonusRateS());
            case "A":
                return nullSafe(policy.getBonusRateA());
            case "B":
                return nullSafe(policy.getBonusRateB());
            case "C":
                return nullSafe(policy.getBonusRateC());
            default:
                return BigDecimal.ZERO;
        }
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 出勤扣分：旷工 + 迟到
     */
    public static double attendancePenalty(AttendanceSummaryDTO summary) {
        if (summary == null) {
            return 0D;
        }
        int absentDays = summary.getAbsentDays() == null ? 0 : summary.getAbsentDays();
        int lateCount = summary.getLateCount() == null ? 0 : summary.getLateCount();
        double penalty = absentDays * PENALTY_PER_ABSENT_DAY + lateCount * PENALTY_PER_LATE;
        return Math.min(penalty, MAX_ATTENDANCE_PENALTY);
    }

    /**
     * 把综合得分与等级写回绩效记录（以季度出勤为基准）
     */
    @Transactional
    public Performance applyEvaluation(Performance performance) {
        if (performance.getEmployee() == null || performance.getYear() == null || performance.getQuarter() == null) {
            return performance;
        }
        Employee employee = performance.getEmployee();
        AttendanceSummaryDTO quarterSummary = attendanceStatsService
                .forQuarter(employee, performance.getYear(), performance.getQuarter());
        double penalty = attendancePenalty(quarterSummary);
        double finalScore = finalScore(performance.getScore(), performance.getAbilityScore(),
                performance.getAttitudeScore(), penalty);
        performance.setFinalScore(finalScore);
        performance.setGrade(gradeOf(finalScore));
        return performanceRepository.save(performance);
    }

    public Performance createPerformance(Performance performance) {
        return applyEvaluation(performance);
    }

    public Performance updatePerformance(Performance performance) {
        return applyEvaluation(performance);
    }

    public List<Performance> getAllPerformances() {
        return performanceRepository.findAll();
    }

    public Performance getPerformanceById(Long id) {
        return performanceRepository.findById(id).orElse(null);
    }
}
