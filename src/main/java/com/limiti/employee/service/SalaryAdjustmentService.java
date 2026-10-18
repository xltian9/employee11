package com.limiti.employee.service;

import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.SalaryAdjustment;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.repository.SalaryAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补发 / 扣款服务。
 *
 * <p>工资之外的加减项，例如全勤奖、设备赔偿、迟到罚款。每笔都记录操作人与原因，
 * 并写审计日志，方便追溯"谁改了谁的工资"。</p>
 *
 * @author Limiti
 */
@Service
public class SalaryAdjustmentService {

    @Autowired
    private SalaryAdjustmentRepository salaryAdjustmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<SalaryAdjustment> listByMonth(int year, int month) {
        return salaryAdjustmentRepository.findByYearAndMonth(year, month);
    }

    public List<SalaryAdjustment> listByEmployee(Long employeeId) {
        return salaryAdjustmentRepository.findByEmployeeIdOrderByCreateTimeDesc(employeeId);
    }

    @Transactional
    public SalaryAdjustment create(SalaryAdjustment adjustment, String operatorName) {
        Employee employee = employeeRepository.findById(adjustment.getEmployeeId())
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        if (Boolean.TRUE.equals(employee.getIsDel())) {
            throw BusinessException.notFound("员工已离职");
        }
        if (adjustment.getType() == null
                || (adjustment.getType() != SalaryAdjustment.TYPE_ADD
                && adjustment.getType() != SalaryAdjustment.TYPE_DEDUCT)) {
            throw BusinessException.badRequest("类型只能是 1(补发) 或 2(扣款)");
        }
        if (adjustment.getAmount() == null || adjustment.getAmount().signum() <= 0) {
            throw BusinessException.badRequest("金额必须大于 0");
        }
        if (adjustment.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            throw BusinessException.badRequest("金额不能超过 1000000");
        }
        if (adjustment.getYear() == null || adjustment.getYear() < 2000 || adjustment.getYear() > 2100) {
            throw BusinessException.badRequest("年份必须在 2000 到 2100 之间");
        }
        if (adjustment.getMonth() == null || adjustment.getMonth() < 1 || adjustment.getMonth() > 12) {
            throw BusinessException.badRequest("月份必须在 1 到 12 之间");
        }
        if (adjustment.getReason() == null || adjustment.getReason().trim().isEmpty()) {
            throw BusinessException.badRequest("请填写原因");
        }
        adjustment.setId(null);
        adjustment.setEmployeeName(employee.getName());
        adjustment.setReason(adjustment.getReason().trim());
        SalaryAdjustment saved = salaryAdjustmentRepository.save(adjustment);

        String typeName = saved.getType() == SalaryAdjustment.TYPE_ADD ? "补发" : "扣款";
        auditLogService.record(AuditLogService.SALARY_ADJUST, "登记" + typeName,
                "SALARY", employee.getId(), employee.getName(),
                String.format("%d年%d月 %s %s 元，原因：%s", saved.getYear(), saved.getMonth(),
                        typeName, saved.getAmount().setScale(2, java.math.RoundingMode.HALF_UP), saved.getReason()));
        return saved;
    }

    @Transactional
    public void delete(Long id, String operatorName) {
        SalaryAdjustment adjustment = salaryAdjustmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("记录不存在"));
        salaryAdjustmentRepository.delete(adjustment);
        auditLogService.record(AuditLogService.SALARY_ADJUST, "删除调整",
                "SALARY", adjustment.getEmployeeId(), adjustment.getEmployeeName(),
                String.format("删除 %d年%d月 %s %s 元", adjustment.getYear(), adjustment.getMonth(),
                        adjustment.getType() == SalaryAdjustment.TYPE_ADD ? "补发" : "扣款",
                        adjustment.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)));
    }
}
