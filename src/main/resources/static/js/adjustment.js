/**
 * 管理端补发 / 扣款与加班记录。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var TYPE_NAME = { 1: '补发', 2: '扣款' };
    var OT_NAME = { 1: '工作日', 2: '周末', 3: '节假日' };
    var filtersReady = false;

    function pad(value) {
        return value < 10 ? '0' + value : '' + value;
    }

    function initFilters() {
        var monthSelect = document.getElementById('adjustMonth');
        if (!filtersReady) {
            var html = '';
            for (var i = 1; i <= 12; i++) {
                html += '<option value="' + i + '">' + i + ' 月</option>';
            }
            monthSelect.innerHTML = html;
            var now = new Date();
            document.getElementById('adjustYear').value = now.getFullYear();
            monthSelect.value = now.getMonth() + 1;
            filtersReady = true;
        }
    }

    function period() {
        return {
            year: Number(document.getElementById('adjustYear').value) || new Date().getFullYear(),
            month: Number(document.getElementById('adjustMonth').value) || (new Date().getMonth() + 1)
        };
    }

    function loadEmployeesInto(selectId) {
        return Api.get('/api/employees').then(function (employees) {
            var select = document.getElementById(selectId);
            select.innerHTML = (employees || []).map(function (employee) {
                return '<option value="' + employee.id + '">'
                    + Utils.escapeHtml(employee.name + '（' + (employee.employeeNumber || '') + '）')
                    + '</option>';
            }).join('');
        });
    }

    function renderAdjustments(rows) {
        var body = document.getElementById('adjustmentTableBody');
        document.getElementById('adjustHint').textContent = '共 ' + (rows ? rows.length : 0) + ' 条';
        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="7">本月没有补发或扣款</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (row) {
            var add = Number(row.type) === 1;
            return '<tr>'
                + '<td><strong>' + Utils.escapeHtml(row.employeeName || '-') + '</strong></td>'
                + '<td><span class="badge ' + (add ? 'badge-ok' : 'badge-danger') + '">'
                + (TYPE_NAME[Number(row.type)] || '-') + '</span></td>'
                + '<td class="num">' + (add ? '+' : '−') + Utils.money(row.amount) + '</td>'
                + '<td>' + Utils.escapeHtml(row.reason || '-') + '</td>'
                + '<td class="muted">' + Utils.escapeHtml(row.operatorName || '-') + '</td>'
                + '<td class="muted">' + Utils.dateTime(row.createTime) + '</td>'
                + '<td><button class="link-btn danger" onclick="deleteAdjustment(' + row.id + ')">删除</button></td>'
                + '</tr>';
        }).join('');
    }

    function renderOvertimes(rows) {
        var body = document.getElementById('overtimeTableBody');
        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="6">本月没有加班记录</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (row) {
            return '<tr>'
                + '<td><strong>' + Utils.escapeHtml(row.employeeName || '-') + '</strong></td>'
                + '<td>' + Utils.escapeHtml(row.workDate || '-') + '</td>'
                + '<td>' + Utils.escapeHtml(OT_NAME[Number(row.otType)] || '-') + '</td>'
                + '<td class="num">' + Utils.num(row.hours, 1) + '</td>'
                + '<td>' + Utils.escapeHtml(row.reason || '-') + '</td>'
                + '<td><button class="link-btn danger" onclick="deleteOvertime(' + row.id + ')">删除</button></td>'
                + '</tr>';
        }).join('');
    }

    function loadAdjustments() {
        initFilters();
        var p = period();
        Api.get('/api/salary-adjustments?year=' + p.year + '&month=' + p.month)
            .then(renderAdjustments)
            .catch(function (error) {
                Toast.error(error.message);
            });
        Api.get('/api/overtimes?year=' + p.year + '&month=' + p.month)
            .then(renderOvertimes)
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function showAdjustmentModal() {
        var p = period();
        loadEmployeesInto('adjustEmployeeId').then(function () {
            document.getElementById('adjustType').value = '1';
            document.getElementById('adjustAmount').value = '';
            document.getElementById('adjustReason').value = '';
            document.getElementById('adjustmentModal').hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
        document.getElementById('adjustYear').value = p.year;
    }

    function hideAdjustmentModal() {
        document.getElementById('adjustmentModal').hidden = true;
    }

    function saveAdjustment() {
        var p = period();
        var amount = Number(document.getElementById('adjustAmount').value);
        var reason = document.getElementById('adjustReason').value.trim();
        if (!amount || amount <= 0) {
            Toast.warning('金额必须大于 0');
            return;
        }
        if (!reason) {
            Toast.warning('请填写原因');
            return;
        }
        var payload = {
            employeeId: Number(document.getElementById('adjustEmployeeId').value),
            year: p.year,
            month: p.month,
            type: Number(document.getElementById('adjustType').value),
            amount: amount,
            reason: reason
        };
        Api.post('/api/salary-adjustments', payload)
            .then(function () {
                Toast.success('已新增，当月工资重新计算');
                hideAdjustmentModal();
                loadAdjustments();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function deleteAdjustment(id) {
        if (!window.confirm('确定删除这条补发/扣款记录吗？')) {
            return;
        }
        Api.del('/api/salary-adjustments/' + id)
            .then(function () {
                Toast.success('已删除');
                loadAdjustments();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function showOvertimeModal() {
        var p = period();
        loadEmployeesInto('overtimeEmployeeId').then(function () {
            document.getElementById('overtimeType').value = '1';
            document.getElementById('overtimeDate').value =
                p.year + '-' + pad(p.month) + '-01';
            document.getElementById('overtimeHours').value = '';
            document.getElementById('overtimeReason').value = '';
            document.getElementById('overtimeModal').hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function hideOvertimeModal() {
        document.getElementById('overtimeModal').hidden = true;
    }

    function saveOvertime() {
        var hours = Number(document.getElementById('overtimeHours').value);
        var workDate = document.getElementById('overtimeDate').value;
        if (!workDate) {
            Toast.warning('请选择加班日期');
            return;
        }
        if (!hours || hours <= 0 || hours > 24) {
            Toast.warning('小时数必须在 0 到 24 之间');
            return;
        }
        var payload = {
            employeeId: Number(document.getElementById('overtimeEmployeeId').value),
            workDate: workDate,
            otType: Number(document.getElementById('overtimeType').value),
            hours: hours,
            reason: document.getElementById('overtimeReason').value.trim()
        };
        Api.post('/api/overtimes', payload)
            .then(function () {
                Toast.success('已新增加班记录');
                hideOvertimeModal();
                loadAdjustments();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function deleteOvertime(id) {
        if (!window.confirm('确定删除这条加班记录吗？')) {
            return;
        }
        Api.del('/api/overtimes/' + id)
            .then(function () {
                Toast.success('已删除');
                loadAdjustments();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    window.loadAdjustments = loadAdjustments;
    window.showAdjustmentModal = showAdjustmentModal;
    window.hideAdjustmentModal = hideAdjustmentModal;
    window.saveAdjustment = saveAdjustment;
    window.deleteAdjustment = deleteAdjustment;
    window.showOvertimeModal = showOvertimeModal;
    window.hideOvertimeModal = hideOvertimeModal;
    window.saveOvertime = saveOvertime;
    window.deleteOvertime = deleteOvertime;
})(window);
