package com.workstudy.service;

import com.workstudy.dto.DeptDTO;
import com.workstudy.dto.DeptPageQueryDTO;
import com.workstudy.entity.Dept;
import com.workstudy.result.PageResult;

import java.util.List;

/**
 * 用工部门管理服务
 */
public interface DeptService {

    /**
     * 新增部门
     *
     * @param deptDTO
     */
    void save(DeptDTO deptDTO);

    /**
     * 更新部门
     *
     * @param deptDTO
     */
    void update(DeptDTO deptDTO);

    /**
     * 分页查询部门
     *
     * @param deptPageQueryDTO
     * @return
     */
    PageResult pageQuery(DeptPageQueryDTO deptPageQueryDTO);

    /**
     * 查询启用中的部门列表（下拉用）
     *
     * @return
     */
    List<Dept> list();

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    Dept getById(Long id);

    /**
     * 启用/禁用部门
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id删除部门
     *
     * @param id
     */
    void deleteById(Long id);

}
