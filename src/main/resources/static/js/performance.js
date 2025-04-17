/**
 * 加载业绩列表
 */
function loadPerformances() {
    console.log('Loading performances...');
    const department = $('#departmentFilter').val();
    const month = $('#monthFilter').val();
    const name = $('#searchName').val().trim();

    let url = '/api/performances';
    if (department || month || name) {
        url += '?';
        if (department) url += `department=${encodeURIComponent(department)}&`;
        if (month) url += `month=${month}&`;
        if (name) url += `name=${encodeURIComponent(name)}`;
    }

    $.ajax({
        url: url,
        method: 'GET',
        success: function(response) {
            console.log('Performances response:', response);
            if (response.code === 200) {
                displayPerformances(response.data || []);
            } else {
                showError('加载业绩列表失败：' + response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('Error loading performances:', error);
            console.error('Response:', xhr.responseText);
            showError('加载业绩列表失败：' + error);
        }
    });
}

/**
 * 显示业绩列表
 */
function displayPerformances(performances) {
    console.log('Displaying performances:', performances);
    let html = '';
    if (performances && performances.length > 0) {
        performances.forEach(function(perf) {
            html += `
                <tr>
                    <td>${perf.employee ? perf.employee.name : '-'}</td>
                    <td>${perf.employee ? perf.employee.department : '-'}</td>
                    <td>${perf.month}</td>
                    <td>${perf.workScore}</td>
                    <td>${perf.attitudeScore}</td>
                    <td>${perf.attendanceScore}</td>
                    <td>${perf.totalScore}</td>
                    <td>${perf.evaluator ? perf.evaluator.name : '-'}</td>
                    <td>
                        <button class="btn btn-sm btn-info me-2" onclick="viewPerformance(${perf.id})">查看</button>
                        <button class="btn btn-sm btn-primary" onclick="editPerformance(${perf.id})">编辑</button>
                    </td>
                </tr>
            `;
        });
    }
    $('#performanceTableBody').html(html || '<tr><td colspan="9" class="text-center">暂无业绩记录</td></tr>');
}

/**
 * 显示添加业绩评估模态框
 */
function showAddPerformanceModal() {
    $('#performanceId').val('');
    $('#performanceForm')[0].reset();
    $('#performanceModalTitle').text('添加业绩评估');
    loadEmployeeOptions();
    $('#performanceModal').modal('show');
}

/**
 * 加载员工选项
 */
function loadEmployeeOptions() {
    $.ajax({
        url: '/api/employees',
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                let html = '<option value="">选择员工</option>';
                response.data.forEach(function(emp) {
                    html += `<option value="${emp.id}">${emp.name} - ${emp.department}</option>`;
                });
                $('#employeeSelect').html(html);
            }
        }
    });
}

/**
 * 保存业绩评估
 */
function savePerformance() {
    const id = $('#performanceId').val();
    const data = {
        employeeId: $('#employeeSelect').val(),
        month: $('#performanceMonth').val(),
        workScore: parseInt($('#workScore').val()),
        attitudeScore: parseInt($('#attitudeScore').val()),
        attendanceScore: parseInt($('#attendanceScore').val()),
        comment: $('#performanceComment').val().trim()
    };

    // 表单验证
    if (!data.employeeId) {
        showError('请选择员工');
        return;
    }
    if (!data.month) {
        showError('请选择评估月份');
        return;
    }
    if (isNaN(data.workScore) || data.workScore < 0 || data.workScore > 100) {
        showError('工作表现评分必须在0-100之间');
        return;
    }
    if (isNaN(data.attitudeScore) || data.attitudeScore < 0 || data.attitudeScore > 100) {
        showError('工作态度评分必须在0-100之间');
        return;
    }
    if (isNaN(data.attendanceScore) || data.attendanceScore < 0 || data.attendanceScore > 100) {
        showError('出勤评分必须在0-100之间');
        return;
    }
    if (!data.comment) {
        showError('请输入评价内容');
        return;
    }

    $.ajax({
        url: '/api/performances' + (id ? `/${id}` : ''),
        method: id ? 'PUT' : 'POST',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(response) {
            if (response.code === 200) {
                $('#performanceModal').modal('hide');
                loadPerformances();
                showSuccess(id ? '更新成功' : '创建成功');
            } else {
                showError(response.message || '保存失败');
            }
        },
        error: function(xhr, status, error) {
            showError('保存失败：' + error);
        }
    });
}

/**
 * 查看业绩详情
 */
function viewPerformance(id) {
    $.ajax({
        url: `/api/performances/${id}`,
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                const perf = response.data;
                $('#performanceDetail').html(`
                    <div class="mb-3">
                        <h6>基本信息</h6>
                        <p>员工：${perf.employee.name}</p>
                        <p>部门：${perf.employee.department}</p>
                        <p>月份：${perf.month}</p>
                        <p>评估人：${perf.evaluator.name}</p>
                    </div>
                    <div class="mb-3">
                        <h6>评分详情</h6>
                        <p>工作表现：${perf.workScore}</p>
                        <p>工作态度：${perf.attitudeScore}</p>
                        <p>出勤情况：${perf.attendanceScore}</p>
                        <p>总评分：${perf.totalScore}</p>
                    </div>
                    <div>
                        <h6>评价内容</h6>
                        <p>${perf.comment}</p>
                    </div>
                `);
                $('#viewModal').modal('show');
            } else {
                showError(response.message || '获取详情失败');
            }
        },
        error: function(xhr, status, error) {
            showError('获取详情失败：' + error);
        }
    });
}

// 页面加载完成后初始化
$(document).ready(function() {
    loadPerformances();
    // 加载部门选项
    $.ajax({
        url: '/api/departments',
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                let html = '<option value="">选择部门</option>';
                response.data.forEach(function(dept) {
                    html += `<option value="${dept.name}">${dept.name}</option>`;
                });
                $('#departmentFilter').html(html);
            }
        }
    });
    // 设置月份默认值为当前月
    $('#monthFilter').val(new Date().toISOString().slice(0,7));
}); 