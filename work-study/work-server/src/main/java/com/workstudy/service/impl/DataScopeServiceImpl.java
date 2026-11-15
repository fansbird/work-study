package com.workstudy.service.impl;

import com.workstudy.context.BaseContext;
import com.workstudy.entity.Student;
import com.workstudy.entity.SysUser;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.StudentMapper;
import com.workstudy.mapper.SysUserMapper;
import com.workstudy.service.DataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据归属（越权）校验工具实现
 */
@Service
public class DataScopeServiceImpl implements DataScopeService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public Integer currentRole() {
        return BaseContext.getCurrentRole();
    }

    @Override
    public boolean isSuperAdmin() {
        return Integer.valueOf(ROLE_SUPER_ADMIN).equals(BaseContext.getCurrentRole());
    }

    @Override
    public boolean isDeptAdmin() {
        return Integer.valueOf(ROLE_DEPT_ADMIN).equals(BaseContext.getCurrentRole());
    }

    @Override
    public boolean isCenterAdmin() {
        return Integer.valueOf(ROLE_CENTER_ADMIN).equals(BaseContext.getCurrentRole());
    }

    @Override
    public boolean isFinance() {
        return Integer.valueOf(ROLE_FINANCE).equals(BaseContext.getCurrentRole());
    }

    @Override
    public boolean isStudent() {
        return Integer.valueOf(ROLE_STUDENT).equals(BaseContext.getCurrentRole());
    }

    /**
     * 当前部门管理员的归属部门id（取自 sys_user.dept_id），非部门管理员返回 null
     */
    @Override
    public Long getCurrentDeptId() {
        if (!isDeptAdmin()) {
            return null;
        }
        SysUser sysUser = currentUser();
        return sysUser == null ? null : sysUser.getDeptId();
    }

    /**
     * 当前学生账号关联的学生id：优先取 sys_user.student_id；
     * 若账号侧未回填（为空），则按 user_id = 当前登录账号id 反查 student 档案兜底；
     * 仍查不到返回 null。非学生角色返回 null。
     */
    @Override
    public Long getCurrentStudentId() {
        if (!isStudent()) {
            return null;
        }
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return null;
        }
        //1、优先取账号侧关联（sys_user.student_id）
        SysUser sysUser = sysUserMapper.getById(userId);
        if (sysUser != null && sysUser.getStudentId() != null) {
            return sysUser.getStudentId();
        }
        //2、保险：账号侧未回填时，按 user_id 反查学生档案（真实关联保存在 student.user_id）
        Student student = studentMapper.getByUserId(userId);
        return student == null ? null : student.getId();
    }

    /**
     * 校验当前用户是否有权访问指定部门数据
     */
    @Override
    public void checkDeptScope(Long deptId) {
        if (deptId == null) {
            return;
        }
        //超级管理员/中心管理员/财务可跨部门
        if (isSuperAdmin() || isCenterAdmin() || isFinance()) {
            return;
        }
        if (isDeptAdmin()) {
            if (!deptId.equals(getCurrentDeptId())) {
                throw new BaseException("无权访问该部门数据");
            }
            return;
        }
        throw new BaseException("无权访问该部门数据");
    }

    /**
     * 查询当前登录用户账号信息
     */
    private SysUser currentUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return null;
        }
        return sysUserMapper.getById(userId);
    }
}
