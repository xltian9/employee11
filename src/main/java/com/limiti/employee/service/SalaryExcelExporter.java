package com.limiti.employee.service;

import com.limiti.employee.dto.SalaryReportDTO;
import com.limiti.employee.entity.Employee;
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
 * 工资报告 / 业绩明细导出为 Excel（.xlsx）
 *
 * @author Limiti
 */
@Component
public class SalaryExcelExporter {

    private static final String[] SALARY_HEADERS = {
            "员工 ID", "员工姓名", "员工部门", "员工岗位", "年份", "月份",
            "当月工作日", "月薪", "当月请假天数", "季度绩效评分", "奖金", "实际工资"
    };

    private static final String[] PERFORMANCE_HEADERS = {
            "员工 ID", "员工姓名", "员工部门", "年份", "季度", "绩效评分", "绩效评价", "录入人", "录入时间"
    };

    /**
     * 生成工资报告工作簿
     *
     * @param rows         工资明细
     * @param performances 统计季度内的业绩记录
     * @param year         年份
     * @param month        月份
     * @param single       是否单人视图（员工自查导出的标题不同）
     * @return xlsx 字节
     */
    public byte[] export(List<SalaryReportDTO> rows, List<Performance> performances,
                         int year, int month, boolean single) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024)) {

            Styles styles = new Styles(workbook);
            writeSalarySheet(workbook, styles, rows, year, month, single);
            if (!single) {
                writePerformanceSheet(workbook, styles, performances, year);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeSalarySheet(Workbook workbook, Styles styles, List<SalaryReportDTO> rows,
                                  int year, int month, boolean single) {
        Sheet sheet = workbook.createSheet("工资报告");
        String title = single
                ? (year + " 年 " + month + " 月个人工资明细")
                : (year + " 年 " + month + " 月工资报告");

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, SALARY_HEADERS.length - 1));
        titleRow.setHeightInPoints(24);

        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < SALARY_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(SALARY_HEADERS[i]);
            cell.setCellStyle(styles.header);
        }
        headerRow.setHeightInPoints(20);

        BigDecimal wageSum = BigDecimal.ZERO;
        BigDecimal bonusSum = BigDecimal.ZERO;
        BigDecimal nowWageSum = BigDecimal.ZERO;
        int leaveSum = 0;

        int rowIndex = 2;
        for (SalaryReportDTO row : rows) {
            Row excelRow = sheet.createRow(rowIndex++);
            writeLong(excelRow, 0, row.getEmployeeId(), styles.integer);
            writeText(excelRow, 1, row.getName(), styles.text);
            writeText(excelRow, 2, row.getDepartment(), styles.text);
            writeText(excelRow, 3, row.getPosition(), styles.text);
            writeInt(excelRow, 4, row.getYearNum(), styles.integer);
            writeInt(excelRow, 5, row.getMonthNum(), styles.integer);
            writeInt(excelRow, 6, row.getWorkDay(), styles.integer);
            writeMoney(excelRow, 7, row.getWage(), styles.money);
            writeInt(excelRow, 8, row.getLeavesDay(), styles.integer);
            if (row.getPerformanceScore() == null) {
                Cell empty = excelRow.createCell(9);
                empty.setCellValue("未录入");
                empty.setCellStyle(styles.text);
            } else {
                writeDouble(excelRow, 9, row.getPerformanceScore(), styles.score);
            }
            writeMoney(excelRow, 10, row.getBonus(), styles.money);
            writeMoney(excelRow, 11, row.getNowWage(), styles.money);

            wageSum = wageSum.add(nvl(row.getWage()));
            bonusSum = bonusSum.add(nvl(row.getBonus()));
            nowWageSum = nowWageSum.add(nvl(row.getNowWage()));
            leaveSum += row.getLeavesDay() == null ? 0 : row.getLeavesDay();
        }

        if (!rows.isEmpty()) {
            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("合计");
            totalLabel.setCellStyle(styles.total);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 6));

            Cell wageTotal = totalRow.createCell(7);
            wageTotal.setCellFormula("SUM(H3:H" + rowIndex + ")");
            wageTotal.setCellStyle(styles.totalMoney);

            writeInt(totalRow, 8, leaveSum, styles.totalInteger);
            writeText(totalRow, 9, "", styles.total);

            Cell bonusTotal = totalRow.createCell(10);
            bonusTotal.setCellFormula("SUM(K3:K" + rowIndex + ")");
            bonusTotal.setCellStyle(styles.totalMoney);

            Cell nowWageTotal = totalRow.createCell(11);
            nowWageTotal.setCellFormula("SUM(L3:L" + rowIndex + ")");
            nowWageTotal.setCellStyle(styles.totalMoney);
        }

        int[] widths = {10, 12, 14, 14, 8, 8, 12, 12, 14, 14, 12, 14};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 2);
    }

    private void writePerformanceSheet(Workbook workbook, Styles styles,
                                       List<Performance> performances, int year) {
        Sheet sheet = workbook.createSheet("业绩明细");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < PERFORMANCE_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PERFORMANCE_HEADERS[i]);
            cell.setCellStyle(styles.header);
        }
        headerRow.setHeightInPoints(20);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int rowIndex = 1;
        for (Performance performance : performances) {
            Employee employee = performance.getEmployee();
            Row row = sheet.createRow(rowIndex++);
            writeLong(row, 0, employee == null ? null : employee.getId(), styles.integer);
            writeText(row, 1, employee == null ? "" : employee.getName(), styles.text);
            writeText(row, 2, employee == null ? "" : employee.getDepartment(), styles.text);
            writeInt(row, 3, performance.getYear(), styles.integer);
            writeInt(row, 4, performance.getQuarter(), styles.integer);
            if (performance.getScore() == null) {
                writeText(row, 5, "", styles.text);
            } else {
                writeDouble(row, 5, performance.getScore(), styles.score);
            }
            writeText(row, 6, performance.getEvaluation(), styles.wrap);
            writeText(row, 7, performance.getEvaluator() == null ? "" : performance.getEvaluator().getName(), styles.text);
            writeText(row, 8, performance.getCreateTime() == null ? "" : format.format(performance.getCreateTime()), styles.text);
        }

        int[] widths = {10, 12, 14, 8, 8, 12, 40, 14, 18};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void writeInt(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeLong(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeDouble(Row row, int column, Double value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 复用同一套单元格样式
     */
    private static class Styles {
        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle wrap;
        private final CellStyle integer;
        private final CellStyle score;
        private final CellStyle money;
        private final CellStyle total;
        private final CellStyle totalInteger;
        private final CellStyle totalMoney;

        Styles(Workbook workbook) {
            Font baseFont = workbook.createFont();
            baseFont.setFontName("微软雅黑");
            baseFont.setFontHeightInPoints((short) 10);

            Font titleFont = workbook.createFont();
            titleFont.setFontName("微软雅黑");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("微软雅黑");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);

            this.title = workbook.createCellStyle();
            this.title.setFont(titleFont);
            this.title.setAlignment(HorizontalAlignment.CENTER);
            this.title.setVerticalAlignment(VerticalAlignment.CENTER);

            this.header = workbook.createCellStyle();
            this.header.setFont(headerFont);
            this.header.setAlignment(HorizontalAlignment.CENTER);
            this.header.setVerticalAlignment(VerticalAlignment.CENTER);
            this.header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            this.header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(this.header);

            this.text = workbook.createCellStyle();
            this.text.setFont(baseFont);
            this.text.setVerticalAlignment(VerticalAlignment.CENTER);
            border(this.text);

            this.wrap = workbook.createCellStyle();
            this.wrap.setFont(baseFont);
            this.wrap.setWrapText(true);
            this.wrap.setVerticalAlignment(VerticalAlignment.CENTER);
            border(this.wrap);

            this.integer = workbook.createCellStyle();
            this.integer.setFont(baseFont);
            this.integer.setAlignment(HorizontalAlignment.CENTER);
            this.integer.setDataFormat(workbook.createDataFormat().getFormat("0"));
            border(this.integer);

            this.score = workbook.createCellStyle();
            this.score.setFont(baseFont);
            this.score.setAlignment(HorizontalAlignment.CENTER);
            this.score.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
            border(this.score);

            this.money = workbook.createCellStyle();
            this.money.setFont(baseFont);
            this.money.setAlignment(HorizontalAlignment.RIGHT);
            this.money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            border(this.money);

            this.total = workbook.createCellStyle();
            this.total.setFont(headerFont);
            this.total.setAlignment(HorizontalAlignment.RIGHT);
            this.total.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            this.total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(this.total);

            this.totalInteger = workbook.createCellStyle();
            this.totalInteger.setFont(headerFont);
            this.totalInteger.setAlignment(HorizontalAlignment.CENTER);
            this.totalInteger.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            this.totalInteger.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            this.totalInteger.setDataFormat(workbook.createDataFormat().getFormat("0"));
            border(this.totalInteger);

            this.totalMoney = workbook.createCellStyle();
            this.totalMoney.setFont(headerFont);
            this.totalMoney.setAlignment(HorizontalAlignment.RIGHT);
            this.totalMoney.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            this.totalMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            this.totalMoney.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            border(this.totalMoney);
        }

        private static void border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        }
    }
}
