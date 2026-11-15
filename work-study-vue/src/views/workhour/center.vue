<!--
  工时中心复核（勤工助学中心管理员）
  ====================================================================
  路由：/workhour/center    可见角色：勤工助学中心管理员(3)
  契约依据：第 5 节 WorkHourController、第 6 节「工时规则 / 状态机 / 数据归属」

  数据归属：中心看全校（可按部门筛选），后端按角色放行全部数据。
  状态机：1 待中心复核 →(通过) 2 通过（锁定，进入结算范围）/ (驳回) 3 驳回重填。
          锁定后工时不可再改，只有 status=2 且 settlement_id 为空的工时才会被月度结算汇总（契约 6.7）。

  接口对齐：GET /workHour/page  GET /workHour/{id}
            POST /workHour/centerAudit（id,status,rejectReason）   status：2 通过锁定 / 3 驳回
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, centerAuditApi, getByIdApi } from '@/api/workhour'
import { pageApi as positionPageApi } from '@/api/position'
import { listApi as deptListApi } from '@/api/dept'

// ==================== 工时状态机（契约 3 状态码） ====================
const STATUS_MAP = {
  0: { text: '待部门审核', type: 'warning' },
  1: { text: '待中心复核', type: 'primary' },
  2: { text: '通过', type: 'success' },
  3: { text: '驳回重填', type: 'danger' }
}
const STATUS_OPTIONS = Object.keys(STATUS_MAP).map((key) => ({
  value: Number(key),
  label: STATUS_MAP[key].text
}))
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// ==================== 搜索条件 ====================
// 默认只看"待中心复核"，逐条处理
const searchForm = ref({ deptId: null, positionId: null, status: 1, dateRange: [] })

// ==================== 列表数据 ====================
const workhourList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 下拉辅助数据 ====================
const deptOptions = ref([])
const positionOptions = ref([])

// ==================== 钩子 ====================
onMounted(() => {
  loadDeptOptions()
  loadPositionOptions()
  search()
})

