package com.workstudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录返回的数据格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //主键值
    private Long id;

    //用户名
    private String username;

    //姓名
    private String name;

    //角色 0超级管理员 1学生 2用工部门管理员 3勤工助学中心管理员 4财务
    private Integer role;

    //jwt令牌
    private String token;

    //部门管理员所属部门id
    private Long deptId;

    //学生关联的学生id
    private Long studentId;

    //头像
    private String avatar;

}
