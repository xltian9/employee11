package com.limiti.employee.repository;

import com.limiti.employee.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where "
            + "(:operator is null or a.operatorName like concat('%', :operator, '%')) and "
            + "(:action is null or a.action = :action) and "
            + "(:targetType is null or a.targetType = :targetType) and "
            + "(:from is null or a.createTime >= :from) and "
            + "(:to is null or a.createTime <= :to) "
            + "order by a.createTime desc")
    Page<AuditLog> search(@Param("operator") String operator,
                          @Param("action") String action,
                          @Param("targetType") String targetType,
                          @Param("from") Date from,
                          @Param("to") Date to,
                          Pageable pageable);
}
