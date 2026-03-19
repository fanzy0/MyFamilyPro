package com.my.family.paxl.util;

import cn.hutool.core.date.ChineseDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 农历转阳历工具类，封装 hutool ChineseDate，屏蔽库 API 细节
 * 对外提供统一的农历→阳历转换接口，供定时扫描任务使用
 *
 * @author ai
 * @date 2026/03/18
 */
@Component
@Slf4j
public class LunarCalendarUtil {

    /**
     * 将指定阳历年份对应的农历月/日转换为阳历 LocalDate
     *
     * <p>
     * 由于农历年与阳历年不完全对齐（春节前后存在跨年），
     * 本方法会先尝试以 targetYear 作为农历年转换，
     * 若结果不落在 targetYear 的阳历范围内，再尝试 targetYear-1 作为农历年，
     * 最大程度保证转换结果落在目标阳历年内。
     * </p>
     *
     * @param targetYear  目标阳历年份（如 2026）
     * @param lunarMonth  农历月（1-12，不含闰月）
     * @param lunarDay    农历日（1-30）
     * @return 对应的阳历日期；转换失败时返回 targetYear 年的 1 月 1 日作为兜底
     */
    public LocalDate lunarToSolar(int targetYear, int lunarMonth, int lunarDay) {
        LocalDate result = tryConvert(targetYear, lunarMonth, lunarDay);
        if (result != null && result.getYear() == targetYear) {
            return result;
        }

        // 农历年份可能需要用前一年（处理春节前后的月份）
        result = tryConvert(targetYear - 1, lunarMonth, lunarDay);
        if (result != null && result.getYear() == targetYear) {
            return result;
        }

        // 兜底：返回当年 1 月 1 日，保证程序不中断
        log.warn("[LunarToSolar] 转换结果未落在目标年，使用兜底值. targetYear={}, lunarMonth={}, lunarDay={}",
                targetYear, lunarMonth, lunarDay);
        return LocalDate.of(targetYear, 1, 1);
    }

    /**
     * 尝试以指定农历年转换，返回阳历日期；失败时返回 null
     */
    private LocalDate tryConvert(int lunarYear, int lunarMonth, int lunarDay) {
        try {
            ChineseDate chineseDate = new ChineseDate(lunarYear, lunarMonth, lunarDay);
            Calendar cal = chineseDate.getGregorianCalendar();
            Date solarDate = cal.getTime();
            return solarDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception e) {
            log.debug("[LunarToSolar] 转换失败, lunarYear={}, lunarMonth={}, lunarDay={}, msg={}",
                    lunarYear, lunarMonth, lunarDay, e.getMessage());
            return null;
        }
    }
}
