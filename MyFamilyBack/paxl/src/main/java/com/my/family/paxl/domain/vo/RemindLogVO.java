package com.my.family.paxl.domain.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 活跃提醒响应 VO，用于家庭主页浮层展示（状态为 PENDING/READ 的提醒）
 *
 * @author ai
 * @date 2026/03/18
 */
@Data
public class RemindLogVO {

    /**
     * 提醒记录ID，执行 done/close 操作时回传
     */
    private Long remindLogId;

    /**
     * 关联事项ID
     */
    private Long eventId;

    /**
     * 事项名称（来自 mf_event）
     */
    private String title;

    /**
     * 事项类别（来自 mf_event）
     */
    private String category;

    /**
     * 当前状态：PENDING / READ
     */
    private String status;

    /**
     * 当年事件实际发生日期（阳历），用于前端计算"还有X天"
     */
    private LocalDate eventDate;

    /**
     * 当年应开始提醒日期
     */
    private LocalDate remindDate;

    /**
     * 提前提醒天数（展示"提前X天提醒"）
     */
    private Integer remindAdvanceDays;
}
