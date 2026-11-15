package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 工时填报/修改传递的数据模型
 */
@Data
public class WorkHourDTO implements Serializable {

    private Long id;

    //岗位id
    private Long positionId;

    //工作日期
    private LocalDate workDate;

    //开始时间
    private LocalTime startTime;

    //结束时间
    private LocalTime endTime;

    //工时数
    private BigDecimal hours;

    //工作内容
    private String workContent;

    //状态
    private Integer status;

    //驳回原因
    private String rejectReason;

}
