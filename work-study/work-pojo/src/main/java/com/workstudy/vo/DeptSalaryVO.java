package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门薪酬统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptSalaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //部门名称
    private String deptName;

    //累计工时
    private BigDecimal hours;

    //累计金额
    private BigDecimal amount;

}
