package com.limiti.employee.service;

import com.limiti.employee.entity.Notice;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.repository.NoticeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;

/**
 * 公告服务
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@Service
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    /**
     * 创建公告
     */
    @Transactional
    public Notice createNotice(Notice notice, Admin publisher) {
        notice.setPublisher(publisher);
        notice.setStatus(0); // 草稿状态
        notice.setCreateTime(new Date());
        notice.setUpdateTime(new Date());
        
        return noticeRepository.save(notice);
    }

    /**
     * 更新公告
     */
    @Transactional
    public Notice updateNotice(Long id, Notice notice, Admin publisher) {
        Notice existingNotice = findById(id);
        
        // 只能更新自己发布的公告
        if (!existingNotice.getPublisher().getId().equals(publisher.getId())) {
            throw new IllegalStateException("只能更新自己发布的公告");
        }
        
        existingNotice.setTitle(notice.getTitle());
        existingNotice.setContent(notice.getContent());
        existingNotice.setUpdateTime(new Date());
        
        return noticeRepository.save(existingNotice);
    }

    /**
     * 发布公告
     */
    @Transactional
    public Notice publishNotice(Long id) {
        Notice notice = findById(id);
        notice.setStatus(1); // 已发布状态
        notice.setPublishTime(new Date());
        notice.setUpdateTime(new Date());
        
        return noticeRepository.save(notice);
    }

    /**
     * 撤回公告
     */
    @Transactional
    public Notice withdrawNotice(Long id) {
        Notice notice = findById(id);
        notice.setStatus(2); // 已撤回状态
        notice.setUpdateTime(new Date());
        
        return noticeRepository.save(notice);
    }

    /**
     * 根据ID查找公告
     */
    public Notice findById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("公告不存在: " + id));
    }

    /**
     * 获取已发布的公告
     */
    public List<Notice> getPublishedNotices() {
        return noticeRepository.findByStatusOrderByCreateTimeDesc(1);
    }

    /**
     * 搜索公告
     */
    public List<Notice> searchNotices(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublishedNotices();
        }
        return noticeRepository.findByTitleContainingOrderByCreateTimeDesc(keyword.trim());
    }

    /**
     * 获取管理员发布的公告
     */
    public List<Notice> getNoticesByAdmin(Long adminId) {
        return noticeRepository.findByPublisherId(adminId);
    }

    /**
     * 获取所有公告
     */
    public List<Notice> findAll() {
        return noticeRepository.findAll();
    }

    /**
     * 获取未发布的公告
     */
    public List<Notice> getDraftNotices() {
        return noticeRepository.findByStatusOrderByCreateTimeDesc(0);
    }
} 