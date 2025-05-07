/**
 * 筛选员工列表
 */
function filterEmployees() {
    // 获取搜索输入框中的关键词，并去除首尾空格
    const keyword = document.getElementById('searchInput').value.trim();
    // 获取部门筛选下拉框中选中的部门
    const department = document.getElementById('departmentFilter').value;
    // 获取员工状态筛选下拉框中选中的状态
    const status = document.getElementById('statusFilter').value;

    let url = '/api/employees/search?';
    // 创建 URLSearchParams 对象，用于构建请求参数
    const params = new URLSearchParams();
    
    if (keyword) params.append('keyword', keyword);
    if (department) params.append('department', department);
    if (status) params.append('status', status);

    // 发送 GET 请求，将基础 URL 和请求参数拼接后作为请求地址
    fetch(url + params.toString())
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                displayEmployees(data.data || []);
            } else {
                showError('搜索失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Search error:', error);
            showError('搜索失败：' + error.message);
        });
}

/**
 * 加载员工列表
 */
function loadEmployees() {
    console.log('Loading employees...');

    
    // 确保表格主体元素存在
    const tableBody = document.getElementById('employeeTableBody');
    if (!tableBody) {
        console.error('Employee table body not found');
        return;
    }

    // 获取筛选条件
    const department = document.getElementById('departmentFilter')?.value;
    const status = document.getElementById('statusFilter')?.value;
    const keyword = document.getElementById('searchInput')?.value?.trim();

    // 构建URL
    let url = '/api/employees';
    
    // 如果有任何搜索条件，使用search端点
    if (keyword || department || status) {
        url += '/search';
        const params = new URLSearchParams();
        if (keyword) params.append('keyword', keyword);
        if (department) params.append('department', department);
        if (status) params.append('status', status === 'true');
        url += '?' + params.toString();
    }

    // 显示加载状态
    tableBody.innerHTML = '<tr><td colspan="10" class="text-center">加载中...</td></tr>';

    // 加载部门列表
    loadDepartmentOptions();

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            console.log('Employees response:', data);
            if (data.code === 200) {
                displayEmployees(data.data || []);
            } else {
                showError('加载员工列表失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading employees:', error);
            showError('加载员工列表失败：' + error.message);
            // 显示错误状态
            tableBody.innerHTML = '<tr><td colspan="10" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 显示员工列表
 */
function displayEmployees(employees) {
    console.log('Displaying employees:', employees);
    const tableBody = document.getElementById('employeeTableBody');
    if (!tableBody) {
        console.error('Employee table body not found');
        return;
    }

    let html = '';
    if (employees && employees.length > 0) {
        employees.forEach(function(emp) {
            html += `
                <tr>
                    <td>${emp.employeeNumber}</td>
                    <td>${emp.name}</td>
                    <td>${emp.department}</td>
                    <td>${emp.position}</td>
                    <td>${emp.phone || '-'}</td>
                    <td>${emp.email || '-'}</td>
                    <td>${emp.wage}</td>
                    <td>${formatDate(emp.entryDate)}</td>
                    <td>
                        <span class="badge ${emp.status ? 'bg-success' : 'bg-secondary'}">
                            ${emp.status ? '在职' : '离职'}
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-primary me-2" onclick="editEmployee(${emp.id})">编辑</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteEmployee(${emp.id})">删除</button>
                    </td>
                </tr>
            `;
        });
    }
    tableBody.innerHTML = html || '<tr><td colspan="10" class="text-center">暂无员工数据</td></tr>';
}

/**
 * 加载部门选项
 */
function loadDepartmentOptions() {
    console.log('Loading department options...');
    fetch('/api/departments')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                let html = '<option value="">选择部门</option>';
                data.data.forEach(dept => {
                    html += `<option value="${dept.name}">${dept.name}</option>`;
                });
                
                // 更新所有包含部门选项的下拉框
                const selectors = ['employeeDepartment', 'departmentFilter'];
                selectors.forEach(id => {
                    const element = document.getElementById(id);
                    if (element) {
                        element.innerHTML = html;
                        console.log(`Updated ${id} with options`);
                    } else {
                        console.log(`Element ${id} not found`);
                    }
                });
            } else {
                console.error('Failed to load departments:', data);
            }
        })
        .catch(error => {
            console.error('Error loading departments:', error);
        });
}

/**
 * 显示添加员工模态框
 */
function showAddEmployeeModal() {
    document.getElementById('employeeForm').reset();
    document.getElementById('employeeId').value = '';
    document.getElementById('employeeModalTitle').textContent = '添加员工';
    
    loadDepartmentOptions();
    
    const modal = new bootstrap.Modal(document.getElementById('employeeModal'));
    modal.show();
}

