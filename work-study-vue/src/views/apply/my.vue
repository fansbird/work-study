<!--
  我的报名（学生）
  ====================================================================
  路由：/apply/my    可见角色：学生(1)
  契约依据：第 5 节 PositionApplyController、第 3/6 节「报名状态机 / 数据归属」

  数据归属：只查本人报名（GET /apply/myPage，后端按 BaseContext 的 studentId 过滤）。
  状态机：0 待审核 →（1 已录用 / 2 已驳回）；0 →(撤销) 3 已撤销；1 已录用 →(放弃录用) 4 已放弃。
  名额规则（契约 6.5）：放弃录用会触发 hired-1 释放名额，故二次确认时明确提示。

  接口对齐：GET /apply/myPage  POST /apply/cancel/{id}  POST /apply/giveUp/{id}
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myPageApi, cancelApi, giveUpApi } from '@/api/apply'

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
const searchForm = ref({ status: null })

// ==================== 列表数据 ====================
const applyList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 钩子 ====================
onMounted(() => {
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  loading.value = true
  const params = {
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await myPageApi(params)
    if (res.code) {
      applyList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '报名记录查询失败')
    }
  } catch (e) {
    ElMessage.error('报名记录查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { status: null }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 状态机操作 ====================
// 撤销报名：0 待审核 → 3 已撤销
const cancelApply = (row) => {
  ElMessageBox.confirm(
    `确认撤销对岗位【${row.positionName || row.positionId}】的报名吗？撤销后如需报名请重新提交。`,
    '提示',
    { confirmButtonText: '确定撤销', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await cancelApi(row.id)
      if (res.code) {
        ElMessage.success('已撤销报名')
        search()
      } else {
        ElMessage.error(res.msg || '撤销失败')
      }
    } catch (e) {
      ElMessage.error('撤销失败')
    }
  }).catch(() => {})
}

// 放弃录用：1 已录用 → 4 已放弃（后端同时把岗位 hired-1 释放名额）
const giveUp = (row) => {
  ElMessageBox.confirm(
    `确认放弃岗位【${row.positionName || row.positionId}】的录用资格吗？\n` +
      '放弃后系统会释放该岗位的一个录用名额，其他同学可再次被录用。',
    '放弃录用',
    { confirmButtonText: '确定放弃', cancelButtonText: '再想想', type: 'warning' }
  ).then(async () => {
    try {
      const res = await giveUpApi(row.id)
      if (res.code) {
        ElMessage.success('已放弃录用，名额已释放')
        search()
      } else {
        ElMessage.error(res.msg || '放弃录用失败')
      }
    } catch (e) {
      ElMessage.error('放弃录用失败')
    }
  }).catch(() => {})
}
</script>

<template>
  <div class="apply-my-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="报名状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="applyList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="岗位名称" min-width="160" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || ('岗位ID：' + scope.row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="报名备注" min-width="180" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.applyRemark || '--' }}</template>
        </el-table-column>
        <el-table-column label="报名时间" width="170" align="center">
          <template #default="scope">{{ scope.row.createTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="审核时间" width="170" align="center">
          <template #default="scope">{{ scope.row.auditTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="160" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.status === 2" class="reject-text">{{ scope.row.rejectReason || '--' }}</span>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <!-- 0 待审核：可撤销 -->
            <el-button
              v-if="scope.row.status === 0"
              link
              type="danger"
              @click="cancelApply(scope.row)"
            >撤销报名</el-button>
            <!-- 1 已录用：可放弃录用（释放名额） -->
            <el-button
              v-else-if="scope.row.status === 1"
              link
              type="warning"
              @click="giveUp(scope.row)"
            >放弃录用</el-button>
            <span v-else>--</span>
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
