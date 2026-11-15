<!--
  我的工时（学生填报）
  ====================================================================
  路由：/workhour/fill    可见角色：学生(1)
  契约依据：第 5 节 WorkHourController、第 6 节「工时规则 / 状态机 / 数据归属」

  数据归属：只填报/只查看本人工时（GET /workHour/myPage，后端按 BaseContext 的 studentId 过滤）；
            可选岗位只来自本人"已录用"的报名（GET /apply/myPage?status=1）。
  状态机：POST /workHour 填报后直接进入 0 待部门审核 → 1 待中心复核 → 2 通过（锁定）；
          任一环节驳回 → 3 驳回重填，修改后 POST /workHour/submit/{id} 回到 0。
  工时规则（契约 6.6，前端只做基础提示，最终以后端校验为准）：
    · 结束时间必须晚于开始时间（半小时为最小单位）；
    · 不能填报未来日期；
    · 后端另有"同一学生同日时段不重叠 / 学期区间 / 日周月上限"等校验，前端不重复实现。

  接口对齐：POST /workHour  PUT /workHour  POST /workHour/submit/{id}  GET /workHour/myPage
  ====================================================================
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addApi, updateApi, submitApi, myPageApi } from '@/api/workhour'
import { myPageApi as applyMyPageApi } from '@/api/apply'

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

// ==================== 我"已录用"的岗位（填报时可选） ====================
const myPositions = ref([])

// ==================== 搜索条件（WorkHourPageQueryDTO{positionId,deptId,semesterId,status,studentId,startDate,endDate,page,pageSize}） ====================
const searchForm = ref({ positionId: null, status: null, dateRange: [] })

// ==================== 列表数据 ====================
const workhourList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 填报表单 ====================
// 0 待部门审核 →(部门通过) 1 待中心复核 →(中心通过) 2 通过锁定；任一驳回 → 3 驳回重填
const fillFormRef = ref()
const editingId = ref(null)
const submitting = ref(false)

const form = reactive({
  positionId: null,
  workDate: '',
  // el-time-picker(is-range) + value-format 后为 ['HH:mm:ss','HH:mm:ss']
  timeRange: [],
  workContent: ''
})

const rules = ref({
  positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择工作日期', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择起止时间', trigger: 'change' }],
  workContent: [{ required: true, message: '请填写工作内容', trigger: 'blur' }]
})

// 不能选择未来日期（契约 6.6 work_date <= 今天）
const disabledFutureDate = (time) => {
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  return time.getTime() > endOfToday.getTime()
}

// 时段 → 秒
const toSeconds = (text) => {
  if (!text) return null
  const parts = String(text).split(':')
  if (parts.length < 2) return null
  const hour = Number(parts[0])
  const minute = Number(parts[1])
  const second = parts.length > 2 ? Number(parts[2]) : 0
  if (Number.isNaN(hour) || Number.isNaN(minute) || Number.isNaN(second)) return null
  return hour * 3600 + minute * 60 + second
}

// 前端预览时长（后端会按半小时最小单位重新计算并校验，前端只做提示）
const previewHours = computed(() => {
  const range = form.timeRange
  if (!range || range.length !== 2) return '--'
  const start = toSeconds(range[0])
  const end = toSeconds(range[1])
  if (start === null || end === null || end <= start) return '--'
  return ((end - start) / 3600).toFixed(2)
})

// ==================== 钩子 ====================
onMounted(() => {
  loadMyPositions()
  search()
})

// ==================== 数据加载 ====================
// 只有"已录用"（status=1）的报名对应岗位才能填报工时
const loadMyPositions = async () => {
  try {
    const res = await applyMyPageApi({ status: 1, page: 1, pageSize: 200 })
    if (res.code) {
      const records = res.data.records || []
      myPositions.value = records.map((item) => ({
        positionId: item.positionId,
        positionName: item.positionName || '岗位ID：' + item.positionId
      }))
    }
  } catch (e) {
    console.warn('[我的工时] 已录用岗位加载失败', e)
  }
}

