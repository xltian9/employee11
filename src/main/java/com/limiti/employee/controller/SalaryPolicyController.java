package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.SalaryPolicy;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.SalaryPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 薪酬规则 / 考勤规则配置接口（仅管理员）
 *
 * <p>这里的每一项都直接参与工资计算：请假怎么扣、旷工怎么罚、迟到怎么扣、
 * 加班费倍数、社保公积金比例、个税起征点、绩效奖金系数、上下班时间。</p>
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/salary-policy")
public class SalaryPolicyController {

    @Autowired
    private SalaryPolicyService salaryPolicyService;

    @GetMapping
    public Result<SalaryPolicy> get(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(salaryPolicyService.get());
    }

    @PutMapping
    public Result<SalaryPolicy> update(@RequestBody SalaryPolicy policy, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        String operator = admin == null ? "系统" : admin.getName();
        return Result.success("保存成功", salaryPolicyService.update(policy, operator));
    }
}
