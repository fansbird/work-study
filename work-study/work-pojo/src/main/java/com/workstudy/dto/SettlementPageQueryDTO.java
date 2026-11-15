package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 结算单分页查询传递的数据模型
 */
@Data
public class SettlementPageQueryDTO implements Serializable {

    //学期id
    private Long semesterId;

    //结算月份 如 2026-10
    private String month;

    //部门id
    private Long deptId;

    //状态
    private Integer status;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
