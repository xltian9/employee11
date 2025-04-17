/**
 * 加载公告列表
 */
function loadNotices() {
    console.log('Loading notices...');
    fetch('/api/notices')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            console.log('Notices response:', data);
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                showError('加载公告列表失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading notices:', error);
            showError('加载公告列表失败：' + error.message);
        });
}

/**
 * 显示公告列表
 */
function displayNotices(notices) {
    console.log('Displaying notices:', notices);
    const tableBody = document.getElementById('noticeTableBody');
    if (!tableBody) {
        console.error('Notice table body not found');
        return;
    }

    let html = '';
    if (notices && notices.length > 0) {
        notices.forEach(function(notice) {
            html += `
                <tr>
                    <td>${notice.title}</td>
                    <td>${notice.publisher ? notice.publisher.name : '-'}</td>
                    <td>${formatDateTime(notice.publishTime)}</td>
                    <td>${getStatusText(notice.status)}</td>
                    <td>
                        <button class="btn btn-sm btn-info me-2" onclick="viewNotice(${notice.id})">查看</button>
                        ${notice.status === 0 ? `
                            <button class="btn btn-sm btn-success me-2" onclick="publishNotice(${notice.id})">发布</button>
                            <button class="btn btn-sm btn-primary me-2" onclick="editNotice(${notice.id})">编辑</button>
                        ` : ''}
                        ${notice.status === 1 ? `
                            <button class="btn btn-sm btn-warning" onclick="withdrawNotice(${notice.id})">撤回</button>
                        ` : ''}
                    </td>
                </tr>
            `;
        });
    }
    tableBody.innerHTML = html || '<tr><td colspan="5" class="text-center">暂无公告</td></tr>';
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
    switch (parseInt(status)) {
        case 0: return '<span class="badge bg-secondary">草稿</span>';
        case 1: return '<span class="badge bg-success">已发布</span>';
        case 2: return '<span class="badge bg-warning">已撤回</span>';
        default: return '<span class="badge bg-secondary">未知</span>';
    }
}

/**
 * 格式化日期时间
 */
function formatDateTime(datetime) {
    if (!datetime) return '-';
    return new Date(datetime).toLocaleString('zh-CN');
}

/**
 * 查看公告详情
 */
function viewNotice(id) {
    console.log('Viewing notice:', id);
    fetch(`/api/notices/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                const notice = data.data;
                document.getElementById('viewNoticeTitle').textContent = notice.title;
                document.getElementById('viewNoticeContent').textContent = notice.content;
                document.getElementById('viewNoticePublisher').textContent =
                    notice.publisher ? notice.publisher.name : '-';
                document.getElementById('viewNoticeTime').textContent =
                    formatDateTime(notice.publishTime);

                const modal = new bootstrap.Modal(document.getElementById('viewNoticeModal'));
                modal.show();
            } else {
                showError('获取公告详情失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error viewing notice:', error);
            showError('获取公告详情失败：' + error.message);
        });
}

/**
 * 发布公告
 */
function publishNotice(id) {
    console.log('Publishing notice:', id);
    if (!confirm('确定要发布这条公告吗？')) {
        return;
    }

    fetch(`/api/notices/${id}/publish`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                showSuccess('公告发布成功');
                loadNotices();
            } else {
                showError('发布失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error publishing notice:', error);
            showError('发布失败：' + error.message);
        });
}

/**
 * 撤回公告
 */
function withdrawNotice(id) {
    console.log('Withdrawing notice:', id);
    if (!confirm('确定要撤回这条公告吗？')) {
        return;
    }

    fetch(`/api/notices/${id}/withdraw`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                showSuccess('公告撤回成功');
                loadNotices();
            } else {
                showError('撤回失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error withdrawing notice:', error);
            showError('撤回失败：' + error.message);
        });
}

/**
 * 编辑公告
 */
function editNotice(id) {
    console.log('Editing notice:', id);
    fetch(`/api/notices/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                const notice = data.data;
                document.getElementById('noticeId').value = notice.id;
                document.getElementById('noticeTitle').value = notice.title;
                document.getElementById('noticeContent').value = notice.content;

                document.getElementById('noticeModalTitle').textContent = '编辑公告';
                const modal = new bootstrap.Modal(document.getElementById('noticeModal'));
                modal.show();
            } else {
                showError('获取公告信息失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading notice:', error);
            showError('获取公告信息失败：' + error.message);
        });
}

