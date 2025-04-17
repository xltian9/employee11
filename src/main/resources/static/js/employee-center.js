// 初始化标志位，用于记录是否需要刷新个人信息页面
let shouldRefreshPersonalInfo = false;
// 记录是否是首次加载
let isFirstLoad = true;

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 默认显示个人信息页面
    showContent('personal-info');
    // 首次加载完成，将标志位设为 false
    isFirstLoad = false;
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
    `;
    document.getElementById('employeeInfo').innerHTML = infoHtml;
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
const employeeId = sessionStorage.getItem('employeeId');
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
        reason: document.getElementById('leaveReason').value.trim()
    };

    // 表单验证
    if (!data.type || !data.startTime || !data.endTime || !data.reason) {
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
    const file = document.getElementById('photoUpload').files[0];
    const formData = new FormData();
    formData.append('photo', file);

    fetch(`/api/employees/${employeeId}/upload-photo`, {
        method: 'POST',
        body: formData
    }).then(response => response.text())
      .then(data => console.log(data));
	   window.location.reload(true);
}

// 从后端加载照片
fetch(`/api/employees/${employeeId}/photo`)
    .then(response => response.blob())
    .then(blob => {
        const url = URL.createObjectURL(blob);
        document.getElementById('employeePhoto').src = url;
    });
	  
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