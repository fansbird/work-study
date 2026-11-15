package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.SysSemesterDTO;
import com.workstudy.entity.SysSemester;
import com.workstudy.result.Result;
import com.workstudy.service.SemesterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学期管理
 */
@RestController
@RequestMapping("/semester")
@Slf4j
@Tag(name = "学期管理接口")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    /**
     * 查询学期列表
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询学期列表")
    public Result<List<SysSemester>> list() {
        log.info("查询学期列表");
        List<SysSemester> list = semesterService.list();
        return Result.success(list);
    }

    /**
     * 新增学期
     *
     * @param sysSemesterDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增学期")
    @Log(title = "学期管理")
    public Result save(@RequestBody SysSemesterDTO sysSemesterDTO) {
        log.info("新增学期：{}", sysSemesterDTO);
        semesterService.save(sysSemesterDTO);
        return Result.success();
    }

    /**
     * 修改学期
     *
     * @param sysSemesterDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改学期")
    @Log(title = "学期管理")
    public Result update(@RequestBody SysSemesterDTO sysSemesterDTO) {
        log.info("修改学期：{}", sysSemesterDTO);
        semesterService.update(sysSemesterDTO);
        return Result.success();
    }

    /**
     * 设置当前学期
     *
     * @param id
     * @return
     */
    @PostMapping("/setCurrent/{id}")
    @Operation(summary = "设置当前学期")
    @Log(title = "学期管理")
    public Result setCurrent(@PathVariable Long id) {
        log.info("设置当前学期：{}", id);
        semesterService.setCurrent(id);
        return Result.success();
    }

    /**
     * 删除学期
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学期")
    @Log(title = "学期管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除学期：{}", id);
        semesterService.deleteById(id);
        return Result.success();
    }

}
