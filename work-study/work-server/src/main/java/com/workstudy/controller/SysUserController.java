package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.PasswordEditDTO;
import com.workstudy.dto.SysUserDTO;
import com.workstudy.dto.SysUserPageQueryDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.SysUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 统一账号管理
 */
@RestController
@RequestMapping("/sysUser")
@Slf4j
@Tag(name = "账号管理接口")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 分页查询账号
     *
     * @param sysUserPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询账号")
    public Result<PageResult> page(SysUserPageQueryDTO sysUserPageQueryDTO) {
        log.info("分页查询账号：{}", sysUserPageQueryDTO);
        PageResult pageResult = sysUserService.pageQuery(sysUserPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增账号
     *
     * @param sysUserDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增账号")
    @Log(title = "账号管理")
    public Result save(@RequestBody SysUserDTO sysUserDTO) {
        log.info("新增账号：{}", sysUserDTO);
        sysUserService.save(sysUserDTO);
        return Result.success();
    }

    /**
     * 修改账号
     *
     * @param sysUserDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改账号")
    @Log(title = "账号管理")
    public Result update(@RequestBody SysUserDTO sysUserDTO) {
        log.info("修改账号：{}", sysUserDTO);
        sysUserService.update(sysUserDTO);
        return Result.success();
    }

    /**
     * 根据id查询账号
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询账号")
    public Result<SysUser> getById(@PathVariable Long id) {
        log.info("根据id查询账号：{}", id);
        SysUser sysUser = sysUserService.getById(id);
        return Result.success(sysUser);
    }

    /**
     * 启用/禁用账号
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用/禁用账号")
    @Log(title = "账号管理")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用/禁用账号：status={}, id={}", status, id);
        sysUserService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 重置密码
     *
     * @param passwordEditDTO
     * @return
     */
    @PostMapping("/resetPassword")
    @Operation(summary = "重置密码")
    @Log(title = "账号管理")
    public Result resetPassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        log.info("重置密码：{}", passwordEditDTO);
        sysUserService.resetPassword(passwordEditDTO);
        return Result.success();
    }

}
