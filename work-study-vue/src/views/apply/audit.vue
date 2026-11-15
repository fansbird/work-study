<!--
  报名审核（用工部门管理员 / 勤工助学中心管理员）
  ====================================================================
  路由：/apply/audit    可见角色：用工部门管理员(2)、勤工助学中心管理员(3)
  契约依据：第 5 节 PositionApplyController、第 6 节「状态机 / 名额并发 / 数据归属」

  数据归属：部门管理员只审核本部门岗位的报名，中心管理员审核全部（后端按 BaseContext.role + dept_id 过滤）。
  名额并发（契约 6.5）：录用走后端条件更新 hired = hired + 1 WHERE hired < quota，
                        名额已满时后端返回业务异常，前端统一提示"名额已满"。
  状态机：0 待审核 →(录用) 1 已录用 / (驳回) 2 已驳回（驳回必须填原因）。

  接口对齐：GET /apply/page  GET /position/page（岗位下拉）  POST /apply/audit（id,status,rejectReason）
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi as applyPageApi, auditApi } from '@/api/apply'
import { pageApi as positionPageApi } from '@/api/position'

// ==================== 报名状态机（契约 3 状态码） ====================
const STATUS_MAP = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已录用', type: 'success' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已撤销', type: 'info' },
  4: { text: '已放弃', type: 'info' }
}
const STATUS_OPTIONS = Object.keys(STATUS_MAP).map((key) => ({
  value: Number(key),
  label: STATUS_MAP[key].text
}))
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// ==================== 搜索条件（PositionApplyPageQueryDTO{positionId,semesterId,status,studentId,page,pageSize}） ====================
// 默认只看待审核，方便逐条处理
const searchForm = ref({ positionId: null, status: 0 })

// ==================== 列表数据 ====================
const applyList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 岗位下拉（部门管理员只会拿到本部门岗位） ====================
const positionOptions = ref([])

// ==================== 从异常/响应体里取后端业务提示 ====================
const pickMsg = (res, e, fallback) => {
  if (res && !res.code && res.msg) return res.msg
  const bodyMsg = e && e.response && e.response.data && e.response.data.msg
  return bodyMsg || (e && e.message) || fallback
}

// ==================== 钩子 ====================
onMounted(() => {
  loadPositionOptions()
  search()
})

// ==================== 数据加载 ====================
const loadPositionOptions = async () => {
  try {
    const res = await positionPageApi({ page: 1, pageSize: 200 })
    if (res.code) {
      const records = res.data.records || []
      positionOptions.value = records.map((item) => ({ value: item.id, label: item.name }))
    }
  } catch (e) {
    console.warn('[报名审核] 岗位下拉加载失败', e)
  }
}

const search = async () => {
  loading.value = true
  const params = {
    positionId: searchForm.value.positionId === null ? undefined : searchForm.value.positionId,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await applyPageApi(params)
    if (res.code) {
      applyList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '报名列表查询失败')
    }
  } catch (e) {
    ElMessage.error('报名列表查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { positionId: null, status: 0 }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 报名详情 ====================
const detailVisible = ref(false)
const detailRow = ref({})
const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

// ==================== 状态机操作：录用 / 驳回 ====================
// 录用：0 待审核 → 1 已录用；名额已满时后端拒绝，前端提示"名额已满"
const approve = (row) => {
  ElMessageBox.confirm(
    `确认录用学生【${row.studentName || row.studentNo}】到岗位【${row.positionName}】吗？\n` +
      '录用会占用该岗位一个名额，名额已满时将无法录用。',
    '报名录用',
    { confirmButtonText: '确定录用', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    let res = null
    try {
      res = await auditApi({ id: row.id, status: 1 })
      if (res.code) {
        ElMessage.success('已录用')
        search()
        return
      }
      // 名额并发：后端 UPDATE ... WHERE hired < quota 影响行数为 0 时回滚报名并抛业务异常
      const msg = res.msg || ''
      ElMessage.error(msg.indexOf('名额') > -1 ? '名额已满，录用失败' : (msg || '录用失败'))
    } catch (e) {
      const msg = pickMsg(res, e, '录用失败')
      ElMessage.error(msg.indexOf('名额') > -1 ? '名额已满，录用失败' : msg)
    }
  }).catch(() => {})
}

// 驳回：0 待审核 → 2 已驳回（必须填写原因，学生端可见）
const reject = (row) => {
  ElMessageBox.prompt(
    `请填写驳回学生【${row.studentName || row.studentNo}】报名的原因`,
    '报名驳回',
    {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：课时冲突、岗位已招满、资料不符…',
      inputPattern: /^[\s\S]{1,255}$/,
      inputErrorMessage: '驳回原因不能为空，且不超过 255 字',
      type: 'warning'
    }
  ).then(async ({ value }) => {
    let res = null
    try {
      res = await auditApi({ id: row.id, status: 2, rejectReason: value })
      if (res.code) {
        ElMessage.success('已驳回')
        search()
      } else {
        ElMessage.error(res.msg || '驳回失败')
      }
    } catch (e) {
      ElMessage.error(pickMsg(res, e, '驳回失败'))
    }
  }).catch(() => {})
}
</script>

<template>
  <div class="apply-audit-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="岗位">
          <el-select v-model="searchForm.positionId" placeholder="全部岗位" clearable filterable style="width: 200px">
            <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <span class="audit-tip">录用将占用岗位名额，名额已满时无法录用</span>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="applyList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="岗位名称" min-width="150" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || ('岗位ID：' + scope.row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="学生姓名" min-width="100" align="center">
          <template #default="scope">{{ scope.row.studentName || '--' }}</template>
        </el-table-column>
        <el-table-column label="学号" min-width="120" align="center">
          <template #default="scope">{{ scope.row.studentNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="报名备注" min-width="160" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.applyRemark || '--' }}</template>
        </el-table-column>
        <el-table-column label="报名时间" width="170" align="center">
          <template #default="scope">{{ scope.row.createTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="170" align="center">
          <template #default="scope">{{ scope.row.auditTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <!-- 只有待审核才渲染审核动作 -->
            <template v-if="scope.row.status === 0">
              <el-button link type="success" @click="approve(scope.row)">录用</el-button>
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

    <!-- ==================== 报名详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="报名详情" width="620px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="岗位名称">{{ detailRow.positionName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detailRow.status)" size="small">{{ statusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ detailRow.studentName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detailRow.studentNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ detailRow.createTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailRow.auditTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="报名备注" :span="2">{{ detailRow.applyRemark || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.rejectReason" label="驳回原因" :span="2">
          <span class="reject-text">{{ detailRow.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="detailRow.status === 0"
            type="success"
            @click="detailVisible = false; approve(detailRow)"
          >录用</el-button>
          <el-button
            v-if="detailRow.status === 0"
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