// ==================== 数据加载（下拉失败不阻塞主流程） ====================
const loadDeptOptions = async () => {
  try {
    const res = await deptListApi()
    if (res.code) {
      deptOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('[工时中心复核] 部门下拉加载失败，已隐藏部门筛选', e)
  }
}

const loadPositionOptions = async () => {
  try {
    const res = await positionPageApi({ page: 1, pageSize: 200 })
    if (res.code) {
      positionOptions.value = (res.data.records || []).map((item) => ({ value: item.id, label: item.name }))
    }
  } catch (e) {
    console.warn('[工时中心复核] 岗位下拉加载失败', e)
  }
}

const search = async () => {
  loading.value = true
  const range = searchForm.value.dateRange || []
  const params = {
    deptId: searchForm.value.deptId === null ? undefined : searchForm.value.deptId,
    positionId: searchForm.value.positionId === null ? undefined : searchForm.value.positionId,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    startDate: range[0] || undefined,
    endDate: range[1] || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      workhourList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '工时列表查询失败')
    }
  } catch (e) {
    ElMessage.error('工时列表查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { deptId: null, positionId: null, status: 1, dateRange: [] }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 展示格式化 ====================
const shortTime = (value) => (value ? String(value).substring(0, 5) : '--')

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailRow = ref({})
const openDetail = async (row) => {
  detailRow.value = { ...row }
  detailVisible.value = true
  try {
    const res = await getByIdApi(row.id)
    if (res.code) {
      detailRow.value = res.data || row
    }
  } catch (e) {
    console.warn('[工时中心复核] 工时详情加载失败', e)
  }
}

// ==================== 状态机操作：通过（锁定）/ 驳回 ====================
// 通过：1 待中心复核 → 2 通过（锁定，纳入结算）
const approve = (row) => {
  ElMessageBox.confirm(
    `确认复核通过学生【${row.studentName || row.studentNo}】${row.workDate} 的工时吗？\n` +
      '通过后工时将被锁定，进入月度结算汇总，不可再修改。',
    '工时中心复核',
    { confirmButtonText: '通过并锁定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await centerAuditApi({ id: row.id, status: 2 })
      if (res.code) {
        ElMessage.success('已通过并锁定')
        search()
      } else {
        ElMessage.error(res.msg || '复核失败')
      }
    } catch (e) {
      ElMessage.error('复核失败')
    }
  }).catch(() => {})
}

// 驳回：1 待中心复核 → 3 驳回重填（必须填原因，学生可修改后重新提交）
const reject = (row) => {
  ElMessageBox.prompt(
    `请填写驳回学生【${row.studentName || row.studentNo}】${row.workDate} 工时的原因`,
    '工时驳回',
    {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：与部门考勤记录不符、时长异常…',
      inputPattern: /^[\s\S]{1,255}$/,
      inputErrorMessage: '驳回原因不能为空，且不超过 255 字',
      type: 'warning'
    }
  ).then(async ({ value }) => {
    try {
      const res = await centerAuditApi({ id: row.id, status: 3, rejectReason: value })
      if (res.code) {
        ElMessage.success('已驳回，学生可修改后重新提交')
        search()
      } else {
        ElMessage.error(res.msg || '驳回失败')
      }
    } catch (e) {
      ElMessage.error('驳回失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <div class="workhour-center-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item v-if="deptOptions.length" label="用工部门">
          <el-select v-model="searchForm.deptId" placeholder="全部部门" clearable style="width: 170px">
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="searchForm.positionId" placeholder="全部岗位" clearable filterable style="width: 190px">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <span class="audit-tip">复核通过后工时锁定，纳入结算汇总</span>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="workhourList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="学生姓名" min-width="100" align="center">
          <template #default="scope">{{ scope.row.studentName || '--' }}</template>
        </el-table-column>
        <el-table-column label="学号" min-width="120" align="center">
          <template #default="scope">{{ scope.row.studentNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="用工部门" min-width="120" align="center">
          <template #default="scope">{{ scope.row.deptName || '--' }}</template>
        </el-table-column>
        <el-table-column label="岗位名称" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || ('岗位ID：' + scope.row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="工作日期" width="120" align="center">
          <template #default="scope">{{ scope.row.workDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="工作时段" width="140" align="center">
          <template #default="scope">
            {{ shortTime(scope.row.startTime) }} ~ {{ shortTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="时长(小时)" width="100" align="center">
          <template #default="scope">{{ scope.row.hours === null || scope.row.hours === undefined ? '--' : scope.row.hours }}</template>
        </el-table-column>
        <el-table-column label="工作内容" min-width="150" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.workContent || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <!-- 只有"待中心复核"才渲染复核动作 -->
            <template v-if="scope.row.status === 1">
              <el-button link type="success" @click="approve(scope.row)">通过</el-button>
              <el-button link type="danger" @click="reject(scope.row)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页条 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- ==================== 工时详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="工时详情" width="660px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学生姓名">{{ detailRow.studentName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detailRow.studentNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="用工部门">{{ detailRow.deptName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="岗位名称">{{ detailRow.positionName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="工作日期">{{ detailRow.workDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detailRow.status)" size="small">{{ statusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ shortTime(detailRow.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ shortTime(detailRow.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="时长(小时)">{{ detailRow.hours ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算单ID">{{ detailRow.settlementId || '未结算' }}</el-descriptions-item>
        <el-descriptions-item label="部门审核时间">{{ detailRow.deptAuditTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="中心复核时间">{{ detailRow.centerAuditTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="工作内容" :span="2">{{ detailRow.workContent || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.rejectReason" label="驳回原因" :span="2">
          <span class="reject-text">{{ detailRow.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="detailRow.status === 1"
            type="success"
            @click="detailVisible = false; approve(detailRow)"
          >通过并锁定</el-button>
          <el-button
            v-if="detailRow.status === 1"
            type="danger"
            @click="detailVisible = false; reject(detailRow)"
          >驳回</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
}

.audit-tip {
  font-size: 12px;
  color: #909399;
  line-height: 32px;
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

.reject-text {
  color: #f56c6c;
}
</style>
