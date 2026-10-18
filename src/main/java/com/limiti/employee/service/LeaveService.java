package com.limiti.employee.service;

import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Leave;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.LeaveRepository;
import com.limiti.employee.utils.WorkdayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 请假服务。
 *
 * <p>状态流转：0 待审批 → 1 已通过 / 2 已拒绝；已通过或被拒绝的记录可以由本人或管理员撤销，
 * 撤销后状态置为 3，不再计入考勤与工资（原来的"通过"状态不再生效）。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class LeaveService {

    /** 待审批 */
    public static final int STATUS_PENDING = 0;
    /** 已通过 */
    public static final int STATUS_APPROVED = 1;
    /** 已拒绝 */
    public static final int STATUS_REJECTED = 2;
    /** 已撤销 */
    public static final int STATUS_CANCELLED = 3;

    /** 请假类型：1-事假，2-病假，3-年假，4-调休 */
    private static final int TYPE_MIN = 1;
    private static final int TYPE_MAX = 4;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 提交请假申请：校验时间与类型，按工作日自动计算请假天数
     */
    @Transactional
    public Leave applyLeave(Leave leave, Employee employee) {
        if (leave.getStartTime() == null || leave.getEndTime() == null) {
            throw BusinessException.badRequest("请选择请假开始与结束时间");
        }
        if (leave.getStartTime().after(leave.getEndTime())) {
            throw BusinessException.badRequest("开始时间不能晚于结束时间");
        }
        if (leave.getType() == null || leave.getType() < TYPE_MIN || leave.getType() > TYPE_MAX) {
            throw BusinessException.badRequest("请假类型只能是 1(事假)、2(病假)、3(年假)、4(调休)");
        }
        int workdays = countWorkdays(leave.getStartTime(), leave.getEndTime());
        if (workdays <= 0) {
            throw BusinessException.badRequest("请假区间内没有工作日，请检查日期");
        }
        leave.setId(null);
        leave.setEmployee(employee);
        leave.setStatus(STATUS_PENDING);
        leave.setApprover(null);
        leave.setComment(null);
        leave.setApproveTime(null);
        leave.setLeavesDay(workdays);
        leave.setCreateTime(new Date());
        leave.setUpdateTime(new Date());

        Leave saved = leaveRepository.save(leave);
        auditLogService.record(AuditLogService.LEAVE_APPLY, "提交请假",
                "LEAVE", saved.getId(), employee.getName(),
                String.format("%s %s 至 %s，共 %d 个工作日，原因：%s",
                        typeName(saved.getType()), format(saved.getStartTime()), format(saved.getEndTime()),
                        workdays, saved.getReason()));
        return saved;
    }

    /**
     * 审批请假：只有待审批的记录可以审批
     */
    @Transactional
    public Leave approveLeave(Long id, Integer status, String comment, Admin admin) {
        if (status == null || (status != STATUS_APPROVED && status != STATUS_REJECTED)) {
            throw BusinessException.badRequest("审批结果只能是 1(通过) 或 2(拒绝)");
        }
        Leave leave = findById(id);
        if (leave.getStatus() != null && leave.getStatus() == STATUS_APPROVED) {
            throw BusinessException.conflict("该请假已经通过，无需重复审批");
        }
        if (leave.getStatus() != null && leave.getStatus() == STATUS_CANCELLED) {
            throw BusinessException.conflict("该请假已被撤销，不能再审批");
        }
        leave.setStatus(status);
        leave.setApprover(admin);
        leave.setComment(comment);
        leave.setApproveTime(new Date());
        leave.setUpdateTime(new Date());
        Leave saved = leaveRepository.save(leave);

        String employeeName = saved.getEmployee() == null ? "-" : saved.getEmployee().getName();
        auditLogService.record(AuditLogService.LEAVE_APPROVE,
                status == STATUS_APPROVED ? "通过请假" : "拒绝请假",
                "LEAVE", saved.getId(), employeeName,
                String.format("%s %s 至 %s，意见：%s", typeName(saved.getType()),
                        format(saved.getStartTime()), format(saved.getEndTime()),
                        comment == null || comment.trim().isEmpty() ? "无" : comment));
        return saved;
    }

    /**
     * 撤销请假：本人或管理员操作，撤销后不再计入考勤与工资
     */
    @Transactional
    public Leave cancelLeave(Long id, Employee employee, Admin admin, String reason) {
        Leave leave = findById(id);
        if (leave.getStatus() != null && leave.getStatus() == STATUS_CANCELLED) {
            throw BusinessException.conflict("该请假已经撤销");
        }
        Long ownerId = leave.getEmployee() == null ? null : leave.getEmployee().getId();
        boolean isOwner = employee != null && ownerId != null && ownerId.equals(employee.getId());
        if (admin == null && !isOwner) {
            throw BusinessException.forbidden("只能撤销本人的请假");
        }
        leave.setStatus(STATUS_CANCELLED);
        leave.setComment(reason == null || reason.trim().isEmpty() ? "申请人撤销" : reason.trim());
        leave.setUpdateTime(new Date());
        Leave saved = leaveRepository.save(leave);

        String operator = admin != null ? (admin.getName() == null ? admin.getUsername() : admin.getName())
                : (employee != null ? employee.getName() : "系统");
        auditLogService.record(AuditLogService.LEAVE_CANCEL, "撤销请假",
                "LEAVE", saved.getId(), saved.getEmployee() == null ? "-" : saved.getEmployee().getName(),
                String.format("%s 撤销了 %s 至 %s 的%s", operator, format(saved.getStartTime()),
                        format(saved.getEndTime()), typeName(saved.getType())));
        return saved;
    }

    public Leave findById(Long id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("请假记录不存在: " + id));
    }

    public List<Leave> getEmployeeLeaves(Long employeeId) {
        return leaveRepository.findByEmployee_IdOrderByCreateTimeDesc(employeeId);
    }

    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatusOrderByCreateTimeDesc(STATUS_PENDING);
    }

    public List<Leave> findAll() {
        return leaveRepository.findAll();
    }

    public List<Leave> findByStatus(Integer status) {
        return leaveRepository.findByStatusOrderByCreateTimeDesc(status);
    }

    @Transactional
    public Leave save(Leave leave) {
        if (leave.getEmployee() == null) {
            throw BusinessException.badRequest("员工信息不能为空");
        }
        return leaveRepository.save(leave);
    }

    /**
     * 统计区间内的工作日数量（含首尾）
     */
    public static int countWorkdays(Date start, Date end) {
        LocalDate from = toLocalDate(start);
        LocalDate to = toLocalDate(end);
        if (from == null || to == null || to.isBefore(from)) {
            return 0;
        }
        int count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (WorkdayUtils.isWorkday(date)) {
                count++;
            }
        }
        return count;
    }

    public static String typeName(Integer type) {
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

    public static String statusName(Integer status) {
        if (status == null) {
            return "-";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待审批";
            case STATUS_APPROVED:
                return "已通过";
            case STATUS_REJECTED:
                return "已拒绝";
            case STATUS_CANCELLED:
                return "已撤销";
            default:
                return "-";
        }
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static String format(Date date) {
        LocalDate localDate = toLocalDate(date);
        return localDate == null ? "-" : localDate.toString();
    }
}
