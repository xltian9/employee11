/**
 * 前端登录态守卫
 * 1) 统一给 fetch 补上 same-origin 凭据，并在收到 401 时自动跳回登录页
 * 2) 提供 AuthGuard.require(role) 供页面按角色校验
 */
(function () {
    'use strict';

    var LOGIN_PAGE = '/login.html';

    function goLogin() {
        try {
            sessionStorage.clear();
            localStorage.removeItem('adminName');
            localStorage.removeItem('adminId');
        } catch (e) {
            // 忽略隐私模式下的存储异常
        }
        if (window.location.pathname !== LOGIN_PAGE) {
            window.location.href = LOGIN_PAGE;
        }
    }

    var nativeFetch = window.fetch ? window.fetch.bind(window) : null;

    if (nativeFetch) {
        window.fetch = function (input, init) {
            init = init || {};
            if (!init.credentials) {
                init.credentials = 'same-origin';
            }
            return nativeFetch(input, init).then(function (response) {
                if (response.status === 401) {
                    goLogin();
                }
                return response;
            });
        };
    }

    window.AuthGuard = {
        goLogin: goLogin,
        /**
         * 校验当前登录态
         * @param {string} [role] 'admin' | 'employee'，不传则只要求已登录
         * @returns {Promise<object|null>} 登录者信息，未登录/角色不符返回 null（并跳登录页）
         */
        require: function (role) {
            if (!nativeFetch) {
                return Promise.resolve(null);
            }
            return nativeFetch('/api/auth/me', { credentials: 'same-origin' })
                .then(function (response) {
                    if (response.status === 401) {
                        goLogin();
                        return null;
                    }
                    return response.json();
                })
                .then(function (body) {
                    if (!body || body.code !== 200 || !body.data) {
                        goLogin();
                        return null;
                    }
                    var me = body.data;
                    if (role && me.type !== role) {
                        goLogin();
                        return null;
                    }
                    return me;
                })
                .catch(function () {
                    return null;
                });
        }
    };
})();
