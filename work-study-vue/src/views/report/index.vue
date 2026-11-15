<!--
  统计报表
  ====================================================================
  路由：/report/index    可见角色：超级管理员(0)、勤工助学中心管理员(3)、财务(4)
  契约依据：第 5 节 ReportController（/report）、第 4 节报表 VO

  接口与图表（6 个接口 / 5 张 ECharts 图 + 1 张岗位统计表）：
    1) GET /report/overview                → OverviewVO，顶部概览卡片（9 项指标）
    2) GET /report/workHourStat?start&end  → LineReportVO{dateList,valueList}，工时趋势折线（受顶部日期范围驱动）
    3) GET /report/salaryStat?start&end    → LineReportVO{dateList,valueList}，薪酬趋势折线（受顶部日期范围驱动）
    4) GET /report/positionStat            → List<PositionStatsVO>，岗位统计条形图 + 明细表格
    5) GET /report/categoryDistribution    → PieReportVO{nameList,valueList}，岗位类别分布饼图
    6) GET /report/deptSalary              → List<DeptSalaryVO>，部门薪酬柱状图

  ECharts 用法对照参照工程 sky-take-out-vue/src/views/statistics/index.vue：
  懒初始化 + setOption 复用实例、window resize 自适应、onUnmounted dispose 释放。
  注：折线接口后端限制区间 ≤ 366 天，前端提交前做同样校验。
  ====================================================================
-->
<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
// echarts 需要整体引入（用 import * as echarts 才能调用 echarts.init）
import * as echarts from 'echarts'
import {
  overviewApi,
  workHourStatApi,
  salaryStatApi,
  positionStatApi,
  categoryDistributionApi,
  deptSalaryApi
} from '@/api/report'

// ==================== 概览指标（OverviewVO） ====================
const overview = ref({
  positionCount: 0,
  publishedCount: 0,
  applyCount: 0,
  hiredCount: 0,
  studentCount: 0,
  deptCount: 0,
  pendingWorkHourCount: 0,
  totalHoursMonth: 0,
  totalAmountMonth: 0
})

// ==================== 岗位统计表（List<PositionStatsVO>） ====================
const positionStats = ref([])

// ==================== 顶部日期范围（驱动两张趋势折线） ====================
const dateRange = ref([])
// 日期快捷页签：近7日 / 近30日 / 本月
const quickTabs = [
  { label: '近7日', value: 'last7' },
  { label: '近30日', value: 'last30' },
  { label: '本月', value: 'month' }
]
// 折线接口后端限制：区间不能超过 366 天
const MAX_STAT_DAYS = 366

// ==================== 图表容器 ref ====================
const workHourDom = ref(null)
const salaryDom = ref(null)
const positionDom = ref(null)
const categoryDom = ref(null)
const deptSalaryDom = ref(null)

// 图表实例：用普通对象保存（echarts 实例不需要响应式，被代理反而可能出现问题）
const charts = {
  workHour: null,
  salary: null,
  position: null,
  category: null,
  deptSalary: null
}

// 统一配色（黄色主色 + 红色辅色，与项目主题一致）
const CHART_COLORS = ['#ffb400', '#f56c6c', '#5470c6', '#52c41a', '#909399']

// ==================== 展示格式化 ====================
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '0.00' : Number(value).toFixed(2)
const formatHours = (value) =>
  value === null || value === undefined || value === '' ? '0.00' : Number(value).toFixed(2)

// ==================== 日期工具 ====================
const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 快捷区间：近7日 / 近30日 / 本月
const calcDateRange = (type) => {
  const today = new Date()
  const endDate = new Date()
  let start = new Date()
  if (type === 'last7') {
    start.setDate(today.getDate() - 6)
  } else if (type === 'last30') {
    start.setDate(today.getDate() - 29)
  } else {
    // 本月：1 号 ~ 今天
    start = new Date(today.getFullYear(), today.getMonth(), 1)
  }
  return [formatDate(start), formatDate(endDate)]
}

// 两个日期之间的天数（含首尾）
const daysBetween = (start, end) => {
  const s = new Date(start + 'T00:00:00')
  const e = new Date(end + 'T00:00:00')
  return Math.round((e.getTime() - s.getTime()) / 86400000) + 1
}

// ==================== 图表通用方法 ====================
const initChart = (key, domRef, option) => {
  // 容器还没渲染（例如页面已卸载）直接返回
  if (!domRef.value) return
  if (!charts[key]) {
    charts[key] = echarts.init(domRef.value)
  }
  charts[key].setOption(option, true)
}

