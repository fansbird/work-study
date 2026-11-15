package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysSemester implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //学期名称
    private String name;

    //开始日期
    private LocalDate startDate;

    //结束日期
    private LocalDate endDate;

    //状态 1当前学期 0非当前学期
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
