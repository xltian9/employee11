/**
 * 加载业绩列表
 */
// function loadPerformances() {
//     console.log('正在加载业绩数据...');
//     const department = jQuery('#departmentFilter').val();
//     const month = jQuery('#monthFilter').val();
//     const name = jQuery('#searchName').val().trim();
//     // API 接口地址
//     const apiUrl = '/api/performances'; 

//     let url = '/api/performances';
//     if (department || month || name) {
//         url += '?';
//         if (department) url += `department=${encodeURIComponent(department)}&`;
//         if (month) url += `month=${month}&`;
//         if (name) url += `name=${encodeURIComponent(name)}`;
//     }

//     jQuery.ajax({
//         url: url,
//         method: 'GET',
//         success: function(response) {
//             console.log('业绩数据响应:', response);
//             if (response.code === 200) {
//                 displayPerformances(response.data || []);
//             } else {
//                 showError('加载业绩列表失败：' + response.message);
//             }
//         },
//         error: function(xhr, status, error) {
//             console.error('加载业绩数据出错:', error);
//             console.error('响应内容:', xhr.responseText);
//             showError('加载业绩列表失败：' + error);
//         }
//     });
// }

function displayPerformances(data) {
    // 将业绩数据显示在页面上的逻辑
    const performanceTableBody = document.getElementById('performanceTableBody');
    performanceTableBody.innerHTML = '';
    data.forEach(performance => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${performance.id}</td>
            <td>${performance.employeeName}</td>
            <td>${performance.amount}</td>
            <td>${performance.date}</td>
        `;
        performanceTableBody.appendChild(row);
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
    const employeeId = $('#employeeSelect').val();
    // 检查 employeeId 是否有效
    if (!employeeId) {
        showError('请选择员工');
        return;
    }
    const data = {
        employeeId: parseInt(employeeId),
        month: $('#performanceMonth').val(),
        workScore: parseInt($('#workScore').val()),
        attitudeScore: parseInt($('#attitudeScore').val()),
        attendanceScore: parseInt($('#attendanceScore').val()),
        comment: $('#performanceComment').val().trim()
    };

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

function getPerformanceById(id) {
    // 检查 id 是否有效
    if (!id) {
        showError('未提供有效的业绩记录 ID');
        return;
    }
    $.ajax({
        url: `/api/performances/${id}`,
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                const performance = response.data;
                // 这里可以根据需求处理获取到的业绩记录，比如显示在模态框中
                console.log('获取到的业绩记录:', performance);
            } else {
                showError('获取业绩记录失败：' + response.message);
            }
        },
        error: function(xhr, status, error) {
            showError('获取业绩记录失败：' + error);
        }
    });
}


function loadPerformances() {
    console.log('正在加载业绩数据...');
    $.ajax({
        url: '/api/performances',
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                const performances = response.data;
                const tableBody = $('#performanceBody');
                tableBody.empty();

                performances.forEach(function(performance) {
                    const row = `
                        <tr>
                            <td>${performance.employee.id}</td>
                            <td>${performance.year}</td>
                            <td>${performance.quarter}</td>
                            <td>${performance.score}</td>
                            <td>${performance.evaluation}</td>
                            <td>${performance.status === 0 ? '待确认' : '已确认'}</td>
                            <td>
                                <button onclick="showEditModal(${performance.id})">编辑</button>
                                <button onclick="deletePerformance(${performance.id})">删除</button>
                            </td>
                        </tr>
                    `;
                    tableBody.append(row);
                });
            } else {
                showError('加载业绩记录失败：' + response.message);
            }
        },
        error: function(xhr, status, error) {
            showError('加载业绩记录失败：' + error);
        }
    });
}

function showCreateModal() {
    $('#modalTitle').text('新建业绩');
    $('#performanceId').val('');
    $('#employeeId').val('');
    $('#year').val('');
    $('#quarter').val('');
    $('#score').val('');
    $('#evaluation').val('');
    $('#status').val('0');
    $('#performanceModal').show();
}

function showEditModal(id) {
    $.ajax({
        url: `/api/performances/${id}`,
        method: 'GET',
        success: function(response) {
            if (response.code === 200) {
                const performance = response.data;
                $('#modalTitle').text('编辑业绩');
                $('#performanceId').val(performance.id);
                $('#employeeId').val(performance.employee.id);
                $('#year').val(performance.year);
                $('#quarter').val(performance.quarter);
                $('#score').val(performance.score);
                $('#evaluation').val(performance.evaluation);
                $('#status').val(performance.status);
                $('#performanceModal').show();
            } else {
                showError('获取业绩记录失败：' + response.message);
            }
        },
        error: function(xhr, status, error) {
            showError('获取业绩记录失败：' + error);
        }
    });
}

function savePerformance() {
    const id = $('#performanceId').val();
    const employeeId = parseInt($('#employeeId').val());
    // 检查 employeeId 是否有效
    if (isNaN(employeeId)) {
        showError('请选择有效的员工');
        return;
    }
    const data = {
        employeeId: parseInt($('#employeeId').val()),
        year: parseInt($('#year').val()),
        quarter: parseInt($('#quarter').val()),
        score: parseFloat($('#score').val()),
        evaluation: $('#evaluation').val(),
        status: parseInt($('#status').val())
    };
    console.log('Saving performance:', data);
    const url = id ? `/api/performances/${id}` : '/api/performances';
    const method = id ? 'PUT' : 'POST';

    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        beforeSend: function(xhr) {
            console.log('Request Headers:', xhr.getAllResponseHeaders());
            console.log('Request Body:', JSON.stringify(data));
        },
        success: function(response) {
            if (response.code === 200) {
                $('#performanceModal').hide();
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

function deletePerformance(id) {
    if (confirm('确定要删除这条业绩记录吗？')) {
        $.ajax({
            url: `/api/performances/${id}`,
            method: 'DELETE',
            success: function(response) {
                if (response.code === 200) {
                    loadPerformances();
                    showSuccess('删除成功');
                } else {
                    showError('删除失败：' + response.message);
                }
            },
            error: function(xhr, status, error) {
                showError('删除失败：' + error);
            }
        });
    }
}

function closeModal() {
    $('#performanceModal').hide();
}

function showSuccess(message) {
    alert(message);
}

function showError(message) {
    alert(message);
}

// 页面加载时加载业绩记录
$(document).ready(function() {
    loadPerformances();
});

// 页面加载完成后初始化
$(document).ready(function() {
    loadPerformances();
    // 加载部门选项
    let deptUrl = '/api/departments';
    // 添加时间戳参数
    deptUrl += `?_=${new Date().getTime()}`;
    $.ajax({
        url: deptUrl,
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