// 「逗号分隔字符串」→ 数组（LineReportVO / PieReportVO 的返回格式）
const splitStr = (value) => (value ? String(value).split(',') : [])
const splitNum = (value) =>
  value ? String(value).split(',').map((item) => Number(item) || 0) : []

// ==================== 1. 概览卡片 ====================
const loadOverview = async () => {
  try {
    const res = await overviewApi()
    if (res.code) {
      overview.value = { ...overview.value, ...(res.data || {}) }
    } else {
      ElMessage.error(res.msg || '概览数据查询失败')
    }
  } catch (e) {
    ElMessage.error('概览数据查询失败')
  }
}

// ==================== 2. 工时趋势折线 ====================
const loadWorkHourChart = async () => {
  const [start, end] = dateRange.value
  try {
    const res = await workHourStatApi({ start, end })
    if (!res.code) {
      ElMessage.error(res.msg || '工时统计查询失败')
      return
    }
    initChart('workHour', workHourDom, {
      title: { text: '工时趋势（已通过）', left: 20, top: 10 },
      tooltip: { trigger: 'axis' },
      grid: { left: 70, right: 40, top: 60, bottom: 45 },
      xAxis: { type: 'category', data: splitStr(res.data.dateList), boundaryGap: false },
      yAxis: { type: 'value', name: '工时(小时)' },
      series: [
        {
          name: '工时(小时)',
          type: 'line',
          smooth: true,
          data: splitNum(res.data.valueList),
          itemStyle: { color: CHART_COLORS[0] },
          lineStyle: { color: CHART_COLORS[0] },
          areaStyle: { opacity: 0.08 }
        }
      ]
    })
  } catch (e) {
    ElMessage.error('工时统计查询失败')
  }
}

// ==================== 3. 薪酬趋势折线 ====================
const loadSalaryChart = async () => {
  const [start, end] = dateRange.value
  try {
    const res = await salaryStatApi({ start, end })
    if (!res.code) {
      ElMessage.error(res.msg || '薪酬统计查询失败')
      return
    }
    initChart('salary', salaryDom, {
      title: { text: '薪酬趋势（结算金额）', left: 20, top: 10 },
      tooltip: { trigger: 'axis' },
      grid: { left: 70, right: 40, top: 60, bottom: 45 },
      xAxis: { type: 'category', data: splitStr(res.data.dateList), boundaryGap: false },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [
        {
          name: '金额(元)',
          type: 'line',
          smooth: true,
          data: splitNum(res.data.valueList),
          itemStyle: { color: CHART_COLORS[1] },
          lineStyle: { color: CHART_COLORS[1] },
          areaStyle: { opacity: 0.08 }
        }
      ]
    })
  } catch (e) {
    ElMessage.error('薪酬统计查询失败')
  }
}

// ==================== 4. 岗位统计（表格 + 条形图） ====================
const loadPositionStat = async () => {
  try {
    const res = await positionStatApi()
    if (!res.code) {
      ElMessage.error(res.msg || '岗位统计查询失败')
      return
    }
    const list = res.data || []
    positionStats.value = list

    // 条形图取金额前 10 的岗位，避免类目过多导致标签拥挤
    const top = [...list].sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0)).slice(0, 10)
    const names = top.map((item) => item.positionName || '未命名岗位')
    const hours = top.map((item) => Number(item.hours || 0))
    const amounts = top.map((item) => Number(item.amount || 0))

    initChart('position', positionDom, {
      title: { text: '岗位工时 / 金额 TOP10', left: 20, top: 10 },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['工时(小时)', '金额(元)'], bottom: 0 },
      grid: { left: 70, right: 70, top: 60, bottom: 70 },
      xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: 25 } },
      // 双数值轴：左轴工时、右轴金额（单位不同，分开刻度更清晰）
      yAxis: [
        { type: 'value', name: '工时(小时)' },
        { type: 'value', name: '金额(元)', position: 'right' }
      ],
      series: [
        {
          name: '工时(小时)',
          type: 'bar',
          data: hours,
          barWidth: 12,
          itemStyle: { color: CHART_COLORS[2] }
        },
        {
          name: '金额(元)',
          type: 'bar',
          yAxisIndex: 1,
          data: amounts,
          barWidth: 12,
          itemStyle: { color: CHART_COLORS[0] }
        }
      ]
    })
  } catch (e) {
    ElMessage.error('岗位统计查询失败')
  }
}

