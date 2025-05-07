/**
 * 加载请假记录
 */
function loadLeaves() {
    console.log('Loading leaves...');

    // 确保表格主体元素存在
    const tableBody = document.getElementById('leaveTableBody');
    if (!tableBody) {
        console.error('Leave table body not found');
        return;
    }

    // 获取筛选状态
    const status = document.getElementById('leaveStatus')?.value;

    // 根据状态选择不同的API端点
    let url = '/api/leaves';
    if (status === '0') {
        url = '/api/leaves/pending';  // 待审核
    } else if (status === '1' || status === '2') {
        // 使用search接口按状态筛选
        url = `/api/leaves/search?status=${status}`;
    }
    // 如果status为空，则获取所有记录

    // 显示加载状态
    tableBody.innerHTML = '<tr><td colspan="6" class="text-center">加载中...</td></tr>';

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            console.log('Leaves response:', data);
            if (data.code === 200) {
                displayLeaves(data.data || []);
            } else {
                showError('加载请假记录失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading leaves:', error);
            showError('加载请假记录失败：' + error.message);
            // 显示错误状态
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 显示请假记录
 */
function displayLeaves(leaves) {
    console.log('Displaying leaves:', leaves);
    let html = '';
    if (leaves && leaves.length > 0) {
        leaves.forEach(function(leave) {
            html += `
                <tr>
                    <td>${leave.employee ? leave.employee.name : '-'}</td>
                    <td>${getLeaveTypeText(leave.type)}</td>
                    <td>${formatDateTime(leave.startTime)}</td>
                    <td>${formatDateTime(leave.endTime)}</td>
                    <td>${leave.leavesDay}</td>
                    <td>${getLeaveStatusText(leave.status)}</td>
                    <td>${leave.approver ? leave.approver.name : '-'}</td>
                    <td>
                        <button class="btn btn-sm btn-info me-2" onclick="viewLeave(${leave.id})">查看</button>
                        ${leave.status === 0 ? `
                            <button class="btn btn-sm btn-primary" onclick="showApproveModal(${leave.id})">审批</button>
                        ` : ''}
                    </td>
                </tr>
            `;
        });
    }
    const tableBody = document.getElementById('leaveTableBody');
    if (tableBody) {
        tableBody.innerHTML = html || '<tr><td colspan="7" class="text-center">暂无请假记录</td></tr>';
    } else {
        console.error('Leave table body not found');
    }
}

/**
 * 显示申请请假模态框
 */
function showApplyLeaveModal() {
    console.log('Showing apply leave modal');
    document.getElementById('leaveForm').reset();

    // 设置日期时间选择器的最小值为当前时间
    const now = new Date();
    const minDateTime = now.toISOString().slice(0, 16);  // 格式：YYYY-MM-DDTHH:mm
    document.getElementById('leaveStartTime').min = minDateTime;
    document.getElementById('leaveEndTime').min = minDateTime;

    const modal = new bootstrap.Modal(document.getElementById('leaveModal'));
    modal.show();
}

/**
 * 提交请假申请
 */
function submitLeave() {
    console.log('Submitting leave request');
    const data = {
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

    // 获取保存按钮并禁用
    const saveButton = document.querySelector('#leaveModal .modal-footer .btn-primary');
    if (saveButton) {
        saveButton.disabled = true;
    }

    fetch('/api/leaves', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                const modal = bootstrap.Modal.getInstance(document.getElementById('leaveModal'));
                modal.hide();
                loadLeaves();
                showSuccess('请假申请提交成功');
            } else {
                showError(data.message || '提交失败');
            }
        })
        .catch(error => {
            console.error('Submit error:', error);
            showError('提交失败：' + error.message);
        })
        .finally(() => {
            // 在finally中重新启用按钮
            if (saveButton) {
                saveButton.disabled = false;
            }
        });
}

/**
 * 显示审批模态框
 */
function showApproveModal(leaveId) {
    document.getElementById('leaveId').value = leaveId;
    document.getElementById('approveForm').reset();
    const modal = new bootstrap.Modal(document.getElementById('approveModal'));
    modal.show();
}

/**
 * 审批请假
 */
function approveLeave(status) {
    const leaveId = document.getElementById('leaveId').value;
    const comment = document.getElementById('approveComment').value.trim();

    if (!comment) {
        showError('请输入审批意见');
        return;
    }

    const params = new URLSearchParams();
    params.append('status', status);
    params.append('comment', comment);

    // 禁用按钮防止重复提交
    const buttons = document.querySelectorAll('#approveModal .modal-footer button');
    buttons.forEach(btn => btn.disabled = true);

    fetch(`/api/leaves/${leaveId}/approve?${params.toString()}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            const modal = bootstrap.Modal.getInstance(document.getElementById('approveModal'));
            modal.hide();
            loadLeaves();
            showSuccess('审批完成');
        } else {
            showError(data.msg || '审批失败');
        }
    })
    .catch(error => {
        console.error('Approve error:', error);
        showError('审批失败：' + error.message);
    })
    .finally(() => {
        // 恢复按钮状态
        buttons.forEach(btn => btn.disabled = false);
    });
}

/**
 * 获取请假类型文本
 */
function getLeaveTypeText(type) {
    switch (parseInt(type)) {
        case 1: return '事假';
        case 2: return '病假';
        case 3: return '年假';
        case 4: return '调休';
        default: return '未知';
    }
}

/**
 * 获取请假状态文本
 */
function getLeaveStatusText(status) {
    switch (parseInt(status)) {
        case 0: return '<span class="badge bg-warning">待审核</span>';
        case 1: return '<span class="badge bg-success">已通过</span>';
        case 2: return '<span class="badge bg-danger">已拒绝</span>';
        default: return '<span class="badge bg-secondary">未知</span>';
    }
}

/**
 * 格式化日期时间
 */
function formatDateTime(datetime) {
    if (!datetime) return '-';
    const date = new Date(datetime);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * 显示错误提示
 */
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

/**
 * 显示成功提示
 */
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

/**
 * 查看请假详情
 */
function viewLeave(id) {
    console.log('Viewing leave:', id);
    fetch(`/api/leaves/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                const leave = data.data;
                // 构建详情HTML
                const detailHtml = `
                    <div class="mb-3">
                        <h6>基本信息</h6>
                        <p>申请人：${leave.employee ? leave.employee.name : '-'}</p>
                        <p>请假类型：${getLeaveTypeText(leave.type)}</p>
                        <p>开始时间：${formatDateTime(leave.startTime)}</p>
                        <p>结束时间：${formatDateTime(leave.endTime)}</p>
                        <p>状态：${getLeaveStatusText(leave.status)}</p>
                    </div>
                    <div class="mb-3">
                        <h6>请假原因</h6>
                        <p>${leave.reason || '-'}</p>
                    </div>
                    ${leave.status !== 0 ? `
                        <div class="mb-3">
                            <h6>审批信息</h6>
                            <p>审批人：${leave.approver ? leave.approver.name : '-'}</p>
                            <p>审批时间：${formatDateTime(leave.approveTime)}</p>
                            <p>审批意见：${leave.comment || '-'}</p>
                        </div>
                    ` : ''}
                `;

                document.getElementById('leaveDetail').innerHTML = detailHtml;
                const modal = new bootstrap.Modal(document.getElementById('viewLeaveModal'));
                modal.show();
            } else {
                showError('获取请假详情失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error viewing leave:', error);
            showError('获取请假详情失败：' + error.message);
        });
}

/**
 * 筛选请假记录
 */
function filterLeaves() {
    console.log('Filtering leaves...');
    loadLeaves(); // 重用loadLeaves函数的逻辑
}

// 页面加载完成后初化
document.addEventListener('DOMContentLoaded', function() {
    console.log('Leave page initialized');
    loadLeaves();
});

// 确保函数在全局作用域可用
window.loadLeaves = loadLeaves;
window.showApplyLeaveModal = showApplyLeaveModal;
window.submitLeave = submitLeave;
window.showApproveModal = showApproveModal;
window.approveLeave = approveLeave;
window.displayLeaves = displayLeaves;
window.getLeaveTypeText = getLeaveTypeText;
window.getLeaveStatusText = getLeaveStatusText;
window.formatDateTime = formatDateTime;
window.viewLeave = viewLeave;
window.filterLeaves = filterLeaves;