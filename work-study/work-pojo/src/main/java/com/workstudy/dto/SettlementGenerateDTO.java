package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 结算生成传递的数据模型
 */
@Data
public class SettlementGenerateDTO implements Serializable {

    //学期id
    private Long semesterId;

    //结算月份 如 2026-10
    private String month;

    //部门id（空=全校）
    private Long deptId;

}
