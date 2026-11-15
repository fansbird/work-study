package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 薪酬规则分页查询传递的数据模型
 */
@Data
public class SalaryRulePageQueryDTO implements Serializable {

    //规则名称
    private String name;

    //学期id
    private Long semesterId;

    //岗位类别
    private String category;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
