/**
 * 部门管理：列表、搜索、新建、编辑、删除。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    function render(departments) {
        var body = document.getElementById('departmentTableBody');
        if (!departments || departments.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="6">暂无部门数据</td></tr>';
            return;
        }
        body.innerHTML = departments.map(function (dept, index) {
            return '<tr>'
                + '<td class="muted">' + (index + 1) + '</td>'
                + '<td><strong>' + Utils.escapeHtml(dept.name || '') + '</strong></td>'
                + '<td>' + Utils.escapeHtml(dept.manager || '-') + '</td>'
                + '<td class="muted">' + Utils.escapeHtml(dept.description || '-') + '</td>'
                + '<td class="muted">' + Utils.dateTime(dept.createTime) + '</td>'
                + '<td>'
                + '<button class="link-btn" onclick="editDepartment(' + dept.id + ')">编辑</button>'
                + '<button class="link-btn danger" onclick="deleteDepartment(' + dept.id + ')">删除</button>'
                + '</td>'
                + '</tr>';
        }).join('');
    }

    function loadDepartments() {
        Api.get('/api/departments').then(render).catch(function (error) {
            Toast.error(error.message);
            document.getElementById('departmentTableBody').innerHTML =
                '<tr class="empty-row"><td colspan="6">加载失败</td></tr>';
        });
    }

    function searchDepartments() {
        var keyword = document.getElementById('departmentKeyword').value.trim();
        if (!keyword) {
            loadDepartments();
            return;
        }
        Api.get('/api/departments/search?keyword=' + encodeURIComponent(keyword))
            .then(render)
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function showAddDepartmentModal() {
        document.getElementById('departmentId').value = '';
        document.getElementById('departmentName').value = '';
        document.getElementById('departmentManager').value = '';
        document.getElementById('departmentDescription').value = '';
        document.getElementById('departmentModalTitle').textContent = '新建部门';
        document.getElementById('departmentModal').hidden = false;
    }

    function hideDepartmentModal() {
        document.getElementById('departmentModal').hidden = true;
    }

    function editDepartment(id) {
        Api.get('/api/departments/' + id).then(function (dept) {
            document.getElementById('departmentId').value = dept.id;
            document.getElementById('departmentName').value = dept.name || '';
            document.getElementById('departmentManager').value = dept.manager || '';
            document.getElementById('departmentDescription').value = dept.description || '';
            document.getElementById('departmentModalTitle').textContent = '编辑部门';
            document.getElementById('departmentModal').hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function saveDepartment() {
        var id = document.getElementById('departmentId').value;
        var payload = {
            name: document.getElementById('departmentName').value.trim(),
            manager: document.getElementById('departmentManager').value.trim(),
            description: document.getElementById('departmentDescription').value.trim()
        };
        if (!payload.name) {
            Toast.warning('部门名称不能为空');
            return;
        }
        var request = id ? Api.put('/api/departments/' + id, payload) : Api.post('/api/departments', payload);
        request.then(function () {
            hideDepartmentModal();
            Toast.success(id ? '部门已更新' : '部门已创建');
            loadDepartments();
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function deleteDepartment(id) {
        if (!window.confirm('删除后该部门下若有员工会保留部门名称快照，确定删除？')) {
            return;
        }
        Api.del('/api/departments/' + id).then(function () {
            Toast.success('部门已删除');
            loadDepartments();
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    window.loadDepartments = loadDepartments;
    window.searchDepartments = searchDepartments;
    window.showAddDepartmentModal = showAddDepartmentModal;
    window.hideDepartmentModal = hideDepartmentModal;
    window.editDepartment = editDepartment;
    window.saveDepartment = saveDepartment;
    window.deleteDepartment = deleteDepartment;
})(window);
