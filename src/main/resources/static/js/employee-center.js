// 初始化标志位，用于记录是否需要刷新个人信息页面
let shouldRefreshPersonalInfo = false;
// 记录是否是首次加载
let isFirstLoad = true;
// 当前登录员工 ID（最终以服务端 /api/auth/me 为准，防止前端篡改）
let employeeId = sessionStorage.getItem('employeeId');

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    if (window.AuthGuard) {
        // 先校验登录态并同步真实员工 ID，再加载页面内容
        AuthGuard.require('employee').then(function(me) {
            if (me && me.id) {
                employeeId = me.id;
                sessionStorage.setItem('employeeId', me.id);
            }
            showContent('personal-info');
            isFirstLoad = false;
        });
    } else {
        showContent('personal-info');
        isFirstLoad = false;
    }
});

// 加载员工信息
function loadEmployeeInfo() {
    // 从 session 中获取员工 ID
    const employeeId = sessionStorage.getItem('employeeId');
    if (!employeeId) {
        console.error('没有发现员工ID');
        window.location.href = '/login.html';
        return;
    }

    fetch(`/api/employees/${employeeId}`)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                displayEmployeeInfo(data.data);
            } else {
                console.error('加载员工信息失败:', data);
                // 如果未登录，跳转到登录页面
                if (data.msg === '未登录') {
                    window.location.href = '/login.html';
                    return;
                }
                displayEmployeeInfo({});
            }
			
        })
        .catch(error => {
            console.error('加载员工信息时出错:', error);
            displayEmployeeInfo({});
        });
		// window.location.href = window.location.href;
}

// 显示员工信息
function displayEmployeeInfo(employee) {
    const infoHtml = `
        <div class="col-md-4">
            <p><strong>工号：</strong>${employee.employeeNumber || '-'}</p>
            <p><strong>姓名：</strong>${employee.name || '-'}</p>
            <p><strong>部门：</strong>${employee.department || '-'}</p>
        </div>
        <div class="col-md-4">
            <p><strong>职位：</strong>${employee.position || '-'}</p>
            <p><strong>手机：</strong>${employee.phone || '-'}</p>
            <p><strong>邮箱：</strong>${employee.email || '-'}</p>
        </div>
        <div class="col-md-4">
            <p><strong>入职时间：</strong>${formatDate(employee.entryDate) || '-'}</p>
            <p><strong>状态：</strong>${employee.status ? '在职' : '离职'}</p>
        </div>
        <div class="avatar-container">
            <p><strong><img class="avatar" id="employeePhoto" src="" alt="头像"></strong></p>
        </div>
        <div>
            <p><strong><input type="file" id="photoUpload" accept="image/*"></p>
			<button id="refreshBtn" onclick="uploadPhoto()">上传头像</button>
		</div>
        <div class="mt-3">
            <button class="btn btn-outline-secondary btn-sm" onclick="showChangePassword()">修改登录密码</button>
        </div>
        <div id="changePasswordBox"></div>
    `;
    document.getElementById('employeeInfo').innerHTML = infoHtml;
    loadEmployeePhoto();
}

// 加载当前登录员工的头像
function loadEmployeePhoto() {
    const img = document.getElementById('employeePhoto');
    if (!img || !employeeId) {
        return;
    }
    fetch(`/api/employees/${employeeId}/photo`)
        .then(response => {
            if (!response.ok) {
                throw new Error('无头像');
            }
            return response.blob();
        })
        .then(blob => {
            img.src = URL.createObjectURL(blob);
        })
        .catch(() => {
            // 没有头像时保持空白
            img.removeAttribute('src');
        });
}

