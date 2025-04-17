package com.limiti.employee.repository;

import com.limiti.employee.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员数据访问接口
 * @author Limiti
 * @date 2024/11/15
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * 根据用户名查找管理员
     * @param username 用户名
     * @return 管理员信息
     */
    Optional<Admin> findByUsername(String username);
} 