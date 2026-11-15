package com.workstudy.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.workstudy.entity.SysOperLog;
import com.workstudy.mapper.SysOperLogMapper;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 操作日志查询（M8，只读，日志写入由 OperLogAspect 完成）
 */
@RestController
@RequestMapping("/operLog")
@Slf4j
@Tag(name = "操作日志接口")
public class OperLogController {

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    /**
     * 分页查询操作日志
     *
     * @param title        模块标题（模糊）
     * @param operUserName 操作人姓名（模糊）
     * @param status       状态 1正常 0异常
     * @param begin        操作日期起（yyyy-MM-dd）
     * @param end          操作日期止（yyyy-MM-dd）
     * @param page         页码
     * @param pageSize     每页条数
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    public Result<PageResult> page(@RequestParam(required = false) String title,
                                   @RequestParam(required = false) String operUserName,
                                   @RequestParam(required = false) Integer status,
                                   @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate begin,
                                   @DateTimeFormat(pattern = "yyyy-MM-dd") @RequestParam(required = false) LocalDate end,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("分页查询操作日志：title={}, operUserName={}, status={}, begin={}, end={}",
                title, operUserName, status, begin, end);
        PageHelper.startPage(page, pageSize);
        Page<SysOperLog> pageData = sysOperLogMapper.pageQuery(
                title,
                operUserName,
                status,
                begin == null ? null : LocalDateTime.of(begin, LocalTime.MIN),
                end == null ? null : LocalDateTime.of(end, LocalTime.MAX));
        return Result.success(new PageResult(pageData.getTotal(), pageData.getResult()));
    }

    /**
     * 根据id查询操作日志
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询操作日志")
    public Result<SysOperLog> getById(@PathVariable Long id) {
        log.info("根据id查询操作日志：{}", id);
        SysOperLog sysOperLog = sysOperLogMapper.getById(id);
        return Result.success(sysOperLog);
    }

}
