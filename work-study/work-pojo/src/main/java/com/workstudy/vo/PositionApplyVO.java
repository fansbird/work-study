package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名信息（含岗位名称、学生姓名/学号、部门id）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionApplyVO implements Serializable {

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

    //状态
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

    //岗位名称
    private String positionName;

    //学生姓名
    private String studentName;

    //学号
    private String studentNo;

    //用工部门id
    private Long deptId;

}
