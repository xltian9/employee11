/**
 * 月度工资报告（含季度业绩奖金）
 *
 * 数据来源：GET /api/report/list?year=&month=
 * 录入业绩：POST /api/performances
 */
(function () {
    'use strict';

    /* ---------------- 工具函数 ---------------- */

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function money(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }
        var number = Number(value);
        if (isNaN(number)) {
            return '-';
        }
        return number.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    function currentYear() {
        return new Date().getFullYear();
    }

    function currentMonth() {
        return new Date().getMonth() + 1;
    }

    function currentQuarter() {
        return Math.floor((currentMonth() - 1) / 3) + 1;
    }

    function alertError(message) {
        window.alert(message);
    }

    /* ---------------- 筛选区初始化 ---------------- */

    function initReportFilters() {
        var monthSelect = document.getElementById('reportMonth');
        var yearInput = document.getElementById('reportYear');
        if (!monthSelect || !yearInput) {
            return;
        }
        if (monthSelect.options.length === 0) {
            for (var month = 1; month <= 12; month++) {
                var option = document.createElement('option');
                option.value = String(month);
                option.textContent = month + ' 月';
                monthSelect.appendChild(option);
            }
        }
        monthSelect.value = String(currentMonth());
        yearInput.value = String(currentYear());
    }

    /* ---------------- 报告加载与渲染 ---------------- */

    function loadPerformances() {
        // 页面内容每次切换都会重新注入，这里按 DOM 是否已初始化来判断，避免重置用户已选的年月
        var monthSelect = document.getElementById('reportMonth');
        if (monthSelect && monthSelect.options.length === 0) {
            initReportFilters();
        }
        var yearInput = document.getElementById('reportYear');
        var tbody = document.getElementById('performanceBody');
        if (!yearInput || !monthSelect || !tbody) {
            return;
        }
        var year = parseInt(yearInput.value, 10);
        var month = parseInt(monthSelect.value, 10);
        if (isNaN(year) || isNaN(month)) {
            alertError('请选择有效的年份和月份');
            return;
        }

        tbody.innerHTML = '<tr><td colspan="13" class="text-center text-muted">加载中...</td></tr>';
        var foot = document.getElementById('performanceFoot');
        if (foot) {
            foot.innerHTML = '';
        }

        fetch('/api/report/list?year=' + encodeURIComponent(year) + '&month=' + encodeURIComponent(month), {
            credentials: 'same-origin'
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (result) {
                if (!result || result.code !== 200) {
                    var message = result && result.msg ? result.msg : '加载工资报告失败';
                    tbody.innerHTML = '<tr><td colspan="13" class="text-center text-danger">'
                        + escapeHtml(message) + '</td></tr>';
                    return;
                }
                renderReport(result.data || []);
            })
            .catch(function (error) {
                console.error('加载工资报告出错:', error);
                tbody.innerHTML = '<tr><td colspan="13" class="text-center text-danger">加载工资报告失败，请稍后重试</td></tr>';
            });
    }

    function renderReport(rows) {
        var tbody = document.getElementById('performanceBody');
        var foot = document.getElementById('performanceFoot');
        if (!tbody) {
            return;
        }
        if (!rows.length) {
            tbody.innerHTML = '<tr><td colspan="13" class="text-center text-muted">当前月份没有在职员工</td></tr>';
            if (foot) {
                foot.innerHTML = '';
            }
            return;
        }

        var totalWage = 0;
        var totalBonus = 0;
        var totalNowWage = 0;
        var totalLeaves = 0;
        var html = '';

        rows.forEach(function (row) {
            var wage = Number(row.wage || 0);
            var bonus = Number(row.bonus || 0);
            var nowWage = Number(row.nowWage || 0);
            var leaves = Number(row.leavesDay || 0);
            totalWage += wage;
            totalBonus += bonus;
            totalNowWage += nowWage;
            totalLeaves += leaves;

            html += '<tr>'
                + '<td>' + escapeHtml(row.employeeNumber || row.employeeId) + '</td>'
                + '<td>' + escapeHtml(row.name) + '</td>'
                + '<td>' + escapeHtml(row.department) + '</td>'
                + '<td>' + escapeHtml(row.position) + '</td>'
                + '<td>' + escapeHtml(row.yearNum) + '</td>'
                + '<td>' + escapeHtml(row.monthNum) + '</td>'
                + '<td>' + escapeHtml(row.workDay) + '</td>'
                + '<td>' + money(row.wage) + '</td>'
                + '<td>' + escapeHtml(row.leavesDay) + '</td>'
                + '<td>' + (row.performanceScore === null || row.performanceScore === undefined
                    ? '<span class="text-muted">未录入</span>'
                    : escapeHtml(row.performanceScore)) + '</td>'
                + '<td class="text-success">' + money(row.bonus) + '</td>'
                + '<td class="fw-bold">' + money(row.nowWage) + '</td>'
                + '<td><button type="button" class="btn btn-sm btn-outline-primary" onclick="openPerformanceModal('
                + escapeHtml(row.employeeId) + ', '
                + (row.performanceId === null || row.performanceId === undefined ? 'null' : escapeHtml(row.performanceId))
                + ')">' + (row.performanceId ? '修改业绩' : '录入业绩') + '</button></td>'
                + '</tr>';
        });

        tbody.innerHTML = html;

        if (foot) {
            foot.innerHTML = '<tr>'
                + '<td colspan="7" class="text-end fw-bold">合计</td>'
                + '<td class="fw-bold">' + money(totalWage) + '</td>'
                + '<td class="fw-bold">' + totalLeaves + '</td>'
                + '<td></td>'
                + '<td class="fw-bold text-success">' + money(totalBonus) + '</td>'
                + '<td class="fw-bold">' + money(totalNowWage) + '</td>'
                + '</tr>';
        }
    }

    /* ---------------- 录入业绩 ---------------- */

    function loadEmployeeOptions(selectedId) {
        var select = document.getElementById('perfEmployeeId');
        if (!select) {
            return;
        }
        select.innerHTML = '<option value="">加载中...</option>';
        fetch('/api/employees', { credentials: 'same-origin' })
            .then(function (response) {
                return response.json();
            })
            .then(function (result) {
                if (!result || result.code !== 200 || !result.data) {
                    select.innerHTML = '<option value="">员工列表加载失败</option>';
                    return;
                }
                var options = '<option value="">请选择员工</option>';
                result.data.forEach(function (employee) {
                    var label = (employee.employeeNumber ? employee.employeeNumber + ' ' : '')
                        + (employee.name || '') + (employee.department ? '（' + employee.department + '）' : '');
                    options += '<option value="' + escapeHtml(employee.id) + '">' + escapeHtml(label) + '</option>';
                });
                select.innerHTML = options;
                if (selectedId) {
                    select.value = String(selectedId);
                }
            })
            .catch(function (error) {
                console.error('加载员工列表出错:', error);
                select.innerHTML = '<option value="">员工列表加载失败</option>';
            });
    }

    function resetModal() {
        document.getElementById('performanceId').value = '';
        document.getElementById('perfScore').value = '';
        document.getElementById('perfEvaluation').value = '';
        document.getElementById('perfStatus').value = '1';
        document.getElementById('modalTitle').textContent = '录入业绩';
        var employeeSelect = document.getElementById('perfEmployeeId');
        if (employeeSelect) {
            employeeSelect.disabled = false;
        }
    }

    /**
     * 打开业绩录入/修改浮层
     * @param {number|null} employeeId    员工ID，为空表示由用户在弹窗里选择
     * @param {number|null} performanceId 已有业绩记录ID，为空表示新增
     */
    function openPerformanceModal(employeeId, performanceId) {
        var modal = document.getElementById('performanceModal');
        if (!modal) {
            return;
        }
        resetModal();

        var yearInput = document.getElementById('reportYear');
        var monthSelect = document.getElementById('reportMonth');
        var reportYear = yearInput ? parseInt(yearInput.value, 10) : currentYear();
        var reportMonth = monthSelect ? parseInt(monthSelect.value, 10) : currentMonth();

        loadEmployeeOptions(employeeId);
        document.getElementById('perfYear').value = String(isNaN(reportYear) ? currentYear() : reportYear);
        document.getElementById('perfQuarter').value = String(
            isNaN(reportMonth) ? currentQuarter() : (Math.floor((reportMonth - 1) / 3) + 1));

        if (performanceId) {
            document.getElementById('performanceId').value = String(performanceId);
            document.getElementById('modalTitle').textContent = '修改业绩';
            var employeeSelect = document.getElementById('perfEmployeeId');
            if (employeeSelect) {
                employeeSelect.disabled = true;
            }
            fetch('/api/performances/' + encodeURIComponent(performanceId), { credentials: 'same-origin' })
                .then(function (response) {
                    return response.json();
                })
                .then(function (result) {
                    if (!result || result.code !== 200 || !result.data) {
                        alertError(result && result.msg ? result.msg : '读取业绩记录失败');
                        return;
                    }
                    var record = result.data;
                    document.getElementById('perfYear').value = String(record.year);
                    document.getElementById('perfQuarter').value = String(record.quarter);
                    document.getElementById('perfScore').value = String(record.score);
                    document.getElementById('perfEvaluation').value = record.evaluation || '';
                    document.getElementById('perfStatus').value = String(record.status === null || record.status === undefined ? 1 : record.status);
                })
                .catch(function (error) {
                    console.error('读取业绩记录出错:', error);
                    alertError('读取业绩记录失败，请稍后重试');
                });
        }

        modal.style.display = 'block';
    }

    function showCreateModal() {
        openPerformanceModal(null, null);
    }

    function closeModal() {
        var modal = document.getElementById('performanceModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    function savePerformance() {
        var performanceId = document.getElementById('performanceId').value;
        var employeeId = document.getElementById('perfEmployeeId').value;
        var year = parseInt(document.getElementById('perfYear').value, 10);
        var quarter = parseInt(document.getElementById('perfQuarter').value, 10);
        var scoreText = document.getElementById('perfScore').value;
        var score = parseFloat(scoreText);
        var evaluation = document.getElementById('perfEvaluation').value;
        var status = parseInt(document.getElementById('perfStatus').value, 10);

        if (!performanceId && !employeeId) {
            alertError('请选择员工');
            return;
        }
        if (isNaN(year) || year < 2000 || year > 2100) {
            alertError('请填写有效的年份');
            return;
        }
        if (isNaN(quarter) || quarter < 1 || quarter > 4) {
            alertError('请选择季度');
            return;
        }
        if (isNaN(score) || score < 0 || score > 100) {
            alertError('评分必须在 0 到 100 之间');
            return;
        }
        if (!evaluation || !evaluation.trim()) {
            alertError('请填写评价');
            return;
        }

        var payload = {
            year: year,
            quarter: quarter,
            score: score,
            evaluation: evaluation.trim(),
            status: isNaN(status) ? 1 : status
        };
        if (!performanceId) {
            payload.employeeId = parseInt(employeeId, 10);
        }

        var url = performanceId
            ? '/api/performances/' + encodeURIComponent(performanceId)
            : '/api/performances';
        var method = performanceId ? 'PUT' : 'POST';

        fetch(url, {
            method: method,
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (result) {
                if (!result || result.code !== 200) {
                    alertError(result && result.msg ? result.msg : '保存业绩失败');
                    return;
                }
                closeModal();
                loadPerformances();
            })
            .catch(function (error) {
                console.error('保存业绩出错:', error);
                alertError('保存业绩失败，请稍后重试');
            });
    }

    /* ---------------- 导出 Excel ---------------- */

    function downloadFile(url, fileName) {
        fetch(url, { credentials: 'same-origin' })
            .then(function (response) {
                var type = response.headers.get('Content-Type') || '';
                if (type.indexOf('spreadsheetml') === -1) {
                    return response.json().then(function (result) {
                        alertError(result && result.msg ? result.msg : '导出失败');
                        throw new Error('EXPORT_FAILED');
                    });
                }
                return response.blob().then(function (blob) {
                    var link = document.createElement('a');
                    var objectUrl = URL.createObjectURL(blob);
                    link.href = objectUrl;
                    link.download = fileName;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    setTimeout(function () {
                        URL.revokeObjectURL(objectUrl);
                    }, 1000);
                });
            })
            .catch(function (error) {
                if (error && error.message === 'EXPORT_FAILED') {
                    return;
                }
                console.error('导出失败:', error);
                alertError('导出失败，请稍后重试');
            });
    }

    function exportReport() {
        var yearInput = document.getElementById('reportYear');
        var monthSelect = document.getElementById('reportMonth');
        if (!yearInput || !monthSelect) {
            return;
        }
        var year = parseInt(yearInput.value, 10);
        var month = parseInt(monthSelect.value, 10);
        if (isNaN(year) || year < 2000 || year > 2100) {
            alertError('请填写有效年份（2000-2100）');
            return;
        }
        if (isNaN(month)) {
            month = currentMonth();
        }
        var monthText = month < 10 ? '0' + month : String(month);
        downloadFile('/api/report/export?year=' + year + '&month=' + month,
            '工资报告-' + year + '-' + monthText + '.xlsx');
    }

    /* ---------------- 对外暴露 ---------------- */

    window.exportReport = exportReport;
    window.loadPerformances = loadPerformances;
    window.showCreateModal = showCreateModal;
    window.openPerformanceModal = openPerformanceModal;
    window.closeModal = closeModal;
    window.savePerformance = savePerformance;
})();
