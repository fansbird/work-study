package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 学期新增/修改传递的数据模型
 */
@Data
public class SysSemesterDTO implements Serializable {

    private Long id;

    //学期名称
    private String name;

    //开始日期
    private LocalDate startDate;

    //结束日期
    private LocalDate endDate;

    //状态 1当前学期 0非当前学期
    private Integer status;

}
