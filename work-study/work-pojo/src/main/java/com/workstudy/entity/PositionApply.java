package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位报名
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionApply implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //岗位id
    private Long positionId;

    //学生id
    private Long studentId;

    //学期id
    private Long semesterId;

    //报名备注
    private String applyRemark;

    //状态 0待审核 1已录用 2已驳回 3已撤销 4已放弃
    private Integer status;

    //驳回原因
    private String rejectReason;

    //审核时间
    private LocalDateTime auditTime;

    //审核人
    private Long auditUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
