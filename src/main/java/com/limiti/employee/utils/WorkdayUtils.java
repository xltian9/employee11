package com.limiti.employee.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作日计算工具类
 *
 * <p>节假日为内置示例数据（目前只覆盖 2023 年国庆），实际部署时应改为从数据库或配置读取。</p>
 *
 * @author Limiti
 * @create 2025-04-27 17:31:22
 */
public class WorkdayUtils {

    /**
     * 获取指定月份的工作日天数（自动排除周末和自定义节假日）
     *
     * @param year  年份
     * @param month 月份 (1-12)
     * @return 工作日天数
     */
    public static int getWorkdaysInMonth(int year, int month) {
        return getWorkdayDates(year, month).size();
    }

    /**
     * 获取指定月份的所有工作日日期（升序）
     *
     * <p>考勤统计、绩效出勤系数都按"具体哪几天"算，因此除天数外还需要日期列表。</p>
     */
    public static List<LocalDate> getWorkdayDates(int year, int month) {
        Set<String> holidays = getHolidays(year, month);
        Set<String> extraWorkdays = getExtraWorkdays(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        List<LocalDate> dates = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            if (isWorkday(date, holidays, extraWorkdays)) {
                dates.add(date);
            }
        }
        return dates;
    }

    /**
     * 判断某个具体日期是否为工作日
     */
    public static boolean isWorkday(LocalDate date) {
        return isWorkday(date, getHolidays(date.getYear(), date.getMonthValue()),
                getExtraWorkdays(date.getYear(), date.getMonthValue()));
    }

    private static boolean isWorkday(LocalDate date, Set<String> holidays, Set<String> extraWorkdays) {
        String dateStr = date.getMonthValue() + "-" + date.getDayOfMonth();
        if (extraWorkdays.contains(dateStr)) {
            return true;
        }
        if (holidays.contains(dateStr)) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 获取自定义节假日（示例数据，实际应从数据库或配置读取）
     */
    private static Set<String> getHolidays(int year, int month) {
        Set<String> holidays = new HashSet<>();
        if (year == 2023 && month == 10) {
            holidays.add("10-1");
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
        if (year == 2023 && month == 10) {
            extraWorkdays.add("10-7");
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