/**
 * 保存公告
 */
function saveNotice() {
    console.log('Saving notice');
    const id = document.getElementById('noticeId').value;
    const data = {
        title: document.getElementById('noticeTitle').value.trim(),
        content: document.getElementById('noticeContent').value.trim()
    };

    // 表单验证
    if (!data.title || !data.content) {
        showError('请填写公告标题和内容');
        return;
    }

    // 获取保存按钮并禁用
    const saveButton = document.querySelector('#noticeModal .modal-footer .btn-primary');
    if (saveButton) {
        saveButton.disabled = true;
    }

    fetch('/api/notices' + (id ? `/${id}` : ''), {
        method: id ? 'PUT' : 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                const modal = bootstrap.Modal.getInstance(document.getElementById('noticeModal'));
                modal.hide();
                loadNotices();
                showSuccess(id ? '更新成功' : '创建成功');
            } else {
                showError(data.message || '保存失败');
            }
        })
        .catch(error => {
            console.error('Save error:', error);
            showError('保存失败：' + error.message);
        })
        .finally(() => {
            // 在finally中重新启用按钮
            if (saveButton) {
                saveButton.disabled = false;
            }
        });
}

/**
 * 显示添加公告模态框
 */
function showAddNoticeModal() {
    console.log('Showing add notice modal');
    document.getElementById('noticeId').value = '';
    document.getElementById('noticeForm').reset();
    document.getElementById('noticeModalTitle').textContent = '发布公告';
    const modal = new bootstrap.Modal(document.getElementById('noticeModal'));
    modal.show();
}

/**
 * 搜索公告
 */
function searchNotices() {
    console.log('Searching notices');
    const keyword = document.getElementById('searchKeyword')?.value?.trim();

    if (!keyword) {
        loadAllNotices();  // 如果搜索框为空，显示所有公告
        return;
    }

    fetch('/api/notices/search?keyword=' + encodeURIComponent(keyword))
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                showError('搜索公告失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Search error:', error);
            showError('搜索公告失败：' + error.message);
        });
}

/**
 * 加载所有公告
 */
function loadAllNotices() {
    console.log('Loading all notices...');
    fetch('/api/notices')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                showError('加载公告列表失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading notices:', error);
            showError('加载公告列表失败：' + error.message);
        });
}

/**
 * 加载已发布公告
 */
function loadPublishedNotices() {
    console.log('Loading published notices...');
    fetch('/api/notices/published')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                showError('加载公告列表失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading notices:', error);
            showError('加载公告列表失败：' + error.message);
        });
}

/**
 * 加载未发布公告
 */
function loadDraftNotices() {
    console.log('Loading draft notices...');
    fetch('/api/notices/draft')
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 200) {
                displayNotices(data.data || []);
            } else {
                showError('加载公告列表失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error loading notices:', error);
            showError('加载公告列表失败：' + error.message);
        });
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('Notice page initialized');
    // 确保在页面加载完成后再加载数据
    setTimeout(loadNotices, 100);
});

// 确保函数在全局作用域可用
window.loadNotices = loadNotices;
window.displayNotices = displayNotices;
window.getStatusText = getStatusText;
window.formatDateTime = formatDateTime;
window.viewNotice = viewNotice;
window.publishNotice = publishNotice;
window.withdrawNotice = withdrawNotice;
window.editNotice = editNotice;
window.saveNotice = saveNotice;
window.showAddNoticeModal = showAddNoticeModal;
window.searchNotices = searchNotices;
window.loadAllNotices = loadAllNotices;
window.loadPublishedNotices = loadPublishedNotices;
window.loadDraftNotices = loadDraftNotices;

// 在文件中添加这两个函数
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