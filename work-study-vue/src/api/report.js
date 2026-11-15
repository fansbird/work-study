import request from '@/utils/request'

/**
 * 统计报表（契约第 5 节 ReportController，根路径 /report）
 */

// 总览统计：GET /report/overview，返回 OverviewVO{positionCount,publishedCount,applyCount,hiredCount,studentCount,deptCount,pendingWorkHourCount,totalHoursMonth,totalAmountMonth}
export const overviewApi = () => {
  return request.get('/report/overview')
}

// 工时统计（折线）：GET /report/workHourStat?start=&end=，返回 LineReportVO{dateList,valueList}
export const workHourStatApi = (params) => {
  return request.get('/report/workHourStat', { params })
}

// 薪酬统计（折线）：GET /report/salaryStat?start=&end=，返回 LineReportVO{dateList,valueList}
export const salaryStatApi = (params) => {
  return request.get('/report/salaryStat', { params })
}

// 岗位统计（柱状）：GET /report/positionStat，返回 List<PositionStatsVO>
export const positionStatApi = () => {
  return request.get('/report/positionStat')
}

// 岗位类别分布（饼图）：GET /report/categoryDistribution，返回 PieReportVO{nameList,valueList}
export const categoryDistributionApi = () => {
  return request.get('/report/categoryDistribution')
}

// 部门薪酬统计：GET /report/deptSalary，返回 List<DeptSalaryVO>
export const deptSalaryApi = () => {
  return request.get('/report/deptSalary')
}
