package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪酬规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRule implements Serializable {

    private static final long serialVersionUID = 1L;

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

    //优先级（数字小优先）
    private Integer priority;

    //状态 1启用 0禁用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