/**
 * 编辑员工
 */
function editEmployee(id) {
    loadDepartmentOptions();
    
    fetch(`/api/employees/${id}`)
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                const emp = result.data;
                console.log('员工信息：',emp)
                
                // 填充表单
                document.getElementById('employeeId').value = emp.id;
                document.getElementById('employeeName').value = emp.name;
                document.getElementById('employeeDepartment').value = emp.department;
                document.getElementById('employeePosition').value = emp.position;
                document.getElementById('employeePhone').value = emp.phone || '';
                document.getElementById('employeeEmail').value = emp.email || '';
                document.getElementById('employeeWage').value = emp.wage || '';
                document.getElementById('employeeStatus').value = emp.status.toString();
                // 处理日期格式 yyyy-MM-dd
                if (emp.entryDate) {
                    document.getElementById('employeeHireDate').value = 
                        new Date(emp.entryDate).toISOString().split('T')[0];
                }
                
                document.getElementById('employeeModalTitle').textContent = '编辑员工';
                
                const modal = new bootstrap.Modal(document.getElementById('employeeModal'));
                modal.show();
            } else {
                showError(result.msg || '获取员工信息失败');
            }
        });
        // .catch(error => {
        //     console.error('Edit error:', error);
        //     showError('获取员工信息失败：' + error.message);
        // });
}

/**
 * 保存员工信息
 */
function saveEmployee() {
    const id = document.getElementById('employeeId').value;
    const data = {
        name: document.getElementById('employeeName').value.trim(),
        department: document.getElementById('employeeDepartment').value,
        position: document.getElementById('employeePosition').value.trim(),
        phone: document.getElementById('employeePhone').value.trim(),
        email: document.getElementById('employeeEmail').value.trim(),
        wage:document.getElementById('employeeWage').value,
        entryDate: document.getElementById('employeeHireDate').value,
        status: document.getElementById('employeeStatus').value === 'true',
    };

    // 验证必填字段
    if (!data.name || !data.department || !data.position || !data.phone || !data.entryDate) {
        showError('请填写所有必填项');
        return;
    }

    // employees.forEach(function(emp) {
    //     if(data.name==emp.name && data.phone==emp.phone) {
    //         showError('重复的姓名和号码');
    //         return;
    //     }
    // });
    
    // 验证手机号格式
    if (!/^1[3-9]\d{9}$/.test(data.phone)) {
        showError('请输入正确的手机号');
        return;
    }

    // 验证日期格式
    if (new Date(data.entryDate) > new Date()) {
        showError('入职日期不能大于当前日期');
        return;
    }

    fetch('/api/employees' + (id ? `/${id}` : ''), {
        method: id ? 'PUT' : 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            const modal = bootstrap.Modal.getInstance(document.getElementById('employeeModal'));
            modal.hide();
            loadEmployees();
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
 * 删除员工
 */
function deleteEmployee(id) {
    if (confirm('确定要删除这个员工吗？')) {
        fetch(`/api/employees/${id}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                loadEmployees();
                showSuccess('删除成功');
            } else {
                showError('删除失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Delete error:', error);
            showError('删除失败：' + error.message);
        });
    }
}

/**
 * 格式化日期
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('zh-CN');
}

// 确保在页面加载完成后再执行初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('Employee page initialized');
    loadEmployees();
    
    // 只在员工管理页面加载部门选项
    const employeeDepartment = document.getElementById('employeeDepartment');
    if (employeeDepartment) {
        loadDepartmentOptions();
    }
});

// 确保函数在全局作用域可用
window.loadEmployees = loadEmployees;
window.loadDepartmentOptions = loadDepartmentOptions;
window.showAddEmployeeModal = showAddEmployeeModal;
window.editEmployee = editEmployee;
window.deleteEmployee = deleteEmployee;
window.saveEmployee = saveEmployee;
window.filterEmployees = filterEmployees;

// 在文件中添加这两个函数
function showError(message) {
    const errorModal = document.getElementById('errorModal');
    const errorMessage = document.getElementById('errorMessage');
    if (errorModal && errorMessage) {
        errorMessage.textContent = message;
        const modal = new bootstrap.Modal(errorModal);
        modal.show();
    } else {
        alert(message); // 如果找不到模态框，就使用alert
    }
}

function showSuccess(message) {
    const successModal = document.getElementById('successModal');
    const successMessage = document.getElementById('successMessage');
    if (successModal && successMessage) {
        successMessage.textContent = message;
        const modal = new bootstrap.Modal(successModal);
        modal.show();
        setTimeout(() => modal.hide(), 2000); // 2秒后自动关闭
    } else {
        alert(message); // 如果找不到模态框，就使用alert
    }
} 