// ==================== 5. 岗位类别分布饼图 ====================
const loadCategoryChart = async () => {
  try {
    const res = await categoryDistributionApi()
    if (!res.code) {
      ElMessage.error(res.msg || '类别分布查询失败')
      return
    }
    const names = splitStr(res.data.nameList)
    const values = splitNum(res.data.valueList)
    // 饼图数据：name → value
    const data = names.map((name, index) => ({ name, value: values[index] || 0 }))

    initChart('category', categoryDom, {
      title: { text: '岗位类别分布', left: 20, top: 10 },
      tooltip: { trigger: 'item', formatter: '{b}：{c} 个（{d}%）' },
      legend: { bottom: 0, type: 'scroll' },
      color: CHART_COLORS,
      series: [
        {
          name: '岗位类别',
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['50%', '52%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{c}' },
          data
        }
      ]
    })
  } catch (e) {
    ElMessage.error('类别分布查询失败')
  }
}

// ==================== 6. 部门薪酬柱状图 ====================
const loadDeptSalaryChart = async () => {
  try {
    const res = await deptSalaryApi()
    if (!res.code) {
      ElMessage.error(res.msg || '部门薪酬查询失败')
      return
    }
    const list = res.data || []
    const names = list.map((item) => item.deptName || '未命名部门')
    const hours = list.map((item) => Number(item.hours || 0))
    const amounts = list.map((item) => Number(item.amount || 0))

    initChart('deptSalary', deptSalaryDom, {
      title: { text: '部门薪酬统计', left: 20, top: 10 },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['工时(小时)', '金额(元)'], bottom: 0 },
      grid: { left: 70, right: 70, top: 60, bottom: 55 },
      xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 5 ? 20 : 0 } },
      yAxis: [
        { type: 'value', name: '工时(小时)' },
        { type: 'value', name: '金额(元)', position: 'right' }
      ],
      series: [
        {
          name: '工时(小时)',
          type: 'bar',
          data: hours,
          barWidth: 18,
          itemStyle: { color: CHART_COLORS[2] }
        },
        {
          name: '金额(元)',
          type: 'bar',
          yAxisIndex: 1,
          data: amounts,
          barWidth: 18,
          itemStyle: { color: CHART_COLORS[0] }
        }
      ]
    })
  } catch (e) {
    ElMessage.error('部门薪酬查询失败')
  }
}

// ==================== 顶部日期范围 → 重载两张趋势图 ====================
const applyDateRange = async () => {
  const range = dateRange.value || []
  if (!range || range.length !== 2 || !range[0] || !range[1]) {
    ElMessage.warning('请选择完整的起止日期')
    return
  }
  if (range[0] > range[1]) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  if (daysBetween(range[0], range[1]) > MAX_STAT_DAYS) {
    ElMessage.warning(`统计区间不能超过 ${MAX_STAT_DAYS} 天`)
    return
  }
  await Promise.all([loadWorkHourChart(), loadSalaryChart()])
}

// 快捷页签：点一下即按规则算出日期并刷新趋势
const activeQuick = ref('month')
const changeQuick = (type) => {
  activeQuick.value = type
  dateRange.value = calcDateRange(type)
  applyDateRange()
}

// ==================== 生命周期 ====================
// 窗口尺寸变化时让所有图表重新计算尺寸（否则缩放后图表会变形或留白）
const handleResize = () => {
  Object.keys(charts).forEach((key) => {
    if (charts[key]) {
      charts[key].resize()
    }
  })
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  // 默认区间：本月 1 日 ~ 今天
  dateRange.value = calcDateRange('month')
  // 概览 + 静态图表 + 两张趋势图并行加载
  await Promise.all([
    loadOverview(),
    loadPositionStat(),
    loadCategoryChart(),
    loadDeptSalaryChart(),
    loadWorkHourChart(),
    loadSalaryChart()
  ])
})

// 页面卸载：移除监听并销毁图表实例，释放内存
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.keys(charts).forEach((key) => {
    if (charts[key]) {
      charts[key].dispose()
      charts[key] = null
    }
  })
})
</script>

