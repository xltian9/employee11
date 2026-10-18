package com.limiti.employee.service;

import com.limiti.employee.dto.SalaryAdjustmentDTO;
import com.limiti.employee.dto.SalaryReportDTO;
import com.limiti.employee.entity.Performance;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 工资报告 Excel 导出。
 *
 * <p>第一个 sheet 是工资明细（含考勤、绩效、加减项、社保公积金个税），
 * 全员导出时第二个 sheet 追加业绩明细；合计行用 SUM 公式，改数据后自动重算。</p>
 *
 * @author Limiti
 */
@Component
public class SalaryExcelExporter {

    private static final String[] SALARY_HEADERS = {
            "员工ID", "工号", "姓名", "部门", "岗位", "年份", "月份",
            "工作日", "出勤", "迟到", "早退", "旷工", "事假", "病假", "其他假",
            "工作日加班(h)", "周末加班(h)", "节假日加班(h)", "加班费",
            "月薪", "请假扣款", "旷工扣款", "迟到扣款",
            "绩效得分", "绩效等级", "奖金",
            "补发", "扣款", "应发", "社保", "公积金", "个税", "实发"
    };

    private static final int[] SALARY_WIDTHS = {
            8, 12, 10, 12, 12, 7, 6,
            7, 7, 6, 6, 6, 6, 6, 7,
            12, 11, 12, 11,
            11, 11, 11, 11,
            10, 9, 11,
            10, 10, 11, 10, 10, 10, 12
    };

    /** 需要求和的金额类列（0 基） */
    private static final int[] MONEY_COLUMNS = {
            18, 19, 20, 21, 22, 25, 26, 27, 28, 29, 30, 31, 32
    };

    private static final String[] PERFORMANCE_HEADERS = {
            "工号", "姓名", "年份", "季度", "业绩分", "能力分", "态度分",
            "综合得分", "等级", "评价", "录入时间"
    };

    private static final int[] PERFORMANCE_WIDTHS = {12, 10, 7, 6, 9, 9, 9, 10, 7, 40, 18};

