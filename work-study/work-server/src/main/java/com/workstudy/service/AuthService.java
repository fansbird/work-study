package com.workstudy.service;

import com.workstudy.dto.LoginDTO;
import com.workstudy.entity.SysUser;

/**
 * 统一登录鉴权服务
 */
public interface AuthService {

    /**
     * 账号密码登录校验
     *
     * @param loginDTO
     * @return 校验通过的账号
     */
    SysUser login(LoginDTO loginDTO);

}
