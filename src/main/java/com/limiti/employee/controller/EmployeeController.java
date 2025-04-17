package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    
    @Autowired
    private EmployeeService employeeService;

    /**
     * 获取所有员工
     */
    @GetMapping
    @ApiOperation("获取所有员工")
    public Result<List<Employee>> getAllEmployees() {
        try {
            List<Employee> employees = employeeService.findUserList();
            return Result.success(employees);
        } catch (Exception e) {
            log.error("获取员工列表失败", e);
            return Result.error("获取员工列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取员工
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取员工")
    public Result<Employee> getEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.findById(id);
            return Result.success(employee);
        } catch (Exception e) {
            log.error("获取员工信息失败", e);
            return Result.error("获取员工信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建员工
     */
    @PostMapping
    @ApiOperation("创建员工")
    public Result<Employee> createEmployee(@RequestBody Employee employee) {
        try {
            Employee created = employeeService.save(employee);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建员工失败", e);
            return Result.error("创建员工失败：" + e.getMessage());
        }
    }

    /**
     * 更新员工
     */
    @PutMapping("/{id}")
    @ApiOperation("更新员工")
    public Result<Employee> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee employee) {
        try {
            Employee updated = employeeService.update(id, employee);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新员工失败", e);
            return Result.error("更新员工失败：" + e.getMessage());
        }
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除员工")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.findById(id);
            employee.setIsDel(true);
            employeeService.update(id, employee);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除员工失败", e);
            return Result.error("删除员工失败：" + e.getMessage());
        }
    }

    /**
     * 搜索员工
     */
    @GetMapping("/search")
    @ApiOperation("搜索员工")
    public Result<List<Employee>> searchEmployees(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean status) {
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


    @Value("${file.upload.dir}")
    private String uploadDir;

    // 上传照片并保存路径到数据库
    @PostMapping("/{id}/upload-photo")
    public String uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file) throws IOException {

        // 1. 创建存储目录（如果不存在）
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. 生成唯一文件名（避免冲突）
        String fileName = id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // 3. 保存文件到本地
        file.transferTo(filePath.toFile());

        // 4. 将文件路径保存到数据库（伪代码）
        employeeService.updateEmployeePhoto(id, filePath.toString());

        return "照片上传成功: " + fileName;
    }

    // 获取照片（返回文件流或Base64）
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) throws IOException {
        String photoPath = employeeService.getPhotoPath(id); // 从数据库获取路径
        byte[] imageBytes = Files.readAllBytes(Paths.get(photoPath));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // 根据实际类型调整
                .body(imageBytes);
    }
}