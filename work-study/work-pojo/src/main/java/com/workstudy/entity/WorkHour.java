package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工时
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkHour implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //岗位id
    private Long positionId;

    //学生id
    private Long studentId;

    //用工部门id
    private Long deptId;

    //学期id
    private Long semesterId;

    //工作日期
    private LocalDate workDate;

    //开始时间
    private LocalTime startTime;

    //结束时间
    private LocalTime endTime;

    //工时数
    private BigDecimal hours;

    //工作内容
    private String workContent;

    //状态 0待部门审核 1待中心复核 2通过 3驳回重填
    private Integer status;

    //驳回原因
    private String rejectReason;

    //部门审核人
    private Long deptAuditUser;

    //部门审核时间
    private LocalDateTime deptAuditTime;

    //中心审核人
    private Long centerAuditUser;

    //中心审核时间
    private LocalDateTime centerAuditTime;

    //结算单id（结算后回填并锁定）
    private Long settlementId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
