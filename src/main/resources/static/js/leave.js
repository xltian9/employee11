/**
 * 管理端请假：审批 / 撤销 / 状态统计。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var META_TYPES = { '1': '事假', '2': '病假', '3': '年假', '4': '调休' };
    var META_STATUS = { '0': '待审批', '1': '已通过', '2': '已拒绝', '3': '已撤销' };
    var filtersReady = false;

    function pad(value) {
        return value < 10 ? '0' + value : '' + value;
    }

    function initFilters() {
        if (filtersReady) {
            return;
        }
        var select = document.getElementById('leaveStatusFilter');
        var html = '<option value="">全部状态</option>';
        Object.keys(META_STATUS).forEach(function (key) {
            html += '<option value="' + key + '">' + META_STATUS[key] + '</option>';
        });
        select.innerHTML = html;
        filtersReady = true;
    }

    function statusBadge(status) {
        var cls = { 0: 'badge-warn', 1: 'badge-ok', 2: 'badge-danger', 3: 'badge-muted' }[status] || '';
        var text = META_STATUS[String(status)] || '未知';
        return '<span class="badge ' + cls + '">' + text + '</span>';
    }

    function approverCell(leave) {
        var name = leave.approver && leave.approver.name ? leave.approver.name : '-';
        var comment = leave.comment
            ? '<div class="muted" style="font-size:12px;margin-top:2px;">' + Utils.escapeHtml(leave.comment) + '</div>'
            : '';
        return '<div>' + Utils.escapeHtml(name) + '</div>' + comment;
    }

    function actionCell(leave) {
        var buttons = [];
        if (Number(leave.status) === 0) {
            buttons.push('<button class="link-btn" onclick="approveLeave(' + leave.id + ',1)">通过</button>');
            buttons.push('<button class="link-btn danger" onclick="approveLeave(' + leave.id + ',2)">拒绝</button>');
        }
        if (Number(leave.status) === 0 || Number(leave.status) === 1) {
            buttons.push('<button class="link-btn muted" onclick="cancelLeave(' + leave.id + ')">撤销</button>');
        }
        return buttons.length ? buttons.join('') : '<span class="muted">-</span>';
    }

    function render(rows) {
        var body = document.getElementById('leaveTableBody');
        var pending = 0;
        var approved = 0;
        var closed = 0;
        var daysThisMonth = 0;
        var now = new Date();
        var monthPrefix = now.getFullYear() + '-' + pad(now.getMonth() + 1);

        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="9">暂无请假记录</td></tr>';
        } else {
            body.innerHTML = rows.map(function (leave) {
                if (Number(leave.status) === 0) {
                    pending++;
                } else if (Number(leave.status) === 1) {
                    approved++;
                } else {
                    closed++;
                }
                var startText = Utils.dateTime(leave.startTime);
                if (Number(leave.status) === 1 && startText.indexOf(monthPrefix) === 0) {
                    daysThisMonth += Number(leave.leavesDay) || 0;
                }
                var employeeName = leave.employee && leave.employee.name ? leave.employee.name : '-';
                var employeeNumber = leave.employee && leave.employee.employeeNumber ? leave.employee.employeeNumber : '';
                return '<tr>'
                    + '<td><strong>' + Utils.escapeHtml(employeeName) + '</strong>'
                    + '<div class="muted" style="font-size:12px;">' + Utils.escapeHtml(employeeNumber) + '</div></td>'
                    + '<td>' + Utils.escapeHtml(META_TYPES[String(leave.type)] || '-') + '</td>'
                    + '<td>' + startText + '</td>'
                    + '<td>' + Utils.dateTime(leave.endTime) + '</td>'
                    + '<td class="num">' + Utils.num(leave.leavesDay) + '</td>'
                    + '<td style="max-width:200px;">' + Utils.escapeHtml(leave.reason || '-') + '</td>'
                    + '<td>' + statusBadge(leave.status) + '</td>'
                    + '<td>' + approverCell(leave) + '</td>'
                    + '<td>' + actionCell(leave) + '</td>'
                    + '</tr>';
            }).join('');
        }

        document.getElementById('statLeavePending').textContent = pending;
        document.getElementById('statLeaveApproved').textContent = approved;
        document.getElementById('statLeaveClosed').textContent = closed;
        document.getElementById('statLeaveDays').textContent = daysThisMonth;
    }

    function loadLeaves() {
        initFilters();
        var status = document.getElementById('leaveStatusFilter').value;
        var url = status === '' ? '/api/leaves' : '/api/leaves/search?status=' + status;
        Api.get(url)
            .then(render)
            .catch(function (error) {
                Toast.error(error.message);
                document.getElementById('leaveTableBody').innerHTML =
                    '<tr class="empty-row"><td colspan="9">加载失败</td></tr>';
            });
    }

    function approveLeave(id, status) {
        var title = status === 1 ? '通过' : '拒绝';
        var comment = window.prompt('请输入审批意见（可留空）', status === 1 ? '同意' : '不同意');
        if (comment === null) {
            return;
        }
        Api.post('/api/leaves/' + id + '/approve?status=' + status + '&comment=' + encodeURIComponent(comment))
            .then(function () {
                Toast.success('已' + title + '该请假');
                loadLeaves();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function cancelLeave(id) {
        var reason = window.prompt('撤销原因（可留空）', '');
        if (reason === null) {
            return;
        }
        Api.post('/api/leaves/' + id + '/cancel?reason=' + encodeURIComponent(reason))
            .then(function () {
                Toast.success('已撤销，考勤与工资已同步回滚');
                loadLeaves();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    window.loadLeaves = loadLeaves;
    window.approveLeave = approveLeave;
    window.cancelLeave = cancelLeave;
})(window);
