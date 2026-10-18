package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PageResult;
import com.limiti.employee.entity.Admin;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.AuditLogService;
import com.limiti.employee.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 员工管理控制器
 *
 * <p>异常统一抛给 {@code GlobalExceptionHandler} 处理，这里只写业务分支，
 * 不再逐个方法 try/catch（返回体与 HTTP 状态码都由全局处理器统一）。</p>
 *
 * @author Limiti
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/api/employees")
@Api(tags = "员工管理接口")
public class EmployeeController {

    /** 允许上传的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp"));

    /** 单张图片大小上限：5MB */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AuditLogService auditLogService;

    @Value("${file.upload.dir}")
    private String uploadDir;

    /**
     * 获取所有员工（仅管理员）
     */
    @GetMapping
    @ApiOperation("获取所有员工")
    public Result<List<Employee>> getAllEmployees(HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(employeeService.findUserList());
    }

    /**
     * 根据ID获取员工（管理员，或员工本人）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取员工")
    public Result<Employee> getEmployee(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAccessEmployee(request, id);
        Employee employee = employeeService.findById(id);
        if (employee == null) {
            throw BusinessException.notFound("员工不存在");
        }
        return Result.success(employee);
    }

    /**
     * 创建员工（仅管理员）
     */
    @PostMapping
    @ApiOperation("创建员工")
    public Result<Employee> createEmployee(@RequestBody Employee employee, HttpServletRequest request) {
        Admin admin = SessionUser.requireAdmin(request);
        Employee created = employeeService.save(employee);
        auditLogService.record(AuditLogService.EMPLOYEE_CREATE, "新增员工",
                "EMPLOYEE", created.getId(), created.getName(),
                String.format("工号 %s，部门 %s，岗位 %s，月薪 %s",
                        created.getEmployeeNumber(), text(created.getDepartment()),
                        text(created.getPosition()), money(created.getWage())));
        return Result.success("创建成功", created);
    }

    /**
     * 更新员工（仅管理员）
     *
     * <p>会记录"谁把谁的月薪从多少改成了多少"这类变更明细到审计日志。</p>
     */
    @PutMapping("/{id}")
    @ApiOperation("更新员工")
    public Result<Employee> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee employee,
            HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        Employee before = employeeService.findById(id);
        if (before == null) {
            throw BusinessException.notFound("员工不存在");
        }
        List<String> changes = new ArrayList<>();
        appendChange(changes, "姓名", before.getName(), employee.getName());
        appendChange(changes, "部门", before.getDepartment(), employee.getDepartment());
        appendChange(changes, "岗位", before.getPosition(), employee.getPosition());
        appendChange(changes, "手机号", before.getPhone(), employee.getPhone());
        appendChange(changes, "邮箱", before.getEmail(), employee.getEmail());
        if (employee.getWage() != null && !sameMoney(before.getWage(), employee.getWage())) {
            changes.add("月薪 " + money(before.getWage()) + " → " + money(employee.getWage()));
        }
        appendChange(changes, "在职状态",
                Boolean.TRUE.equals(before.getStatus()) ? "在职" : "离职",
                employee.getStatus() == null ? null
                        : (Boolean.TRUE.equals(employee.getStatus()) ? "在职" : "离职"));

