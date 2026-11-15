<!--
  岗位浏览报名（学生）
  ====================================================================
  路由：/apply/positions    可见角色：学生(1)
  契约依据：第 5 节 PositionController / PositionApplyController、第 6 节「名额并发 / 数据归属」

  数据归属：学生只浏览"已发布"岗位（GET /position/list 只返回已发布），
            并且只能查到自己（GET /apply/myPage）的报名状态，不能看到他人报名。
  名额规则（契约 6.5）：剩余名额 = quota - hired；满员（≤0）时置灰且不可报名。
  已报名标记：用我的报名列表回填，避免逐个岗位调用 /apply/check/{positionId}。

  接口对齐：GET /position/list  GET /apply/myPage  POST /apply（positionId,applyRemark）
  ====================================================================
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listApi as positionListApi } from '@/api/position'
import { listApi as dictListApi } from '@/api/dict'
import { myPageApi, applyApi } from '@/api/apply'

// ==================== 报名状态机（契约 3 状态码，学生视角） ====================
// 0 待审核 →（1 已录用 / 2 已驳回）；0 → 3 已撤销；1 → 4 已放弃
const APPLY_STATUS_MAP = {
  0: { text: '已报名', type: 'warning' },
  1: { text: '已录用', type: 'success' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已撤销', type: 'info' },
  4: { text: '已放弃', type: 'info' }
}
const applyStatusText = (status) =>
  APPLY_STATUS_MAP[status] ? APPLY_STATUS_MAP[status].text : '已报名'
const applyStatusType = (status) =>
  APPLY_STATUS_MAP[status] ? APPLY_STATUS_MAP[status].type : 'info'

// ==================== 搜索条件（列表接口无分页，前端按条件过滤 + 前端分页） ====================
const searchForm = ref({ name: '', deptId: null, category: null })

// ==================== 已发布岗位数据 ====================
const positions = ref([])
const loading = ref(false)

// ==================== 我的报名（用于"已报名"标记，只涉及本人数据） ====================
const myApplies = ref([])

// ==================== 前端分页 ====================
const currentPage = ref(1)
const pageSize = ref(9)

// ==================== 岗位类别字典（dict_code = position_category） ====================
const categoryOptions = ref([])

// ==================== 钩子 ====================
onMounted(() => {
  loadDictList()
  loadMyApplies()
  loadPositions()
})

// ==================== 数据加载 ====================
const loadPositions = async () => {
  loading.value = true
  try {
    const res = await positionListApi()
    if (res.code) {
      // /position/list 返回已发布岗位集合；兼容返回 PageResult 的情况
      const data = res.data
      positions.value = Array.isArray(data) ? data : (data && data.records) || []
    } else {
      ElMessage.error(res.msg || '岗位列表加载失败')
    }
  } catch (e) {
    ElMessage.error('岗位列表加载失败')
  } finally {
    loading.value = false
  }
}

const loadMyApplies = async () => {
  try {
    const res = await myPageApi({ page: 1, pageSize: 200 })
    if (res.code) {
      myApplies.value = res.data.records || []
    }
  } catch (e) {
    console.warn('[岗位浏览报名] 我的报名加载失败，已报名标记可能不完整', e)
  }
}

const loadDictList = async () => {
  try {
    const res = await dictListApi('position_category')
    if (res.code) {
      categoryOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('[岗位浏览报名] 岗位类别字典加载失败', e)
  }
}

// ==================== 计算属性 ====================
// 岗位 id → 我的报名记录（含状态）
const appliedMap = computed(() => {
  const map = {}
  myApplies.value.forEach((item) => {
    map[item.positionId] = item
  })
  return map
})

// 部门下拉：直接从已发布岗位里提取（学生无权限读 /dept/list）
const deptOptions = computed(() => {
  const map = new Map()
  positions.value.forEach((item) => {
    if (item.deptId !== null && item.deptId !== undefined) {
      map.set(item.deptId, item.deptName || '部门ID：' + item.deptId)
    }
  })
  return Array.from(map, ([value, label]) => ({ value, label }))
})

// 剩余名额 = quota - hired（契约 6.5）
const remainQuota = (row) => {
  if (row.remainQuota !== null && row.remainQuota !== undefined) {
    return row.remainQuota
  }
  return (row.quota || 0) - (row.hired || 0)
}
const isFull = (row) => remainQuota(row) <= 0
const hasApplied = (row) => !!appliedMap.value[row.id]
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)
const categoryLabel = (row) => row.categoryName || row.category || '--'

// 过滤后的岗位列表（名称 / 部门 / 类别）
const filteredList = computed(() => {
  return positions.value.filter((item) => {
    const nameOk =
      !searchForm.value.name || (item.name || '').indexOf(searchForm.value.name) > -1
    const deptOk =
      searchForm.value.deptId === null ||
      searchForm.value.deptId === undefined ||
      item.deptId === searchForm.value.deptId
    const categoryOk =
      !searchForm.value.category || item.category === searchForm.value.category
    return nameOk && deptOk && categoryOk
  })
})

const total = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// 满员行整行置灰
const rowClassName = ({ row }) => (isFull(row) ? 'full-row' : '')

// ==================== 查询 / 重置 / 分页 ====================
const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchForm.value = { name: '', deptId: null, category: null }
  currentPage.value = 1
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const refresh = () => {
  loadMyApplies()
  loadPositions()
}

// ==================== 岗位详情 ====================
const detailVisible = ref(false)
const detailRow = ref({})
const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

// ==================== 报名（填备注） ====================
const applyVisible = ref(false)
const applyForm = reactive({ positionId: null, positionName: '', applyRemark: '' })
const applySubmitting = ref(false)

const openApply = (row) => {
  applyForm.positionId = row.id
  applyForm.positionName = row.name
  applyForm.applyRemark = ''
  applyVisible.value = true
}

const submitApply = async () => {
  if (!applyForm.positionId) return
  applySubmitting.value = true
  try {
    // POST /apply?positionId=&applyRemark=（后端用 @RequestParam 接收）
    const res = await applyApi(applyForm.positionId, applyForm.applyRemark || '')
    if (res.code) {
      ElMessage.success('报名成功，等待部门/中心审核')
      applyVisible.value = false
      refresh()
    } else {
      ElMessage.error(res.msg || '报名失败')
    }
  } catch (e) {
    // 业务异常（如重复报名、名额已满）由后端返回，兜底从响应体取 msg
    const msg =
      (e && e.response && e.response.data && e.response.data.msg) || (e && e.message) || '报名失败'
    ElMessage.error(msg)
  } finally {
    applySubmitting.value = false
  }
}
</script>

<template>
  <div class="apply-positions-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="岗位名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入岗位名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用工部门">
          <el-select v-model="searchForm.deptId" placeholder="全部部门" clearable style="width: 170px">
            <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别">
          <el-select v-model="searchForm.category" placeholder="全部类别" clearable style="width: 150px">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.itemText"
              :value="item.itemValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <!-- ==================== 已发布岗位卡片式表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="pagedList" :row-class-name="rowClassName" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="岗位名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="用工部门" min-width="120" align="center">
          <template #default="scope">{{ scope.row.deptName || '--' }}</template>
        </el-table-column>
        <el-table-column label="岗位类别" min-width="110" align="center">
          <template #default="scope">{{ categoryLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="剩余名额" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="isFull(scope.row)" type="danger" size="small">已满员</el-tag>
            <span v-else>{{ remainQuota(scope.row) }} / {{ scope.row.quota }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时薪(元)" width="100" align="center">
          <template #default="scope">{{ formatMoney(scope.row.hourlyWage) }}</template>
        </el-table-column>
        <el-table-column label="工作地点" min-width="120" align="center">
          <template #default="scope">{{ scope.row.workLocation || '--' }}</template>
        </el-table-column>
        <el-table-column label="报名状态" width="110" align="center">
          <template #default="scope">
            <el-tag
              v-if="hasApplied(scope.row)"
              :type="applyStatusType(appliedMap[scope.row.id].status)"
              size="small"
            >{{ applyStatusText(appliedMap[scope.row.id].status) }}</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看详情</el-button>
            <!-- 已报名：不再提供报名入口；满员：置灰不可点 -->
            <el-button
              v-if="hasApplied(scope.row)"
              link
              type="info"
              disabled
            >已报名</el-button>
            <el-button
              v-else-if="isFull(scope.row)"
              link
              type="info"
              disabled
            >名额已满</el-button>
            <el-button v-else link type="success" @click="openApply(scope.row)">报名</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 前端分页条（/position/list 无分页，前端切页） -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[9, 18, 30, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- ==================== 岗位详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="岗位详情" width="660px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="岗位名称">{{ detailRow.name || '--' }}</el-descriptions-item>
        <el-descriptions-item label="用工部门">{{ detailRow.deptName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="岗位类别">{{ categoryLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="剩余名额">
          <el-tag v-if="isFull(detailRow)" type="danger" size="small">已满员</el-tag>
          <span v-else>{{ remainQuota(detailRow) }} / {{ detailRow.quota }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="时薪(元)">{{ formatMoney(detailRow.hourlyWage) }}</el-descriptions-item>
        <el-descriptions-item label="工作地点">{{ detailRow.workLocation || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailRow.contactPerson || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailRow.contactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="工作内容" :span="2">{{ detailRow.workContent || '--' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="!hasApplied(detailRow) && !isFull(detailRow) && detailRow.id"
            type="success"
            @click="detailVisible = false; openApply(detailRow)"
          >我要报名</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- ==================== 报名弹窗（填备注） ==================== -->
    <el-dialog v-model="applyVisible" title="岗位报名" width="520px">
      <el-form :model="applyForm" label-width="90px">
        <el-form-item label="岗位名称">
          <el-input v-model="applyForm.positionName" disabled />
        </el-form-item>
        <el-form-item label="报名备注">
          <el-input
            v-model="applyForm.applyRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="可填写个人优势、空闲时间等，便于部门审核"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="applyVisible = false">取消</el-button>
          <el-button type="primary" :loading="applySubmitting" @click="submitApply">确认报名</el-button>
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

/* 满员岗位整行置灰 */
:deep(.el-table .full-row) {
  color: #c0c4cc;
  background-color: #fafafa;
}

:deep(.el-table .full-row td) {
  color: #c0c4cc;
}
</style>