// 展开修改密码表单
function showChangePassword() {
    const box = document.getElementById('changePasswordBox');
    if (!box) {
        return;
    }
    box.innerHTML = `
        <div class="border rounded p-3 mt-3" style="max-width: 360px;">
            <div class="mb-2"><input type="password" class="form-control" id="oldPassword" placeholder="原密码"></div>
            <div class="mb-2"><input type="password" class="form-control" id="newPassword" placeholder="新密码（至少 6 位）"></div>
            <div class="mb-2"><input type="password" class="form-control" id="confirmPassword" placeholder="确认新密码"></div>
            <button class="btn btn-primary btn-sm" onclick="changePassword()">确认修改</button>
            <button class="btn btn-secondary btn-sm" onclick="document.getElementById('changePasswordBox').innerHTML=''">取消</button>
        </div>`;
}

// 提交修改密码
function changePassword() {
    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!oldPassword || !newPassword) {
        showError('请填写原密码和新密码');
        return;
    }
    if (newPassword.length < 6) {
        showError('新密码至少 6 位');
        return;
    }
    if (newPassword !== confirmPassword) {
        showError('两次输入的新密码不一致');
        return;
    }

    fetch('/api/auth/change-password', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ oldPassword: oldPassword, newPassword: newPassword })
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            document.getElementById('changePasswordBox').innerHTML = '';
            showSuccess('密码修改成功，请牢记新密码');
        } else {
            showError(result.msg || '修改失败');
        }
    })
    .catch(error => {
        console.error('Change password error:', error);
        showError('修改失败：' + error.message);
    });
}

// ---------------- 我的工资 ----------------

var salaryFiltersReady = false;

function formatMoney(value) {
    var number = Number(value === null || value === undefined || value === '' ? 0 : value);
    if (isNaN(number)) {
        return '0.00';
    }
    return number.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function initSalaryFilters() {
    var yearInput = document.getElementById('salaryYear');
    var monthSelect = document.getElementById('salaryMonth');
    if (!yearInput || !monthSelect || salaryFiltersReady) {
        return;
    }
    var now = new Date();
    yearInput.value = String(now.getFullYear());
    monthSelect.innerHTML = '';
    for (var m = 1; m <= 12; m++) {
        var option = document.createElement('option');
        option.value = String(m);
        option.textContent = m + ' 月';
        monthSelect.appendChild(option);
    }
    monthSelect.value = String(now.getMonth() + 1);
    salaryFiltersReady = true;
}

function salaryPeriod() {
    initSalaryFilters();
    var year = document.getElementById('salaryYear').value;
    var month = document.getElementById('salaryMonth').value;
    return { year: year, month: month };
}

function salaryCard(label, value) {
    return '<div class="col-6 col-md-3"><div class="card text-center h-100"><div class="card-body py-2">'
        + '<div class="text-muted small">' + label + '</div>'
        + '<div class="fs-5">' + value + '</div></div></div></div>';
}

function renderSalaryRow(row) {
    var tbody = document.getElementById('salaryTableBody');
    var cards = document.getElementById('salaryCards');
    if (!tbody || !cards) {
        return;
    }
    if (!row) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无工资数据</td></tr>';
        cards.innerHTML = '';
        return;
    }
    var wage = Number(row.wage || 0);
    var bonus = Number(row.bonus || 0);
    var nowWage = Number(row.nowWage || 0);
    var deduction = Math.max(0, wage - nowWage + bonus);
    var scoreText = (row.performanceScore === null || row.performanceScore === undefined) ? '未录入' : row.performanceScore;
    var leavesDay = (row.leavesDay === null || row.leavesDay === undefined) ? 0 : row.leavesDay;
    tbody.innerHTML = '<tr>'
        + '<td>' + row.yearNum + '</td>'
        + '<td>' + row.monthNum + '</td>'
        + '<td>' + row.workDay + '</td>'
        + '<td>¥ ' + formatMoney(row.wage) + '</td>'
        + '<td>' + leavesDay + '</td>'
        + '<td>' + scoreText + '</td>'
        + '<td>¥ ' + formatMoney(row.bonus) + '</td>'
        + '<td class="fw-bold">¥ ' + formatMoney(row.nowWage) + '</td>'
        + '</tr>';
    cards.innerHTML = salaryCard('月薪', '¥ ' + formatMoney(wage))
        + salaryCard('绩效奖金', '¥ ' + formatMoney(bonus))
        + salaryCard('请假扣款', '¥ ' + formatMoney(deduction))
        + salaryCard('实际工资', '¥ ' + formatMoney(nowWage));
}

