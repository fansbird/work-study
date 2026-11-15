package com.workstudy.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录时传递的数据模型
 */
@Data
public class LoginDTO implements Serializable {

    private String username;

    private String password;

}
