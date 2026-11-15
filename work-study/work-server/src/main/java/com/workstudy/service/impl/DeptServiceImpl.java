package com.workstudy.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.DeptDTO;
import com.workstudy.dto.DeptPageQueryDTO;
import com.workstudy.entity.Dept;
import com.workstudy.mapper.DeptMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DeptService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    /**
     * 新增部门
     *
     * @param deptDTO
     */
    @Override
    @Transactional
    public void save(DeptDTO deptDTO) {
        Dept dept = new Dept();
        BeanUtils.copyProperties(deptDTO, dept);
        //默认状态为启用
        if (dept.getStatus() == null) {
            dept.setStatus(StatusConstant.ENABLE);
        }
        deptMapper.insert(dept);
    }

    /**
     * 更新部门
     *
     * @param deptDTO
     */
    @Override
    @Transactional
    public void update(DeptDTO deptDTO) {
        Dept dept = new Dept();
        BeanUtils.copyProperties(deptDTO, dept);
        deptMapper.update(dept);
    }

    /**
     * 分页查询部门
     *
     * @param deptPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DeptPageQueryDTO deptPageQueryDTO) {
        PageHelper.startPage(deptPageQueryDTO.getPage(), deptPageQueryDTO.getPageSize());
        Page<Dept> page = deptMapper.pageQuery(deptPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 查询启用中的部门列表（下拉用）
     *
     * @return
     */
    @Override
    public List<Dept> list() {
        return deptMapper.list();
    }

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    @Override
    public Dept getById(Long id) {
        return deptMapper.getById(id);
    }

    /**
     * 启用/禁用部门
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dept dept = Dept.builder()
                .id(id)
                .status(status)
                .build();
        deptMapper.update(dept);
    }

    /**
     * 根据id删除部门
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        deptMapper.deleteById(id);
    }
}
