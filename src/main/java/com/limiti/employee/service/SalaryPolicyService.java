package com.limiti.employee.service;

import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.SalaryPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪酬规则服务：请假扣款、迟到扣款、社保公积金比例、个税起征点、加班费率、绩效奖金系数。
 *
 * <p>规则存库（单行 singleton），管理员在「系统设置 → 薪酬规则」修改后立即生效，
 * 修改动作会写审计日志。</p>
 *
 * @author Limiti
 */
@Slf4j
@Service
public class SalaryPolicyService {

    @Autowired
    private SalaryPolicyRepository salaryPolicyRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 读取当前规则；库里还没有时按默认值初始化
     */
    @Transactional
    public SalaryPolicy get() {
        return salaryPolicyRepository.findById(SalaryPolicy.SINGLETON_ID).orElseGet(() -> {
            SalaryPolicy policy = new SalaryPolicy();
            policy.setId(SalaryPolicy.SINGLETON_ID);
            policy.setUpdateTime(new Date());
            policy.setUpdateBy("系统初始化");
            log.info("初始化薪酬规则默认配置");
            return salaryPolicyRepository.save(policy);
        });
    }

    /**
     * 更新规则（整体覆盖，带范围校验与审计）
     */
    @Transactional
    public SalaryPolicy update(SalaryPolicy incoming, String operatorName) {
        SalaryPolicy policy = get();

        StringBuilder detail = new StringBuilder();
        appendChange(detail, "请假扣款方式", policy.getLeaveDeductMode(), incoming.getLeaveDeductMode());
        appendChange(detail, "固定扣款金额", policy.getLeaveDeductAmount(), incoming.getLeaveDeductAmount());
        appendChange(detail, "病假扣款比例", policy.getSickLeaveRate(), incoming.getSickLeaveRate());
        appendChange(detail, "旷工折算天数", policy.getAbsentDeductDays(), incoming.getAbsentDeductDays());
        appendChange(detail, "迟到扣款", policy.getLateDeductAmount(), incoming.getLateDeductAmount());
        appendChange(detail, "工作日加班倍数", policy.getOvertimeWeekdayRate(), incoming.getOvertimeWeekdayRate());
        appendChange(detail, "周末加班倍数", policy.getOvertimeWeekendRate(), incoming.getOvertimeWeekendRate());
        appendChange(detail, "节假日加班倍数", policy.getOvertimeHolidayRate(), incoming.getOvertimeHolidayRate());
        appendChange(detail, "社保比例", policy.getSocialInsuranceRate(), incoming.getSocialInsuranceRate());
        appendChange(detail, "公积金比例", policy.getHousingFundRate(), incoming.getHousingFundRate());
        appendChange(detail, "个税起征点", policy.getTaxThreshold(), incoming.getTaxThreshold());
        appendChange(detail, "S 级奖金系数", policy.getBonusRateS(), incoming.getBonusRateS());
        appendChange(detail, "A 级奖金系数", policy.getBonusRateA(), incoming.getBonusRateA());
        appendChange(detail, "B 级奖金系数", policy.getBonusRateB(), incoming.getBonusRateB());
        appendChange(detail, "C 级奖金系数", policy.getBonusRateC(), incoming.getBonusRateC());
        appendChange(detail, "D 级奖金系数", policy.getBonusRateD(), incoming.getBonusRateD());
        appendChange(detail, "上班时间", policy.getWorkStart(), incoming.getWorkStart());
        appendChange(detail, "下班时间", policy.getWorkEnd(), incoming.getWorkEnd());
        appendChange(detail, "迟到转旷工分钟", policy.getLateToAbsentMinutes(), incoming.getLateToAbsentMinutes());

        validate(incoming);

        policy.setLeaveDeductMode(incoming.getLeaveDeductMode());
        policy.setLeaveDeductAmount(incoming.getLeaveDeductAmount());
        policy.setSickLeaveRate(incoming.getSickLeaveRate());
        policy.setAbsentDeductDays(incoming.getAbsentDeductDays());
        policy.setLateDeductAmount(incoming.getLateDeductAmount());
        policy.setOvertimeWeekdayRate(incoming.getOvertimeWeekdayRate());
        policy.setOvertimeWeekendRate(incoming.getOvertimeWeekendRate());
        policy.setOvertimeHolidayRate(incoming.getOvertimeHolidayRate());
        policy.setSocialInsuranceRate(incoming.getSocialInsuranceRate());
        policy.setHousingFundRate(incoming.getHousingFundRate());
        policy.setTaxThreshold(incoming.getTaxThreshold());
        policy.setBonusRateS(incoming.getBonusRateS());
        policy.setBonusRateA(incoming.getBonusRateA());
        policy.setBonusRateB(incoming.getBonusRateB());
        policy.setBonusRateC(incoming.getBonusRateC());
        policy.setBonusRateD(incoming.getBonusRateD());
        policy.setWorkStart(incoming.getWorkStart());
        policy.setWorkEnd(incoming.getWorkEnd());
        policy.setLateToAbsentMinutes(incoming.getLateToAbsentMinutes());
        policy.setUpdateBy(operatorName);
        policy.setUpdateTime(new Date());

        SalaryPolicy saved = salaryPolicyRepository.save(policy);
        auditLogService.record(AuditLogService.POLICY_UPDATE, "修改薪酬规则", "POLICY",
                SalaryPolicy.SINGLETON_ID, "薪酬规则",
                detail.length() == 0 ? "提交了与当前值相同的配置" : detail.toString());
        return saved;
    }