function loadMySalary() {
    var period = salaryPeriod();
    fetch('/api/report/my?year=' + encodeURIComponent(period.year) + '&month=' + encodeURIComponent(period.month))
        .then(response => response.json())
        .then(result => {
            if (!result || result.code !== 200 || !result.data) {
                renderSalaryRow(null);
                showError(result && result.msg ? result.msg : '查询工资失败');
                return;
            }
            renderSalaryRow(result.data);
        })
        .catch(error => {
            console.error('Load salary error:', error);
            renderSalaryRow(null);
            showError('查询工资失败，请稍后重试');
        });
}

function exportMySalary() {
    var period = salaryPeriod();
    var month = period.month;
    if (month.length < 2) {
        month = '0' + month;
    }
    fetch('/api/report/my/export?year=' + encodeURIComponent(period.year) + '&month=' + encodeURIComponent(period.month))
        .then(response => {
            var type = response.headers.get('Content-Type') || '';
            if (type.indexOf('spreadsheetml') === -1) {
                return response.json().then(result => {
                    showError(result && result.msg ? result.msg : '导出失败');
                });
            }
            return response.blob().then(blob => {
                var link = document.createElement('a');
                var objectUrl = URL.createObjectURL(blob);
                link.href = objectUrl;
                link.download = '工资明细-' + period.year + '-' + month + '.xlsx';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                setTimeout(function () {
                    URL.revokeObjectURL(objectUrl);
                }, 1000);
            });
        })
        .catch(error => {
            console.error('Export salary error:', error);
            showError('导出失败，请稍后重试');
        });
}

// 显示内容区域
function showContent(sectionId) {
    try {
        // 隐藏所有内容区域
        document.querySelectorAll('.content-section').forEach(section => {
            section.style.display = 'none';
        });
        
        // 显示选中的内容区域
        const selectedSection = document.getElementById(sectionId);
        if (selectedSection) {
            selectedSection.style.display = 'block';
        }
        
        // 更新导航栏活动状态
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        const activeLink = document.querySelector(`[onclick="showContent('${sectionId}')"]`);
        if (activeLink) {
            activeLink.classList.add('active');
        }

        // 加载相应的数据
        switch(sectionId) {
            case 'personal-info':
                if (shouldRefreshPersonalInfo && !isFirstLoad) {
                    // 强制刷新页面
                    window.location.reload(true); 
                    return; // 刷新后直接返回，避免后续代码执行
                }
                loadEmployeeInfo();
                // 重置标志位
                shouldRefreshPersonalInfo = false; 
                break;
            case 'leave-manage':
                // 切换到其他页面时，设置标志位为 true，下次进入个人信息页面会刷新
                shouldRefreshPersonalInfo = true; 
                loadMyLeaves();
                break;
            case 'notice-view':
                // 切换到其他页面时，设置标志位为 true，下次进入个人信息页面会刷新
                shouldRefreshPersonalInfo = true; 
                loadNotices();
                break;
            case 'salary-view':
                shouldRefreshPersonalInfo = true;
                initSalaryFilters();
                loadMySalary();
                break;
        }
    } catch (error) {
        console.error('Error showing content:', error);
    }
}

// 加载请假记录
function loadMyLeaves() {
    // 获取当前登录的员工ID
    const employeeId = sessionStorage.getItem('employeeId');
    if (!employeeId) {
        console.error('No employee ID found');
        return;
    }

    // 使用员工ID查询请假记录
    fetch(`/api/leaves/employee/${employeeId}`)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                displayLeaves(data.data || []);
            } else {
                console.error('Failed to load leaves:', data);
                displayLeaves([]);
            }
        })
        .catch(error => {
            console.error('Error loading leaves:', error);
            displayLeaves([]);
        });
}

