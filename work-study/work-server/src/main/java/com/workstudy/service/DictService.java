package com.workstudy.service;

import com.workstudy.dto.SysDictDTO;
import com.workstudy.entity.SysDict;

import java.util.List;

/**
 * 数据字典管理服务
 */
public interface DictService {

    /**
     * 新增字典项
     *
     * @param sysDictDTO
     */
    void save(SysDictDTO sysDictDTO);

    /**
     * 更新字典项
     *
     * @param sysDictDTO
     */
    void update(SysDictDTO sysDictDTO);

    /**
     * 查询字典项（dictCode 为空时查询全部）
     *
     * @param dictCode
     * @return
     */
    List<SysDict> list(String dictCode);

    /**
     * 根据id删除字典项
     *
     * @param id
     */
    void deleteById(Long id);

}
