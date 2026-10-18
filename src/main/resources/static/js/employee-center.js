/**
 * 员工自助中心：个人信息 / 考勤打卡 / 请假 / 绩效 / 工资 / 公告。
 *
 * @author Limiti
 */
(function (window) {
    'use strict';

    var employeeId = null;
    var employee = null;
    var attYear = 0;
    var attMonth = 0;
    var salaryFiltersReady = false;
    var attFiltersReady = false;

    var LEAVE_TYPE = { 1: '事假', 2: '病假', 3: '年假', 4: '调休' };
    var LEAVE_STATUS = {
        0: { text: '待审批', cls: 'badge-warn' },
        1: { text: '已通过', cls: 'badge-ok' },
        2: { text: '已拒绝', cls: 'badge-danger' },
        3: { text: '已撤销', cls: 'badge-muted' }
    };
    var GRADE_CLASS = { S: 'badge-danger', A: 'badge-warn', B: 'badge-info', C: 'badge-muted', D: 'badge-muted' };

    function pad(value) {
        return value < 10 ? '0' + value : '' + value;
    }

    function monthOptions(count) {
        var html = '';
        for (var i = 1; i <= 12; i++) {
            html += '<option value="' + i + '">' + i + ' 月</option>';
        }
        return html;
    }

    function init() {
        AuthGuard.require('employee').then(function (me) {
            employeeId = me.id;
            document.getElementById('topbarName').textContent = me.name || '员工';
            document.getElementById('topbarAvatar').src =
                '/api/employees/' + me.id + '/photo?v=' + Date.now();
            return Api.get('/api/employees/' + me.id);
        }).then(function (data) {
            employee = data;
            renderProfile(data);
        }).catch(function (error) {
            Toast.error(error.message);
        });

        var now = new Date();
        attYear = now.getFullYear();
        attMonth = now.getMonth() + 1;
        document.getElementById('attYear').value = attYear;
        document.getElementById('attMonth').innerHTML = monthOptions();
        document.getElementById('attMonth').value = attMonth;
        document.getElementById('salaryYear').value = now.getFullYear();
        document.getElementById('salaryMonth').innerHTML = monthOptions();
        document.getElementById('salaryMonth').value = now.getMonth() + 1;
        attFiltersReady = true;
        salaryFiltersReady = true;
    }

    function showSection(name, link) {
        var sections = document.querySelectorAll('.content-section');
        for (var i = 0; i < sections.length; i++) {
            sections[i].style.display = sections[i].id === name + '-section' ? '' : 'none';
        }
        var items = document.querySelectorAll('.nav-item');
        for (var j = 0; j < items.length; j++) {
            items[j].classList.remove('active');
        }
        if (link) {
            link.classList.add('active');
        }
        var titles = {
            profile: ['我的信息', '查看个人档案与登录密码'],
            attendance: ['我的考勤', '打卡记录与月度出勤统计'],
            leave: ['我的请假', '申请、查看与撤销请假'],
            performance: ['我的绩效', '季度绩效评分与等级'],
            salary: ['我的工资', '本人当月工资明细'],
            notice: ['公司公告', '管理员发布的公告']
        };
        var title = titles[name] || ['员工自助', ''];
        document.getElementById('pageTitle').textContent = title[0];
        document.getElementById('pageSub').textContent = title[1];

        if (name === 'attendance') {
            loadMyAttendance();
        } else if (name === 'leave') {
            loadMyLeaves();
        } else if (name === 'performance') {
            loadMyPerformance();
        } else if (name === 'salary') {
            loadMySalary();
        } else if (name === 'notice') {
            loadNotices();
        }
    }

    function renderProfile(data) {
        var rows = [
            ['工号', data.employeeNumber || '-'],
            ['姓名', data.name || '-'],
            ['部门', data.department || '-'],
            ['职位', data.position || '-'],
            ['手机号', data.phone || '-'],
            ['邮箱', data.email || '-'],
            ['月薪', Utils.money(data.wage)],
            ['入职日期', Utils.date(data.entryDate)],
            ['状态', Number(data.status) === 1 || data.status === true ? '在职' : '离职']
        ];
        document.getElementById('profileBody').innerHTML = rows.map(function (row) {
            return '<div class="kv"><span class="kv-label">' + row[0] + '</span>'
                + '<span class="kv-value">' + Utils.escapeHtml(String(row[1])) + '</span></div>';
        }).join('') + '<div class="hint" style="margin-top:12px;">如需修改个人信息，请联系管理员。</div>';
    }

    function changePassword() {
        var oldPassword = document.getElementById('oldPassword').value;
        var newPassword = document.getElementById('newPassword').value;
        var confirmPassword = document.getElementById('confirmPassword').value;
        if (!oldPassword || !newPassword) {
            Toast.warning('请填写当前密码和新密码');
            return;
        }
        if (newPassword.length < 6 || newPassword.length > 64) {
            Toast.warning('新密码长度必须在 6 到 64 位之间');
            return;
        }
        if (newPassword !== confirmPassword) {
            Toast.warning('两次输入的新密码不一致');
            return;
        }
        Api.post('/api/auth/change-password', {
            oldPassword: oldPassword,
            newPassword: newPassword
        }).then(function () {
            Toast.success('密码已修改，请重新登录');
            document.getElementById('oldPassword').value = '';
            document.getElementById('newPassword').value = '';
            document.getElementById('confirmPassword').value = '';
            window.setTimeout(logout, 1200);
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function loadMyAttendance() {
        if (!employeeId) {
            return;
        }
        var year = Number(document.getElementById('attYear').value) || attYear;
        var month = Number(document.getElementById('attMonth').value) || attMonth;
        attYear = year;
        attMonth = month;

        Api.get('/api/attendance/summary?employeeId=' + employeeId + '&year=' + year + '&month=' + month)
            .then(function (summary) {
                document.getElementById('attWorkDays').textContent = Utils.num(summary.workDays);
                document.getElementById('attActualDays').textContent = Utils.num(summary.actualDays);
                document.getElementById('attLeaveDays').textContent = Utils.num(summary.leaveDays);
                document.getElementById('attAbsentLate').textContent =
                    Utils.num(summary.absentDays) + ' / ' + Utils.num(summary.lateCount);
            })
            .catch(function (error) {
                Toast.error(error.message);
            });

        Api.get('/api/attendance/daily?employeeId=' + employeeId + '&year=' + year + '&month=' + month)
            .then(function (rows) {
                var body = document.getElementById('myAttendanceBody');
                if (!rows || rows.length === 0) {
                    body.innerHTML = '<tr class="empty-row"><td colspan="6">该月没有工作日</td></tr>';
                    return;
                }
                body.innerHTML = rows.map(function (day) {
                    var cls = {
                        '正常': 'badge-ok',
                        '迟到': 'badge-warn',
                        '早退': 'badge-warn',
                        '迟到+早退': 'badge-warn',
                        '旷工': 'badge-danger',
                        '请假': 'badge-info'
                    }[day.status] || '';
                    return '<tr>'
                        + '<td>' + Utils.escapeHtml(day.date || '') + '</td>'
                        + '<td><span class="badge ' + cls + '">' + Utils.escapeHtml(day.status || '-') + '</span></td>'
                        + '<td>' + Utils.escapeHtml(day.checkIn || '-') + '</td>'
                        + '<td>' + Utils.escapeHtml(day.checkOut || '-') + '</td>'
                        + '<td>' + Utils.escapeHtml(day.leaveType || '-') + '</td>'
                        + '<td class="muted">' + Utils.escapeHtml(day.note || '') + '</td>'
                        + '</tr>';
                }).join('');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });

        var today = Utils.today();
        document.getElementById('todayHint').textContent = today;
        Api.get('/api/attendance/daily?employeeId=' + employeeId + '&year=' + year + '&month=' + month)
            .then(function (rows) {
                var target = (rows || []).filter(function (day) {
                    return day.date === today;
                })[0];
                if (target) {
                    document.getElementById('todayStatus').textContent =
                        (target.status || '未打卡')
                        + (target.checkIn ? ' · 签到 ' + target.checkIn : '')
                        + (target.checkOut ? ' · 签退 ' + target.checkOut : '');
                }
            })
            .catch(function () {
                /* 今日状态失败不影响主流程 */
            });
    }

    function checkIn() {
        if (!employeeId) {
            return;
        }
        Api.post('/api/attendance/check-in/' + employeeId)
            .then(function (record) {
                Toast.success('签到成功' + (record.status === 1 ? '（迟到）' : ''));
                loadMyAttendance();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function checkOut() {
        if (!employeeId) {
            return;
        }
        Api.get('/api/attendance/employee/' + employeeId)
            .then(function (records) {
                var today = Utils.today();
                var target = (records || []).filter(function (record) {
                    var text = Utils.dateTime(record.checkInTime);
                    return text.indexOf(today) === 0 && !record.checkOutTime;
                })[0];
                if (!target) {
                    Toast.warning('今天还没有签到，或已经签退过了');
                    return;
                }
                return Api.post('/api/attendance/check-out/' + target.id).then(function () {
                    Toast.success('签退成功');
                    loadMyAttendance();
                });
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function loadMyLeaves() {
        Api.get('/api/leaves/my')
            .then(function (rows) {
                var body = document.getElementById('myLeaveBody');
                if (!rows || rows.length === 0) {
                    body.innerHTML = '<tr class="empty-row"><td colspan="8">还没有请假记录</td></tr>';
                    return;
                }
                body.innerHTML = rows.map(function (leave) {
                    var status = LEAVE_STATUS[Number(leave.status)] || { text: '未知', cls: '' };
                    var canCancel = Number(leave.status) === 0 || Number(leave.status) === 1;
                    return '<tr>'
                        + '<td>' + Utils.escapeHtml(LEAVE_TYPE[Number(leave.type)] || '-') + '</td>'
                        + '<td>' + Utils.dateTime(leave.startTime) + '</td>'
                        + '<td>' + Utils.dateTime(leave.endTime) + '</td>'
                        + '<td class="num">' + Utils.num(leave.leavesDay) + '</td>'
                        + '<td>' + Utils.escapeHtml(leave.reason || '-') + '</td>'
                        + '<td><span class="badge ' + status.cls + '">' + status.text + '</span></td>'
                        + '<td class="muted">' + Utils.escapeHtml(leave.comment || '-') + '</td>'
                        + '<td>' + (canCancel
                            ? '<button class="link-btn danger" onclick="cancelMyLeave(' + leave.id + ')">撤销</button>'
                            : '<span class="muted">-</span>') + '</td>'
                        + '</tr>';
                }).join('');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function showLeaveForm() {
        var now = new Date();
        var start = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) + 'T09:00';
        document.getElementById('leaveStart').value = start;
        document.getElementById('leaveEnd').value = start;
        document.getElementById('leaveReason').value = '';
        document.getElementById('leaveFormCard').hidden = false;
    }

    function hideLeaveForm() {
        document.getElementById('leaveFormCard').hidden = true;
    }

    function submitLeave() {
        var start = document.getElementById('leaveStart').value;
        var end = document.getElementById('leaveEnd').value;
        var reason = document.getElementById('leaveReason').value.trim();
        if (!start || !end) {
            Toast.warning('请选择请假起止时间');
            return;
        }
        if (new Date(start) > new Date(end)) {
            Toast.warning('开始时间不能晚于结束时间');
            return;
        }
        if (!reason) {
            Toast.warning('请填写请假原因');
            return;
        }
        Api.post('/api/leaves', {
            type: Number(document.getElementById('leaveType').value),
            startTime: start.replace('T', ' ') + ':00',
            endTime: end.replace('T', ' ') + ':00',
            reason: reason
        }).then(function (saved) {
            Toast.success('已提交，共 ' + (saved.leavesDay || 0) + ' 个工作日，等待审批');
            hideLeaveForm();
            loadMyLeaves();
        }).catch(function (error) {
            Toast.error(error.message);
        });
    }

    function cancelMyLeave(id) {
        var reason = window.prompt('撤销原因（可留空）', '');
        if (reason === null) {
            return;
        }
        Api.post('/api/leaves/' + id + '/cancel?reason=' + encodeURIComponent(reason))
            .then(function () {
                Toast.success('已撤销');
                loadMyLeaves();
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function loadMyPerformance() {
        Api.get('/api/performances/my')
            .then(function (rows) {
                var body = document.getElementById('myPerformanceBody');
                if (!rows || rows.length === 0) {
                    body.innerHTML = '<tr class="empty-row"><td colspan="9">还没有绩效记录</td></tr>';
                    return;
                }
                body.innerHTML = rows.map(function (item) {
                    var grade = item.grade || '-';
                    return '<tr>'
                        + '<td>' + Utils.num(item.year) + '</td>'
                        + '<td>第 ' + Utils.num(item.quarter) + ' 季度</td>'
                        + '<td class="num">' + Utils.num(item.score, 1) + '</td>'
                        + '<td class="num">' + Utils.num(item.abilityScore, 1) + '</td>'
                        + '<td class="num">' + Utils.num(item.attitudeScore, 1) + '</td>'
                        + '<td class="num"><strong>' + Utils.num(item.finalScore, 1) + '</strong></td>'
                        + '<td><span class="badge ' + (GRADE_CLASS[grade] || '') + '">' + Utils.escapeHtml(grade) + '</span></td>'
                        + '<td style="max-width:280px;">' + Utils.escapeHtml(item.evaluation || '-') + '</td>'
                        + '<td class="muted">' + Utils.dateTime(item.createTime) + '</td>'
                        + '</tr>';
                }).join('');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function salaryPeriod() {
        var year = Number(document.getElementById('salaryYear').value) || new Date().getFullYear();
        var month = Number(document.getElementById('salaryMonth').value) || (new Date().getMonth() + 1);
        return { year: year, month: month };
    }

    function renderSalary(row) {
        document.getElementById('cardNet').textContent = Utils.money(row.netPay);
        document.getElementById('cardGross').textContent = Utils.money(row.grossPay);
        document.getElementById('cardBonus').textContent = Utils.money(row.bonus);
        var deduct = (Number(row.leaveDeduct) || 0) + (Number(row.absentDeduct) || 0)
            + (Number(row.lateDeduct) || 0) + (Number(row.socialInsurance) || 0)
            + (Number(row.housingFund) || 0) + (Number(row.tax) || 0)
            + (Number(row.adjustDeduct) || 0);
        document.getElementById('cardDeduct').textContent = Utils.money(deduct);

        var totalHours = (Number(row.overtimeWeekdayHours) || 0)
            + (Number(row.overtimeWeekendHours) || 0)
            + (Number(row.overtimeHolidayHours) || 0);

        var lines = [
            ['月份', row.yearNum + ' 年 ' + row.monthNum + ' 月', '工作日 ' + row.workDay + ' 天，出勤 ' + row.actualDays + ' 天'],
            ['月薪', Utils.money(row.wage), '合同月薪'],
            ['请假扣款', Utils.money(row.leaveDeduct), '事假 ' + row.casualLeaveDays + ' 天，病假 ' + row.sickLeaveDays
                + ' 天，其他 ' + row.otherLeaveDays + ' 天'],
            ['旷工扣款', Utils.money(row.absentDeduct), '旷工 ' + row.absentDays + ' 天'],
            ['迟到扣款', Utils.money(row.lateDeduct), '迟到 ' + row.lateCount + ' 次，早退 ' + row.earlyCount + ' 次'],
            ['加班费', Utils.money(row.overtimePay), '共 ' + Utils.num(totalHours, 1) + ' 小时'],
            ['绩效奖金', Utils.money(row.bonus), row.performanceScore === null || row.performanceScore === undefined
                ? '本季度绩效未录入'
                : '综合分 ' + Utils.num(row.performanceScore, 1) + '（' + (row.performanceGrade || '-') + '）'],
            ['补发', Utils.money(row.adjustAdd), '管理员登记'],
            ['其他扣款', Utils.money(row.adjustDeduct), '管理员登记'],
            ['应发工资', Utils.money(row.grossPay), '扣社保公积金个税之前'],
            ['社保（个人）', Utils.money(row.socialInsurance), '月薪 × 策略比例'],
            ['公积金（个人）', Utils.money(row.housingFund), '月薪 × 策略比例'],
            ['个人所得税', Utils.money(row.tax), '七级超额累进'],
            ['实发工资', Utils.money(row.netPay), '最终到账金额']
        ];
        document.getElementById('salaryDetailBody').innerHTML = lines.map(function (line, index) {
            var strong = index === lines.length - 1 ? ' style="font-weight:700;color:#0f766e;"' : '';
            return '<tr><td' + strong + '>' + line[0] + '</td>'
                + '<td class="num"' + strong + '>' + Utils.escapeHtml(String(line[1])) + '</td>'
                + '<td class="muted">' + Utils.escapeHtml(line[2]) + '</td></tr>';
        }).join('');
    }

    function loadMySalary() {
        var p = salaryPeriod();
        Api.get('/api/report/my?year=' + p.year + '&month=' + p.month)
            .then(function (row) {
                if (!row) {
                    document.getElementById('salaryDetailBody').innerHTML =
                        '<tr class="empty-row"><td colspan="3">暂无工资数据</td></tr>';
                    return;
                }
                renderSalary(row);
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function exportMySalary() {
        var p = salaryPeriod();
        var fileName = '工资明细-' + p.year + '-' + pad(p.month) + '.xlsx';
        Api.download('/api/report/my/export?year=' + p.year + '&month=' + p.month, fileName)
            .then(function () {
                Toast.success('导出成功');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function loadNotices() {
        Api.get('/api/notices/published')
            .then(function (rows) {
                var host = document.getElementById('noticeList');
                if (!rows || rows.length === 0) {
                    host.innerHTML = '<div class="empty-state">暂时没有公告</div>';
                    return;
                }
                host.innerHTML = rows.map(function (notice) {
                    return '<div class="card" style="margin-bottom:14px;">'
                        + '<div class="card-head"><h3 class="card-title">' + Utils.escapeHtml(notice.title || '') + '</h3>'
                        + '<span class="hint">' + Utils.dateTime(notice.publishTime || notice.createTime) + '</span></div>'
                        + '<div class="card-body" style="white-space:pre-wrap;line-height:1.8;">'
                        + Utils.escapeHtml(notice.content || '') + '</div></div>';
                }).join('');
            })
            .catch(function (error) {
                Toast.error(error.message);
            });
    }

    function logout() {
        Api.post('/api/auth/logout').catch(function () {
            /* 忽略退出失败，直接清理本地状态 */
        }).then(function () {
            window.sessionStorage.clear();
            window.localStorage.removeItem('adminName');
            window.localStorage.removeItem('adminId');
            window.location.href = '/login.html';
        });
    }

    window.showSection = showSection;
    window.changePassword = changePassword;
    window.loadMyAttendance = loadMyAttendance;
    window.checkIn = checkIn;
    window.checkOut = checkOut;
    window.loadMyLeaves = loadMyLeaves;
    window.showLeaveForm = showLeaveForm;
    window.hideLeaveForm = hideLeaveForm;
    window.submitLeave = submitLeave;
    window.cancelMyLeave = cancelMyLeave;
    window.loadMyPerformance = loadMyPerformance;
    window.loadMySalary = loadMySalary;
    window.exportMySalary = exportMySalary;
    window.logout = logout;

    document.addEventListener('DOMContentLoaded', init);
})(window);
