package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.PositionDTO;
import com.workstudy.dto.PositionPageQueryDTO;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.PositionService;
import com.workstudy.vo.PositionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理
 * 状态机：0 草稿 → 1 待审批 →（2 已发布 / 3 已驳回）；2 已发布 → 4 已下架；3 已驳回修改后可重新提交回 1
 */
@RestController
@RequestMapping("/position")
@Slf4j
@Tag(name = "岗位管理接口")
public class PositionController {

    @Autowired
    private PositionService positionService;

    /**
     * 分页查询岗位
     *
     * @param positionPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询岗位")
    public Result<PageResult> page(PositionPageQueryDTO positionPageQueryDTO) {
        log.info("分页查询岗位：{}", positionPageQueryDTO);
        PageResult pageResult = positionService.pageQuery(positionPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询已发布岗位列表（学生浏览/下拉）
     *
     * @param positionPageQueryDTO
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询已发布岗位列表")
    public Result<List<PositionVO>> list(PositionPageQueryDTO positionPageQueryDTO) {
        log.info("查询已发布岗位列表：{}", positionPageQueryDTO);
        List<PositionVO> list = positionService.list(positionPageQueryDTO);
        return Result.success(list);
    }

    /**
     * 新增岗位（存草稿）
     *
     * @param positionDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增岗位")
    @Log(title = "岗位管理")
    public Result save(@RequestBody PositionDTO positionDTO) {
        log.info("新增岗位：{}", positionDTO);
        positionService.save(positionDTO);
        return Result.success();
    }

    /**
     * 修改岗位（仅草稿/已驳回可改）
     *
     * @param positionDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改岗位")
    @Log(title = "岗位管理")
    public Result update(@RequestBody PositionDTO positionDTO) {
        log.info("修改岗位：{}", positionDTO);
        positionService.update(positionDTO);
        return Result.success();
    }

    /**
     * 根据id查询岗位详情
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询岗位")
    public Result<PositionVO> getById(@PathVariable Long id) {
        log.info("根据id查询岗位：{}", id);
        PositionVO positionVO = positionService.getById(id);
        return Result.success(positionVO);
    }

    /**
     * 提交审批（0 草稿/3 已驳回 → 1 待审批）
     *
     * @param id
     * @return
     */
    @PostMapping("/submit/{id}")
    @Operation(summary = "提交岗位审批")
    @Log(title = "岗位管理")
    public Result submit(@PathVariable Long id) {
        log.info("提交岗位审批：{}", id);
        positionService.submit(id);
        return Result.success();
    }

    /**
     * 审批岗位（1 待审批 → 2 已发布 / 3 已驳回）
     *
     * @param positionDTO 含 id、status(2/3)、rejectReason（前端以查询参数传递）
     * @return
     */
    @PostMapping("/audit")
    @Operation(summary = "审批岗位")
    @Log(title = "岗位审批")
    public Result audit(PositionDTO positionDTO) {
        log.info("审批岗位：{}", positionDTO);
        positionService.audit(positionDTO);
        return Result.success();
    }

    /**
     * 下架岗位（2 已发布 → 4 已下架）
     *
     * @param id
     * @return
     */
    @PostMapping("/offline/{id}")
    @Operation(summary = "下架岗位")
    @Log(title = "岗位管理")
    public Result offline(@PathVariable Long id) {
        log.info("下架岗位：{}", id);
        positionService.offline(id);
        return Result.success();
    }

    /**
     * 删除岗位（仅草稿/已驳回可删）
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位")
    @Log(title = "岗位管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除岗位：{}", id);
        positionService.deleteById(id);
        return Result.success();
    }

}
