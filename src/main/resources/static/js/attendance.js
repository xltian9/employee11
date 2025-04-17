/**
 * 更新当前时间显示
 * 每秒更新一次显示的时间
 */
function updateCurrentTime() {
    const now = new Date();
    $('#currentTime').text(now.toLocaleTimeString());
}

/**
 * 员工签到
 * 记录当前时间作为签到时间
 */
function checkIn() {
    // 这里暂时使用固定的员工ID，实际应该从登录信息获取
    const employeeId = 1; // 测试用，应该替换为实际的员工ID
    console.log('Checking in for employee:', employeeId);
    
    $.ajax({
        url: `/api/attendance/check-in/${employeeId}`,
        method: 'POST',
        contentType: 'application/json',
        success: function(response) {
            console.log('Check-in response:', response);
            if (response.code === 200) {
                showSuccess('签到成功！');
                loadTodayAttendance();
            } else {
                showError(response.message || '签到失败');
            }
        },
        error: function(xhr, status, error) {
            console.error('Check-in error:', error);
            showError('签到失败：' + error);
        }
    });
}

/**
 * 员工签退
 * 更新考勤记录的签退时间
 */
function checkOut() {
    // 获取今日考勤记录的ID
    const attendanceId = $('#todayAttendance').data('attendance-id');
    if (!attendanceId) {
        showError('未找到今日签到记录');
        return;
    }

    console.log('Checking out attendance:', attendanceId); // 添加调试日志

    $.ajax({
        url: `/api/attendance/check-out/${attendanceId}`,
        method: 'POST',
        contentType: 'application/json',
        success: function(response) {
            console.log('Check-out response:', response); // 添加调试日志
            if (response.code === 200) {
                showSuccess('签退成功！');
                loadTodayAttendance();
            } else {
                showError(response.message || '签退失败');
            }
        },
        error: function(xhr, status, error) {
            console.error('Check-out error:', error);
            console.error('Status:', status);
            console.error('Response:', xhr.responseText);
            showError('签退失败：' + (xhr.responseJSON?.message || error));
        }
    });
}

/**
 * 加载今日考勤信息
 * 显示当前员工的今日考勤状态和历史记录
 */
function loadTodayAttendance() {
    const employeeId = 1; // 测试用，应该替换为实际的员工ID
    console.log('Loading attendance for employee:', employeeId);

    $.ajax({
        url: `/api/attendance/employee/${employeeId}`,
        method: 'GET',
        success: function(response) {
            console.log('Attendance response:', response);
            if (response.code === 200) {
                const attendances = response.data;
                // 显示今日考勤状态
                displayTodayAttendance(attendances);
                // 显示考勤记录列表
                displayAttendanceRecords(attendances);
            } else {
                showError(response.message || '加载考勤信息失败');
            }
        },
        error: function(xhr, status, error) {
            console.error('Load attendance error:', error);
            showError('加载考勤信息失败：' + error);
        }
    });
}

/**
 * 显示今日考勤状态
 * @param {Array} attendances - 考勤记录列表
 */
function displayTodayAttendance(attendances) {
    const today = new Date().toISOString().split('T')[0];
    const todayAttendance = attendances.find(a => 
        a.checkInTime && a.checkInTime.startsWith(today));
    
    if (todayAttendance) {
        $('#todayAttendance')
            .data('attendance-id', todayAttendance.id)
            .html(`
                <div class="card">
                    <div class="card-body">
                        <h6 class="card-title">今日考勤状态</h6>
                        <p class="mb-1">签到时间：${formatTime(todayAttendance.checkInTime)}</p>
                        <p class="mb-1">签退时间：${todayAttendance.checkOutTime ? formatTime(todayAttendance.checkOutTime) : '-'}</p>
                        <p class="mb-1">状态：${todayAttendance.status || '正常'}</p>
                    </div>
                </div>
            `);
    } else {
        $('#todayAttendance')
            .removeData('attendance-id')
            .html(`
                <div class="card">
                    <div class="card-body">
                        <h6 class="card-title">今日考勤状态</h6>
                        <p class="mb-0">今日未签到</p>
                    </div>
                </div>
            `);
    }
}

/**
 * 显示考勤记录列表
 * @param {Array} attendances - 考勤记录列表
 */
function displayAttendanceRecords(attendances) {
    let html = '';
    // 按日期降序排序
    attendances.sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime));
    
    attendances.forEach(record => {
        const date = new Date(record.checkInTime).toLocaleDateString();
        html += `
            <tr>
                <td>${date}</td>
                <td>${formatTime(record.checkInTime)}</td>
                <td>${record.checkOutTime ? formatTime(record.checkOutTime) : '-'}</td>
                <td>${record.status || '正常'}</td>
                <td>${record.note || '-'}</td>
            </tr>
        `;
    });
    
    $('#attendanceTableBody').html(html || '<tr><td colspan="5" class="text-center">暂无考勤记录</td></tr>');
}

/**
 * 格式化时间显示
 * @param {string} timeString - ISO格式的时间字符串
 * @returns {string} 格式化后的时间字符串
 */
function formatTime(timeString) {
    if (!timeString) return '-';
    const date = new Date(timeString);
    return date.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

/**
 * 显示成功提示
 * @param {string} message - 成功提示信息
 */
function showSuccess(message) {
    const alertHtml = `
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    $('#content').prepend(alertHtml);
}

/**
 * 显示错误提示
 * @param {string} message - 错误提示信息
 */
function showError(message) {
    console.error('Error:', message);
    const alertHtml = `
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    $('#content').prepend(alertHtml);
}

// 页面加载完成后的初始化
$(document).ready(function() {
    console.log('Attendance page initialized');
    // 更新当前时间
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);
    
    // 加载考勤信息
    loadTodayAttendance();
    
    // 每分钟刷新一次考勤信息
    setInterval(loadTodayAttendance, 60000);
}); 