        Employee updated = employeeService.update(id, employee);
        if (changes.isEmpty()) {
            changes.add("无字段变化");
        }
        auditLogService.record(AuditLogService.EMPLOYEE_UPDATE, "修改员工",
                "EMPLOYEE", id, updated.getName(), String.join("；", changes));
        return Result.success("修改成功", updated);
    }

    /**
     * 删除员工（仅管理员）—— 逻辑删除
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除员工")
    public Result<Void> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        Employee employee = employeeService.findById(id);
        if (employee == null) {
            throw BusinessException.notFound("员工不存在");
        }
        employeeService.softDelete(id);
        auditLogService.record(AuditLogService.EMPLOYEE_DELETE, "删除员工",
                "EMPLOYEE", id, employee.getName(), "标记为离职（逻辑删除）");
        return Result.success("删除成功", null);
    }

    /**
     * 分页查询员工（仅管理员）
     *
     * <p>page 从 1 开始，size 上限 100；keyword 匹配姓名/工号，department 为部门名精确匹配，
     * status 为在职状态。</p>
     */
    @GetMapping("/page")
    @ApiOperation("分页查询员工")
    public Result<PageResult<Employee>> pageEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean status,
            HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(employeeService.searchPage(keyword, department, status, page, size));
    }

    /**
     * 搜索员工（仅管理员）
     */
    @GetMapping("/search")
    @ApiOperation("搜索员工")
    public Result<List<Employee>> searchEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean status,
            HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        return Result.success(employeeService.searchEmployees(keyword, department, status));
    }

    /**
     * 上传员工照片（管理员，或员工本人）
     *
     * <p>加固要点：只接受白名单内的图片扩展名、校验真实图片内容、限制大小、
     * 服务端生成随机文件名（不再使用原始文件名，避免路径穿越与覆盖）。</p>
     */
    @PostMapping("/{id}/upload-photo")
    @ApiOperation("上传员工照片")
    public Result<String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file,
            HttpServletRequest request) {

        SessionUser.requireAccessEmployee(request, id);
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("图片大小不能超过 5MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BusinessException.badRequest("仅支持 jpg、jpeg、png、gif、bmp 格式的图片");
        }
        // 校验文件内容确实是图片，防止改扩展名上传任意文件
        try (InputStream in = file.getInputStream()) {
            if (ImageIO.read(in) == null) {
                throw BusinessException.badRequest("文件内容不是有效的图片");
            }
        } catch (IOException e) {
            log.warn("校验上传图片失败", e);
            throw BusinessException.badRequest("图片校验失败，请重新选择");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", uploadPath, e);
            throw new BusinessException(500, "服务器无法创建上传目录，请联系管理员");
        }

        // 服务端生成随机文件名，彻底丢弃客户端传入的文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw BusinessException.badRequest("非法的文件路径");
        }

        // 覆盖前删除旧头像，避免磁盘上残留孤儿文件
        deleteOldPhoto(id, uploadPath);

        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", filePath, e);
            throw new BusinessException(500, "图片保存失败，请稍后重试");
        }
        employeeService.updateEmployeePhoto(id, filePath.toString());
        log.info("员工 [{}] 头像上传成功: {}", id, fileName);
        return Result.success("上传成功", fileName);
    }

    /**
     * 获取照片（管理员，或员工本人）
     *
     * <p>二进制接口不便返回统一 JSON，鉴权失败仍返回 403，但由全局处理器负责 JSON 化；
     * 这里只在文件不存在时返回 404。</p>
     */
    @GetMapping("/{id}/photo")
    @ApiOperation("获取员工照片")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id, HttpServletRequest request) {
        SessionUser.requireAccessEmployee(request, id);

        String photoPath = employeeService.getPhotoPath(id);
        if (photoPath == null || photoPath.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 只允许读取上传目录内的文件，防止数据库里的路径被改成任意系统文件
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = Paths.get(photoPath).toAbsolutePath().normalize();
        if (!filePath.startsWith(root) || !Files.isRegularFile(filePath)) {
            log.warn("拒绝读取上传目录之外的文件: {}", photoPath);
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取头像文件失败: {}", filePath, e);
            throw new BusinessException(500, "头像读取失败");
        }
        return ResponseEntity.ok()
                .contentType(mediaTypeOf(filePath.toString()))
                .body(imageBytes);
    }

    /**
     * 删除旧头像文件（只删上传目录内的文件）
     */
    private void deleteOldPhoto(Long id, Path uploadRoot) {
        try {
            String oldPath = employeeService.getPhotoPath(id);
            if (oldPath == null || oldPath.trim().isEmpty()) {
                return;
            }
            Path oldFile = Paths.get(oldPath).toAbsolutePath().normalize();
            if (oldFile.startsWith(uploadRoot)) {
                Files.deleteIfExists(oldFile);
            }
        } catch (Exception e) {
            log.warn("删除旧头像失败, employeeId: {}", id, e);
        }
    }

    private void appendChange(List<String> changes, String label, String before, String after) {
        if (after == null || after.trim().isEmpty()) {
            return;
        }
        String oldValue = before == null ? "" : before;
        if (!oldValue.equals(after)) {
            changes.add(label + " " + (oldValue.isEmpty() ? "空" : oldValue) + " → " + after);
        }
    }

    private boolean sameMoney(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        return left.compareTo(b == null ? BigDecimal.ZERO : b) == 0;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    private String text(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase();
    }

    private MediaType mediaTypeOf(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".bmp")) {
            return MediaType.valueOf("image/bmp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
