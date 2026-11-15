package com.workstudy.controller;

import com.workstudy.result.Result;
import com.workstudy.service.ReportService;
import com.workstudy.vo.DeptSalaryVO;
import com.workstudy.vo.LineReportVO;
import com.workstudy.vo.OverviewVO;
import com.workstudy.vo.PieReportVO;
import com.workstudy.vo.PositionStatsVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计报表
 */
@RestController
@RequestMapping("/report")
@Slf4j
@Tag(name = "统计报表接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 工作台数据概览
     *
     * @return
     */
    @GetMapping("/overview")
    @Operation(summary = "工作台数据概览")
    public Result<OverviewVO> overview() {
        log.info("工作台数据概览");
        OverviewVO overviewVO = reportService.overview();
        return Result.success(overviewVO);
    }

    /**
     * 工时统计（折线图，按日期）
     *
     * @param start
     * @param end
     * @return
     */
    @GetMapping("/workHourStat")
    @Operation(summary = "工时统计")
    public Result<LineReportVO> workHourStat(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("工时统计，开始日期：{}，结束日期：{}", start, end);
        LineReportVO lineReportVO = reportService.workHourStat(start, end);
        return Result.success(lineReportVO);
    }

    /**
     * 薪酬统计（折线图，按日期，数据取自结算明细）
     *
     * @param start
     * @param end
     * @return
     */
    @GetMapping("/salaryStat")
    @Operation(summary = "薪酬统计")
    public Result<LineReportVO> salaryStat(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("薪酬统计，开始日期：{}，结束日期：{}", start, end);
        LineReportVO lineReportVO = reportService.salaryStat(start, end);
        return Result.success(lineReportVO);
    }

    /**
     * 岗位统计
     *
     * @return
     */
    @GetMapping("/positionStat")
    @Operation(summary = "岗位统计")
    public Result<List<PositionStatsVO>> positionStat() {
        log.info("岗位统计");
        List<PositionStatsVO> list = reportService.positionStat();
        return Result.success(list);
    }

    /**
     * 岗位类别分布（饼图）
     *
     * @return
     */
    @GetMapping("/categoryDistribution")
    @Operation(summary = "岗位类别分布")
    public Result<PieReportVO> categoryDistribution() {
        log.info("岗位类别分布");
        PieReportVO pieReportVO = reportService.categoryDistribution();
        return Result.success(pieReportVO);
    }

    /**
     * 部门薪酬统计
     *
     * @return
     */
    @GetMapping("/deptSalary")
    @Operation(summary = "部门薪酬统计")
    public Result<List<DeptSalaryVO>> deptSalary() {
        log.info("部门薪酬统计");
        List<DeptSalaryVO> list = reportService.deptSalary();
        return Result.success(list);
    }

}
