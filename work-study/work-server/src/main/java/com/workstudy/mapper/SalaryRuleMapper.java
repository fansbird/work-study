package com.workstudy.mapper;

import com.github.pagehelper.Page;
import com.workstudy.annotation.AutoFill;
import com.workstudy.dto.SalaryRulePageQueryDTO;
import com.workstudy.entity.SalaryRule;
import com.workstudy.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 薪酬规则数据访问层
 */
@Mapper
public interface SalaryRuleMapper {

    /**
     * 根据id查询薪酬规则
     *
     * @param id
     * @return
     */
    @Select("select * from salary_rule where id = #{id}")
    SalaryRule getById(Long id);

    /**
     * 分页查询薪酬规则
     *
     * @param salaryRulePageQueryDTO
     * @return
     */
    Page<SalaryRule> pageQuery(SalaryRulePageQueryDTO salaryRulePageQueryDTO);

    /**
     * 查询启用中的薪酬规则（按 priority 升序）
     * <p>semesterId 非空时返回「该学期命中 + 通用（semester_id 为空）」的规则，供结算匹配使用。</p>
     *
     * @param semesterId 可为空
     * @return
     */
    List<SalaryRule> list(@Param("semesterId") Long semesterId);

    /**
     * 新增薪酬规则
     *
     * @param salaryRule
     */
    @AutoFill(OperationType.INSERT)
    void insert(SalaryRule salaryRule);

    /**
     * 更新薪酬规则（全字段更新，允许清空学期/类别/封顶/保底等可空字段）
     *
     * @param salaryRule
     */
    @AutoFill(OperationType.UPDATE)
    void update(SalaryRule salaryRule);

    /**
     * 根据id删除薪酬规则
     *
     * @param id
     */
    @Delete("delete from salary_rule where id = #{id}")
    void deleteById(Long id);

}
