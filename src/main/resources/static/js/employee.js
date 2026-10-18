/** 每页条数 */
const EMPLOYEE_PAGE_SIZE = 10;
/** 当前页码（从 1 开始） */
let employeePage = 1;
/** 总记录数 / 总页数 */
let employeeTotal = 0;
let employeePages = 0;

/**
 * 筛选员工列表（条件变化时回到第 1 页）
 */
function filterEmployees() {
    employeePage = 1;
    loadEmployees(1);
}

/**
 * 加载员工列表（服务端分页）
 *
 * @param {number} [page] 目标页码，不传则沿用当前页
 */
function loadEmployees(page) {
    // 确保表格主体元素存在
    const tableBody = document.getElementById('employeeTableBody');
    if (!tableBody) {
        console.error('Employee table body not found');
        return;
    }

    if (page && page > 0) {
        employeePage = page;
    }

    // 获取筛选条件
    const department = document.getElementById('departmentFilter')?.value || '';
    const status = document.getElementById('statusFilter')?.value || '';
    const keyword = document.getElementById('searchInput')?.value?.trim() || '';

    const params = new URLSearchParams();
    params.append('page', employeePage);
    params.append('size', EMPLOYEE_PAGE_SIZE);
    if (keyword) params.append('keyword', keyword);
    if (department) params.append('department', department);
    if (status) params.append('status', status);

    // 显示加载状态
    tableBody.innerHTML = '<tr><td colspan="10" class="text-center">加载中...</td></tr>';

    // 加载部门列表
    loadDepartmentOptions();

    fetch('/api/employees/page?' + params.toString(), { credentials: 'same-origin' })
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                const pageData = data.data || {};
                employeeTotal = pageData.total || 0;
                employeePages = pageData.pages || 0;
                // 删除最后一页的最后一条后，页码可能越界，自动回退
                if (employeePages > 0 && employeePage > employeePages) {
                    employeePage = employeePages;
                    loadEmployees(employeePage);
                    return;
                }
                displayEmployees(pageData.list || []);
                renderPager();
            } else {
                showError('加载员工列表失败：' + (data.msg || data.message));
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
 * 跳转到指定页
 */
function goEmployeePage(page) {
    if (page < 1 || (employeePages > 0 && page > employeePages)) {
        return;
    }
    loadEmployees(page);
}

/**
 * 渲染分页条
 */
function renderPager() {
    const pager = document.getElementById('employeePager');
    if (!pager) {
        return;
    }
    if (!employeePages || employeePages <= 1) {
        pager.innerHTML = employeeTotal
            ? `<span class="text-muted">共 ${employeeTotal} 条记录</span>`
            : '';
        return;
    }
    const disabledFirst = employeePage <= 1 ? 'disabled' : '';
    const disabledLast = employeePage >= employeePages ? 'disabled' : '';
    pager.innerHTML = `
        <span class="text-muted me-3">共 ${employeeTotal} 条记录，第 ${employeePage}/${employeePages} 页</span>
        <button class="btn btn-sm btn-outline-secondary me-1" ${disabledFirst} onclick="goEmployeePage(1)">首页</button>
        <button class="btn btn-sm btn-outline-secondary me-1" ${disabledFirst} onclick="goEmployeePage(${employeePage - 1})">上一页</button>
        <button class="btn btn-sm btn-outline-secondary me-1" ${disabledLast} onclick="goEmployeePage(${employeePage + 1})">下一页</button>
        <button class="btn btn-sm btn-outline-secondary" ${disabledLast} onclick="goEmployeePage(${employeePages})">末页</button>
    `;
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