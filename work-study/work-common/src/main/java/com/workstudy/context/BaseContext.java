package com.workstudy.context;

/**
 * 基于 ThreadLocal 的上下文，保存当前登录用户信息
 */
public class BaseContext {

    /**
     * 当前登录用户 id（sys_user.id）
     */
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 当前登录用户角色（sys_user.role）
     */
    public static ThreadLocal<Integer> roleThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

    public static void setCurrentRole(Integer role) {
        roleThreadLocal.set(role);
    }

    public static Integer getCurrentRole() {
        return roleThreadLocal.get();
    }

    public static void removeCurrentRole() {
        roleThreadLocal.remove();
    }

    /**
     * 清理线程变量，拦截器 afterCompletion 中必须调用
     */
    public static void remove() {
        threadLocal.remove();
        roleThreadLocal.remove();
    }

}
