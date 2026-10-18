/**
 * 管理端审计日志：多条件筛选 + 分页。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var PAGE_SIZE = 15;
    var currentPage = 1;
    var actionsReady = false;

    function statusBadge(log) {
        var action = log.action || '';
        var cls = '';
        if (action.indexOf('CANCEL') >= 0 || action.indexOf('DELETE') >= 0) {
            cls = 'badge-danger';
        } else if (action.indexOf('LOGIN') >= 0) {
            cls = 'badge-info';
        } else if (action.indexOf('UPDATE') >= 0 || action.indexOf('SAVE') >= 0) {
            cls = 'badge-warn';
        } else {
            cls = 'badge-ok';
        }
        return '<span class="badge ' + cls + '">' + Utils.escapeHtml(log.actionName || action) + '</span>';
    }

    function targetCell(log) {
        var name = log.targetName ? Utils.escapeHtml(log.targetName) : '-';
        var type = log.targetType ? Utils.escapeHtml(log.targetType) : '';
        return '<div>' + name + (log.targetId ? ' <span class="muted" style="font-size:12px;">#' + log.targetId + '</span>' : '') + '</div>'
            + (type ? '<div class="muted" style="font-size:12px;">' + type + '</div>' : '');
    }

    function render(pageData) {
        var body = document.getElementById('auditTableBody');
        var rows = pageData && pageData.list ? pageData.list : [];
        document.getElementById('auditHint').textContent = '共 ' + (pageData ? pageData.total : 0) + ' 条';

        if (!rows.length) {
            body.innerHTML = '<tr class="empty-row"><td colspan="6">没有符合条件的日志</td></tr>';
        } else {
            body.innerHTML = rows.map(function (log) {
                return '<tr>'
                    + '<td class="muted">' + Utils.dateTime(log.createTime) + '</td>'
                    + '<td><strong>' + Utils.escapeHtml(log.operatorName || '-') + '</strong>'
                    + '<div class="muted" style="font-size:12px;">' + Utils.escapeHtml(log.operatorType || '') + '</div></td>'
                    + '<td>' + statusBadge(log) + '</td>'
                    + '<td>' + targetCell(log) + '</td>'
                    + '<td style="max-width:420px;font-size:13px;">' + Utils.escapeHtml(log.detail || '') + '</td>'
                    + '<td class="muted">' + Utils.escapeHtml(log.ip || '-') + '</td>'
                    + '</tr>';
            }).join('');
        }

        renderPager(pageData);
    }

    function renderPager(pageData) {
        var pager = document.getElementById('auditPager');
        if (!pageData || pageData.pages <= 1) {
            pager.innerHTML = '';
            return;
        }
        var page = pageData.page;
        var pages = pageData.pages;
        pager.innerHTML = '<span class="hint">共 ' + pageData.total + ' 条，第 ' + page + '/' + pages + ' 页</span>'
            + '<button class="btn btn-ghost btn-sm" ' + (page <= 1 ? 'disabled' : '')
            + ' onclick="goAuditPage(' + (page - 1) + ')">上一页</button>'
            + '<button class="btn btn-ghost btn-sm" ' + (page >= pages ? 'disabled' : '')
            + ' onclick="goAuditPage(' + (page + 1) + ')">下一页</button>';
    }

    function loadActions() {
        if (actionsReady) {
            return Promise.resolve();
        }
        return Api.get('/api/audit-logs/actions').then(function (map) {
            var select = document.getElementById('auditAction');
            var html = '<option value="">全部动作</option>';
            Object.keys(map || {}).forEach(function (key) {
                html += '<option value="' + key + '">' + Utils.escapeHtml(map[key]) + '</option>';
            });
            select.innerHTML = html;
            actionsReady = true;
        }).catch(function () {
            actionsReady = false;
        });
    }

    function buildUrl(page) {
        var params = ['page=' + page, 'size=' + PAGE_SIZE];
        var operator = document.getElementById('auditOperator').value.trim();
        var action = document.getElementById('auditAction').value;
        var targetType = document.getElementById('auditTargetType').value;
        var startDate = document.getElementById('auditStartDate').value;
        var endDate = document.getElementById('auditEndDate').value;
        if (operator) {
            params.push('operator=' + encodeURIComponent(operator));
        }
        if (action) {
            params.push('action=' + encodeURIComponent(action));
        }
        if (targetType) {
            params.push('targetType=' + encodeURIComponent(targetType));
        }
        if (startDate) {
            params.push('startDate=' + startDate);
        }
        if (endDate) {
            params.push('endDate=' + endDate);
        }
        return '/api/audit-logs?' + params.join('&');
    }

    function loadAuditLogs(page) {
        currentPage = page || 1;
        loadActions().then(function () {
            Api.get(buildUrl(currentPage))
                .then(render)
                .catch(function (error) {
                    Toast.error(error.message);
                    document.getElementById('auditTableBody').innerHTML =
                        '<tr class="empty-row"><td colspan="6">加载失败</td></tr>';
                });
        });
    }

    function goAuditPage(page) {
        if (page < 1) {
            return;
        }
        loadAuditLogs(page);
    }

    function resetAuditFilter() {
        document.getElementById('auditOperator').value = '';
        document.getElementById('auditAction').value = '';
        document.getElementById('auditTargetType').value = '';
        document.getElementById('auditStartDate').value = '';
        document.getElementById('auditEndDate').value = '';
        loadAuditLogs(1);
    }

    window.loadAuditLogs = loadAuditLogs;
    window.goAuditPage = goAuditPage;
    window.resetAuditFilter = resetAuditFilter;
})(window);
