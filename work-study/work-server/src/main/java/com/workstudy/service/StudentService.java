package com.workstudy.service;

import com.workstudy.dto.StudentDTO;
import com.workstudy.dto.StudentPageQueryDTO;
import com.workstudy.entity.Student;
import com.workstudy.result.PageResult;

import java.util.List;

/**
 * 学生档案管理服务
 */
public interface StudentService {

    /**
     * 新增学生（同时联动创建学生登录账号）
     *
     * @param studentDTO
     */
    void save(StudentDTO studentDTO);

    /**
     * 更新学生档案
     *
     * @param studentDTO
     */
    void update(StudentDTO studentDTO);

    /**
     * 分页查询学生（含登录用户名）
     *
     * @param studentPageQueryDTO
     * @return
     */
    PageResult pageQuery(StudentPageQueryDTO studentPageQueryDTO);

    /**
     * 查询启用中的学生列表
     *
     * @return
     */
    List<Student> list();

    /**
     * 根据id查询学生
     *
     * @param id
     * @return
     */
    Student getById(Long id);

    /**
     * 根据关联账号id查询学生
     *
     * @param userId
     * @return
     */
    Student getByUserId(Long userId);

    /**
     * 根据id删除学生
     *
     * @param id
     */
    void deleteById(Long id);

}