// 加载公告列表
function loadNotices() {
    fetch('/api/notices/published')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                console.error('Failed to load notices:', data);
                displayNotices([]);
            }
        })
        .catch(error => {
            console.error('Error loading notices:', error);
            displayNotices([]);
        });
}

// 格式化日期
function formatDate(dateString) {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('zh-CN');
}

// 格式化日期时间
function formatDateTime(datetime) {
    if (!datetime) return '-';
    return new Date(datetime).toLocaleString('zh-CN');
}

// 获取请假类型文本
function getLeaveTypeText(type) {
    const types = {
        1: '事假',
        2: '病假',
        3: '年假',
        4: '调休'
    };
    return types[type] || '未知';
}

// 获取请假状态文本
function getLeaveStatusText(status) {
    const statuses = {
        0: '<span class="badge bg-warning">待审核</span>',
        1: '<span class="badge bg-success">已通过</span>',
        2: '<span class="badge bg-danger">已拒绝</span>'
    };
    return statuses[status] || '未知';
}

// 显示错误提示
function showError(message) {
    const errorModal = document.getElementById('errorModal');
    const errorMessage = document.getElementById('errorMessage');
    if (errorModal && errorMessage) {
        errorMessage.textContent = message;
        const modal = new bootstrap.Modal(errorModal);
        modal.show();
    } else {
        alert(message);
    }
}

// 显示成功提示
function showSuccess(message) {
    const successModal = document.getElementById('successModal');
    const successMessage = document.getElementById('successMessage');
    if (successModal && successMessage) {
        successMessage.textContent = message;
        const modal = new bootstrap.Modal(successModal);
        modal.show();
        setTimeout(() => modal.hide(), 2000);
    } else {
        alert(message);
    }
}

// 退出登录
function logout() {
    if (confirm('确定要退出登录吗？')) {
        fetch('/api/auth/logout', {
            method: 'POST'
        })
        .then(() => {
            window.location.href = '/login.html';
        })
        .catch(() => {
            window.location.href = '/login.html';
        });
    }
}

// 显示请假记录
function displayLeaves(leaves) {
    let html = '';
    if (leaves && leaves.length > 0) {
        leaves.forEach(leave => {
            html += `
                <tr>
                    <td>${getLeaveTypeText(leave.type)}</td>
                    <td>${formatDateTime(leave.startTime)}</td>
                    <td>${formatDateTime(leave.endTime)}</td>
                    <td>${leave.leavesDay}</td>
                    <td>${getLeaveStatusText(leave.status)}</td>
                    <td>${leave.approver ? leave.approver.name : '-'}</td>
                    <td>${leave.comment || '-'}</td>
                    <td>
                        ${leave.status === 0 ? 
                            `<button class="btn btn-sm btn-danger" onclick="cancelLeave(${leave.id})">撤销</button>` : 
                            '-'}
                    </td>
                </tr>
            `;
        });
    }
    document.getElementById('leaveTableBody').innerHTML = 
        html || '<tr><td colspan="7" class="text-center">暂无请假记录</td></tr>';
}

// 显示公告列表
function displayNotices(notices) {
    let html = '';
    if (notices && notices.length > 0) {
        notices.forEach(notice => {
            html += `
                <div class="card mb-3">
                    <div class="card-body">
                        <h5 class="card-title">${notice.title}</h5>
                        <p class="card-text">${notice.content}</p>
                        <div class="text-muted">
                            <small>发布时间：${formatDateTime(notice.publishTime)}</small>
                        </div>
                    </div>
                </div>
            `;
        });
    } else {
        html = '<div class="alert alert-info">暂无公告</div>';
    }
    document.getElementById('noticeList').innerHTML = html;
}

