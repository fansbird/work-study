package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.SysUserPageQueryDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查询账号
     *
     * @param username
     * @return
     */
    @Select("select * from sys_user where username = #{username}")
    SysUser getByUsername(String username);

    /**
     * 根据id查询账号
     *
     * @param id
     * @return
     */
    @Select("select * from sys_user where id = #{id}")
    SysUser getById(Long id);

    /**
     * 新增账号
     *
     * @param sysUser
     */
    @AutoFill(OperationType.INSERT)
    void insert(SysUser sysUser);

    /**
     * 更新账号（动态sql）
     *
     * @param sysUser
     */
    @AutoFill(OperationType.UPDATE)
    void update(SysUser sysUser);

    /**
     * 分页查询账号
     *
     * @param sysUserPageQueryDTO
     * @return
     */
    Page<SysUser> pageQuery(SysUserPageQueryDTO sysUserPageQueryDTO);

}
