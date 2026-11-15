package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.SysDictDTO;
import com.workstudy.entity.SysDict;
import com.workstudy.result.Result;
import com.workstudy.service.DictService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典管理
 */
@RestController
@RequestMapping("/dict")
@Slf4j
@Tag(name = "字典管理接口")
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 查询字典项（dictCode 为空时查询全部）
     *
     * @param dictCode
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询字典列表")
    public Result<List<SysDict>> list(String dictCode) {
        log.info("查询字典列表：{}", dictCode);
        List<SysDict> list = dictService.list(dictCode);
        return Result.success(list);
    }

    /**
     * 新增字典项
     *
     * @param sysDictDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增字典项")
    @Log(title = "字典管理")
    public Result save(@RequestBody SysDictDTO sysDictDTO) {
        log.info("新增字典项：{}", sysDictDTO);
        dictService.save(sysDictDTO);
        return Result.success();
    }

    /**
     * 修改字典项
     *
     * @param sysDictDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改字典项")
    @Log(title = "字典管理")
    public Result update(@RequestBody SysDictDTO sysDictDTO) {
        log.info("修改字典项：{}", sysDictDTO);
        dictService.update(sysDictDTO);
        return Result.success();
    }

    /**
     * 删除字典项
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典项")
    @Log(title = "字典管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除字典项：{}", id);
        dictService.deleteById(id);
        return Result.success();
    }

}
