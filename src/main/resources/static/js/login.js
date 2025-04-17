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
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            console.log('Login response:', data);
            if (data.code === 200) {
                // 保存用户信息
                localStorage.setItem('adminId', data.data.id);
                localStorage.setItem('adminName', data.data.name);
                window.location.href = '/index.html';
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