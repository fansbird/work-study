package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用工部门
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //部门名称
    private String name;

    //联系人
    private String contactPerson;

    //联系电话
    private String contactPhone;

    //状态 1启用 0禁用
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
