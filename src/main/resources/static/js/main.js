/**
 * 管理端骨架逻辑：侧边栏切换、页面片段加载、管理员信息弹层、退出登录。
 *
 * 页面片段放在 /pages/*.html，加载后由 PAGE_LOADERS 里同名的初始化函数接管。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var PAGES = {
        dashboard: { title: '工作台', sub: '今天要处理的事情一目了然', loader: 'loadDashboard' },
        department: { title: '部门管理', sub: '组织架构与负责人', loader: 'loadDepartments' },
        employee: { title: '员工管理', sub: '花名册、职级与月薪', loader: 'loadEmployees' },
        attendance: { title: '考勤管理', sub: '打卡记录与月度汇总', loader: 'loadAttendance' },
        leave: { title: '请假管理', sub: '审批、撤销与销假', loader: 'loadLeaves' },
        notice: { title: '公告管理', sub: '发布与撤回内部通知', loader: 'loadNotices' },
        performance: { title: '业绩工资', sub: '季度绩效、奖金与实发工资', loader: 'loadPerformances' },
        adjustment: { title: '加班与补发', sub: '加班时长与补发扣款明细', loader: 'loadAdjustments' },
        policy: { title: '工资策略', sub: '请假扣款、社保公积金与个税参数', loader: 'loadSalaryPolicy' },
        audit: { title: '审计日志', sub: '谁改了谁的工资、谁批了谁的假', loader: 'loadAuditLogs' }
    };

    function setActiveNav(page) {
        var items = document.querySelectorAll('.sidebar-nav .nav-item');
        for (var i = 0; i < items.length; i++) {
            items[i].classList.toggle('active', items[i].getAttribute('data-page') === page);
        }
    }

    function setTitle(page) {
        var meta = PAGES[page] || { title: page, sub: '' };
        var title = document.getElementById('pageTitle');
        var sub = document.getElementById('pageSub');
        if (title) {
            title.textContent = meta.title;
        }
        if (sub) {
            sub.textContent = meta.sub;
        }
        document.title = meta.title + ' · 员工管理系统';
    }

    function runLoader(page) {
        var meta = PAGES[page];
        if (!meta || !meta.loader) {
            return;
        }
        var fn = window[meta.loader];
        if (typeof fn === 'function') {
            try {
                fn();
            } catch (e) {
                console.error('页面初始化失败:', e);
                if (window.Toast) {
                    Toast.error(e.message || '页面初始化失败');
                }
            }
        }
    }

    /**
     * 加载页面片段并执行对应初始化函数。
     */
    function loadContent(page) {
        var meta = PAGES[page];
        if (!meta) {
            return;
        }
        setActiveNav(page);
        setTitle(page);
        var host = document.getElementById('content');
        host.innerHTML = '<div class="loading">正在加载…</div>';

        fetch('/pages/' + page + '.html', { credentials: 'same-origin', cache: 'no-store' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('页面 ' + page + '.html 加载失败（HTTP ' + response.status + '）');
                }
                return response.text();
            })
            .then(function (html) {
                host.innerHTML = html;
                runLoader(page);
            })
            .catch(function (error) {
                console.error('加载页面失败:', error);
                host.innerHTML = '<div class="card"><div class="card-body">'
                    + '<p class="text-danger" style="margin:0 0 8px;">页面加载失败</p>'
                    + '<p class="hint" style="margin:0;">' + (window.Utils ? Utils.escapeHtml(error.message) : error.message) + '</p>'
                    + '</div></div>';
            });
    }

    /* -------------------------- 管理员信息弹层 -------------------------- */

    function showAdminModal() {
        var mask = document.getElementById('adminModal');
        if (!mask) {
            return;
        }
        Api.get('/api/admin/info').then(function (admin) {
            document.getElementById('adminId').value = admin && admin.id ? admin.id : '';
            document.getElementById('adminUsername').value = admin && admin.username ? admin.username : '';
            document.getElementById('adminPassword').value = '';
            mask.hidden = false;
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function hideAdminModal() {
        var mask = document.getElementById('adminModal');
        if (mask) {
            mask.hidden = true;
        }
    }

    function submitAdminForm() {
        var id = document.getElementById('adminId').value;
        var username = document.getElementById('adminUsername').value.trim();
        var password = document.getElementById('adminPassword').value;
        if (!username) {
            Toast.warning('用户名不能为空');
            return;
        }
        if (password && password.length < 6) {
            Toast.warning('新密码长度不能少于 6 位');
            return;
        }
        var payload = { id: id ? Number(id) : null, username: username };
        if (password) {
            payload.password = password;
        }
        var button = document.getElementById('adminSubmitBtn');
        button.disabled = true;
        Api.put('/api/admin/update', payload).then(function () {
            hideAdminModal();
            Toast.success('管理员信息已更新');
            if (password) {
                window.setTimeout(function () {
                    window.location.href = '/login.html';
                }, 900);
            }
        }).catch(function (error) {
            Toast.error(error.message);
        }).then(function () {
            button.disabled = false;
        });
    }

    /* ------------------------------ 登录态 ------------------------------ */

    function logout() {
        Api.post('/api/auth/logout').catch(function () {
            /* 退出失败也回登录页 */
        }).then(function () {
            window.location.href = '/login.html';
        });
    }

    function initUser() {
        Api.get('/api/auth/me').then(function (me) {
            if (!me || me.type !== 'admin') {
                window.location.href = '/login.html';
                return;
            }
            var nameEl = document.getElementById('adminName');
            var avatarEl = document.getElementById('adminAvatar');
            var displayName = me.name || '管理员';
            if (nameEl) {
                nameEl.textContent = displayName;
            }
            if (avatarEl) {
                avatarEl.textContent = displayName.substring(0, 1);
            }
            loadContent('dashboard');
        }).catch(function () {
            window.location.href = '/login.html';
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var items = document.querySelectorAll('.sidebar-nav .nav-item');
        for (var i = 0; i < items.length; i++) {
            items[i].addEventListener('click', function () {
                loadContent(this.getAttribute('data-page'));
            });
        }
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                hideAdminModal();
            }
        });
        initUser();
    });

    window.loadContent = loadContent;
    window.logout = logout;
    window.showAdminModal = showAdminModal;
    window.hideAdminModal = hideAdminModal;
    window.submitAdminForm = submitAdminForm;
})(window);
