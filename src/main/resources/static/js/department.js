/**
 * 加载部门列表
 */


function loadDepartments() {
    console.log('Loading departments...');
    fetch('/api/departments')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            console.log('Departments response:', data);
            if (data.code === 200) {
                displayDepartments(data.data || []);
            } else {
                showError('加载部门列表失败：' + data.msg);
            }
        })
        .catch(error => {
            console.error('Error loading departments:', error);
            showError('加载部门列表失败：' + error.message);
        });
}

/**
 * 显示部门列表
 */
function displayDepartments(departments) {
    console.log('Displaying departments:', departments);
    let html = '';
    if (departments && departments.length > 0) {
        departments.forEach(function(dept) {
            html += `
                <tr>
                    <td>${dept.name}</td>
                    <td>${dept.description || '-'}</td>
                    <td>${dept.manager || '-'}</td>
                    <td>${formatDateTime(dept.createTime)}</td>
                    <td>
                        <button class="btn btn-sm btn-primary me-2" onclick="editDepartment(${dept.id})">编辑</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteDepartment(${dept.id})">删除</button>
                    </td>
                </tr>
            `;
        });
    }
    const tableBody = document.getElementById('departmentTableBody');
    if (tableBody) {
        tableBody.innerHTML = html || '<tr><td colspan="5" class="text-center">暂无部门数据</td></tr>';
    } else {
        console.error('Department table body not found');
    }
}

/**
 * 格式化日期时间
 */
function formatDateTime(datetime) {
    if (!datetime) return '-';
    return new Date(datetime).toLocaleString('zh-CN');
}

/**
 * 显示错误提示
 */
function showError(message) {
    alert(message); // 临时使用alert，后续可以改为模态框
}

/**
 * 显示添加部门模态框
 */
function showAddDepartmentModal() {
    // 重置表单
    document.getElementById('departmentForm').reset();
    document.getElementById('departmentId').value = '';
    document.getElementById('departmentModalTitle').textContent = '添加部门';
    
    // 显示模态框
    const modal = new bootstrap.Modal(document.getElementById('departmentModal'));
    modal.show();
}

/**
 * 保存部门信息
 */
function saveDepartment() {
    // 获取表单数据
    const id = document.getElementById('departmentId').value;
    const data = {
        name: document.getElementById('departmentName').value.trim(),
        description: document.getElementById('departmentDescription').value.trim(),
        manager: document.getElementById('departmentManager').value.trim()
    };

    // 验证必填字段
    if (!data.name) {
        showError('部门名称不能为空');
        return;
    }

    // 发送请求
    fetch('/api/departments' + (id ? `/${id}` : ''), {
        method: id ? 'PUT' : 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            // 关闭模态框
            const modal = bootstrap.Modal.getInstance(document.getElementById('departmentModal'));
            modal.hide();
            
            // 刷新列表
            loadDepartments();
            
            // 显示成功提示
            showSuccess(id ? '更新成功' : '添加成功');
        } else {
            showError(result.msg || '保存失败');
        }
    })
    .catch(error => {
        console.error('Save error:', error);
        showError('保存失败：' + error.message);
    });
}

/**
 * 编辑部门
 */
function editDepartment(id) {
    // 获取部门信息
    fetch(`/api/departments/${id}`)
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                const dept = result.data;
                
                // 填充表单
                document.getElementById('departmentId').value = dept.id;
                document.getElementById('departmentName').value = dept.name;
                document.getElementById('departmentDescription').value = dept.description || '';
                document.getElementById('departmentManager').value = dept.manager || '';
                
                // 更新标题
                document.getElementById('departmentModalTitle').textContent = '编辑部门';
                
                // 显示模态框
                const modal = new bootstrap.Modal(document.getElementById('departmentModal'));
                modal.show();
            } else {
                showError(result.msg || '获取部门信息失败');
            }
        })
        .catch(error => {
            console.error('Edit error:', error);
            showError('获取部门信息失败：' + error.message);
        });
}

/**
 * 删除部门
 */
function deleteDepartment(id) {
    if (confirm('确定要删除这个部门吗？')) {
        fetch(`/api/departments/${id}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                loadDepartments(); // 重新加载部门列表
            } else {
                showError('删除失败：' + data.msg);
            }
        })
        .catch(error => {
            showError('删除失败：' + error.message);
        });
    }
}

/**
 * 确保函数在全局作用域可用
 */
window.loadDepartments = loadDepartments;
window.showAddDepartmentModal = showAddDepartmentModal;
window.saveDepartment = saveDepartment;
window.editDepartment = editDepartment;
window.deleteDepartment = deleteDepartment; 