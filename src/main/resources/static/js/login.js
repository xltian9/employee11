/**
 * 显示错误提示
 * @param {string} message - 错误信息
 */
function showError(message) {
    const errorModal = document.getElementById('errorModal');
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.textContent = message;
    const modal = new bootstrap.Modal(errorModal);
    modal.show();
}

// 等待文档加载完成
document.addEventListener('DOMContentLoaded', function() {
    // 获取表单元素
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const submitButton = loginForm.querySelector('button[type="submit"]');

    // 处理表单提交
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        if (!username || !password) {
            showError('请输入用户名和密码');
            return;
        }

        submitButton.disabled = true;

        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        })
        // .then(response => {
        //     if (!response.ok) {
        //         throw new Error('网络请求失败');
        //     }
        //     return response.json();
        // })
        .then(data => {
            
            console.log('完整的登录响应数据:', data); // 打印完整响应数据
            if (data.code === 200) {
                let adminId;
                let adminName;
                // 根据不同登录方案解析数据
                if (data.data.role === 'admin') { // 假设管理员角色
                    adminId = data.data.adminId;
                    adminName = data.data.adminName;
                } else if (data.data.role === 'employee') { // 假设普通员工角色
                    adminId = data.data.employeeId;
                    adminName = data.data.employeeName;
                }

                if (adminId && adminName) {
                    // 保存用户信息
                    localStorage.setItem('adminId', adminId);
                    localStorage.setItem('adminName', adminName);
                    window.location.href = '/index.html';
                } else {
                    console.error('响应数据中缺少必要信息');
                    showError('登录成功，但缺少用户信息');
                    submitButton.disabled = false;
                }
            } else {
                showError(data.message || '登录失败');
                submitButton.disabled = false;
            }
        })
        .catch(error => {
            console.error('Login error:', error);
            showError('登录失败：' + error.message);
            submitButton.disabled = false;
        });
    });

    // 聚焦用户名输入框
    usernameInput.focus();
}); 