package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.WorkHourDTO;
import com.workstudy.dto.WorkHourPageQueryDTO;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.WorkHourService;
import com.workstudy.vo.WorkHourVO;
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

/**
 * 工时管理（M4：填报 + 两级审核状态机）
 */
@RestController
@RequestMapping("/workHour")
@Slf4j
@Tag(name = "工时管理接口")
public class WorkHourController {

    @Autowired
    private WorkHourService workHourService;

    /**
     * 学生填报工时（直接 status=0 待部门审核）
     *
     * @param workHourDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "填报工时")
    @Log(title = "工时管理")
    public Result save(@RequestBody WorkHourDTO workHourDTO) {
        log.info("填报工时：{}", workHourDTO);
        workHourService.save(workHourDTO);
        return Result.success();
    }

    /**
     * 修改工时（仅 status=3 驳回重填可改）
     *
     * @param workHourDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改工时（仅驳回重填可改）")
    @Log(title = "工时管理")
    public Result update(@RequestBody WorkHourDTO workHourDTO) {
        log.info("修改工时：{}", workHourDTO);
        workHourService.update(workHourDTO);
        return Result.success();
    }

    /**
     * 驳回后重新提交（3→0）
     *
     * @param id
     * @return
     */
    @PostMapping("/submit/{id}")
    @Operation(summary = "重新提交工时")
    @Log(title = "工时管理")
    public Result submit(@PathVariable Long id) {
        log.info("重新提交工时：{}", id);
        workHourService.submit(id);
        return Result.success();
    }

    /**
     * 我的工时分页查询（学生）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    @GetMapping("/myPage")
    @Operation(summary = "我的工时分页查询")
    public Result<PageResult> myPage(WorkHourPageQueryDTO workHourPageQueryDTO) {
        log.info("我的工时分页查询：{}", workHourPageQueryDTO);
        PageResult pageResult = workHourService.myPage(workHourPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 工时分页查询（部门/中心/财务/超管）
     *
     * @param workHourPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "工时分页查询")
    public Result<PageResult> page(WorkHourPageQueryDTO workHourPageQueryDTO) {
        log.info("工时分页查询：{}", workHourPageQueryDTO);
        PageResult pageResult = workHourService.pageQuery(workHourPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询工时详情
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询工时")
    public Result<WorkHourVO> getById(@PathVariable Long id) {
        log.info("根据id查询工时：{}", id);
        WorkHourVO workHourVO = workHourService.getById(id);
        return Result.success(workHourVO);
    }

    /**
     * 部门审核（限本部门，1=通过转中心复核，3=驳回）
     *
     * @param id
     * @param status
     * @param rejectReason
     * @return
     */
    @PostMapping("/deptAudit")
    @Operation(summary = "工时部门审核")
    @Log(title = "工时管理")
    public Result deptAudit(@RequestParam Long id,
                            @RequestParam Integer status,
                            @RequestParam(required = false) String rejectReason) {
        log.info("工时部门审核：id={}，status={}，rejectReason={}", id, status, rejectReason);
        workHourService.deptAudit(id, status, rejectReason);
        return Result.success();
    }

    /**
     * 中心复核（2=通过锁定，3=驳回）
     *
     * @param id
     * @param status
     * @param rejectReason
     * @return
     */
    @PostMapping("/centerAudit")
    @Operation(summary = "工时中心复核")
    @Log(title = "工时管理")
    public Result centerAudit(@RequestParam Long id,
                              @RequestParam Integer status,
                              @RequestParam(required = false) String rejectReason) {
        log.info("工时中心复核：id={}，status={}，rejectReason={}", id, status, rejectReason);
        workHourService.centerAudit(id, status, rejectReason);
        return Result.success();
    }

    /**
     * 删除工时
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工时")
    @Log(title = "工时管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除工时：{}", id);
        workHourService.deleteById(id);
        return Result.success();
    }

}
