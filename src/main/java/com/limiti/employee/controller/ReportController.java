package com.limiti.employee.controller;

import com.limiti.employee.common.Result;
import com.limiti.employee.dto.SalaryReportDTO;
import com.limiti.employee.entity.Employee;
import com.limiti.employee.entity.Performance;
import com.limiti.employee.exception.BusinessException;
import com.limiti.employee.repository.PerformanceRepository;
import com.limiti.employee.security.SessionUser;
import com.limiti.employee.service.SalaryExcelExporter;
import com.limiti.employee.service.SalaryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 报表接口
 *
 * <p>鉴权失败抛 {@link BusinessException}，由全局处理器返回统一 JSON（即使调用方期待的是 xlsx，
 * 前端也会根据 Content-Type 判断该显示文件还是错误提示）。</p>
 *
 * @author Limiti
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private SalaryReportService salaryReportService;

    @Autowired
    private SalaryExcelExporter salaryExcelExporter;

    @Autowired
    private PerformanceRepository performanceRepository;

    /**
     * 月度工资报告（仅管理员）
     */
    @GetMapping("/list")
    public Result<List<SalaryReportDTO>> list(@RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Integer month,
                                              HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        int[] period = resolvePeriod(year, month);
        return Result.success(salaryReportService.buildReport(period[0], period[1]));
    }

    /**
     * 我的工资（员工查看自己的当月工资明细，不含他人）
     */
    @GetMapping("/my")
    public Result<SalaryReportDTO> my(@RequestParam(required = false) Integer year,
                                      @RequestParam(required = false) Integer month,
                                      HttpServletRequest request) {
        Employee employee = SessionUser.employee(request);
        if (employee == null) {
            throw BusinessException.forbidden("请使用员工账号登录后查看本人工资");
        }
        int[] period = resolvePeriod(year, month);
        SalaryReportDTO row = salaryReportService.buildEmployeeReport(employee.getId(), period[0], period[1]);
        if (row == null) {
            throw BusinessException.notFound("未找到该员工的工资信息");
        }
        return Result.success(row);
    }

    /**
     * 导出工资报告（Excel，仅管理员）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month,
                                         HttpServletRequest request) {
        SessionUser.requireAdmin(request);
        int[] period = resolvePeriod(year, month);
        int quarter = (period[1] - 1) / 3 + 1;
        List<SalaryReportDTO> rows = salaryReportService.buildReport(period[0], period[1]);
        List<Performance> performances =
                performanceRepository.findByYearAndQuarterOrderByScoreDesc(period[0], quarter);
        byte[] bytes = salaryExcelExporter.export(rows, performances, period[0], period[1], false);
        return excel(bytes, "工资报告-" + period[0] + "-" + pad(period[1]) + ".xlsx",
                "salary-report-" + period[0] + "-" + pad(period[1]) + ".xlsx");
    }

    /**
     * 导出本人工资明细（Excel，员工自查）
     */
    @GetMapping("/my/export")
    public ResponseEntity<byte[]> exportMine(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month,
                                             HttpServletRequest request) {
        Employee employee = SessionUser.employee(request);
        if (employee == null) {
            throw BusinessException.forbidden("请使用员工账号登录后导出本人工资");
        }
        int[] period = resolvePeriod(year, month);
        SalaryReportDTO row = salaryReportService.buildEmployeeReport(employee.getId(), period[0], period[1]);
        if (row == null) {
            throw BusinessException.notFound("未找到该员工的工资信息");
        }
        byte[] bytes = salaryExcelExporter.export(Collections.singletonList(row),
                Collections.<Performance>emptyList(), period[0], period[1], true);
        return excel(bytes, "工资明细-" + period[0] + "-" + pad(period[1]) + ".xlsx",
                "salary-detail-" + period[0] + "-" + pad(period[1]) + ".xlsx");
    }

    /**
     * 解析年月参数，缺省为当前年月；非法值抛 400
     */
    private int[] resolvePeriod(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year == null ? now.getYear() : year;
        int targetMonth = month == null ? now.getMonthValue() : month;
        if (targetMonth < 1 || targetMonth > 12) {
            throw BusinessException.badRequest("月份必须在 1 到 12 之间");
        }
        if (targetYear < 2000 || targetYear > 2100) {
            throw BusinessException.badRequest("年份必须在 2000 到 2100 之间");
        }
        return new int[]{targetYear, targetMonth};
    }

    private String pad(int month) {
        return month < 10 ? "0" + month : String.valueOf(month);
    }

    private ResponseEntity<byte[]> excel(byte[] bytes, String fileName, String asciiName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''"
                + encode(fileName));
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "report.xlsx";
        }
    }
}
