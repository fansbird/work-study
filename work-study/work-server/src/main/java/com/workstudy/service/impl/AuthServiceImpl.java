package com.workstudy.service.impl;

import com.workstudy.constant.MessageConstant;
import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.LoginDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.exception.AccountLockedException;
import com.workstudy.exception.AccountNotFoundException;
import com.workstudy.exception.PasswordErrorException;
import com.workstudy.mapper.SysUserMapper;
import com.workstudy.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 账号密码登录校验
     *
     * @param loginDTO
     * @return
     */
    @Override
    public SysUser login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        SysUser sysUser = sysUserMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被禁用）
        if (sysUser == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对（数据库存储的为MD5密文）
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!md5Password.equals(sysUser.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (!StatusConstant.ENABLE.equals(sysUser.getStatus())) {
            //账号被禁用
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return sysUser;
    }
}
