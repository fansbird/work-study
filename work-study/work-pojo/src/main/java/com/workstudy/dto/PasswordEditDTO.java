package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码/重置密码传递的数据模型
 */
@Data
public class PasswordEditDTO implements Serializable {

    //用户id
    private Long id;

    //新密码
    private String newPassword;

}
