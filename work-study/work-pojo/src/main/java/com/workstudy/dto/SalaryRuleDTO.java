package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 薪酬规则新增/修改传递的数据模型
 */
@Data
public class SalaryRuleDTO implements Serializable {

    private Long id;

    //规则名称
    private String name;

    //学期id（空=通用）
    private Long semesterId;

    //岗位类别（空=全部类别）
    private String category;

    //困难等级（空=全部等级）
    private Integer difficultyLevel;

    //基础时薪
    private BigDecimal baseWage;

    //系数
    private BigDecimal coefficient;

    //月封顶金额
    private BigDecimal maxMonthly;

    //月保底金额
    private BigDecimal minMonthly;

    //优先级
    private Integer priority;

    //状态
    private Integer status;

}
