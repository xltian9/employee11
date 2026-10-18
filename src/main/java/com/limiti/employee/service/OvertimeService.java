package com.limiti.employee.service;

import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.OvertimeRecord;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.EmployeeRepository;
import com.limiti.employee.repository.OvertimeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加班登记服务。
 *
 * <p>加班费在工资报告里按「时薪 × 小时数 × 费率」计算，时薪 = 月薪 ÷ 当月工作日 ÷ 8，
 * 费率分工作日/周末/节假日三档，见 {@link com.limiti.employee.entity.SalaryPolicy}。</p>
 *
 * @author Limiti
 */
@Service
public class OvertimeService {

    @Autowired
    private OvertimeRecordRepository overtimeRecordRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<OvertimeRecord> listByMonth(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        return overtimeRecordRepository.findByWorkDateBetween(first, first.withDayOfMonth(first.lengthOfMonth()));
    }

    public List<OvertimeRecord> listByEmployee(Long employeeId, int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        return overtimeRecordRepository.findByEmployeeIdAndWorkDateBetween(employeeId, first,
                first.withDayOfMonth(first.lengthOfMonth()));
    }

    @Transactional
    public OvertimeRecord create(OvertimeRecord record, String operatorName) {
        Employee employee = employeeRepository.findById(record.getEmployeeId())
                .orElseThrow(() -> BusinessException.notFound("员工不存在"));
        if (Boolean.TRUE.equals(employee.getIsDel())) {
            throw BusinessException.notFound("员工已离职");
        }
        if (record.getWorkDate() == null) {
            throw BusinessException.badRequest("请选择加班日期");
        }
        if (record.getHours() == null || record.getHours().signum() <= 0) {
            throw BusinessException.badRequest("加班小时数必须大于 0");
        }
        if (record.getHours().compareTo(new BigDecimal("24")) > 0) {
            throw BusinessException.badRequest("单日加班小时数不能超过 24");
        }
        if (record.getOtType() == null
                || (record.getOtType() != OvertimeRecord.TYPE_WEEKDAY
                && record.getOtType() != OvertimeRecord.TYPE_WEEKEND
                && record.getOtType() != OvertimeRecord.TYPE_HOLIDAY)) {
            throw BusinessException.badRequest("加班类型只能是 1(工作日) / 2(周末) / 3(法定节假日)");
        }
        record.setId(null);
        record.setEmployeeName(employee.getName());
        OvertimeRecord saved = overtimeRecordRepository.save(record);
        auditLogService.record(AuditLogService.OVERTIME_SAVE, "登记加班",
                "OVERTIME", saved.getId(), employee.getName(),
                String.format("%s 加班 %s 小时（%s）", saved.getWorkDate(), saved.getHours(),
                        typeName(saved.getOtType())));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        OvertimeRecord record = overtimeRecordRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("加班记录不存在"));
        overtimeRecordRepository.delete(record);
        auditLogService.record(AuditLogService.OVERTIME_SAVE, "删除加班",
                "OVERTIME", record.getId(), record.getEmployeeName(),
                String.format("%s 加班 %s 小时", record.getWorkDate(), record.getHours()));
    }

    public static String typeName(Integer otType) {
        if (otType == null) {
            return "-";
        }
        switch (otType) {
            case OvertimeRecord.TYPE_WEEKDAY:
                return "工作日";
            case OvertimeRecord.TYPE_WEEKEND:
                return "周末";
            case OvertimeRecord.TYPE_HOLIDAY:
                return "法定节假日";
            default:
                return "-";
        }
    }
}
