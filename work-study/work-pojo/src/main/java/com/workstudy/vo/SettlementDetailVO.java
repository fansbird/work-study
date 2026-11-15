package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算明细信息（含学生姓名/学号、岗位名称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //结算单id
    private Long settlementId;

    //工时id
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

    //学生姓名
    private String studentName;

    //学号
    private String studentNo;

    //岗位名称
    private String positionName;

}
