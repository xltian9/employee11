package com.limiti.employee.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
/**
 * @author Limiti
 * @create 2025-04-27 17:31:22
 */
/**
 * 工作日计算工具类
 */
public class WorkdayUtils {

    /**
     * 获取指定月份的工作日天数（自动排除周末和自定义节假日）
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 工作日天数
     */
    public static int getWorkdaysInMonth(int year, int month) {
        // 自定义节假日集合（日期格式："MM-dd"）
        Set<String> holidays = getHolidays(year, month);

        // 自定义调休工作日集合（周末但需要上班的日期）
        Set<String> extraWorkdays = getExtraWorkdays(year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        int workdays = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            if (isWorkday(date, holidays, extraWorkdays)) {
                workdays++;
            }
        }

        return workdays;
    }

    /**
     * 判断是否为工作日
     */
    private static boolean isWorkday(LocalDate date, Set<String> holidays, Set<String> extraWorkdays) {
        String dateStr = date.getMonthValue() + "-" + date.getDayOfMonth();

        // 1. 检查是否是调休工作日
        if (extraWorkdays.contains(dateStr)) {
            return true;
        }

        // 2. 检查是否是节假日
        if (holidays.contains(dateStr)) {
            return false;
        }

        // 3. 检查是否是周末
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 获取自定义节假日（示例数据，实际应从数据库或配置读取）
     */
    private static Set<String> getHolidays(int year, int month) {
        Set<String> holidays = new HashSet<>();
        // 示例：2023年10月节假日
        if (year == 2023 && month == 10) {
            holidays.add("10-1"); // 国庆节
            holidays.add("10-2");
            holidays.add("10-3");
            holidays.add("10-4");
            holidays.add("10-5");
            holidays.add("10-6");
        }
        return holidays;
    }

    /**
     * 获取自定义调休工作日（示例数据）
     */
    private static Set<String> getExtraWorkdays(int year, int month) {
        Set<String> extraWorkdays = new HashSet<>();
        // 示例：2023年10月调休
        if (year == 2023 && month == 10) {
            extraWorkdays.add("10-7"); // 国庆调休上班
            extraWorkdays.add("10-8");
        }
        return extraWorkdays;
    }

    /**
     * 获取当前月份的工作日天数
     */
    public static int getCurrentMonthWorkdays() {
        LocalDate today = LocalDate.now();
        return getWorkdaysInMonth(today.getYear(), today.getMonthValue());
    }
}