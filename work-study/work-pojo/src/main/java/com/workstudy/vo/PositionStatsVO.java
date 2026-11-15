package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 岗位统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //岗位名称
    private String positionName;

    //部门名称
    private String deptName;

    //招聘名额
    private Integer quota;

    //已录用人数
    private Integer hired;

    //累计工时
    private BigDecimal hours;

    //累计金额
    private BigDecimal amount;

}
