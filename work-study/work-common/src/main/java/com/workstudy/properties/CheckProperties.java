package com.workstudy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 工时规则校验阈值配置，对应 application-dev.yml 中：
 * <pre>
 * work:
 *   check:
 *     daily-max-hours: 8
 *     weekly-max-hours: 40
 *     monthly-max-hours: 160
 * </pre>
 * 单位均为小时，用于工时填报/修改/提交时的日、周、月累计上限校验。
 */
@Component
@ConfigurationProperties(prefix = "work.check")
@Data
public class CheckProperties {

    //单日工时上限（小时），默认 8
    private BigDecimal dailyMaxHours = new BigDecimal("8");

    //单周工时上限（小时），默认 40
    private BigDecimal weeklyMaxHours = new BigDecimal("40");

    //单月工时上限（小时），默认 160
    private BigDecimal monthlyMaxHours = new BigDecimal("160");

}
