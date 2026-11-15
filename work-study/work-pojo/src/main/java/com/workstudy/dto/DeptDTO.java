package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增/修改传递的数据模型
 */
@Data
public class DeptDTO implements Serializable {

    private Long id;

    //部门名称
    private String name;

    //联系人
    private String contactPerson;

    //联系电话
    private String contactPhone;

    //状态
    private Integer status;

}
