package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 账号新增/修改传递的数据模型
 */
@Data
public class SysUserDTO implements Serializable {

    private Long id;

    //用户名
    private String username;

    //姓名
    private String name;

    //手机号
    private String phone;

    //角色
    private Integer role;

    //部门管理员所属部门id
    private Long deptId;

    //学生关联的学生id
    private Long studentId;

    //头像
    private String avatar;

    //状态
    private Integer status;

}
