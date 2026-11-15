package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 账号分页查询传递的数据模型
 */
@Data
public class SysUserPageQueryDTO implements Serializable {

    //姓名
    private String name;

    //角色
    private Integer role;

    //页码
    private Integer page;

    //每页记录数
    private Integer pageSize;

}
