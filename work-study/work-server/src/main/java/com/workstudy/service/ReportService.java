package com.workstudy.service;

import com.workstudy.vo.DeptSalaryVO;
import com.workstudy.vo.LineReportVO;
import com.workstudy.vo.OverviewVO;
import com.workstudy.vo.PieReportVO;
import com.workstudy.vo.PositionStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计报表服务
 */
public interface ReportService {

    /**
     * 工作台数据概览
     *
     * @return
     */
    OverviewVO overview();

    /**
     * 工时统计（折线图，按日期）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return
     */
    LineReportVO workHourStat(LocalDate start, LocalDate end);

    /**
     * 薪酬统计（折线图，按日期）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return
     */
    LineReportVO salaryStat(LocalDate start, LocalDate end);

    /**
     * 岗位统计
     *
     * @return
     */
    List<PositionStatsVO> positionStat();

    /**
     * 岗位类别分布（饼图）
     *
     * @return
     */
    PieReportVO categoryDistribution();

    /**
     * 部门薪酬统计
     *
     * @return
     */
    List<DeptSalaryVO> deptSalary();

}
