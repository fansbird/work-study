package com.workstudy.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 工时分页查询传递的数据模型
 */
@Data
public class WorkHourPageQueryDTO implements Serializable {

    //岗位id
    private Long positionId;

    //用工部门id
    private Long deptId;

    //学期id
    private Long semesterId;

    //状态
    private Integer status;

    //学生id
    private Long studentId;

    //开始日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    //结束日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
