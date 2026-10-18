package com.limiti.employee.dto;

import lombok.Data;

/**
 * 某一天的考勤判定结果，用于考勤日历/明细展示。
 *
 * @author Limiti
 */
@Data
public class DailyAttendanceDTO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 判定状态：正常 / 迟到 / 早退 / 迟到+早退 / 旷工 / 请假 */
    private String status;

    private String checkIn;
    private String checkOut;

    /** 请假类型：事假/病假/年假/调休，非请假时为空 */
    private String leaveType;

    private String note;
}
