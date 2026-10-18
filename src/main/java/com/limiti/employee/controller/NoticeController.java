package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Notice;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.NoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 公告管理控制器
 *
 * <p>异常统一抛给全局处理器，不再逐个方法 try/catch。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@RestController
@RequestMapping("/api/notices")
@Api(tags = "公告管理接口")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 创建公告（仅管理员）
     */
    @PostMapping
    @ApiOperation("创建公告")
    public Result<Notice> createNotice(@RequestBody Notice notice, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        return Result.success("创建成功", noticeService.createNotice(notice, admin));
    }

    /**
     * 发布公告（仅管理员）
     */
    @PostMapping("/{id}/publish")
    @ApiOperation("发布公告")
    public Result<Notice> publishNotice(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success("发布成功", noticeService.publishNotice(id));
    }

    /**
     * 撤回公告（仅管理员）
     */
    @PostMapping("/{id}/withdraw")
    @ApiOperation("撤回公告")
    public Result<Notice> withdrawNotice(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success("撤回成功", noticeService.withdrawNotice(id));
    }

    /**
     * 获取已发布的公告列表（登录即可）
     */
    @GetMapping("/published")
    @ApiOperation("获取已发布的公告")
    public Result<List<Notice>> getPublishedNotices() {
        return Result.success(noticeService.getPublishedNotices());
    }

    /**
     * 搜索公告（仅管理员）
     */
    @GetMapping("/search")
    @ApiOperation("搜索公告")
    public Result<List<Notice>> searchNotices(@RequestParam String keyword, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(noticeService.searchNotices(keyword));
    }

    /**
     * 更新公告（仅管理员）
     */
    @PutMapping("/{id}")
    @ApiOperation("更新公告")
    public Result<Notice> updateNotice(@PathVariable Long id,
                                       @RequestBody Notice notice,
                                       HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        return Result.success("修改成功", noticeService.updateNotice(id, notice, admin));
    }

    /**
     * 获取所有公告（仅管理员）
     */
    @GetMapping
    @ApiOperation("获取所有公告")
    public Result<List<Notice>> getAllNotices(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(noticeService.findAll());
    }

    /**
     * 获取未发布的公告（仅管理员）
     */
    @GetMapping("/draft")
    @ApiOperation("获取未发布的公告")
    public Result<List<Notice>> getDraftNotices(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(noticeService.getDraftNotices());
    }

    /**
     * 根据ID获取公告（登录即可，员工中心的公告详情会用到）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取公告")
    public Result<Notice> getNotice(@PathVariable Long id) {
        Notice notice = noticeService.findById(id);
        if (notice == null) {
            throw BusinessException.notFound("公告不存在或已删除");
        }
        return Result.success(notice);
    }
}
