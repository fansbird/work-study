package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生档案分页查询传递的数据模型
 */
@Data
public class StudentPageQueryDTO implements Serializable {

    //姓名
    private String name;

    //学号
    private String studentNo;

    //学院
    private String college;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
