package com.workstudy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一账号
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户名
    private String username;

    //密码（MD5）
    private String password;

    //姓名
    private String name;

    //角色 0超级管理员 1学生 2用工部门管理员 3勤工助学中心管理员 4财务
    private Integer role;

    //手机号
    private String phone;

    //状态 1启用 0禁用
    private Integer status;

    //部门管理员所属部门id
    private Long deptId;

    //学生关联的学生id
    private Long studentId;

    //头像
    private String avatar;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
