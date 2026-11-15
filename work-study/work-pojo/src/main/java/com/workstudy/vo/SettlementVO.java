package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单信息（含部门名称、学期名称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //批次号
    private String batchNo;

    //学期id
    private Long semesterId;

    //结算月份
    private String month;

    //部门id
    private Long deptId;

    //总工时
    private BigDecimal totalHours;

    //总金额
    private BigDecimal totalAmount;

    //明细条数
    private Integer detailCount;

    //状态
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

    //部门名称
    private String deptName;

    //学期名称
    private String semesterName;

}