    private void validate(SalaryPolicy p) {
        if (p.getLeaveDeductMode() == null || (p.getLeaveDeductMode() != 0 && p.getLeaveDeductMode() != 1)) {
            throw new BusinessException("请假扣款方式只能是 0（按日工资）或 1（固定金额）");
        }
        checkRate("社保比例", p.getSocialInsuranceRate());
        checkRate("公积金比例", p.getHousingFundRate());
        checkRate("病假扣款比例", p.getSickLeaveRate());
        checkRate("S 级奖金系数", p.getBonusRateS());
        checkRate("A 级奖金系数", p.getBonusRateA());
        checkRate("B 级奖金系数", p.getBonusRateB());
        checkRate("C 级奖金系数", p.getBonusRateC());
        checkRate("D 级奖金系数", p.getBonusRateD());
        if (p.getLeaveDeductAmount() == null || p.getLeaveDeductAmount().signum() < 0) {
            throw new BusinessException("请假扣款金额不能为负数");
        }
        if (p.getLateDeductAmount() == null || p.getLateDeductAmount().signum() < 0) {
            throw new BusinessException("迟到扣款金额不能为负数");
        }
        if (p.getAbsentDeductDays() == null || p.getAbsentDeductDays().signum() <= 0) {
            throw new BusinessException("旷工折算天数必须大于 0");
        }
        if (p.getOvertimeWeekdayRate() == null || p.getOvertimeWeekendRate() == null
                || p.getOvertimeHolidayRate() == null
                || p.getOvertimeWeekdayRate().signum() <= 0
                || p.getOvertimeWeekendRate().signum() <= 0
                || p.getOvertimeHolidayRate().signum() <= 0) {
            throw new BusinessException("加班费率必须大于 0");
        }
        if (p.getTaxThreshold() == null || p.getTaxThreshold().signum() < 0) {
            throw new BusinessException("个税起征点不能为负数");
        }
        if (p.getWorkStart() == null || p.getWorkEnd() == null
                || !p.getWorkStart().matches("\\d{2}:\\d{2}") || !p.getWorkEnd().matches("\\d{2}:\\d{2}")) {
            throw new BusinessException("上下班时间格式必须是 HH:mm");
        }
    }

    private void checkRate(String label, BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(label + "必须在 0 到 1 之间");
        }
    }

    private void appendChange(StringBuilder sb, String label, Object before, Object after) {
        if (before == null ? after == null : before.equals(after)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("；");
        }
        sb.append(label).append("：").append(before).append(" → ").append(after);
    }
}
