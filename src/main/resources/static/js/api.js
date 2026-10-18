/**
 * 前端统一请求与提示组件。
 *
 * 约定（与后端 Result / GlobalExceptionHandler 对应）：
 *  - 成功：HTTP 2xx + {code:200, msg:"", data:...}，request() 直接返回 data；
 *  - 业务失败：HTTP 4xx/5xx + {code:400..500, msg:"..."}，抛 Error(msg)；
 *  - 未登录：HTTP 401，清本地登录态并跳登录页。
 *
 * 这样页面里只需要 try/catch 一次，不必每个接口各写一套 if (data.code === 200)。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var LOGIN_PAGE = '/login.html';

    function goLogin() {
        try {
            sessionStorage.clear();
            localStorage.removeItem('adminName');
            localStorage.removeItem('adminId');
        } catch (e) {
            /* 忽略隐私模式下的异常 */
        }
        if (window.location.pathname !== LOGIN_PAGE) {
            window.location.href = LOGIN_PAGE;
        }
    }

    function buildOptions(options) {
        var opts = options || {};
        var headers = {};
        var key;
        for (key in (opts.headers || {})) {
            if (Object.prototype.hasOwnProperty.call(opts.headers, key)) {
                headers[key] = opts.headers[key];
            }
        }
        var body = opts.body;
        if (body !== undefined && body !== null && typeof body !== 'string'
                && !(window.FormData && body instanceof window.FormData)) {
            headers['Content-Type'] = 'application/json;charset=UTF-8';
            body = JSON.stringify(body);
        }
        return {
            method: opts.method || 'GET',
            credentials: 'same-origin',
            headers: headers,
            body: body,
            cache: opts.cache || 'no-store'
        };
    }

    function parseFileName(headers, fallback) {
        var disposition = headers.get('Content-Disposition') || '';
        var utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
        if (utf8) {
            try {
                return decodeURIComponent(utf8[1]);
            } catch (e) {
                return fallback;
            }
        }
        var plain = /filename="?([^";]+)"?/i.exec(disposition);
        return plain ? plain[1] : fallback;
    }

    /**
     * 发请求并解析统一响应；成功返回 data，失败抛 Error。
     */
    function request(url, options) {
        var opts = buildOptions(options);
        return fetch(url, opts).then(function (response) {
            if (response.status === 401) {
                goLogin();
                throw new Error('登录已过期，请重新登录');
            }
            var contentType = response.headers.get('Content-Type') || '';
            if (contentType.indexOf('application/json') < 0) {
                if (!response.ok) {
                    throw new Error('请求失败（HTTP ' + response.status + '）');
                }
                return response;
            }
            return response.json().then(function (body) {
                var code = body && body.code ? body.code : response.status;
                if (!response.ok || (code !== 200 && code !== 0)) {
                    var error = new Error((body && body.msg) || ('请求失败（HTTP ' + response.status + '）'));
                    error.code = code;
                    throw error;
                }
                return body.data;
            });
        });
    }

    /**
     * 下载二进制（Excel 等）；失败时把服务端返回的 msg 抛出来。
     */
    function download(url, fallbackName) {
        return fetch(url, { credentials: 'same-origin', cache: 'no-store' }).then(function (response) {
            if (response.status === 401) {
                goLogin();
                throw new Error('登录已过期，请重新登录');
            }
            var contentType = response.headers.get('Content-Type') || '';
            if (!response.ok || contentType.indexOf('application/json') >= 0) {
                return response.text().then(function (text) {
                    var message = '下载失败（HTTP ' + response.status + '）';
                    try {
                        var body = JSON.parse(text);
                        if (body && body.msg) {
                            message = body.msg;
                        }
                    } catch (e) {
                        /* 非 JSON 响应，用默认文案 */
                    }
                    throw new Error(message);
                });
            }
            var name = parseFileName(response.headers, fallbackName || 'download');
            return response.blob().then(function (blob) {
                var link = document.createElement('a');
                var objectUrl = window.URL.createObjectURL(blob);
                link.href = objectUrl;
                link.download = name;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                window.setTimeout(function () {
                    window.URL.revokeObjectURL(objectUrl);
                }, 1000);
            });
        });
    }

    var Api = {
        request: request,
        get: function (url) {
            return request(url, { method: 'GET' });
        },
        post: function (url, body) {
            return request(url, { method: 'POST', body: body });
        },
        put: function (url, body) {
            return request(url, { method: 'PUT', body: body });
        },
        del: function (url) {
            return request(url, { method: 'DELETE' });
        },
        download: download,
        goLogin: goLogin
    };

    /* ------------------------------ 提示 ------------------------------ */

    function ensureToastHost() {
        var host = document.getElementById('toastHost');
        if (!host) {
            host = document.createElement('div');
            host.id = 'toastHost';
            host.className = 'toast-host';
            document.body.appendChild(host);
        }
        return host;
    }

    var TYPE_CLASS = { success: 'toast-success', error: 'toast-error', warning: 'toast-warning', info: 'toast-info' };
    var TYPE_ICON = { success: '✓', error: '✕', warning: '!', info: 'i' };

    function showToast(message, type) {
        var kind = TYPE_CLASS[type] ? type : 'info';
        var host = ensureToastHost();
        var item = document.createElement('div');
        item.className = 'toast-item ' + TYPE_CLASS[kind];
        var icon = document.createElement('span');
        icon.className = 'toast-icon';
        icon.textContent = TYPE_ICON[kind];
        var text = document.createElement('span');
        text.className = 'toast-text';
        text.textContent = message == null ? '' : String(message);
        item.appendChild(icon);
        item.appendChild(text);
        host.appendChild(item);
        window.setTimeout(function () {
            item.classList.add('toast-hide');
            window.setTimeout(function () {
                if (item.parentNode) {
                    item.parentNode.removeChild(item);
                }
            }, 260);
        }, kind === 'error' ? 4200 : 2600);
    }

    var Toast = {
        show: showToast,
        success: function (message) {
            showToast(message, 'success');
        },
        error: function (message) {
            showToast(message, 'error');
        },
        warning: function (message) {
            showToast(message, 'warning');
        },
        info: function (message) {
            showToast(message, 'info');
        }
    };

    /* ------------------------------ 工具 ------------------------------ */

    var Utils = {
        escapeHtml: function (value) {
            if (value === null || value === undefined) {
                return '';
            }
            return String(value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        },
        money: function (value) {
            var number = Number(value);
            if (isNaN(number)) {
                return '0.00';
            }
            var fixed = number.toFixed(2);
            var parts = fixed.split('.');
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return parts.join('.');
        },
        num: function (value, digits) {
            var number = Number(value);
            if (isNaN(number)) {
                return (0).toFixed(digits === undefined ? 0 : digits);
            }
            return number.toFixed(digits === undefined ? 0 : digits);
        },
        /** 后端 LocalDateTime 序列化为 2026-10-18T10:00:00，转成 2026-10-18 10:00 */
        dateTime: function (value) {
            if (!value) {
                return '';
            }
            return String(value).replace('T', ' ').substring(0, 16);
        },
        date: function (value) {
            if (!value) {
                return '';
            }
            return String(value).replace('T', ' ').substring(0, 10);
        },
        today: function () {
            var now = new Date();
            var month = now.getMonth() + 1;
            var day = now.getDate();
            return now.getFullYear() + '-' + (month < 10 ? '0' + month : month) + '-' + (day < 10 ? '0' + day : day);
        },
        /** 当前年月，用于工资/考勤筛选项默认值 */
        currentPeriod: function () {
            var now = new Date();
            return { year: now.getFullYear(), month: now.getMonth() + 1 };
        }
    };

    window.Api = Api;
    window.Toast = Toast;
    window.Utils = Utils;
})(window);
