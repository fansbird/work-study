package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.SettlementAdjustDTO;
import com.workstudy.dto.SettlementGenerateDTO;
import com.workstudy.dto.SettlementPageQueryDTO;
import com.workstudy.entity.Settlement;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.SettlementService;
import com.workstudy.vo.SettlementDetailVO;
import com.workstudy.vo.SettlementVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 薪酬结算管理（M5：生成/确认/发放/作废/调整/导出）
 */
@RestController
@RequestMapping("/settlement")
@Slf4j
@Tag(name = "薪酬结算管理接口")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    /**
     * 生成月度结算批次（幂等：同学期+月+部门已存在未作废批次则拒绝）
     *
     * @param settlementGenerateDTO
     * @return
     */
    @PostMapping("/generate")
    @Operation(summary = "生成结算批次")
    @Log(title = "结算单管理")
    public Result<Settlement> generate(@RequestBody SettlementGenerateDTO settlementGenerateDTO) {
        log.info("生成结算批次：{}", settlementGenerateDTO);
        Settlement settlement = settlementService.generate(settlementGenerateDTO);
        return Result.success(settlement);
    }

    /**
     * 分页查询结算单
     *
     * @param settlementPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询结算单")
    public Result<PageResult> page(SettlementPageQueryDTO settlementPageQueryDTO) {
        log.info("分页查询结算单：{}", settlementPageQueryDTO);
        PageResult pageResult = settlementService.pageQuery(settlementPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询结算单
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询结算单")
    public Result<SettlementVO> getById(@PathVariable Long id) {
        log.info("根据id查询结算单：{}", id);
        SettlementVO settlementVO = settlementService.getById(id);
        return Result.success(settlementVO);
    }

    /**
     * 查询结算明细
     *
     * @param settlementId
     * @return
     */
    @GetMapping("/detail/{settlementId}")
    @Operation(summary = "查询结算明细")
    public Result<List<SettlementDetailVO>> detail(@PathVariable Long settlementId) {
        log.info("查询结算明细：{}", settlementId);
        List<SettlementDetailVO> list = settlementService.listDetail(settlementId);
        return Result.success(list);
    }

    /**
     * 确认结算单（0 → 1，中心）
     *
     * @param id
     * @return
     */
    @PostMapping("/confirm/{id}")
    @Operation(summary = "确认结算单")
    @Log(title = "结算单管理")
    public Result confirm(@PathVariable Long id) {
        log.info("确认结算单：{}", id);
        settlementService.confirm(id);
        return Result.success();
    }

    /**
     * 发放结算单（1 → 2，财务，发放后锁定）
     *
     * @param id
     * @return
     */
    @PostMapping("/pay/{id}")
    @Operation(summary = "发放结算单")
    @Log(title = "结算单管理")
    public Result pay(@PathVariable Long id) {
        log.info("发放结算单：{}", id);
        settlementService.pay(id);
        return Result.success();
    }

    /**
     * 作废结算单（0/1 → 3，并解绑其下工时以便重算）
     *
     * @param id
     * @return
     */
    @PostMapping("/void/{id}")
    @Operation(summary = "作废结算单")
    @Log(title = "结算单管理")
    public Result voidSettlement(@PathVariable Long id) {
        log.info("作废结算单：{}", id);
        settlementService.voidSettlement(id);
        return Result.success();
    }

    /**
     * 结算调整（补发/扣减）
     *
     * @param settlementAdjustDTO
     * @return
     */
    @PostMapping("/adjust")
    @Operation(summary = "结算调整")
    @Log(title = "结算单管理")
    public Result adjust(@RequestBody SettlementAdjustDTO settlementAdjustDTO) {
        log.info("结算调整：{}", settlementAdjustDTO);
        settlementService.adjust(settlementAdjustDTO);
        return Result.success();
    }

    /**
     * 导出结算明细（Apache POI，xlsx）
     *
     * @param id
     * @param response
     */
    @GetMapping("/export/{id}")
    @Operation(summary = "导出结算明细")
    public void export(@PathVariable Long id, HttpServletResponse response) {
        log.info("导出结算明细：{}", id);
        settlementService.export(id, response);
    }

}
