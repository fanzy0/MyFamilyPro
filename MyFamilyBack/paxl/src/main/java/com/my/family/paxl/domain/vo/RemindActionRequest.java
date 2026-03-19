package com.my.family.paxl.domain.vo;

import lombok.Data;

/**
 * 提醒操作请求体，done 和 close 接口共用
 *
 * @author ai
 * @date 2026/03/18
 */
@Data
public class RemindActionRequest {

    /**
     * 要操作的提醒记录ID，必填
     */
    private Long remindLogId;
}
