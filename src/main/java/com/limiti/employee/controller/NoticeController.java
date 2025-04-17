package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Notice;
import com.limiti.employee.repository.AdminRepository;
import com.limiti.employee.service.NoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理控制器
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/notices")
@Api(tags = "公告管理接口")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private AdminRepository adminRepository;

    /**
     * 创建公告
     */
    @PostMapping
    @ApiOperation("创建公告")
    public Result<Notice> createNotice(@RequestBody Notice notice) {
        try {
            // TODO: 后续应该从Session中获取
            Admin admin = adminRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("管理员不存在"));
            
            Notice created = noticeService.createNotice(notice, admin);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建公告失败", e);
            return Result.error("创建公告失败：" + e.getMessage());
        }
    }

    /**
     * 发布公告
     */
    @PostMapping("/{id}/publish")
    @ApiOperation("发布公告")
    public Result<Notice> publishNotice(@PathVariable Long id) {
        try {
            Notice published = noticeService.publishNotice(id);
            return Result.success(published);
        } catch (Exception e) {
            log.error("发布公告失败", e);
            return Result.error("发布公告失败：" + e.getMessage());
        }
    }

    /**
     * 撤回公告
     */
    @PostMapping("/{id}/withdraw")
    @ApiOperation("撤回公告")
    public Result<Notice> withdrawNotice(@PathVariable Long id) {
        try {
            Notice withdrawn = noticeService.withdrawNotice(id);
            return Result.success(withdrawn);
        } catch (Exception e) {
            log.error("撤回公告失败", e);
            return Result.error("撤回公告失败：" + e.getMessage());
        }
    }

    /**
     * 获取已发布的公告列表
     */
    @GetMapping("/published")
    @ApiOperation("获取已发布的公告")
    public Result<List<Notice>> getPublishedNotices() {
        try {
            List<Notice> notices = noticeService.getPublishedNotices();
            return Result.success(notices);
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return Result.error("获取公告列表失败：" + e.getMessage());
        }
    }

    /**
     * 搜索公告
     */
    @GetMapping("/search")
    @ApiOperation("搜索公告")
    public Result<List<Notice>> searchNotices(@RequestParam String keyword) {
        try {
            List<Notice> notices = noticeService.searchNotices(keyword);
            return Result.success(notices);
        } catch (Exception e) {
            log.error("搜索公告失败", e);
            return Result.error("搜索公告失败：" + e.getMessage());
        }
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    @ApiOperation("更新公告")
    public Result<Notice> updateNotice(@PathVariable Long id, @RequestBody Notice notice) {
        try {
            // TODO: 后续应该从Session中获取
            Admin admin = adminRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("管理员不存在"));
            
            Notice updated = noticeService.updateNotice(id, notice, admin);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新公告失败", e);
            return Result.error("更新公告失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有公告
     */
    @GetMapping
    @ApiOperation("获取所有公告")
    public Result<List<Notice>> getAllNotices() {
        try {
            List<Notice> notices = noticeService.findAll();
            return Result.success(notices);
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return Result.error("获取公告列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取未发布的公告
     */
    @GetMapping("/draft")
    @ApiOperation("获取未发布的公告")
    public Result<List<Notice>> getDraftNotices() {
        try {
            List<Notice> notices = noticeService.getDraftNotices();
            return Result.success(notices);
        } catch (Exception e) {
            log.error("获取未发布公告失败", e);
            return Result.error("获取未发布公告失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取公告
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取公告")
    public Result<Notice> getNotice(@PathVariable Long id) {
        try {
            log.info("获取公告详情, id: {}", id);
            Notice notice = noticeService.findById(id);
            return Result.success(notice);
        } catch (Exception e) {
            log.error("获取公告详情失败", e);
            return Result.error("获取公告详情失败：" + e.getMessage());
        }
    }
} 