/**
 * 工作台：汇总在职人数、当月工资合计、待审批请假、最新公告与最近操作。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    function period() {
        var now = new Date();
        return { year: now.getFullYear(), month: now.getMonth() + 1 };
    }

    function renderEmployees(data) {
        var total = data && data.total != null ? data.total : 0;
        document.getElementById('statEmployees').textContent = total;
        document.getElementById('statEmployeesFoot').textContent = '花名册共 ' + total + ' 人';
    }

    function renderSalary(rows) {
        var gross = 0;
        var net = 0;
        var overtime = 0;
        (rows || []).forEach(function (row) {
            gross += Number(row.grossPay) || 0;
            net += Number(row.netPay) || 0;
            overtime += Number(row.overtimePay) || 0;
        });
        document.getElementById('statGross').textContent = '¥' + Utils.money(gross);
        document.getElementById('statGrossFoot').textContent = '其中加班费 ¥' + Utils.money(overtime);
        document.getElementById('statNet').textContent = '¥' + Utils.money(net);
    }

    function renderPendingLeaves(leaves) {
        document.getElementById('statPendingLeaves').textContent = (leaves || []).length;
        var body = document.getElementById('dashboardPendingBody');
        if (!leaves || leaves.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="6">没有待审批的请假</td></tr>';
            return;
        }
        body.innerHTML = leaves.slice(0, 6).map(function (leave) {
            return '<tr>'
                + '<td>' + Utils.escapeHtml(leave.employeeName || leave.employee && leave.employee.name || '-') + '</td>'
                + '<td>' + Utils.escapeHtml(leave.typeName || '-') + '</td>'
                + '<td>' + Utils.dateTime(leave.startTime) + '</td>'
                + '<td>' + Utils.dateTime(leave.endTime) + '</td>'
                + '<td class="num">' + Utils.num(leave.leavesDay, 1) + '</td>'
                + '<td>' + Utils.escapeHtml(leave.reason || '-') + '</td>'
                + '</tr>';
        }).join('');
    }

    function renderNotices(notices) {
        var host = document.getElementById('dashboardNotices');
        if (!notices || notices.length === 0) {
            host.innerHTML = '<p class="hint">暂无已发布公告</p>';
            return;
        }
        host.innerHTML = notices.slice(0, 5).map(function (notice) {
            return '<div style="padding:10px 0;border-bottom:1px solid #f1f5f9;">'
                + '<div style="display:flex;justify-content:space-between;gap:12px;">'
                + '<strong>' + Utils.escapeHtml(notice.title || '') + '</strong>'
                + '<span class="hint">' + Utils.dateTime(notice.publishTime || notice.createTime) + '</span>'
                + '</div>'
                + '<p class="hint" style="margin:6px 0 0;">'
                + Utils.escapeHtml((notice.content || '').substring(0, 80))
                + ((notice.content || '').length > 80 ? '…' : '') + '</p>'
                + '</div>';
        }).join('');
    }

    function renderAudits(pageData) {
        var host = document.getElementById('dashboardAudits');
        var list = pageData && pageData.list ? pageData.list : [];
        if (list.length === 0) {
            host.innerHTML = '<p class="hint">暂无操作记录</p>';
            return;
        }
        host.innerHTML = list.map(function (log) {
            return '<div style="display:flex;justify-content:space-between;gap:12px;padding:8px 0;border-bottom:1px solid #f1f5f9;">'
                + '<span><strong>' + Utils.escapeHtml(log.operatorName || '系统') + '</strong> '
                + Utils.escapeHtml(log.actionName || log.action || '')
                + (log.targetName ? '（' + Utils.escapeHtml(log.targetName) + '）' : '')
                + '</span>'
                + '<span class="hint">' + Utils.dateTime(log.createTime) + '</span>'
                + '</div>';
        }).join('');
    }

    function loadDashboard() {
        var p = period();
        var label = document.getElementById('dashboardPeriod');
        if (label) {
            label.textContent = p.year + ' 年 ' + p.month + ' 月';
        }
        Api.get('/api/employees/page?page=1&size=1').then(renderEmployees).catch(function (error) {
            Toast.error(error.message);
        });
        Api.get('/api/report/list?year=' + p.year + '&month=' + p.month).then(renderSalary).catch(function (error) {
            Toast.error(error.message);
        });
        Api.get('/api/leaves/pending').then(renderPendingLeaves).catch(function (error) {
            Toast.error(error.message);
        });
        Api.get('/api/notices/published').then(renderNotices).catch(function (error) {
            Toast.error(error.message);
        });
        Api.get('/api/audit-logs?page=1&size=6').then(renderAudits).catch(function (error) {
            Toast.error(error.message);
        });
    }

    window.loadDashboard = loadDashboard;
})(window);
