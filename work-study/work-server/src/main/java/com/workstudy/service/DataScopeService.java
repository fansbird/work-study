package com.workstudy.service;

/**
 * 数据归属（越权）校验工具：部门管理员只能操作本部门数据，学生只能操作自己的数据。
 * 角色码与契约第 2 节一致，后续岗位、报名、工时、结算等模块复用本工具。
 */
public interface DataScopeService {

    /**
     * 超级管理员
     */
    int ROLE_SUPER_ADMIN = 0;

    /**
     * 学生
     */
    int ROLE_STUDENT = 1;

    /**
     * 用工部门管理员（数据归属 dept_id）
     */
    int ROLE_DEPT_ADMIN = 2;

    /**
     * 勤工助学中心管理员
     */
    int ROLE_CENTER_ADMIN = 3;

    /**
     * 财务
     */
    int ROLE_FINANCE = 4;

    /**
     * 当前登录角色（未登录返回 null）
     */
    Integer currentRole();

    /**
     * 是否超级管理员
     */
    boolean isSuperAdmin();

    /**
     * 是否用工部门管理员
     */
    boolean isDeptAdmin();

    /**
     * 是否勤工助学中心管理员
     */
    boolean isCenterAdmin();

    /**
     * 是否财务
     */
    boolean isFinance();

    /**
     * 是否学生
     */
    boolean isStudent();

    /**
     * 当前部门管理员的归属部门id（非部门管理员返回 null）
     */
    Long getCurrentDeptId();

    /**
     * 当前学生账号关联的学生id（非学生返回 null）
     */
    Long getCurrentStudentId();

    /**
     * 校验当前用户是否有权访问指定部门数据，无权时抛业务异常
     *
     * @param deptId
     */
    void checkDeptScope(Long deptId);

}
