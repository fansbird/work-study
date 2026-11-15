package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.PositionApplyPageQueryDTO;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.PositionApplyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位报名管理
 * 状态机：0 待审核 →（1 已录用 / 2 已驳回）；0 → 3 已撤销；1 已录用 → 4 已放弃（释放名额 hired-1）
 */
@RestController
@RequestMapping("/apply")
@Slf4j
@Tag(name = "岗位报名管理接口")
public class PositionApplyController {

    @Autowired
    private PositionApplyService positionApplyService;

    /**
     * 学生报名岗位
     *
     * @param positionId
     * @param applyRemark
     * @return
     */
    @PostMapping
    @Operation(summary = "学生报名岗位")
    @Log(title = "岗位报名")
    public Result apply(@RequestParam Long positionId,
                        @RequestParam(required = false) String applyRemark) {
        log.info("学生报名岗位：positionId={}, applyRemark={}", positionId, applyRemark);
        positionApplyService.apply(positionId, applyRemark);
        return Result.success();
    }

    /**
     * 撤销报名（0 待审核 → 3 已撤销）
     *
     * @param id
     * @return
     */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "撤销报名")
    @Log(title = "岗位报名")
    public Result cancel(@PathVariable Long id) {
        log.info("撤销报名：{}", id);
        positionApplyService.cancel(id);
        return Result.success();
    }

    /**
     * 报名审核（0 待审核 → 1 已录用 / 2 已驳回）
     *
     * @param id
     * @param status        1 已录用 / 2 已驳回
     * @param rejectReason
     * @return
     */
    @PostMapping("/audit")
    @Operation(summary = "报名审核")
    @Log(title = "报名审核")
    public Result audit(@RequestParam Long id,
                        @RequestParam Integer status,
                        @RequestParam(required = false) String rejectReason) {
        log.info("报名审核：id={}, status={}, rejectReason={}", id, status, rejectReason);
        positionApplyService.audit(id, status, rejectReason);
        return Result.success();
    }

    /**
     * 放弃录用（1 已录用 → 4 已放弃，岗位 hired-1）
     *
     * @param id
     * @return
     */
    @PostMapping("/giveUp/{id}")
    @Operation(summary = "放弃录用")
    @Log(title = "岗位报名")
    public Result giveUp(@PathVariable Long id) {
        log.info("放弃录用：{}", id);
        positionApplyService.giveUp(id);
        return Result.success();
    }

    /**
     * 分页查询报名（管理端）
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询报名")
    public Result<PageResult> page(PositionApplyPageQueryDTO positionApplyPageQueryDTO) {
        log.info("分页查询报名：{}", positionApplyPageQueryDTO);
        PageResult pageResult = positionApplyService.pageQuery(positionApplyPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 我的报名分页（学生端）
     *
     * @param positionApplyPageQueryDTO
     * @return
     */
    @GetMapping("/myPage")
    @Operation(summary = "我的报名分页")
    public Result<PageResult> myPage(PositionApplyPageQueryDTO positionApplyPageQueryDTO) {
        log.info("我的报名分页：{}", positionApplyPageQueryDTO);
        PageResult pageResult = positionApplyService.myPage(positionApplyPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 校验当前学生是否已报名某岗位
     *
     * @param positionId
     * @return
     */
    @GetMapping("/check/{positionId}")
    @Operation(summary = "校验当前学生是否已报名该岗位")
    public Result<Boolean> check(@PathVariable Long positionId) {
        log.info("校验当前学生是否已报名岗位：{}", positionId);
        Boolean applied = positionApplyService.check(positionId);
        return Result.success(applied);
    }

}
