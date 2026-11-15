package com.workstudy.service;

import com.workstudy.dto.SysSemesterDTO;
import com.workstudy.entity.SysSemester;

import java.util.List;

/**
 * 学期管理服务
 */
public interface SemesterService {

    /**
     * 新增学期
     *
     * @param sysSemesterDTO
     */
    void save(SysSemesterDTO sysSemesterDTO);

    /**
     * 更新学期
     *
     * @param sysSemesterDTO
     */
    void update(SysSemesterDTO sysSemesterDTO);

    /**
     * 查询学期列表
     *
     * @return
     */
    List<SysSemester> list();

    /**
     * 设置当前学期
     *
     * @param id
     */
    void setCurrent(Long id);

    /**
     * 获取当前学期（status=1）
     *
     * @return
     */
    SysSemester getCurrent();

    /**
     * 根据id删除学期
     *
     * @param id
     */
    void deleteById(Long id);

}
