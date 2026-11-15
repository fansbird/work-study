package com.workstudy.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.constant.PasswordConstant;
import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.PasswordEditDTO;
import com.workstudy.dto.SysUserDTO;
import com.workstudy.dto.SysUserPageQueryDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.mapper.SysUserMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    /**
     * 密码脱敏占位符
     */
    private static final String PASSWORD_MASK = "***";

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 新增账号（初始密码 123456）
     *
     * @param sysUserDTO
     */
    @Override
    @Transactional
    public void save(SysUserDTO sysUserDTO) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(sysUserDTO, sysUser);
        //默认状态为启用
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(StatusConstant.ENABLE);
        }
        //默认密码为 123456
        sysUser.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8)));
        sysUserMapper.insert(sysUser);
    }

    /**
     * 更新账号
     *
     * @param sysUserDTO
     */
    @Override
    @Transactional
    public void update(SysUserDTO sysUserDTO) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(sysUserDTO, sysUser);
        sysUserMapper.update(sysUser);
    }

    /**
     * 分页查询账号
     *
     * @param sysUserPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SysUserPageQueryDTO sysUserPageQueryDTO) {
        PageHelper.startPage(sysUserPageQueryDTO.getPage(), sysUserPageQueryDTO.getPageSize());
        Page<SysUser> page = sysUserMapper.pageQuery(sysUserPageQueryDTO);
        List<SysUser> records = page.getResult();
        //密码脱敏
        for (SysUser sysUser : records) {
            sysUser.setPassword(PASSWORD_MASK);
        }
        return new PageResult(page.getTotal(), records);
    }

    /**
     * 根据id查询账号
     *
     * @param id
     * @return
     */
    @Override
    public SysUser getById(Long id) {
        SysUser sysUser = sysUserMapper.getById(id);
        if (sysUser != null) {
            sysUser.setPassword(PASSWORD_MASK);
        }
        return sysUser;
    }

    /**
     * 启用/禁用账号
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        SysUser sysUser = SysUser.builder()
                .id(id)
                .status(status)
                .build();
        sysUserMapper.update(sysUser);
    }

    /**
     * 重置密码
     *
     * @param passwordEditDTO
     */
    @Override
    @Transactional
    public void resetPassword(PasswordEditDTO passwordEditDTO) {
        SysUser sysUser = SysUser.builder()
                .id(passwordEditDTO.getId())
                .password(DigestUtils.md5DigestAsHex(passwordEditDTO.getNewPassword().getBytes(StandardCharsets.UTF_8)))
                .build();
        sysUserMapper.update(sysUser);
    }
}
