package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.SalaryRuleDTO;
import com.workstudy.dto.SalaryRulePageQueryDTO;
import com.workstudy.entity.SalaryRule;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.SalaryRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 薪酬规则管理
 */
@RestController
@RequestMapping("/salaryRule")
@Slf4j
@Tag(name = "薪酬规则管理接口")
public class SalaryRuleController {

    @Autowired
    private SalaryRuleService salaryRuleService;

    /**
     * 分页查询薪酬规则
     *
     * @param salaryRulePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询薪酬规则")
    public Result<PageResult> page(SalaryRulePageQueryDTO salaryRulePageQueryDTO) {
        log.info("分页查询薪酬规则：{}", salaryRulePageQueryDTO);
        PageResult pageResult = salaryRuleService.pageQuery(salaryRulePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询启用中的薪酬规则列表（按 priority 升序）
     *
     * @param semesterId 可为空
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询薪酬规则列表")
    public Result<List<SalaryRule>> list(@RequestParam(required = false) Long semesterId) {
        log.info("查询薪酬规则列表，semesterId：{}", semesterId);
        List<SalaryRule> list = salaryRuleService.list(semesterId);
        return Result.success(list);
    }

    /**
     * 新增薪酬规则
     *
     * @param salaryRuleDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增薪酬规则")
    @Log(title = "薪酬规则管理")
    public Result save(@RequestBody SalaryRuleDTO salaryRuleDTO) {
        log.info("新增薪酬规则：{}", salaryRuleDTO);
        salaryRuleService.save(salaryRuleDTO);
        return Result.success();
    }

    /**
     * 修改薪酬规则
     *
     * @param salaryRuleDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改薪酬规则")
    @Log(title = "薪酬规则管理")
    public Result update(@RequestBody SalaryRuleDTO salaryRuleDTO) {
        log.info("修改薪酬规则：{}", salaryRuleDTO);
        salaryRuleService.update(salaryRuleDTO);
        return Result.success();
    }

    /**
     * 删除薪酬规则
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除薪酬规则")
    @Log(title = "薪酬规则管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除薪酬规则：{}", id);
        salaryRuleService.deleteById(id);
        return Result.success();
    }

}
