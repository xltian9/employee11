package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.PageResult;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.security.SessionUser;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 员工管理控制器
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

    @Value("${file.upload.dir}")
    private String uploadDir;

    /**
     * 获取所有员工（仅管理员）
     */
    @GetMapping
    @ApiOperation("获取所有员工")
    public Result<List<Employee>> getAllEmployees(HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权访问员工列表");
        }
        try {
            List<Employee> employees = employeeService.findUserList();
            return Result.success(employees);
        } catch (Exception e) {
            log.error("获取员工列表失败", e);
            return Result.error("获取员工列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取员工（管理员，或员工本人）
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取员工")
    public Result<Employee> getEmployee(@PathVariable Long id, HttpServletRequest request) {
        if (!SessionUser.canAccessEmployee(request, id)) {
            return Result.error(403, "无权查看其他员工的信息");
        }
        try {
            Employee employee = employeeService.findById(id);
            return Result.success(employee);
        } catch (Exception e) {
            log.error("获取员工信息失败", e);
            return Result.error("获取员工信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建员工（仅管理员）
     */
    @PostMapping
    @ApiOperation("创建员工")
    public Result<Employee> createEmployee(@RequestBody Employee employee, HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权创建员工");
        }
        try {
            Employee created = employeeService.save(employee);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建员工失败", e);
            return Result.error("创建员工失败：" + e.getMessage());
        }
    }

    /**
     * 更新员工（仅管理员）
     */
    @PutMapping("/{id}")
    @ApiOperation("更新员工")
    public Result<Employee> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee employee,
            HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权修改员工信息");
        }
        try {
            Employee updated = employeeService.update(id, employee);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新员工失败", e);
            return Result.error("更新员工失败：" + e.getMessage());
        }
    }

    /**
     * 删除员工（仅管理员）—— 逻辑删除
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除员工")
    public Result<Void> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权删除员工");
        }
        try {
            employeeService.softDelete(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除员工失败", e);
            return Result.error("删除员工失败：" + e.getMessage());
        }
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
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权查看员工列表");
        }
        try {
            return Result.success(employeeService.searchPage(keyword, department, status, page, size));
        } catch (Exception e) {
            log.error("分页查询员工失败", e);
            return Result.error("分页查询员工失败：" + e.getMessage());
        }
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
        if (!SessionUser.isAdmin(request)) {
            return Result.error(403, "无权搜索员工");
        }
        try {
            log.info("搜索员工, keyword: {}, department: {}, status: {}",
                keyword, department, status);
            List<Employee> employees = employeeService.searchEmployees(
                keyword, department, status);
            return Result.success(employees);
        } catch (Exception e) {
            log.error("搜索员工失败", e);
            return Result.error("搜索员工失败：" + e.getMessage());
        }
    }

    /**
     * 上传员工照片（管理员，或员工本人）
     *
     * <p>加固要点：只接受白名单内的图片扩展名、校验真实图片内容、限制大小、
     * 服务端生成随机文件名（不再使用原始文件名，避免路径穿越与覆盖）。</p>
     */
    @PostMapping("/{id}/upload-photo")
    public Result<String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file,
            HttpServletRequest request) {

        if (!SessionUser.canAccessEmployee(request, id)) {
            return Result.error(403, "无权修改其他员工的照片");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("仅支持 jpg、jpeg、png、gif、bmp 格式的图片");
        }
        // 校验文件内容确实是图片，防止改扩展名上传任意文件
        try (InputStream in = file.getInputStream()) {
            if (ImageIO.read(in) == null) {
                return Result.error("文件内容不是有效的图片");
            }
        } catch (IOException e) {
            log.warn("校验上传图片失败", e);
            return Result.error("图片校验失败：" + e.getMessage());
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 服务端生成随机文件名，彻底丢弃客户端传入的文件名
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return Result.error("非法的文件路径");
            }

            // 覆盖前删除旧头像，避免磁盘上残留孤儿文件
            deleteOldPhoto(id, uploadPath);

            file.transferTo(filePath.toFile());
            employeeService.updateEmployeePhoto(id, filePath.toString());
            log.info("员工 [{}] 头像上传成功: {}", id, fileName);
            return Result.success(fileName);
        } catch (Exception e) {
            log.error("上传照片失败, employeeId: {}", id, e);
            return Result.error("上传照片失败：" + e.getMessage());
        }
    }

    /**
     * 获取照片（管理员，或员工本人）
     */
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id, HttpServletRequest request)
            throws IOException {
        if (!SessionUser.canAccessEmployee(request, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

        byte[] imageBytes = Files.readAllBytes(filePath);
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