const search = async () => {
  loading.value = true
  const range = searchForm.value.dateRange || []
  const params = {
    positionId: searchForm.value.positionId === null ? undefined : searchForm.value.positionId,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    startDate: range[0] || undefined,
    endDate: range[1] || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await myPageApi(params)
    if (res.code) {
      workhourList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '工时记录查询失败')
    }
  } catch (e) {
    ElMessage.error('工时记录查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { positionId: null, status: null, dateRange: [] }
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

// ==================== 填报 / 提交 ====================
const resetForm = () => {
  editingId.value = null
  form.positionId = null
  form.workDate = ''
  form.timeRange = []
  form.workContent = ''
  if (fillFormRef.value) {
    fillFormRef.value.clearValidate()
  }
}

const cancelEdit = () => {
  resetForm()
  ElMessage.info('已取消编辑')
}

const buildPayload = () => ({
  positionId: form.positionId,
  workDate: form.workDate,
  startTime: form.timeRange && form.timeRange[0],
  endTime: form.timeRange && form.timeRange[1],
  hours: previewHours.value === '--' ? null : Number(previewHours.value),
  workContent: form.workContent
})

const save = () => {
  if (!fillFormRef.value) return
  fillFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请完善填报信息')
      return
    }
    // 前端基础校验①：必须选择完整的起止时间
    if (!form.timeRange || form.timeRange.length !== 2 || !form.timeRange[0] || !form.timeRange[1]) {
      ElMessage.warning('请选择完整的开始与结束时间')
      return
    }
    // 前端基础校验②：结束时间必须晚于开始时间
    const start = toSeconds(form.timeRange[0])
    const end = toSeconds(form.timeRange[1])
    if (start === null || end === null || end <= start) {
      ElMessage.warning('结束时间必须晚于开始时间')
      return
    }
    // 前端基础校验③：不能填报未来日期
    const today = new Date()
    today.setHours(23, 59, 59, 999)
    if (new Date(form.workDate + 'T00:00:00').getTime() > today.getTime()) {
      ElMessage.warning('不能填报未来日期')
      return
    }

    submitting.value = true
    try {
      const payload = buildPayload()
      if (editingId.value) {
        // 驳回重填：先改（PUT /workHour，仅 status=3 可改），再重新提交（3 → 0）
        const updateRes = await updateApi({ ...payload, id: editingId.value, status: 3 })
        if (!updateRes.code) {
          ElMessage.error(updateRes.msg || '修改失败')
          return
        }
        const submitRes = await submitApi(editingId.value)
        if (submitRes.code) {
          ElMessage.success('已修改并重新提交，等待部门审核')
          resetForm()
          search()
        } else {
          ElMessage.error(submitRes.msg || '重新提交失败')
        }
      } else {
        // 填报：POST /workHour，后端直接置 status=0（待部门审核）
        const res = await addApi({ ...payload, status: 0 })
        if (res.code) {
          ElMessage.success('填报成功，等待部门审核')
          resetForm()
          search()
        } else {
          ElMessage.error(res.msg || '填报失败')
        }
      }
    } catch (e) {
      const msg =
        (e && e.response && e.response.data && e.response.data.msg) || (e && e.message) || '填报失败'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

// ==================== 驳回重填：把记录回填到表单 ====================
const startEdit = (row) => {
  editingId.value = row.id
  form.positionId = row.positionId
  form.workDate = row.workDate
  form.timeRange = [row.startTime || '', row.endTime || '']
  form.workContent = row.workContent || ''
  if (fillFormRef.value) {
    fillFormRef.value.clearValidate()
  }
  ElMessageBox.alert(
    `该条工时被驳回，原因：${row.rejectReason || '（未填写）'}\n修改后点击"保存并重新提交"即可回到待部门审核。`,
    '驳回原因',
    { confirmButtonText: '知道了' }
  ).catch(() => {})
}
</script>

<template>
  <div class="workhour-fill-page">
    <!-- ==================== 填报卡片 ==================== -->
    <div class="page-card">
      <div class="card-title">
        <span>{{ editingId ? '修改并重新提交工时（驳回重填）' : '工时填报' }}</span>
        <el-tag v-if="editingId" type="danger" size="small">驳回重填中</el-tag>
      </div>
      <el-form ref="fillFormRef" :model="form" :rules="rules" label-width="90px" class="fill-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="岗位" prop="positionId">
              <el-select v-model="form.positionId" placeholder="请选择已录用岗位" style="width: 100%">
                <el-option
                  v-for="item in myPositions"
                  :key="item.positionId"
                  :label="item.positionName"
                  :value="item.positionId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作日期" prop="workDate">
              <!-- 禁止选择未来日期 -->
              <el-date-picker
                v-model="form.workDate"
                type="date"
                placeholder="请选择工作日期"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledFutureDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作时间" prop="timeRange">
              <el-time-picker
                v-model="form.timeRange"
                is-range
                value-format="HH:mm:ss"
                format="HH:mm"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="工作内容" prop="workContent">
              <el-input
                v-model="form.workContent"
                type="textarea"
                :rows="2"
                maxlength="255"
                show-word-limit
                placeholder="请简要描述本次工作的具体内容"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计时长">
              <el-tag type="info" size="large">{{ previewHours }} 小时</el-tag>
              <span class="hour-tip">以半小时为最小单位，最终时长以后端核算为准</span>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-actions">
          <el-button type="primary" :loading="submitting" @click="save">
            {{ editingId ? '保存并重新提交' : '提交工时' }}
          </el-button>
          <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
          <el-button v-else @click="resetForm">重置</el-button>
        </div>
      </el-form>
    </div>

    <!-- ==================== 列表卡片 ==================== -->
    <div class="page-card">
      <el-form :inline="true" :model="searchForm" class="search-bar">
        <el-form-item label="岗位">
          <el-select v-model="searchForm.positionId" placeholder="全部岗位" clearable style="width: 180px">
            <el-option
              v-for="item in myPositions"
              :key="item.positionId"
              :label="item.positionName"
              :value="item.positionId"
            />
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
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="workhourList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="岗位名称" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.positionName || ('岗位ID：' + scope.row.positionId) }}</template>
        </el-table-column>
        <el-table-column label="工作日期" width="120" align="center">
          <template #default="scope">{{ scope.row.workDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="工作时段" width="150" align="center">
          <template #default="scope">
            {{ shortTime(scope.row.startTime) }} ~ {{ shortTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="时长(小时)" width="100" align="center">
          <template #default="scope">{{ scope.row.hours === null || scope.row.hours === undefined ? '--' : scope.row.hours }}</template>
        </el-table-column>
        <el-table-column label="工作内容" min-width="160" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.workContent || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="150" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.status === 3" class="reject-text">{{ scope.row.rejectReason || '--' }}</span>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <!-- 仅驳回重填（status=3）可编辑并重新提交；0/1 审核中、2 已锁定均不可改 -->
            <el-button
              v-if="scope.row.status === 3"
              link
              type="primary"
              @click="startEdit(scope.row)"
            >编辑重提</el-button>
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
.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid #ffb400;
}

.fill-form {
  padding-right: 10px;
}

.hour-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  line-height: 16px;
  margin-top: 4px;
}

.form-actions {
  padding-left: 90px;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  align-items: flex-start;
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
