<!--
  我的薪酬（学生）
  ====================================================================
  路由：/settlement/mine    可见角色：学生(1)
  契约依据：第 5 节 SettlementController、第 6 节⑩「数据归属校验」

  数据归属：GET /settlement/page 在学生角色下由后端按「含本人明细」过滤（契约 6.10），
            前端只用 LoginVO.id 换 student 档案（GET /student/getByUserId/{userId}）拿到本人 studentId，
            再按 studentId 从各批次明细里筛出属于自己的工时与金额，汇总「已发 / 待发」。
  状态机（契约第 3 节）：0 待确认 → 1 已确认 → 2 已发放；0/1 → 3 已作废（学生侧只读，不提供任何操作）。

  接口对齐：GET /settlement/page  GET /settlement/detail/{settlementId}  GET /student/getByUserId/{userId}
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageApi, detailApi } from '@/api/settlement'
import { getByUserIdApi } from '@/api/student'
import { listApi as semesterListApi } from '@/api/semester'
import { getLoginUser } from '@/router'

// ==================== 结算单状态机（契约 3 状态码） ====================
const STATUS_MAP = {
  0: { text: '待确认', type: 'warning' },
  1: { text: '已确认（待发放）', type: 'primary' },
  2: { text: '已发放', type: 'success' },
  3: { text: '已作废', type: 'info' }
}
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// ==================== 本人身份（student 档案） ====================
const loginUser = getLoginUser() || {}
const myStudent = ref(null)
const myStudentId = ref(null)
const profileLoaded = ref(false)

// ==================== 列表数据（本人相关结算单，前端分页） ====================
const settlementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 学期 id → 名称
const semesterList = ref([])
const semesterMap = computed(() => {
  const map = {}
  semesterList.value.forEach((item) => {
    map[item.id] = item.name
  })
  return map
})

// ==================== 展示格式化 ====================
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)
const formatHours = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)
const semesterLabel = (row) =>
  row.semesterName || (row.semesterId ? semesterMap.value[row.semesterId] || '学期ID：' + row.semesterId : '--')
const deptLabel = (row) =>
  row.deptId === null || row.deptId === undefined || row.deptId === ''
    ? '全校'
    : row.deptName || '部门ID：' + row.deptId

// 作废(3)批次后端已解绑工时并删除明细，展示 '--' 而不是容易误解的 0.00
const cellHours = (row) => (row.status === 3 ? '--' : formatHours(row.myHours))
const cellMoney = (row) => (row.status === 3 ? '--' : formatMoney(row.myAmount))

// 银行卡脱敏展示（只保留后 4 位）
const maskedBankCard = computed(() => {
  const card = myStudent.value && myStudent.value.bankCard ? String(myStudent.value.bankCard) : ''
  if (!card) return ''
  if (card.length <= 4) return card
  return '**** **** **** ' + card.slice(-4)
})

// 金额汇总（只统计本人明细；已作废批次不计入待发/累计工时）
const summary = computed(() => {
  let paid = 0
  let pending = 0
  let hours = 0
  let count = 0
  settlementList.value.forEach((row) => {
    if (row.status === 3) return
    count += 1
    hours += Number(row.myHours || 0)
    if (row.status === 2) {
      paid += Number(row.myAmount || 0)
    } else {
      // 待确认(0) / 已确认待发放(1) 均属于待发放
      pending += Number(row.myAmount || 0)
    }
  })
  return {
    paid: paid.toFixed(2),
    pending: pending.toFixed(2),
    hours: hours.toFixed(2),
    count
  }
})

// 前端分页：只有本人相关数据，一次取回后本地切片（保证汇总口径覆盖全部批次）
const pageRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return settlementList.value.slice(start, start + pageSize.value)
})
const total = computed(() => settlementList.value.length)

const handleSizeChange = () => {
  // 每页条数变化后避免停留在超界页码
  const maxPage = Math.max(1, Math.ceil(settlementList.value.length / pageSize.value))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
}

// ==================== 钩子函数 ====================
onMounted(async () => {
  loadSemesterList()
  await loadMyProfile()
  if (myStudentId.value === null) {
    profileLoaded.value = true
    return
  }
  await loadMySettlements()
  profileLoaded.value = true
})

// ==================== 本人档案（拿 studentId 与银行卡） ====================
const loadMyProfile = async () => {
  try {
    const res = await getByUserIdApi(loginUser.id)
    if (res.code && res.data) {
      myStudent.value = res.data
      myStudentId.value = res.data.id
    } else {
      ElMessage.warning(res.msg || '未找到您的学生档案，请联系中心管理员维护')
    }
  } catch (e) {
    ElMessage.error('学生档案加载失败')
  }
}

// ==================== 学期下拉 ====================
const loadSemesterList = async () => {
  try {
    const res = await semesterListApi()
    if (res.code) {
      semesterList.value = res.data || []
    }
  } catch (e) {
    console.warn('[我的薪酬] 学期下拉加载失败', e)
  }
}

// ==================== 本人结算单 + 明细（筛出属于本人的部分） ====================
const sumBy = (list, key) => list.reduce((acc, item) => acc + Number(item[key] || 0), 0)

