package com.workstudy.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.constant.StatusConstant;
import com.workstudy.dto.SalaryRuleDTO;
import com.workstudy.dto.SalaryRulePageQueryDTO;
import com.workstudy.entity.SalaryRule;
import com.workstudy.exception.BaseException;
import com.workstudy.mapper.SalaryRuleMapper;
import com.workstudy.result.PageResult;
import com.workstudy.service.DataScopeService;
import com.workstudy.service.SalaryRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪酬规则管理服务实现（/salaryRule 完整 CRUD）
 */
@Service
@Slf4j
public class SalaryRuleServiceImpl implements SalaryRuleService {

    /**
     * 默认系数
     */
    private static final BigDecimal DEFAULT_COEFFICIENT = new BigDecimal("1.00");

    /**
     * 默认优先级
     */
    private static final int DEFAULT_PRIORITY = 100;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private SalaryRuleMapper salaryRuleMapper;

    @Autowired
    private DataScopeService dataScopeService;

    /**
     * 新增薪酬规则
     *
     * @param salaryRuleDTO
     */
    @Override
    @Transactional
    public void save(SalaryRuleDTO salaryRuleDTO) {
        checkManagePermission();
        SalaryRule salaryRule = new SalaryRule();
        BeanUtils.copyProperties(salaryRuleDTO, salaryRule);
        salaryRule.setId(null);
        fillDefaults(salaryRule);
        validate(salaryRule);
        salaryRuleMapper.insert(salaryRule);
        log.info("新增薪酬规则成功，id：{}，名称：{}", salaryRule.getId(), salaryRule.getName());
    }

    /**
     * 更新薪酬规则
     *
     * @param salaryRuleDTO
     */
    @Override
    @Transactional
    public void update(SalaryRuleDTO salaryRuleDTO) {
        checkManagePermission();
        if (salaryRuleDTO.getId() == null) {
            throw new BaseException("薪酬规则id不能为空");
        }
        if (salaryRuleMapper.getById(salaryRuleDTO.getId()) == null) {
            throw new BaseException("薪酬规则不存在");
        }
        SalaryRule salaryRule = new SalaryRule();
        BeanUtils.copyProperties(salaryRuleDTO, salaryRule);
        fillDefaults(salaryRule);
        validate(salaryRule);
        salaryRuleMapper.update(salaryRule);
        log.info("更新薪酬规则成功，id：{}", salaryRule.getId());
    }

    /**
     * 分页查询薪酬规则
     *
     * @param salaryRulePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SalaryRulePageQueryDTO salaryRulePageQueryDTO) {
        int page = salaryRulePageQueryDTO.getPage() == null ? DEFAULT_PAGE : salaryRulePageQueryDTO.getPage();
        int pageSize = salaryRulePageQueryDTO.getPageSize() == null ? DEFAULT_PAGE_SIZE
                : salaryRulePageQueryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);
        Page<SalaryRule> p = salaryRuleMapper.pageQuery(salaryRulePageQueryDTO);
        return new PageResult(p.getTotal(), p.getResult());
    }

    /**
     * 查询启用中的薪酬规则列表
     *
     * @param semesterId
     * @return
     */
    @Override
    public List<SalaryRule> list(Long semesterId) {
        return salaryRuleMapper.list(semesterId);
    }

    /**
     * 根据id删除薪酬规则
     *
     * @param id
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        checkManagePermission();
        if (salaryRuleMapper.getById(id) == null) {
            throw new BaseException("薪酬规则不存在");
        }
        salaryRuleMapper.deleteById(id);
        log.info("删除薪酬规则成功，id：{}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 权限：仅超级管理员或勤工助学中心管理员可管理薪酬规则
     */
    private void checkManagePermission() {
        if (!(dataScopeService.isSuperAdmin() || dataScopeService.isCenterAdmin())) {
            throw new BaseException("仅超级管理员或勤工助学中心管理员可管理薪酬规则");
        }
    }

    /**
     * 填充默认值：状态启用、系数 1.00、优先级 100
     */
    private void fillDefaults(SalaryRule salaryRule) {
        if (salaryRule.getStatus() == null) {
            salaryRule.setStatus(StatusConstant.ENABLE);
        }
        if (salaryRule.getCoefficient() == null) {
            salaryRule.setCoefficient(DEFAULT_COEFFICIENT);
        }
        if (salaryRule.getPriority() == null) {
            salaryRule.setPriority(DEFAULT_PRIORITY);
        }
    }

    /**
     * 规则合法性校验
     */
    private void validate(SalaryRule salaryRule) {
        if (salaryRule.getBaseWage() == null) {
            throw new BaseException("基础时薪不能为空");
        }
        if (salaryRule.getBaseWage().compareTo(BigDecimal.ZERO) < 0) {
            throw new BaseException("基础时薪不能为负数");
        }
        if (salaryRule.getCoefficient() != null && salaryRule.getCoefficient().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException("薪酬系数必须大于 0");
        }
        if (salaryRule.getMaxMonthly() != null && salaryRule.getMinMonthly() != null
                && salaryRule.getMaxMonthly().compareTo(salaryRule.getMinMonthly()) < 0) {
            throw new BaseException("月封顶金额不能小于月保底金额");
        }
    }

}
