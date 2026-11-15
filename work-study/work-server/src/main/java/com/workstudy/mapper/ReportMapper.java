package com.workstudy.mapper;

import com.workstudy.vo.DeptSalaryVO;
import com.workstudy.vo.PositionStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计报表 Mapper（聚合查询，全部为 SUM/COUNT）
 */
@Mapper
public interface ReportMapper {

    /**
     * 岗位总数
     */
    Long countPosition();

    /**
     * 指定状态的岗位数
     */
    Long countPositionByStatus(@Param("status") Integer status);

    /**
     * 报名总数
     */
    Long countApply();

    /**
     * 指定状态的报名数
     */
    Long countApplyByStatus(@Param("status") Integer status);

    /**
     * 学生总数
     */
    Long countStudent();

    /**
     * 部门总数
     */
    Long countDept();

    /**
     * 指定状态的工时数
     */
    Long countWorkHourByStatus(@Param("status") Integer status);

    /**
     * 指定状态的结算单数
     */
    Long countSettlementByStatus(@Param("status") Integer status);

    /**
     * 日期区间内指定状态的工时合计
     */
    BigDecimal sumWorkHourHours(@Param("begin") LocalDate begin,
                                @Param("end") LocalDate end,
                                @Param("status") Integer status);

    /**
     * 日期区间内指定状态的应付金额合计（Σ hours × 岗位时薪）
     */
    BigDecimal sumWorkHourAmount(@Param("begin") LocalDate begin,
                                 @Param("end") LocalDate end,
                                 @Param("status") Integer status);

    /**
     * 某一天指定状态的工时合计
     */
    BigDecimal sumWorkHourHoursByDate(@Param("date") LocalDate date,
                                      @Param("status") Integer status);

    /**
     * 某一天的结算金额合计（剔除已作废结算单）
     */
    BigDecimal sumSettlementAmountByDate(@Param("date") LocalDate date);

    /**
     * 岗位统计列表
     */
    List<PositionStatsVO> positionStats();

    /**
     * 岗位类别分布（name=类别，cnt=数量）
     */
    List<Map<String, Object>> categoryDistribution();

    /**
     * 部门薪酬统计列表
     */
    List<DeptSalaryVO> deptSalary();

}
