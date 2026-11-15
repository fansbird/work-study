package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工作台数据概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //岗位总数
    private Long positionCount;

    //已发布岗位数
    private Long publishedCount;

    //报名总数
    private Long applyCount;

    //已录用人数
    private Long hiredCount;

    //学生总数
    private Long studentCount;

    //部门总数
    private Long deptCount;

    //待审核工时数
    private Long pendingWorkHourCount;

    //本月总工时
    private BigDecimal totalHoursMonth;

    //本月总金额
    private BigDecimal totalAmountMonth;

}
