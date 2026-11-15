package com.workstudy.service.impl;

import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.SysSemesterDTO;
import com.workstudy.entity.SysSemester;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.SysSemesterMapper;
import com.workstudy.service.SemesterService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SemesterServiceImpl implements SemesterService {

    /**
     * 当前学期标记
     */
    private static final Integer CURRENT = 1;

    @Autowired
    private SysSemesterMapper sysSemesterMapper;

    /**
     * 新增学期（若标记为当前学期，先将其它学期置为非当前，保证全库至多一个）
     *
     * @param sysSemesterDTO
     */
    @Override
    @Transactional
    public void save(SysSemesterDTO sysSemesterDTO) {
        SysSemester sysSemester = new SysSemester();
        BeanUtils.copyProperties(sysSemesterDTO, sysSemester);
        if (sysSemester.getStatus() == null) {
            sysSemester.setStatus(StatusConstant.DISABLE);
        }
        if (CURRENT.equals(sysSemester.getStatus())) {
            sysSemesterMapper.clearCurrent();
        }
        sysSemesterMapper.insert(sysSemester);
    }

    /**
     * 更新学期（若标记为当前学期，先将其它学期置为非当前）
     *
     * @param sysSemesterDTO
     */
    @Override
    @Transactional
    public void update(SysSemesterDTO sysSemesterDTO) {
        SysSemester sysSemester = new SysSemester();
        BeanUtils.copyProperties(sysSemesterDTO, sysSemester);
        if (CURRENT.equals(sysSemester.getStatus())) {
            sysSemesterMapper.clearCurrent();
        }
        sysSemesterMapper.update(sysSemester);
    }

    /**
     * 查询学期列表
     *
     * @return
     */
    @Override
    public List<SysSemester> list() {
        return sysSemesterMapper.list();
    }

    /**
     * 设置当前学期
     *
     * @param id
     */
    @Override
    @Transactional
    public void setCurrent(Long id) {
        if (sysSemesterMapper.getById(id) == null) {
            throw new BaseException("学期不存在");
        }
        //先清空，再置为一
        sysSemesterMapper.clearCurrent();
        SysSemester sysSemester = new SysSemester();
        sysSemester.setId(id);
        sysSemester.setStatus(CURRENT);
        sysSemesterMapper.update(sysSemester);
    }

    /**
     * 获取当前学期（status=1）
     *
     * @return
     */
    @Override
    public SysSemester getCurrent() {
        return sysSemesterMapper.getCurrent();
    }

    /**
     * 根据id删除学期
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        sysSemesterMapper.deleteById(id);
    }
}
