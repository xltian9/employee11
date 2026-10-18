/**
 * 管理端业绩工资：月度工资报告（含考勤扣款、加班费、季度奖金）+ 业绩录入 + Excel 导出。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var filtersReady = false;
    var currentRows = [];
    var gradeClass = {
        S: 'badge-danger',
        A: 'badge-warn',
        B: 'badge-info',
        C: 'badge-muted',
        D: 'badge-muted'
    };

    function pad(value) {
        return value < 10 ? '0' + value : '' + value;
    }

    function initFilters() {
        var monthSelect = document.getElementById('reportMonth');
        if (!monthSelect) {
            return;
        }
        if (!filtersReady) {
            var html = '';
            for (var i = 1; i <= 12; i++) {
                html += '<option value="' + i + '">' + i + ' 月</option>';
            }
            monthSelect.innerHTML = html;
            var now = new Date();
            document.getElementById('reportYear').value = now.getFullYear();
            monthSelect.value = now.getMonth() + 1;
            filtersReady = true;
        }
    }

    function period() {
        var year = Number(document.getElementById('reportYear').value) || new Date().getFullYear();
        var month = Number(document.getElementById('reportMonth').value) || (new Date().getMonth() + 1);
        return { year: year, month: month };
    }

    function hours(row) {
        var weekday = Number(row.overtimeWeekdayHours) || 0;
        var weekend = Number(row.overtimeWeekendHours) || 0;
        var holiday = Number(row.overtimeHolidayHours) || 0;
        return weekday + weekend + holiday;
    }

    function attention(value) {
        var number = Number(value) || 0;
        return number > 0 ? '<span style="color:#dc2626;">' + number + '</span>' : '0';
    }

    function gradeCell(row) {
        if (row.performanceScore === null || row.performanceScore === undefined) {
            return '<span class="muted">未录入</span>';
        }
        var grade = row.performanceGrade || '-';
        return '<strong>' + Utils.num(row.performanceScore, 1) + '</strong> '
            + '<span class="badge ' + (gradeClass[grade] || '') + '">' + Utils.escapeHtml(grade) + '</span>';
    }

    function render(rows) {
        currentRows = rows || [];
        var body = document.getElementById('performanceBody');
        if (!currentRows.length) {
            body.innerHTML = '<tr class="empty-row"><td colspan="24">本月没有在职员工</td></tr>';
            document.getElementById('performanceFoot').innerHTML = '';
            resetStats();
            return;
        }

        body.innerHTML = currentRows.map(function (row) {
            var perfButton = row.performanceId
                ? '<button class="link-btn" onclick="openPerformanceModal(' + row.employeeId + ',' + row.performanceId + ')">修改业绩</button>'
                : '<button class="link-btn" onclick="openPerformanceModal(' + row.employeeId + ')">录入业绩</button>';
            return '<tr>'
                + '<td class="sticky-col"><strong>' + Utils.escapeHtml(row.name || '') + '</strong>'
                + '<div class="muted" style="font-size:12px;">' + Utils.escapeHtml(row.employeeNumber || '') + '</div></td>'
                + '<td>' + Utils.escapeHtml(row.department || '-')
                + '<div class="muted" style="font-size:12px;">' + Utils.escapeHtml(row.position || '') + '</div></td>'
                + '<td class="num">' + Utils.num(row.workDay) + '</td>'
                + '<td class="num">' + Utils.num(row.actualDays) + '</td>'
                + '<td class="num">' + Utils.num(row.lateCount) + '</td>'
                + '<td class="num">' + Utils.num(row.earlyCount) + '</td>'
                + '<td class="num">' + attention(row.absentDays) + '</td>'
                + '<td class="num">' + Utils.num(row.leaveDays) + '</td>'
                + '<td class="num">' + Utils.num(hours(row), 1) + '</td>'
                + '<td class="num">' + Utils.money(row.wage) + '</td>'
                + '<td class="num">' + Utils.money(row.leaveDeduct) + '</td>'
                + '<td class="num">' + Utils.money(row.absentDeduct) + '</td>'
                + '<td class="num">' + Utils.money(row.lateDeduct) + '</td>'
                + '<td class="num">' + Utils.money(row.overtimePay) + '</td>'
                + '<td class="num">' + gradeCell(row) + '</td>'
                + '<td class="num">' + Utils.money(row.bonus) + '</td>'
                + '<td class="num">' + Utils.money(row.adjustAdd) + '</td>'
                + '<td class="num">' + Utils.money(row.adjustDeduct) + '</td>'
                + '<td class="num"><strong>' + Utils.money(row.grossPay) + '</strong></td>'
                + '<td class="num">' + Utils.money(row.socialInsurance) + '</td>'
                + '<td class="num">' + Utils.money(row.housingFund) + '</td>'
                + '<td class="num">' + Utils.money(row.tax) + '</td>'
                + '<td class="num"><strong style="color:#0f766e;">' + Utils.money(row.netPay) + '</strong></td>'
                + '<td>' + perfButton + '</td>'
                + '</tr>';
        }).join('');

        var totals = ['wage', 'leaveDeduct', 'absentDeduct', 'lateDeduct', 'overtimePay', 'bonus',
            'adjustAdd', 'adjustDeduct', 'grossPay', 'socialInsurance', 'housingFund', 'tax', 'netPay'];
        var sums = {};
        totals.forEach(function (key) {
            sums[key] = currentRows.reduce(function (acc, row) {
                return acc + (Number(row[key]) || 0);
            }, 0);
        });

        document.getElementById('performanceFoot').innerHTML = '<tr class="total-row">'
            + '<td class="sticky-col">合计</td>'
            + '<td>' + currentRows.length + ' 人</td>'
            + '<td class="num">-</td><td class="num">-</td><td class="num">-</td><td class="num">-</td>'
            + '<td class="num">-</td><td class="num">-</td><td class="num">-</td>'
            + '<td class="num">' + Utils.money(sums.wage) + '</td>'
            + '<td class="num">' + Utils.money(sums.leaveDeduct) + '</td>'
            + '<td class="num">' + Utils.money(sums.absentDeduct) + '</td>'
            + '<td class="num">' + Utils.money(sums.lateDeduct) + '</td>'
            + '<td class="num">' + Utils.money(sums.overtimePay) + '</td>'
            + '<td class="num">-</td>'
            + '<td class="num">' + Utils.money(sums.bonus) + '</td>'
            + '<td class="num">' + Utils.money(sums.adjustAdd) + '</td>'
            + '<td class="num">' + Utils.money(sums.adjustDeduct) + '</td>'
            + '<td class="num">' + Utils.money(sums.grossPay) + '</td>'
            + '<td class="num">' + Utils.money(sums.socialInsurance) + '</td>'
            + '<td class="num">' + Utils.money(sums.housingFund) + '</td>'
            + '<td class="num">' + Utils.money(sums.tax) + '</td>'
            + '<td class="num">' + Utils.money(sums.netPay) + '</td>'
            + '<td></td>'
            + '</tr>';

        document.getElementById('statGrossPay').textContent = Utils.money(sums.grossPay);
        document.getElementById('statNetPay').textContent = Utils.money(sums.netPay);
        document.getElementById('statBonus').textContent = Utils.money(sums.bonus);
        var withPerformance = currentRows.filter(function (row) {
            return row.performanceScore !== null && row.performanceScore !== undefined;
        }).length;
        document.getElementById('statPerformance').textContent = withPerformance + ' / ' + currentRows.length;
    }

    function resetStats() {
        ['statGrossPay', 'statNetPay', 'statBonus'].forEach(function (id) {
            var element = document.getElementById(id);
            if (element) {
                element.textContent = Utils.money(0);
            }
        });
        var performance = document.getElementById('statPerformance');
        if (performance) {
            performance.textContent = '0 / 0';
        }
    }

    function loadPerformances() {
        initFilters();
        var p = period();
        Api.get('/api/report/list?year=' + p.year + '&month=' + p.month)
            .then(render)
            .catch(function (error) {
                Toast.error(error.message);
                document.getElementById('performanceBody').innerHTML =
                    '<tr class="empty-row"><td colspan="24">加载失败</td></tr>';
            });
    }

    function loadEmployeeOptions(selectedId) {
        return Api.get('/api/employees').then(function (employees) {
            var select = document.getElementById('perfEmployeeId');
            select.innerHTML = (employees || []).map(function (employee) {
                return '<option value="' + employee.id + '">'
                    + Utils.escapeHtml(employee.name + '（' + (employee.employeeNumber || '') + '）')
                    + '</option>';
            }).join('');
            if (selectedId) {
                select.value = String(selectedId);
            }
            return employees;
        });
    }

    function showCreateModal() {
        document.getElementById('performanceId').value = '';
        document.getElementById('performanceModalTitle').textContent = '录入业绩';
        var now = new Date();
        var quarter = Math.floor(now.getMonth() / 3) + 1;
        document.getElementById('perfYear').value = now.getFullYear();
        document.getElementById('perfQuarter').value = String(quarter);
        document.getElementById('perfScore').value = '';
        document.getElementById('perfAbility').value = '';
        document.getElementById('perfAttitude').value = '';
        document.getElementById('perfEvaluation').value = '';
        document.getElementById('perfEmployeeId').disabled = false;
        loadEmployeeOptions(null).then(function () {
            document.getElementById('performanceModal').hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function openPerformanceModal(employeeId, performanceId) {
        loadEmployeeOptions(employeeId).then(function () {
            if (!performanceId) {
                document.getElementById('performanceId').value = '';
                document.getElementById('performanceModalTitle').textContent = '录入业绩';
                document.getElementById('perfEmployeeId').disabled = true;
                var now = new Date();
                document.getElementById('perfYear').value = now.getFullYear();
                document.getElementById('perfQuarter').value = String(Math.floor(now.getMonth() / 3) + 1);
                document.getElementById('perfScore').value = '';
                document.getElementById('perfAbility').value = '';
                document.getElementById('perfAttitude').value = '';
                document.getElementById('perfEvaluation').value = '';
                document.getElementById('performanceModal').hidden = false;
                return;
            }
            return Api.get('/api/performances/' + performanceId).then(function (data) {
                document.getElementById('performanceId').value = data.id;
                document.getElementById('performanceModalTitle').textContent = '修改业绩';
                document.getElementById('perfEmployeeId').disabled = true;
                document.getElementById('perfYear').value = data.year;
                document.getElementById('perfQuarter').value = String(data.quarter);
                document.getElementById('perfScore').value = data.score === null ? '' : data.score;
                document.getElementById('perfAbility').value = data.abilityScore === null ? '' : data.abilityScore;
                document.getElementById('perfAttitude').value = data.attitudeScore === null ? '' : data.attitudeScore;
                document.getElementById('perfEvaluation').value = data.evaluation || '';
                document.getElementById('performanceModal').hidden = false;
            });
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function closeModal() {
        document.getElementById('performanceModal').hidden = true;
    }

    function readScore(id, label) {
        var raw = document.getElementById(id).value.trim();
        if (raw === '') {
            throw new Error('请填写' + label);
        }
        var value = Number(raw);
        if (isNaN(value) || value < 0 || value > 100) {
            throw new Error(label + '必须在 0 到 100 之间');
        }
        return value;
    }

    function savePerformance() {
        var id = document.getElementById('performanceId').value;
        var payload = {
            year: Number(document.getElementById('perfYear').value),
            quarter: Number(document.getElementById('perfQuarter').value),
            evaluation: document.getElementById('perfEvaluation').value.trim()
        };
        try {
            payload.score = readScore('perfScore', '业绩分');
            payload.abilityScore = readScore('perfAbility', '能力分');
            payload.attitudeScore = readScore('perfAttitude', '态度分');
        } catch (error) {
            Toast.warning(error.message);
            return;
        }
        if (!id) {
            payload.employeeId = Number(document.getElementById('perfEmployeeId').value);
        }
        var promise = id ? Api.put('/api/performances/' + id, payload) : Api.post('/api/performances', payload);
        promise.then(function (saved) {
            var grade = saved && saved.grade ? saved.grade : '';
            Toast.success('已保存' + (grade ? '，综合等级 ' + grade : ''));
            closeModal();
            loadPerformances();
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function exportReport() {
        var p = period();
        var fileName = '工资报告-' + p.year + '-' + pad(p.month) + '.xlsx';
        Toast.info('正在生成 Excel…');
        Api.download('/api/report/export?year=' + p.year + '&month=' + p.month, fileName)
            .then(function () {
                Toast.success('导出成功');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    window.loadPerformances = loadPerformances;
    window.showCreateModal = showCreateModal;
    window.openPerformanceModal = openPerformanceModal;
    window.closeModal = closeModal;
    window.savePerformance = savePerformance;
    window.exportReport = exportReport;
})(window);
