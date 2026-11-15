package com.workstudy.service.impl;

import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.SysDictDTO;
import com.workstudy.entity.SysDict;
import com.workstudy.mapper.SysDictMapper;
import com.workstudy.service.DictService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private SysDictMapper sysDictMapper;

    /**
     * 新增字典项
     *
     * @param sysDictDTO
     */
    @Override
    @Transactional
    public void save(SysDictDTO sysDictDTO) {
        SysDict sysDict = new SysDict();
        BeanUtils.copyProperties(sysDictDTO, sysDict);
        //默认状态为启用
        if (sysDict.getStatus() == null) {
            sysDict.setStatus(StatusConstant.ENABLE);
        }
        //默认排序号为0
        if (sysDict.getSort() == null) {
            sysDict.setSort(0);
        }
        sysDictMapper.insert(sysDict);
    }

    /**
     * 更新字典项
     *
     * @param sysDictDTO
     */
    @Override
    @Transactional
    public void update(SysDictDTO sysDictDTO) {
        SysDict sysDict = new SysDict();
        BeanUtils.copyProperties(sysDictDTO, sysDict);
        sysDictMapper.update(sysDict);
    }

    /**
     * 查询字典项（dictCode 为空时查询全部）
     *
     * @param dictCode
     * @return
     */
    @Override
    public List<SysDict> list(String dictCode) {
        return sysDictMapper.list(dictCode);
    }

    /**
     * 根据id删除字典项
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        sysDictMapper.deleteById(id);
    }
}
