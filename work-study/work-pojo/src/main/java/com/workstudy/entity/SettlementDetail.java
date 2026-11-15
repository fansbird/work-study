package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //结算单id
    private Long settlementId;

    //工时id（唯一，保证幂等）
    private Long workHourId;

    //学生id
    private Long studentId;

    //岗位id
    private Long positionId;

    //工作日期
    private LocalDate workDate;

    //工时数
    private BigDecimal hours;

    //时薪
    private BigDecimal hourlyWage;

    //系数
    private BigDecimal coefficient;

    //金额
    private BigDecimal amount;

    private LocalDateTime createTime;

}
