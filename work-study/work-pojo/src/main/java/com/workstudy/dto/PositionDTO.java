package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 岗位新增/修改传递的数据模型
 */
@Data
public class PositionDTO implements Serializable {

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

    //状态
    private Integer status;

    //驳回原因
    private String rejectReason;

}
