/**
 * 员工管理：分页列表、筛选、新增/编辑、照片上传。
 *
 * 后端接口：
 *  - GET  /api/employees/page?page&size&keyword&department&status
 *  - GET  /api/employees/{id}
 *  - POST /api/employees          （新增，password 留空时初始密码 = 手机号）
 *  - PUT  /api/employees/{id}     （修改）
 *  - DELETE /api/employees/{id}   （逻辑删除）
 *  - POST /api/employees/{id}/upload-photo   （multipart，字段名 file）
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var PAGE_SIZE = 10;
    var state = { page: 1, total: 0, pages: 0, keyword: '', department: '', status: '' };
    var departmentOptions = [];

    function photoUrl(id) {
        return '/api/employees/' + id + '/photo?t=' + new Date().getTime();
    }

    /** 日期选择器给的是 yyyy-MM-dd，后端 Jackson 统一按 yyyy-MM-dd HH:mm:ss 解析 Date */
    function dateToDateTime(value) {
        if (!value) {
            return null;
        }
        return value.length === 10 ? value + ' 00:00:00' : value;
    }

    function render(rows) {
        var body = document.getElementById('employeeTableBody');
        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="11">没有符合条件的员工</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (row) {
            var statusBadge = row.status
                ? '<span class="badge badge-ok">在职</span>'
                : '<span class="badge">离职</span>';
            return '<tr>'
                + '<td><img src="' + photoUrl(row.id) + '" alt="" onerror="this.style.visibility=\'hidden\'"'
                + ' style="width:34px;height:34px;object-fit:cover;border-radius:50%;border:1px solid #e5e7eb;background:#f8fafc;"></td>'
                + '<td class="muted">' + Utils.escapeHtml(row.employeeNumber || '-') + '</td>'
                + '<td><strong>' + Utils.escapeHtml(row.name || '') + '</strong></td>'
                + '<td>' + Utils.escapeHtml(row.department || '-') + '</td>'
                + '<td>' + Utils.escapeHtml(row.position || '-') + '</td>'
                + '<td>' + Utils.escapeHtml(row.phone || '-') + '</td>'
                + '<td class="muted">' + Utils.escapeHtml(row.email || '-') + '</td>'
                + '<td class="num">' + Utils.money(row.wage) + '</td>'
                + '<td class="muted">' + Utils.date(row.entryDate) + '</td>'
                + '<td class="center">' + statusBadge + '</td>'
                + '<td>'
                + '<button class="link-btn" onclick="editEmployee(' + row.id + ')">编辑</button>'
                + '<button class="link-btn danger" onclick="deleteEmployee(' + row.id + ')">删除</button>'
                + '</td>'
                + '</tr>';
        }).join('');
    }

    function renderPager(total, page, pages) {
        var host = document.getElementById('employeePager');
        if (!host) {
            return;
        }
        if (total === 0) {
            host.innerHTML = '';
            return;
        }
        host.innerHTML = '<span>共 ' + total + ' 人，第 ' + page + '/' + pages + ' 页</span>'
            + '<button class="btn btn-sm btn-ghost" onclick="goEmployeePage(1)"' + (page <= 1 ? ' disabled' : '') + '>首页</button>'
            + '<button class="btn btn-sm btn-ghost" onclick="goEmployeePage(' + (page - 1) + ')"' + (page <= 1 ? ' disabled' : '') + '>上一页</button>'
            + '<button class="btn btn-sm btn-ghost" onclick="goEmployeePage(' + (page + 1) + ')"' + (page >= pages ? ' disabled' : '') + '>下一页</button>'
            + '<button class="btn btn-sm btn-ghost" onclick="goEmployeePage(' + pages + ')"' + (page >= pages ? ' disabled' : '') + '>末页</button>';
    }

    function loadEmployees(page) {
        state.page = page || state.page || 1;
        var query = '/api/employees/page?page=' + state.page + '&size=' + PAGE_SIZE;
        if (state.keyword) {
            query += '&keyword=' + encodeURIComponent(state.keyword);
        }
        if (state.department) {
            query += '&department=' + encodeURIComponent(state.department);
        }
        if (state.status) {
            query += '&status=' + state.status;
        }
        Api.get(query).then(function (data) {
            state.total = data.total || 0;
            state.pages = data.pages || 0;
            state.page = data.page || 1;
            render(data.list);
            renderPager(state.total, state.page, state.pages);
            var summary = document.getElementById('employeeSummary');
            if (summary) {
                summary.textContent = '共 ' + state.total + ' 人';
            }
        }).catch(function (error) {
            Toast.error(error.message);
            document.getElementById('employeeTableBody').innerHTML =
                '<tr class="empty-row"><td colspan="11">加载失败</td></tr>';
        });
    }

    function goEmployeePage(page) {
        if (page < 1 || (state.pages && page > state.pages)) {
            return;
        }
        loadEmployees(page);
    }

    function filterEmployees() {
        state.keyword = (document.getElementById('searchInput').value || '').trim();
        state.department = document.getElementById('departmentFilter').value || '';
        state.status = document.getElementById('statusFilter').value || '';
        loadEmployees(1);
    }

    function resetEmployeeFilter() {
        document.getElementById('searchInput').value = '';
        document.getElementById('departmentFilter').value = '';
        document.getElementById('statusFilter').value = '';
        state.keyword = '';
        state.department = '';
        state.status = '';
        loadEmployees(1);
    }

    /* ------------------------------ 下拉选项 ------------------------------ */

    function loadDepartmentOptions(selected) {
        Api.get('/api/departments').then(function (list) {
            departmentOptions = list || [];
            var select = document.getElementById('employeeDepartment');
            var filter = document.getElementById('departmentFilter');
            if (select) {
                select.innerHTML = departmentOptions.map(function (dept) {
                    return '<option value="' + Utils.escapeHtml(dept.name) + '">'
                        + Utils.escapeHtml(dept.name) + '</option>';
                }).join('');
                if (selected) {
                    select.value = selected;
                }
            }
            if (filter) {
                var current = filter.value;
                filter.innerHTML = '<option value="">全部部门</option>'
                    + departmentOptions.map(function (dept) {
                        return '<option value="' + Utils.escapeHtml(dept.name) + '">'
                            + Utils.escapeHtml(dept.name) + '</option>';
                    }).join('');
                filter.value = current;
            }
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    /* ------------------------------ 弹层 ------------------------------ */

    function showAddEmployeeModal() {
        document.getElementById('employeeId').value = '';
        document.getElementById('employeeName').value = '';
        document.getElementById('employeeNumber').value = '';
        document.getElementById('employeePosition').value = '';
        document.getElementById('employeePhone').value = '';
        document.getElementById('employeeEmail').value = '';
        document.getElementById('employeeWage').value = '';
        document.getElementById('employeeEntryDate').value = Utils.today();
        document.getElementById('employeeStatus').value = 'true';
        document.getElementById('employeePhotoPreview').src = '';
        document.getElementById('employeeModalTitle').textContent = '新增员工';
        loadDepartmentOptions();
        document.getElementById('employeeModal').hidden = false;
    }

    function hideEmployeeModal() {
        document.getElementById('employeeModal').hidden = true;
    }

    function editEmployee(id) {
        Api.get('/api/employees/' + id).then(function (employee) {
            document.getElementById('employeeId').value = employee.id;
            document.getElementById('employeeName').value = employee.name || '';
            document.getElementById('employeeNumber').value = employee.employeeNumber || '';
            document.getElementById('employeePosition').value = employee.position || '';
            document.getElementById('employeePhone').value = employee.phone || '';
            document.getElementById('employeeEmail').value = employee.email || '';
            document.getElementById('employeeWage').value = employee.wage == null ? '' : employee.wage;
            document.getElementById('employeeEntryDate').value = Utils.date(employee.entryDate);
            document.getElementById('employeeStatus').value = employee.status ? 'true' : 'false';
            document.getElementById('employeePhotoPreview').src = photoUrl(employee.id);
            document.getElementById('employeeModalTitle').textContent = '编辑员工 · ' + (employee.name || '');
            loadDepartmentOptions(employee.department);
            document.getElementById('employeeModal').hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function collectEmployee() {
        var wageText = document.getElementById('employeeWage').value;
        return {
            name: document.getElementById('employeeName').value.trim(),
            employeeNumber: document.getElementById('employeeNumber').value.trim() || null,
            department: document.getElementById('employeeDepartment').value,
            position: document.getElementById('employeePosition').value.trim(),
            phone: document.getElementById('employeePhone').value.trim(),
            email: document.getElementById('employeeEmail').value.trim(),
            wage: wageText === '' ? 0 : Number(wageText),
            entryDate: dateToDateTime(document.getElementById('employeeEntryDate').value),
            status: document.getElementById('employeeStatus').value === 'true'
        };
    }

    function saveEmployee() {
        var id = document.getElementById('employeeId').value;
        var payload = collectEmployee();
        if (!payload.name) {
            Toast.warning('姓名不能为空');
            return;
        }
        if (!payload.department) {
            Toast.warning('请选择部门');
            return;
        }
        if (!payload.position) {
            Toast.warning('职位不能为空');
            return;
        }
        var request = id ? Api.put('/api/employees/' + id, payload) : Api.post('/api/employees', payload);
        request.then(function () {
            hideEmployeeModal();
            Toast.success(id ? '员工信息已更新' : '员工已创建，初始密码为手机号');
            loadEmployees(id ? state.page : 1);
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function uploadEmployeePhoto() {
        var id = document.getElementById('employeeId').value;
        if (!id) {
            Toast.warning('请先保存员工，再上传照片');
            return;
        }
        var input = document.getElementById('employeePhotoFile');
        if (!input.files || input.files.length === 0) {
            Toast.warning('请选择图片文件');
            return;
        }
        var form = new FormData();
        form.append('file', input.files[0]);
        Api.post('/api/employees/' + id + '/upload-photo', form).then(function () {
            document.getElementById('employeePhotoPreview').src = photoUrl(id);
            input.value = '';
            Toast.success('照片已上传');
            loadEmployees(state.page);
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function deleteEmployee(id) {
        if (!window.confirm('删除后该员工将不再出现在花名册与工资报告中，确定删除？')) {
            return;
        }
        Api.del('/api/employees/' + id).then(function () {
            Toast.success('员工已删除');
            loadEmployees(state.page);
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    window.loadEmployees = function () {
        loadDepartmentOptions();
        loadEmployees(1);
    };
    window.goEmployeePage = goEmployeePage;
    window.filterEmployees = filterEmployees;
    window.resetEmployeeFilter = resetEmployeeFilter;
    window.showAddEmployeeModal = showAddEmployeeModal;
    window.hideEmployeeModal = hideEmployeeModal;
    window.editEmployee = editEmployee;
    window.saveEmployee = saveEmployee;
    window.uploadEmployeePhoto = uploadEmployeePhoto;
    window.deleteEmployee = deleteEmployee;
})(window);
