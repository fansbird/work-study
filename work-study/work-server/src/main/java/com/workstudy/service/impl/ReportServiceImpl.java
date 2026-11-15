package com.workstudy.service.impl;

import com.workstudy.exception.BaseException;
import com.workstudy.mapper.ReportMapper;
import com.workstudy.service.ReportService;
import com.workstudy.vo.DeptSalaryVO;
import com.workstudy.vo.LineReportVO;
import com.workstudy.vo.OverviewVO;
import com.workstudy.vo.PieReportVO;
import com.workstudy.vo.PositionStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计报表服务实现
 *
 * 统计口径：
 * 1) 概览：岗位/报名/学生/部门为全表 COUNT；已发布岗位取 position.status=2；已录用取 position_apply.status=1；
 *    待审核工时 = status=0（待部门审核）+ status=1（待中心复核）；
 *    本月工时/金额按“工时口径”：work_hour.work_date 落在本月且 status=2（已通过）的 hours 之和，
 *    金额 = Σ(hours × position.hourly_wage)（不含薪酬系数与封顶保底，属概览预估口径）。
 * 2) 工时统计（折线）：按日期取 work_hour.status=2 的 hours 之和，缺失日期补 0。
 * 3) 薪酬统计（折线）：按日期取 settlement_detail.amount 之和（剔除已作废结算单 status=3），属实际结算口径。
 * 4) 岗位统计：quota/hired 取 position 现值；hours/amount 同“工时口径”按岗位汇总。
 * 5) 类别分布：position.category 分组 COUNT（空类别归入“未分类”）。
 * 6) 部门薪酬：按 dept 汇总“工时口径”的 hours/amount。
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    /**
     * 工时状态：2 通过（锁定）
     */
    private static final int WORK_HOUR_PASSED = 2;

    /**
     * 工时状态：0 待部门审核
     */
    private static final int WORK_HOUR_DEPT_PENDING = 0;

    /**
     * 工时状态：1 待中心复核
     */
    private static final int WORK_HOUR_CENTER_PENDING = 1;

    /**
     * 报名状态：1 已录用
     */
    private static final int APPLY_HIRED = 1;

    /**
     * 岗位状态：2 已发布
     */
    private static final int POSITION_PUBLISHED = 2;

    /**
     * 折线统计最大天数，避免区间过大导致大量单点查询
     */
    private static final int MAX_STAT_DAYS = 366;

    @Autowired
    private ReportMapper reportMapper;

    /**
     * 工作台数据概览
     *
     * @return
     */
    @Override
    public OverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDate monthBegin = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        long pendingWorkHourCount = count(reportMapper.countWorkHourByStatus(WORK_HOUR_DEPT_PENDING))
                + count(reportMapper.countWorkHourByStatus(WORK_HOUR_CENTER_PENDING));

        return OverviewVO.builder()
                .positionCount(count(reportMapper.countPosition()))
                .publishedCount(count(reportMapper.countPositionByStatus(POSITION_PUBLISHED)))
                .applyCount(count(reportMapper.countApply()))
                .hiredCount(count(reportMapper.countApplyByStatus(APPLY_HIRED)))
                .studentCount(count(reportMapper.countStudent()))
                .deptCount(count(reportMapper.countDept()))
                .pendingWorkHourCount(pendingWorkHourCount)
                .totalHoursMonth(scale(reportMapper.sumWorkHourHours(monthBegin, monthEnd, WORK_HOUR_PASSED)))
                .totalAmountMonth(scale(reportMapper.sumWorkHourAmount(monthBegin, monthEnd, WORK_HOUR_PASSED)))
                .build();
    }

    /**
     * 工时统计（折线图，按日期）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return
     */
    @Override
    public LineReportVO workHourStat(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = dateList(start, end);
        List<String> dateStrList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        for (LocalDate date : dateList) {
            dateStrList.add(date.toString());
            BigDecimal hours = reportMapper.sumWorkHourHoursByDate(date, WORK_HOUR_PASSED);
            valueList.add(scale(hours).toPlainString());
        }
        return LineReportVO.builder()
                .dateList(String.join(",", dateStrList))
                .valueList(String.join(",", valueList))
                .build();
    }

    /**
     * 薪酬统计（折线图，按日期，数据取自结算明细）
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return
     */
    @Override
    public LineReportVO salaryStat(LocalDate start, LocalDate end) {
        List<LocalDate> dateList = dateList(start, end);
        List<String> dateStrList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        for (LocalDate date : dateList) {
            dateStrList.add(date.toString());
            BigDecimal amount = reportMapper.sumSettlementAmountByDate(date);
            valueList.add(scale(amount).toPlainString());
        }
        return LineReportVO.builder()
                .dateList(String.join(",", dateStrList))
                .valueList(String.join(",", valueList))
                .build();
    }

    /**
     * 岗位统计
     *
     * @return
     */
    @Override
    public List<PositionStatsVO> positionStat() {
        return reportMapper.positionStats();
    }

    /**
     * 岗位类别分布（饼图）
     *
     * @return
     */
    @Override
    public PieReportVO categoryDistribution() {
        List<Map<String, Object>> rows = reportMapper.categoryDistribution();
        List<String> nameList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object name = row.get("name");
            Object cnt = row.get("cnt");
            nameList.add(name == null ? "未分类" : name.toString());
            valueList.add(cnt == null ? "0" : cnt.toString());
        }
        return PieReportVO.builder()
                .nameList(String.join(",", nameList))
                .valueList(String.join(",", valueList))
                .build();
    }

    /**
     * 部门薪酬统计
     *
     * @return
     */
    @Override
    public List<DeptSalaryVO> deptSalary() {
        return reportMapper.deptSalary();
    }

    /**
     * 构造 [begin, end] 闭区间的日期列表；参数为空时默认取本月 1 日至今天
     */
    private List<LocalDate> dateList(LocalDate start, LocalDate end) {
        LocalDate begin = start;
        LocalDate finish = end;
        if (begin == null || finish == null) {
            LocalDate today = LocalDate.now();
            begin = today.withDayOfMonth(1);
            finish = today;
        }
        if (begin.isAfter(finish)) {
            throw new BaseException("开始日期不能晚于结束日期");
        }
        if (finish.toEpochDay() - begin.toEpochDay() + 1 > MAX_STAT_DAYS) {
            throw new BaseException("统计区间不能超过" + MAX_STAT_DAYS + "天");
        }
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        dateList.add(current);
        while (current.isBefore(finish)) {
            current = current.plusDays(1);
            dateList.add(current);
        }
        return dateList;
    }

    /**
     * 空计数值归一为 0
     */
    private long count(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 汇总值统一保留两位小数，空值归一为 0.00
     */
    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