const loadMySettlements = async () => {
  loading.value = true
  try {
    // 学生角色下后端只返回包含本人明细的结算单；一次取回用于本地汇总与分页
    const res = await pageApi({ page: 1, pageSize: 200 })
    if (!res.code) {
      ElMessage.error(res.msg || '结算单查询失败')
      return
    }
    const rawList = (res.data && res.data.records) || []
    const enriched = await Promise.all(
      rawList.map(async (row) => {
        let myDetails = []
        try {
          const detailRes = await detailApi(row.id)
          if (detailRes.code) {
            myDetails = (detailRes.data || []).filter((item) => item.studentId === myStudentId.value)
          }
        } catch (e) {
          console.warn('[我的薪酬] 结算明细加载失败，批次号：' + row.batchNo, e)
        }
        return {
          ...row,
          myDetails,
          myHours: sumBy(myDetails, 'hours'),
          myAmount: sumBy(myDetails, 'amount')
        }
      })
    )
    settlementList.value = enriched
    currentPage.value = 1
  } catch (e) {
    ElMessage.error('结算单查询失败')
  } finally {
    loading.value = false
  }
}

// ==================== 明细弹窗（仅本人明细 + 合计行） ====================
const detailVisible = ref(false)
const detailRow = ref({})
const detailList = ref([])

const openDetail = (row) => {
  detailRow.value = { ...row }
  detailList.value = row.myDetails || []
  detailVisible.value = true
}

const summaryMethod = ({ columns, data }) => {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.property === 'hours' || column.property === 'amount') {
      const sum = data.reduce((acc, row) => acc + Number(row[column.property] || 0), 0)
      sums[index] = sum.toFixed(2)
      return
    }
    sums[index] = ''
  })
  return sums
}
</script>

<template>
  <div class="settlement-mine-page">
    <!-- ==================== 银行卡提示 ==================== -->
    <el-alert
      v-if="profileLoaded && myStudent && myStudent.bankCard"
      class="bank-tip"
      type="success"
      :closable="false"
      show-icon
      :title="'薪酬发放至银行卡：' + maskedBankCard"
    >
      <template #default>如银行卡信息有误，请联系勤工助学中心管理员在学生档案中修改。</template>
    </el-alert>
    <el-alert
      v-else-if="profileLoaded && myStudent"
      class="bank-tip"
      type="warning"
      :closable="false"
      show-icon
      title="尚未绑定银行卡，薪酬可能无法正常发放"
    >
      <template #default>请携带本人银行卡信息联系勤工助学中心管理员，在学生档案中补充银行卡号。</template>
    </el-alert>
    <el-alert
      v-else-if="profileLoaded && !myStudent"
      class="bank-tip"
      type="error"
      :closable="false"
      show-icon
      title="未找到您的学生档案"
    >
      <template #default>无法查询薪酬信息，请联系勤工助学中心管理员维护学生档案（关联登录账号）。</template>
    </el-alert>

    <!-- ==================== 金额汇总卡片 ==================== -->
    <div class="stat-grid">
      <div class="page-card stat-card">
        <div class="stat-label">已发放金额(元)</div>
        <div class="stat-value paid">{{ summary.paid }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">待发放金额(元)</div>
        <div class="stat-value pending">{{ summary.pending }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">累计工时(小时)</div>
        <div class="stat-value">{{ summary.hours }}</div>
      </div>
      <div class="page-card stat-card">
        <div class="stat-label">结算批次数</div>
        <div class="stat-value">{{ summary.count }}</div>
      </div>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <div class="table-header">
        <span class="table-title">我的结算单</span>
        <span class="table-tip">金额为该批次中属于本人的部分；已作废批次不计入汇总</span>
      </div>
      <el-table v-loading="loading" :data="pageRows" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="batchNo" label="批次号" min-width="170" align="center" show-overflow-tooltip />
        <el-table-column label="学期" min-width="150" align="center">
          <template #default="scope">{{ semesterLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column prop="month" label="月份" width="100" align="center" />
        <el-table-column label="部门" min-width="120" align="center">
          <template #default="scope">{{ deptLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="我的工时(小时)" width="130" align="right">
          <template #default="scope">{{ cellHours(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="我的金额(元)" width="130" align="right">
          <template #default="scope">{{ cellMoney(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发放时间" width="180" align="center">
          <template #default="scope">{{ scope.row.payTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页条（本地分页，底数为本人相关批次总数） -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- ==================== 明细弹窗（仅本人明细 + 合计行） ==================== -->
    <el-dialog v-model="detailVisible" title="我的结算明细" width="880px">
      <el-descriptions :column="3" border size="small" class="detail-meta">
        <el-descriptions-item label="批次号">{{ detailRow.batchNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="学期">{{ semesterLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="月份">{{ detailRow.month || '--' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ deptLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="我的工时">{{ cellHours(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="我的金额">{{ cellMoney(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detailRow.status)" size="small">{{ statusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detailRow.confirmTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="发放时间">{{ detailRow.payTime || '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detailList" style="width: 100%" show-summary :summary-method="summaryMethod">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="岗位" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || ('岗位ID：' + scope.row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="工作日期" width="120" align="center">
          <template #default="scope">{{ scope.row.workDate || '--' }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="工时(小时)" width="110" align="right">
          <template #default="scope">{{ formatHours(scope.row.hours) }}</template>
        </el-table-column>
        <el-table-column prop="hourlyWage" label="时薪(元)" width="110" align="right">
          <template #default="scope">{{ formatMoney(scope.row.hourlyWage) }}</template>
        </el-table-column>
        <el-table-column prop="coefficient" label="系数" width="90" align="right">
          <template #default="scope">{{ formatMoney(scope.row.coefficient) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额(元)" width="120" align="right">
          <template #default="scope">{{ formatMoney(scope.row.amount) }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 银行卡提示 */
.bank-tip {
  margin-bottom: 16px;
}

/* ==================== 金额汇总卡片 ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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
  font-size: 24px;
  font-weight: bold;
  color: #333333;
}

.stat-value.paid {
  color: #52c41a;
}

.stat-value.pending {
  color: #e6a23c;
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

.detail-meta {
  margin-bottom: 12px;
}
</style>
