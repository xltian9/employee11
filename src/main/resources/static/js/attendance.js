/**
 * 管理端考勤：月度全员汇总 + 单人每日明细。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var currentYear = 0;
    var currentMonth = 0;

    function initFilters() {
        var monthSelect = document.getElementById('attendanceMonth');
        if (monthSelect && monthSelect.options.length === 0) {
            var html = '';
            for (var i = 1; i <= 12; i++) {
                html += '<option value="' + i + '">' + i + ' 月</option>';
            }
            monthSelect.innerHTML = html;
        }
        var now = new Date();
        if (!currentYear) {
            currentYear = now.getFullYear();
            currentMonth = now.getMonth() + 1;
        }
        document.getElementById('attendanceYear').value = currentYear;
        monthSelect.value = currentMonth;
    }

    function period() {
        var year = Number(document.getElementById('attendanceYear').value) || new Date().getFullYear();
        var month = Number(document.getElementById('attendanceMonth').value) || (new Date().getMonth() + 1);
        currentYear = year;
        currentMonth = month;
        return { year: year, month: month };
    }

    function renderRows(rows) {
        var body = document.getElementById('attendanceTableBody');
        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="12">暂无考勤数据</td></tr>';
            return;
        }
        var totalWork = 0;
        var totalActual = 0;
        var totalAbsent = 0;
        var totalLate = 0;
        var totalEarly = 0;

        body.innerHTML = rows.map(function (row) {
            totalWork = Math.max(totalWork, Number(row.workDays) || 0);
            totalActual += Number(row.actualDays) || 0;
            totalAbsent += Number(row.absentDays) || 0;
            totalLate += Number(row.lateCount) || 0;
            totalEarly += Number(row.earlyCount) || 0;
            var absentCell = Number(row.absentDays) > 0
                ? '<span style="color:#dc2626;font-weight:600;">' + row.absentDays + '</span>'
                : '0';
            return '<tr>'
                + '<td class="muted">' + Utils.escapeHtml(row.employeeNumber || '-') + '</td>'
                + '<td><strong>' + Utils.escapeHtml(row.name || '') + '</strong></td>'
                + '<td class="num">' + Utils.num(row.workDays) + '</td>'
                + '<td class="num">' + Utils.num(row.actualDays) + '</td>'
                + '<td class="num">' + Utils.num(row.lateCount) + '</td>'
                + '<td class="num">' + Utils.num(row.earlyCount) + '</td>'
                + '<td class="num">' + absentCell + '</td>'
                + '<td class="num">' + Utils.num(row.casualLeaveDays) + '</td>'
                + '<td class="num">' + Utils.num(row.sickLeaveDays) + '</td>'
                + '<td class="num">' + Utils.num(row.otherLeaveDays) + '</td>'
                + '<td class="num">' + Utils.num(row.overtimeHours, 1) + '</td>'
                + '<td><button class="link-btn" onclick="showAttendanceDetail(' + row.employeeId + ')">每日明细</button></td>'
                + '</tr>';
        }).join('');

        document.getElementById('statWorkDays').textContent = totalWork;
        document.getElementById('statActualDays').textContent = totalActual;
        document.getElementById('statAbsent').textContent = totalAbsent;
        document.getElementById('statLateEarly').textContent = totalLate + ' / ' + totalEarly;
    }

    function loadAttendance() {
        initFilters();
        var p = period();
        Api.get('/api/attendance/summary/all?year=' + p.year + '&month=' + p.month)
            .then(renderRows)
            .catch(function (error) {
                Toast.error(error.message);
                document.getElementById('attendanceTableBody').innerHTML =
                    '<tr class="empty-row"><td colspan="12">加载失败</td></tr>';
            });
    }

    function statusBadge(status) {
        var map = {
            '正常': 'badge-ok',
            '迟到': 'badge-warn',
            '早退': 'badge-warn',
            '迟到+早退': 'badge-warn',
            '旷工': 'badge-danger',
            '请假': 'badge-info'
        };
        return '<span class="badge ' + (map[status] || '') + '">' + Utils.escapeHtml(status || '-') + '</span>';
    }

    function showAttendanceDetail(employeeId) {
        var p = period();
        document.getElementById('attendanceDetailTitle').textContent =
            '每日明细 · ' + p.year + ' 年 ' + p.month + ' 月';
        document.getElementById('attendanceDetailBody').innerHTML =
            '<tr class="empty-row"><td colspan="6">正在加载…</td></tr>';
        document.getElementById('attendanceDetailModal').hidden = false;

        Api.get('/api/attendance/daily?employeeId=' + employeeId + '&year=' + p.year + '&month=' + p.month)
            .then(function (rows) {
                var body = document.getElementById('attendanceDetailBody');
                if (!rows || rows.length === 0) {
                    body.innerHTML = '<tr class="empty-row"><td colspan="6">该月没有工作日</td></tr>';
                    return;
                }
                body.innerHTML = rows.map(function (day) {
                    return '<tr>'
                        + '<td>' + Utils.escapeHtml(day.date || '') + '</td>'
                        + '<td>' + statusBadge(day.status) + '</td>'
                        + '<td>' + Utils.escapeHtml(day.checkIn || '-') + '</td>'
                        + '<td>' + Utils.escapeHtml(day.checkOut || '-') + '</td>'
                        + '<td>' + Utils.escapeHtml(day.leaveType || '-') + '</td>'
                        + '<td class="muted">' + Utils.escapeHtml(day.note || '') + '</td>'
                        + '</tr>';
                }).join('');
            })
            .catch(function (error) {
                Toast.error(error.message);
                document.getElementById('attendanceDetailBody').innerHTML =
                    '<tr class="empty-row"><td colspan="6">加载失败</td></tr>';
            });
    }

    function hideAttendanceDetail() {
        document.getElementById('attendanceDetailModal').hidden = true;
    }

    window.loadAttendance = loadAttendance;
    window.showAttendanceDetail = showAttendanceDetail;
    window.hideAttendanceDetail = hideAttendanceDetail;
})(window);
