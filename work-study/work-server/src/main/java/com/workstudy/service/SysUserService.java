package com.workstudy.service;

import com.workstudy.dto.PasswordEditDTO;
import com.workstudy.dto.SysUserDTO;
import com.workstudy.dto.SysUserPageQueryDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.result.PageResult;

/**
 * 统一账号管理服务
 */
public interface SysUserService {

    /**
     * 新增账号（初始密码 123456）
     *
     * @param sysUserDTO
     */
    void save(SysUserDTO sysUserDTO);

    /**
     * 更新账号
     *
     * @param sysUserDTO
     */
    void update(SysUserDTO sysUserDTO);

    /**
     * 分页查询账号
     *
     * @param sysUserPageQueryDTO
     * @return
     */
    PageResult pageQuery(SysUserPageQueryDTO sysUserPageQueryDTO);

    /**
     * 根据id查询账号
     *
     * @param id
     * @return
     */
    SysUser getById(Long id);

    /**
     * 启用/禁用账号
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 重置密码
     *
     * @param passwordEditDTO
     */
    void resetPassword(PasswordEditDTO passwordEditDTO);

}