// 显示申请请假模态框
function showApplyLeaveModal() {
    // 重置表单
    document.getElementById('leaveForm').reset();

    
    // 设置日期时间选择器的最小值为当前时间
    const now = new Date();
    const minDateTime = now.toISOString().slice(0, 16);  // 格式：YYYY-MM-DDTHH:mm
    document.getElementById('leaveStartTime').min = minDateTime;
    document.getElementById('leaveEndTime').min = minDateTime;

    // 显示模态框
    const modal = new bootstrap.Modal(document.getElementById('leaveModal'));
    modal.show();
}
// 提交请假申请
function submitLeave() {
    // 获取当前登录的员工ID
    
    if (!employeeId) {
        showError('未找到员工信息，请重新登录');
        window.location.href = '/login.html';
        return;
    }

    const data = {
        employeeId: employeeId, // 添加员工ID
        type: parseInt(document.getElementById('leaveType').value),
        startTime: document.getElementById('leaveStartTime').value + ':00',
        endTime: document.getElementById('leaveEndTime').value + ':00',
        reason: document.getElementById('leaveReason').value.trim(),
        leavesDay:document.getElementById('leavesDay').value
    };

    // 表单验证
    if (!data.type || !data.startTime || !data.endTime || !data.reason || !data.leavesDay) {
        showError('请填写所有必填项');
        return;
    }

    // 验证时间
    const startTime = new Date(data.startTime);
    const endTime = new Date(data.endTime);
    const now = new Date();

    if (startTime < now) {
        showError('开始时间不能早于当前时间');
        return;
    }
    if (endTime <= startTime) {
        showError('结束时间必须晚于开始时间');
        return;
    }

    fetch('/api/leaves', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            const modal = bootstrap.Modal.getInstance(document.getElementById('leaveModal'));
            modal.hide();
            loadMyLeaves();
            showSuccess('请假申请提交成功');
        } else {
            showError(result.msg || '提交失败');
        }
    })
    .catch(error => {
        console.error('Submit error:', error);
        showError('提交失败：' + error.message);
    });
}

// 撤销请假申请
function cancelLeave(id) {
    if (!confirm('确定要撤销这条请假申请吗？')) {
        return;
    }

    fetch(`/api/leaves/${id}/cancel`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            loadMyLeaves();
            showSuccess('请假申请已撤销');
        } else {
            showError(result.msg || '撤销失败');
        }
    })
    .catch(error => {
        console.error('Cancel error:', error);
        showError('撤销失败：' + error.message);
    });
}
//图片文件上传表单
function uploadPhoto() {
    const input = document.getElementById('photoUpload');
    const file = input && input.files ? input.files[0] : null;
    if (!file) {
        showError('请先选择一个图片文件');
        return;
    }
    if (file.size > 5 * 1024 * 1024) {
        showError('图片大小不能超过 5MB');
        return;
    }

    const formData = new FormData();
    formData.append('photo', file);

    fetch(`/api/employees/${employeeId}/upload-photo`, {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            showSuccess('头像上传成功');
            loadEmployeePhoto();
        } else {
            showError(result.msg || '上传失败');
        }
    })
    .catch(error => {
        console.error('Upload error:', error);
        showError('上传失败：' + error.message);
    });
}
	  
// document.getElementById('refreshBtn').addEventListener('click', function() {
//   // 强制从服务器加载
//   window.location.reload(true);
// });

// 确保所有需要的函数都在全局作用域可用
window.showContent = showContent;
window.logout = logout;
window.showApplyLeaveModal = showApplyLeaveModal;
window.submitLeave = submitLeave;
window.cancelLeave = cancelLeave;
window.uploadPhoto = uploadPhoto;
window.showChangePassword = showChangePassword;
window.changePassword = changePassword;
window.loadMySalary = loadMySalary;
window.exportMySalary = exportMySalary; 