<template>
  <div class="report-page">
    <!-- ==================== 1. 概览卡片（OverviewVO） ==================== -->
    <div class="stat-grid">
      <div class="page-card stat-card">
        <div class="stat-label">岗位总数</div>
        <div class="stat-value">{{ overview.positionCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">已发布岗位</div>
        <div class="stat-value">{{ overview.publishedCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">报名总数</div>
        <div class="stat-value">{{ overview.applyCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">已录用人数</div>
        <div class="stat-value">{{ overview.hiredCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">学生总数</div>
        <div class="stat-value">{{ overview.studentCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">部门总数</div>
        <div class="stat-value">{{ overview.deptCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">待审核工时</div>
        <div class="stat-value warn">{{ overview.pendingWorkHourCount || 0 }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">本月总工时(小时)</div>
        <div class="stat-value">{{ formatHours(overview.totalHoursMonth) }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">本月总金额(元)</div>
        <div class="stat-value money">{{ formatMoney(overview.totalAmountMonth) }}</div>
      </div>
    </div>

    <!-- ==================== 日期范围（驱动两张趋势折线） ==================== -->
    <div class="page-card filter-bar">
      <div class="quick-tabs">
        <div
          v-for="tab in quickTabs"
          :key="tab.value"
          class="quick-tab"
          :class="{ active: activeQuick === tab.value }"
          @click="changeQuick(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
      />
      <el-button class="query-btn" @click="applyDateRange">查询</el-button>
      <span class="filter-tip">日期范围仅影响下方两张「趋势」折线图</span>
    </div>

    <!-- ==================== 图表区（2 列） ==================== -->
    <div class="chart-grid">
      <!-- 2. 工时趋势折线 -->
      <div class="page-card chart-card">
        <div ref="workHourDom" class="chart-box"></div>
      </div>

      <!-- 3. 薪酬趋势折线 -->
      <div class="page-card chart-card">
        <div ref="salaryDom" class="chart-box"></div>
      </div>

      <!-- 4. 岗位统计条形 -->
      <div class="page-card chart-card">
        <div ref="positionDom" class="chart-box"></div>
      </div>

      <!-- 5. 岗位类别分布饼图 -->
      <div class="page-card chart-card">
        <div ref="categoryDom" class="chart-box"></div>
      </div>

      <!-- 6. 部门薪酬柱状（跨两列，类目多时更易读） -->
      <div class="page-card chart-card span-two">
        <div ref="deptSalaryDom" class="chart-box"></div>
      </div>
    </div>

    <!-- ==================== 4'. 岗位统计明细表（PositionStatsVO） ==================== -->
    <div class="page-card">
      <div class="table-header">
        <span class="table-title">岗位统计明细</span>
        <span class="table-tip">工时 / 金额为「已通过工时」口径的累计值</span>
      </div>
      <el-table :data="positionStats" style="width: 100%" max-height="420">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="岗位名称" min-width="160" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || '--' }}</template>
        </el-table-column>
        <el-table-column label="用工部门" min-width="130" align="center">
          <template #default="scope">{{ scope.row.deptName || '--' }}</template>
        </el-table-column>
        <el-table-column label="招聘名额" width="100" align="center">
          <template #default="scope">{{ scope.row.quota ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="已录用" width="100" align="center">
          <template #default="scope">{{ scope.row.hired ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="累计工时(小时)" width="140" align="right">
          <template #default="scope">{{ formatHours(scope.row.hours) }}</template>
        </el-table-column>
        <el-table-column label="累计金额(元)" width="140" align="right">
          <template #default="scope">{{ formatMoney(scope.row.amount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 概览卡片 ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: #333333;
}

.stat-value.warn {
  color: #e6a23c;
}

.stat-value.money {
  color: #f56c6c;
}

/* ==================== 日期范围工具条 ==================== */
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-tabs {
  display: flex;
  align-items: center;
}

.quick-tab {
  min-width: 76px;
  height: 32px;
  line-height: 30px;
  text-align: center;
  padding: 0 14px;
  background: #fff;
  border: 1px solid #e4e7ed;
  margin-right: -1px;
  cursor: pointer;
  font-size: 13px;
  color: #333333;
}

.quick-tab.active {
  background: #ffb400;
  border-color: #ffb400;
  font-weight: bold;
  position: relative;
  z-index: 1;
}

/* 深色查询按钮 */
.query-btn {
  background-color: #333333;
  border-color: #333333;
  color: #ffffff;
}

.query-btn:hover,
.query-btn:focus {
  background-color: #4d4d4d;
  border-color: #4d4d4d;
  color: #ffffff;
}

.filter-tip {
  font-size: 12px;
  color: #909399;
}

/* ==================== 图表区 ==================== */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  margin-bottom: 0;
}

/* 跨两列的图表卡片 */
.span-two {
  grid-column: 1 / -1;
}

/* echarts 容器必须给定高度 */
.chart-box {
  width: 100%;
  height: 340px;
}

/* 表格标题 */
.table-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.table-title {
  font-size: 15px;
  font-weight: bold;
  color: #333333;
}

.table-tip {
  font-size: 12px;
  color: #909399;
}
</style>
