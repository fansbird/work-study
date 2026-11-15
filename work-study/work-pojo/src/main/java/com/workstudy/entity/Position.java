package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //岗位名称
    private String name;

    //用工部门id
    private Long deptId;

    //岗位类别
    private String category;

    //学期id
    private Long semesterId;

    //招聘名额
    private Integer quota;

    //已录用人数
    private Integer hired;

    //时薪
    private BigDecimal hourlyWage;

    //工作内容
    private String workContent;

    //工作地点
    private String workLocation;

    //联系人
    private String contactPerson;

    //联系电话
    private String contactPhone;

    //状态 0草稿 1待审批 2已发布 3已驳回 4已下架
    private Integer status;

    //驳回原因
    private String rejectReason;

    //发布时间
    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
