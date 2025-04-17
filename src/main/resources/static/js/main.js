/**
 * 加载页面内容
 * @param {string} page - 要加载的页面名称
 */
function loadContent(page) {
    console.log('Loading page:', page);
    fetch(`/pages/${page}.html`)
        .then(response => {
            if (!response.ok) {
                throw new Error('页面加载失败');
            }
            return response.text();
        })
        .then(html => {
            document.getElementById('content').innerHTML = html;
            
            // 初始化新加载的页面
            switch(page) {
                case 'department':
                    loadDepartments();
                    break;
                case 'employee':
                    loadEmployees();
                    break;
                case 'notice':
                    loadNotices();
                    break;
                case 'leave':
                    loadLeaves();
                    break;
            }

            // 更新导航栏活动状态
            const navLinks = document.querySelectorAll('.nav-link');
            navLinks.forEach(link => link.classList.remove('active'));
            document.querySelector(`a[onclick="loadContent('${page}')"]`)?.classList.add('active');
        })
        .catch(error => {
            console.error('Error loading content:', error);
            document.getElementById('content').innerHTML = `
                <div class="alert alert-danger">
                    <h4 class="alert-heading">页面加载失败</h4>
                    <p>错误信息：${error.message}</p>
                    <hr>
                    <p class="mb-0">请刷新页面重试或联系管理员</p>
                </div>
            `;
        });
}

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 加载默认内容
    loadContent('department');
});

// 退出登录
function logout() {
    fetch('/api/auth/logout', {
        method: 'POST'
    }).then(() => {
        window.location.href = '/login.html';
    }).catch(error => {
        console.error('退出失败:', error);
        window.location.href = '/login.html';
    });
}

// 确保所有需要的函数都在全局作用域可用
window.loadContent = loadContent;
window.logout = logout;
 