    public byte[] export(List<SalaryReportDTO> rows, List<Performance> performances,
                         int year, int month, boolean single) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSalarySheet(workbook, rows, year, month);
            if (!single && performances != null && !performances.isEmpty()) {
                writePerformanceSheet(workbook, performances);
            }
            if (hasAdjustments(rows)) {
                writeAdjustmentSheet(workbook, rows);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成 Excel 失败：" + e.getMessage(), e);
        }
    }

    private void writeSalarySheet(Workbook workbook, List<SalaryReportDTO> rows, int year, int month) {
        Sheet sheet = workbook.createSheet("工资报告");
        CellStyle titleStyle = titleStyle(workbook);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle textStyle = textStyle(workbook);
        CellStyle intStyle = intStyle(workbook);
        CellStyle scoreStyle = scoreStyle(workbook);
        CellStyle moneyStyle = moneyStyle(workbook);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(26);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(String.format("%d 年 %d 月 工资报告", year, month));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, SALARY_HEADERS.length - 1));

        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < SALARY_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(SALARY_HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, SALARY_WIDTHS[i] * 256);
        }

        int rowIndex = 2;
        for (SalaryReportDTO row : rows) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.setHeightInPoints(18);
            int col = 0;
            writeLong(dataRow, col++, row.getEmployeeId(), intStyle);
            writeText(dataRow, col++, row.getEmployeeNumber(), textStyle);
            writeText(dataRow, col++, row.getName(), textStyle);
            writeText(dataRow, col++, row.getDepartment(), textStyle);
            writeText(dataRow, col++, row.getPosition(), textStyle);
            writeInt(dataRow, col++, row.getYearNum(), intStyle);
            writeInt(dataRow, col++, row.getMonthNum(), intStyle);
            writeInt(dataRow, col++, row.getWorkDay(), intStyle);
            writeInt(dataRow, col++, zero(row.getActualDays()), intStyle);
            writeInt(dataRow, col++, zero(row.getLateCount()), intStyle);
            writeInt(dataRow, col++, zero(row.getEarlyCount()), intStyle);
            writeInt(dataRow, col++, zero(row.getAbsentDays()), intStyle);
            writeInt(dataRow, col++, zero(row.getCasualLeaveDays()), intStyle);
            writeInt(dataRow, col++, zero(row.getSickLeaveDays()), intStyle);
            writeInt(dataRow, col++, zero(row.getOtherLeaveDays()), intStyle);
            writeDecimal(dataRow, col++, row.getOvertimeWeekdayHours(), scoreStyle);
            writeDecimal(dataRow, col++, row.getOvertimeWeekendHours(), scoreStyle);
            writeDecimal(dataRow, col++, row.getOvertimeHolidayHours(), scoreStyle);
            writeMoney(dataRow, col++, row.getOvertimePay(), moneyStyle);
            writeMoney(dataRow, col++, row.getWage(), moneyStyle);
            writeMoney(dataRow, col++, row.getLeaveDeduct(), moneyStyle);
            writeMoney(dataRow, col++, row.getAbsentDeduct(), moneyStyle);
            writeMoney(dataRow, col++, row.getLateDeduct(), moneyStyle);
            writeDouble(dataRow, col++, row.getPerformanceScore(), scoreStyle);
            writeText(dataRow, col++, row.getPerformanceGrade(), textStyle);
            writeMoney(dataRow, col++, row.getBonus(), moneyStyle);
            writeMoney(dataRow, col++, row.getAdjustAdd(), moneyStyle);
            writeMoney(dataRow, col++, row.getAdjustDeduct(), moneyStyle);
            writeMoney(dataRow, col++, row.getGrossPay(), moneyStyle);
            writeMoney(dataRow, col++, row.getSocialInsurance(), moneyStyle);
            writeMoney(dataRow, col++, row.getHousingFund(), moneyStyle);
            writeMoney(dataRow, col++, row.getTax(), moneyStyle);
            writeMoney(dataRow, col++, row.getNetPay(), moneyStyle);
        }

        if (!rows.isEmpty()) {
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.setHeightInPoints(20);
            Cell label = totalRow.createCell(0);
            label.setCellValue("合计");
            label.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 2));
            for (int col = 3; col < SALARY_HEADERS.length; col++) {
                Cell cell = totalRow.createCell(col);
                cell.setCellStyle(moneyStyle);
            }
            for (int col : MONEY_COLUMNS) {
                Cell cell = totalRow.getCell(col);
                cell.setCellFormula(String.format("SUM(%s3:%s%d)",
                        columnName(col), columnName(col), rowIndex));
            }
        }

        sheet.createFreezePane(0, 2);
    }

    private void writePerformanceSheet(Workbook workbook, List<Performance> performances) {
        Sheet sheet = workbook.createSheet("业绩明细");
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle textStyle = textStyle(workbook);
        CellStyle intStyle = intStyle(workbook);
        CellStyle scoreStyle = scoreStyle(workbook);

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < PERFORMANCE_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PERFORMANCE_HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, PERFORMANCE_WIDTHS[i] * 256);
        }

        int rowIndex = 1;
        for (Performance performance : performances) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.setHeightInPoints(18);
            int col = 0;
            writeText(dataRow, col++, performance.getEmployee() == null
                    ? null : performance.getEmployee().getEmployeeNumber(), textStyle);
            writeText(dataRow, col++, performance.getEmployee() == null
                    ? null : performance.getEmployee().getName(), textStyle);
            writeInt(dataRow, col++, performance.getYear(), intStyle);
            writeInt(dataRow, col++, performance.getQuarter(), intStyle);
            writeDouble(dataRow, col++, performance.getScore(), scoreStyle);
            writeDouble(dataRow, col++, performance.getAbilityScore(), scoreStyle);
            writeDouble(dataRow, col++, performance.getAttitudeScore(), scoreStyle);
            writeDouble(dataRow, col++, performance.getFinalScore(), scoreStyle);
            writeText(dataRow, col++, performance.getGrade(), textStyle);
            writeText(dataRow, col++, performance.getEvaluation(), textStyle);
            writeText(dataRow, col++, performance.getCreateTime() == null ? null
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(performance.getCreateTime()), textStyle);
        }

        sheet.createFreezePane(0, 1);
    }

    /** 是否有补发/扣款记录 */
    private boolean hasAdjustments(List<SalaryReportDTO> rows) {
        if (rows == null) {
            return false;
        }
        for (SalaryReportDTO row : rows) {
            if (row.getAdjustments() != null && !row.getAdjustments().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 单独的补发/扣款明细文件，供财务核对使用 */
    public byte[] exportAdjustments(List<SalaryReportDTO> rows, int year, int month) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeAdjustmentSheet(workbook, rows);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成 Excel 失败：" + e.getMessage(), e);
        }
    }

    /** 补发/扣款附加 sheet */
    private void writeAdjustmentSheet(Workbook workbook, List<SalaryReportDTO> rows) {
        Sheet sheet = workbook.createSheet("补发扣款明细");
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle textStyle = textStyle(workbook);
        CellStyle moneyStyle = moneyStyle(workbook);

        String[] headers = {"工号", "姓名", "类型", "金额", "原因", "登记人", "登记时间"};
        int[] widths = {12, 10, 8, 12, 30, 12, 18};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        int rowIndex = 1;
        for (SalaryReportDTO row : rows) {
            if (row.getAdjustments() == null) {
                continue;
            }
            for (SalaryAdjustmentDTO adjustment : row.getAdjustments()) {
                Row dataRow = sheet.createRow(rowIndex++);
                int col = 0;
                writeText(dataRow, col++, row.getEmployeeNumber(), textStyle);
                writeText(dataRow, col++, row.getName(), textStyle);
                writeText(dataRow, col++, adjustment.getTypeName(), textStyle);
                writeMoney(dataRow, col++, adjustment.getAmount(), moneyStyle);
                writeText(dataRow, col++, adjustment.getReason(), textStyle);
                writeText(dataRow, col++, adjustment.getOperatorName(), textStyle);
                writeText(dataRow, col++, adjustment.getCreateTime(), textStyle);
            }
        }
        sheet.createFreezePane(0, 1);
    }

    private static String columnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        int index = columnIndex;
        while (index >= 0) {
            name.insert(0, (char) ('A' + index % 26));
            index = index / 26 - 1;
        }
        return name.toString();
    }

    private static int zero(Integer value) {
        return value == null ? 0 : value;
    }

    private void writeText(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void writeInt(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeLong(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeDouble(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeDecimal(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void writeMoney(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle textStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle intStyle(Workbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle scoreStyle(Workbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
