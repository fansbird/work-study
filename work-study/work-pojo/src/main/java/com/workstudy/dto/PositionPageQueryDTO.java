package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 岗位分页查询传递的数据模型
 */
@Data
public class PositionPageQueryDTO implements Serializable {

    //岗位名称
    private String name;

    //用工部门id
    private Long deptId;

    //岗位类别
    private String category;

    //学期id
    private Long semesterId;

    //状态
    private Integer status;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
