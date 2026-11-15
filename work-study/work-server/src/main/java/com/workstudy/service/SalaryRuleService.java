package com.workstudy.service;

import com.workstudy.dto.SalaryRuleDTO;
import com.workstudy.dto.SalaryRulePageQueryDTO;
import com.workstudy.entity.SalaryRule;
import com.workstudy.result.PageResult;

import java.util.List;

/**
 * 薪酬规则管理服务
 */
public interface SalaryRuleService {

    /**
     * 新增薪酬规则
     *
     * @param salaryRuleDTO
     */
    void save(SalaryRuleDTO salaryRuleDTO);

    /**
     * 更新薪酬规则
     *
     * @param salaryRuleDTO
     */
    void update(SalaryRuleDTO salaryRuleDTO);

    /**
     * 分页查询薪酬规则
     *
     * @param salaryRulePageQueryDTO
     * @return
     */
    PageResult pageQuery(SalaryRulePageQueryDTO salaryRulePageQueryDTO);

    /**
     * 查询启用中的薪酬规则列表（按 priority 升序）
     *
     * @param semesterId 可为空
     * @return
     */
    List<SalaryRule> list(Long semesterId);

    /**
     * 根据id删除薪酬规则
     *
     * @param id
     */
    void deleteById(Long id);

}
