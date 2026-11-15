package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //批次号
    private String batchNo;

    //学期id
    private Long semesterId;

    //结算月份 如 2026-10
    private String month;

    //部门id（空=全校）
    private Long deptId;

    //总工时
    private BigDecimal totalHours;

    //总金额
    private BigDecimal totalAmount;

    //明细条数
    private Integer detailCount;

    //状态 0待确认 1已确认 2已发放 3已作废
    private Integer status;

    //确认人
    private Long confirmUser;

    //确认时间
    private LocalDateTime confirmTime;

    //发放人
    private Long payUser;

    //发放时间
    private LocalDateTime payTime;

    //备注
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
