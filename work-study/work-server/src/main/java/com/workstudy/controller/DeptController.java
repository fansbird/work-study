package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.DeptDTO;
import com.workstudy.dto.DeptPageQueryDTO;
import com.workstudy.entity.Dept;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.DeptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用工部门管理
 */
@RestController
@RequestMapping("/dept")
@Slf4j
@Tag(name = "部门管理接口")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 查询启用中的部门列表
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询部门列表")
    public Result<List<Dept>> list() {
        log.info("查询部门列表");
        List<Dept> list = deptService.list();
        return Result.success(list);
    }

    /**
     * 分页查询部门
     *
     * @param deptPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询部门")
    public Result<PageResult> page(DeptPageQueryDTO deptPageQueryDTO) {
        log.info("分页查询部门：{}", deptPageQueryDTO);
        PageResult pageResult = deptService.pageQuery(deptPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增部门
     *
     * @param deptDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增部门")
    @Log(title = "部门管理")
    public Result save(@RequestBody DeptDTO deptDTO) {
        log.info("新增部门：{}", deptDTO);
        deptService.save(deptDTO);
        return Result.success();
    }

    /**
     * 修改部门
     *
     * @param deptDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改部门")
    @Log(title = "部门管理")
    public Result update(@RequestBody DeptDTO deptDTO) {
        log.info("修改部门：{}", deptDTO);
        deptService.update(deptDTO);
        return Result.success();
    }

    /**
     * 根据id查询部门
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询部门")
    public Result<Dept> getById(@PathVariable Long id) {
        log.info("根据id查询部门：{}", id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 启用/禁用部门
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用/禁用部门")
    @Log(title = "部门管理")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用/禁用部门：status={}, id={}", status, id);
        deptService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 删除部门
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")
    @Log(title = "部门管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除部门：{}", id);
        deptService.deleteById(id);
        return Result.success();
    }

}
