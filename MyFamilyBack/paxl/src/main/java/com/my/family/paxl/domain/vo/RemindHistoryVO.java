package com.my.family.paxl.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 历年提醒记录 VO，用于重要事项详情页"历年完成情况"展示
 *
 * @author ai
 * @date 2026/03/18
 */
@Data
public class RemindHistoryVO {

    /**
     * 提醒记录ID
     */
    private Long remindLogId;

    /**
     * 对应年份（如 2024、2025、2026）
     */
    private Integer triggerYear;

    /**
     * 当年事件实际发生日期（阳历）
     */
    private LocalDate eventDate;

    /**
     * 状态：DONE / CLOSED_BY_USER / CLOSED_BY_SYSTEM / PENDING / READ
     */
    private String status;

    /**
     * 操作时间：DONE 时为完成时间，关闭时为关闭时间，进行中为 null
     */
    private LocalDateTime actionTime;
}
