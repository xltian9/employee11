/**
 * 管理端公告：列表 / 新建 / 编辑 / 发布 / 撤回 / 删除。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var STATUS = {
        0: { text: '草稿', cls: 'badge-muted' },
        1: { text: '已发布', cls: 'badge-ok' },
        2: { text: '已撤回', cls: 'badge-warn' }
    };

    function summary(text) {
        if (!text) {
            return '-';
        }
        var plain = String(text).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
        return Utils.escapeHtml(plain.length > 40 ? plain.substring(0, 40) + '…' : plain);
    }

    function render(rows) {
        var body = document.getElementById('noticeTableBody');
        document.getElementById('noticeHint').textContent = '共 ' + (rows ? rows.length : 0) + ' 条';
        if (!rows || rows.length === 0) {
            body.innerHTML = '<tr class="empty-row"><td colspan="8">还没有公告，点击右上角「新建公告」</td></tr>';
            return;
        }
        body.innerHTML = rows.map(function (notice, index) {
            var status = STATUS[Number(notice.status)] || { text: '未知', cls: '' };
            var actions = [];
            if (Number(notice.status) !== 1) {
                actions.push('<button class="link-btn" onclick="editNotice(' + notice.id + ')">编辑</button>');
                actions.push('<button class="link-btn" onclick="publishNotice(' + notice.id + ')">发布</button>');
            } else {
                actions.push('<button class="link-btn muted" onclick="withdrawNotice(' + notice.id + ')">撤回</button>');
            }
            actions.push('<button class="link-btn danger" onclick="deleteNotice(' + notice.id + ')">删除</button>');
            return '<tr>'
                + '<td class="muted">' + (index + 1) + '</td>'
                + '<td><strong>' + Utils.escapeHtml(notice.title || '') + '</strong></td>'
                + '<td class="muted">' + summary(notice.content) + '</td>'
                + '<td>' + Utils.escapeHtml(notice.publisher && notice.publisher.name ? notice.publisher.name : '-') + '</td>'
                + '<td><span class="badge ' + status.cls + '">' + status.text + '</span></td>'
                + '<td>' + Utils.dateTime(notice.publishTime) + '</td>'
                + '<td>' + Utils.dateTime(notice.createTime) + '</td>'
                + '<td>' + actions.join('') + '</td>'
                + '</tr>';
        }).join('');
    }

    function loadNotices() {
        Api.get('/api/notices')
            .then(render)
            .catch(function (error) {
                Toast.error(error.message);
                document.getElementById('noticeTableBody').innerHTML =
                    '<tr class="empty-row"><td colspan="8">加载失败</td></tr>';
            });
    }

    function searchNotices() {
        var keyword = document.getElementById('noticeKeyword').value.trim();
        if (!keyword) {
            loadNotices();
            return;
        }
        Api.get('/api/notices/search?keyword=' + encodeURIComponent(keyword))
            .then(render)
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function showNoticeModal() {
        document.getElementById('noticeId').value = '';
        document.getElementById('noticeTitle').value = '';
        document.getElementById('noticeContent').value = '';
        document.getElementById('noticeModalTitle').textContent = '新建公告';
        document.getElementById('noticeModal').hidden = false;
    }

    function hideNoticeModal() {
        document.getElementById('noticeModal').hidden = true;
    }

    function editNotice(id) {
        Api.get('/api/notices/' + id)
            .then(function (notice) {
                document.getElementById('noticeId').value = notice.id;
                document.getElementById('noticeTitle').value = notice.title || '';
                document.getElementById('noticeContent').value = notice.content || '';
                document.getElementById('noticeModalTitle').textContent = '编辑公告';
                document.getElementById('noticeModal').hidden = false;
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function saveNotice() {
        var id = document.getElementById('noticeId').value;
        var title = document.getElementById('noticeTitle').value.trim();
        var content = document.getElementById('noticeContent').value.trim();
        if (!title) {
            Toast.warning('请填写公告标题');
            return;
        }
        if (!content) {
            Toast.warning('请填写公告内容');
            return;
        }
        var payload = { title: title, content: content };
        var promise = id ? Api.put('/api/notices/' + id, payload) : Api.post('/api/notices', payload);
        promise.then(function () {
            Toast.success('已保存为草稿');
            hideNoticeModal();
            loadNotices();
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function publishNotice(id) {
        Api.post('/api/notices/' + id + '/publish')
            .then(function () {
                Toast.success('已发布，员工端可见');
                loadNotices();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function withdrawNotice(id) {
        Api.post('/api/notices/' + id + '/withdraw')
            .then(function () {
                Toast.success('已撤回');
                loadNotices();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function deleteNotice(id) {
        if (!window.confirm('确定删除这条公告吗？删除后不可恢复。')) {
            return;
        }
        Api.del('/api/notices/' + id)
            .then(function () {
                Toast.success('已删除');
                loadNotices();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    window.loadNotices = loadNotices;
    window.searchNotices = searchNotices;
    window.showNoticeModal = showNoticeModal;
    window.hideNoticeModal = hideNoticeModal;
    window.editNotice = editNotice;
    window.saveNotice = saveNotice;
    window.publishNotice = publishNotice;
    window.withdrawNotice = withdrawNotice;
    window.deleteNotice = deleteNotice;
})(window);
