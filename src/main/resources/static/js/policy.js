/**
 * 管理端工资策略：读取 / 保存全部计算参数。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var FIELDS = [
        'leaveDeductMode', 'leaveDeductAmount', 'sickLeaveRate', 'absentDeductDays', 'lateDeductAmount',
        'overtimeWeekdayRate', 'overtimeWeekendRate', 'overtimeHolidayRate',
        'socialInsuranceRate', 'housingFundRate', 'taxThreshold',
        'bonusRateS', 'bonusRateA', 'bonusRateB', 'bonusRateC', 'bonusRateD',
        'workStart', 'workEnd', 'lateToAbsentMinutes'
    ];

    function elementId(field) {
        return 'policy' + field.charAt(0).toUpperCase() + field.slice(1);
    }

    function fill(policy) {
        FIELDS.forEach(function (field) {
            var element = document.getElementById(elementId(field));
            if (!element) {
                return;
            }
            if (field === 'workStart' || field === 'workEnd') {
                element.value = policy[field] || '';
            } else if (field === 'leaveDeductMode' || field === 'lateToAbsentMinutes') {
                element.value = policy[field] === null || policy[field] === undefined ? '' : policy[field];
            } else {
                element.value = policy[field] === null || policy[field] === undefined ? '' : policy[field];
            }
        });
        var updated = document.getElementById('policyUpdatedAt');
        if (updated) {
            updated.textContent = policy.updateTime
                ? '最后修改：' + Utils.dateTime(policy.updateTime) + (policy.updateBy ? '（' + policy.updateBy + '）' : '')
                : '';
        }
    }

    function loadSalaryPolicy() {
        Api.get('/api/salary-policy')
            .then(fill)
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function saveSalaryPolicy() {
        var payload = {};
        for (var i = 0; i < FIELDS.length; i++) {
            var field = FIELDS[i];
            var element = document.getElementById(elementId(field));
            if (!element) {
                continue;
            }
            var raw = element.value;
            if (field === 'workStart' || field === 'workEnd') {
                if (!raw) {
                    Toast.warning('请填写上下班时间');
                    return;
                }
                payload[field] = raw;
            } else {
                if (raw === '') {
                    Toast.warning('所有数值字段都不能为空');
                    return;
                }
                payload[field] = Number(raw);
            }
        }
        Api.put('/api/salary-policy', payload)
            .then(function (policy) {
                Toast.success('策略已保存，工资与考勤按新参数重新计算');
                fill(policy);
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    window.loadSalaryPolicy = loadSalaryPolicy;
    window.saveSalaryPolicy = saveSalaryPolicy;
})(window);
