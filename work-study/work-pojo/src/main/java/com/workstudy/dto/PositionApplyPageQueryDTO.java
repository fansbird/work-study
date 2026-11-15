package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 报名分页查询传递的数据模型
 */
@Data
public class PositionApplyPageQueryDTO implements Serializable {

    //岗位id
    private Long positionId;

    //学期id
    private Long semesterId;

    //状态
    private Integer status;

    //学生id
    private Long studentId;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
