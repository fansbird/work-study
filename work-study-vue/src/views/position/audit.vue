<!--
  岗位审批（勤工助学中心 / 超级管理员）
  ====================================================================
  路由：/position/audit    可见角色：超级管理员(0)、勤工助学中心管理员(3)
  契约依据：第 5 节 PositionController、第 6 节「状态机 / 数据归属」

  数据归属：中心管理员与超级管理员审批全校岗位（后端按角色放行，不额外带 deptId）。
  状态机：1 待审批 →(通过) 2 已发布 / (驳回) 3 已驳回；驳回必须填写原因，学生/部门端可见。

  接口对齐：GET /position/page（默认 status=1）  GET /position/{id}  POST /position/audit（id,status,rejectReason）
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, auditApi, getByIdApi } from '@/api/position'

// ==================== 审批视角的状态（中心只关心这三种） ====================
const STATUS_MAP = {
  1: { text: '待审批', type: 'warning' },
  2: { text: '已发布', type: 'success' },
  3: { text: '已驳回', type: 'danger' }
}
const STATUS_OPTIONS = [
  { value: 1, label: '待审批' },
  { value: 2, label: '已发布' },
  { value: 3, label: '已驳回' }
]
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// ==================== 搜索条件（默认只列待审批） ====================
const searchForm = ref({ name: '', status: 1 })

// ==================== 列表数据 ====================
const positionList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 展示格式化 ====================
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)
const categoryLabel = (row) => row.categoryName || row.category || '--'

// ==================== 钩子 ====================
onMounted(() => {
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  const params = {
    name: searchForm.value.name || undefined,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      positionList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '待审批岗位查询失败')
    }
  } catch (e) {
    ElMessage.error('待审批岗位查询失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { name: '', status: 1 }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 查看详情 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref({})

const openDetail = async (row) => {
  detailRow.value = { ...row }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getByIdApi(row.id)
    if (res.code) {
      detailRow.value = res.data || row
    }
  } catch (e) {
    console.warn('[岗位审批] 岗位详情加载失败', e)
  } finally {
    detailLoading.value = false
  }
}

// ==================== 状态机操作：通过 / 驳回 ====================
// 通过：1 待审批 → 2 已发布（通过后学生即可浏览报名）
const approve = (row) => {
  ElMessageBox.confirm(
    `确认通过岗位【${row.name}】的审批吗？通过后该岗位将发布，学生可以浏览并报名。`,
    '岗位审批',
    { confirmButtonText: '通过并发布', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await auditApi({ id: row.id, status: 2 })
      if (res.code) {
        ElMessage.success('已通过并发布')
        search()
      } else {
        ElMessage.error(res.msg || '审批失败')
      }
    } catch (e) {
      ElMessage.error('审批失败')
    }
  }).catch(() => {})
}

// 驳回：1 待审批 → 3 已驳回（必须填写驳回原因，部门端可见并可修改后重新提交）
const reject = (row) => {
  ElMessageBox.prompt(`请填写驳回岗位【${row.name}】的原因`, '岗位驳回', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '例如：岗位工作内容描述不清、时薪与类别不符…',
    inputPattern: /^[\s\S]{1,255}$/,
    inputErrorMessage: '驳回原因不能为空，且不超过 255 字',
    type: 'warning'
  }).then(async ({ value }) => {
    try {
      const res = await auditApi({ id: row.id, status: 3, rejectReason: value })
      if (res.code) {
        ElMessage.success('已驳回')
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
  <div class="position-audit-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="岗位名称">
          <el-input v-model="searchForm.name" placeholder="请输入岗位名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <span class="audit-tip">通过后岗位即刻发布；驳回必须填写原因</span>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="positionList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="岗位名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="用工部门" min-width="120" align="center">
          <template #default="scope">{{ scope.row.deptName || scope.row.deptId || '--' }}</template>
        </el-table-column>
        <el-table-column label="岗位类别" min-width="110" align="center">
          <template #default="scope">{{ categoryLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="名额(已录/总)" width="120" align="center">
          <template #default="scope">{{ scope.row.hired || 0 }} / {{ scope.row.quota }}</template>
        </el-table-column>
        <el-table-column label="时薪(元)" width="100" align="center">
          <template #default="scope">{{ formatMoney(scope.row.hourlyWage) }}</template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" min-width="100" align="center">
          <template #default="scope">{{ scope.row.contactPerson || '--' }}</template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" min-width="130" align="center">
          <template #default="scope">{{ scope.row.contactPhone || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看详情</el-button>
            <!-- 只有待审批才渲染审批动作 -->
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

    <!-- ==================== 岗位详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="岗位详情" width="680px">
      <el-descriptions v-loading="detailLoading" :column="2" border>
        <el-descriptions-item label="岗位名称">{{ detailRow.name || '--' }}</el-descriptions-item>
        <el-descriptions-item label="用工部门">{{ detailRow.deptName || detailRow.deptId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="岗位类别">{{ categoryLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detailRow.status)" size="small">{{ statusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="名额">{{ detailRow.hired || 0 }} / {{ detailRow.quota }}</el-descriptions-item>
        <el-descriptions-item label="时薪(元)">{{ formatMoney(detailRow.hourlyWage) }}</el-descriptions-item>
        <el-descriptions-item label="工作地点">{{ detailRow.workLocation || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailRow.contactPerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailRow.contactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detailRow.publishTime || '--' }}</el-descriptions-item>
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
          >通过并发布</el-button>
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
