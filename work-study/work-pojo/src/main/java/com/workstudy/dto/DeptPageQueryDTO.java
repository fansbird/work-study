package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门分页查询传递的数据模型
 */
@Data
public class DeptPageQueryDTO implements Serializable {

    //部门名称
    private String name;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
