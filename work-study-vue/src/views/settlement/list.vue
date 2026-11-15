<!--
  结算单管理
  ====================================================================
  路由：/settlement/list    可见角色：超级管理员(0)、勤工助学中心管理员(3)、财务(4)
  契约依据：第 5 节 SettlementController（/settlement）、第 6 节⑦「结算对账」

  状态机（契约第 3 节）：0 待确认 →(中心确认) 1 已确认 →(财务发放) 2 已发放（锁定）；0/1 →(作废) 3 已作废。
  按钮按「角色 + 状态」呈现（与后端权限一致）：
    · 生成批次 / 确认 / 作废 / 调整：超级管理员(0)、中心管理员(3)（后端 checkManagePermission）
    · 发放：财务(4)、超级管理员(0)（后端 isFinance || isSuperAdmin）
    · 查看明细：所有可见角色；导出 xlsx：已发放(2)
  生成批次幂等：同「学期 + 月份 + 部门」已存在未作废批次时后端拒绝，前端原样提示后端 msg。

  接口对齐：POST /settlement/generate  GET /settlement/page  GET /settlement/{id}
            GET /settlement/detail/{settlementId}  POST /settlement/confirm/{id}
            POST /settlement/pay/{id}  POST /settlement/void/{id}  POST /settlement/adjust
            GET /settlement/export/{id}（POI xlsx，responseType: blob，前端按批次号.xlsx 下载）
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageApi,
  generateApi,
  detailApi,
  confirmApi,
  payApi,
  voidApi,
  adjustApi,
  exportApi
} from '@/api/settlement'
import { listApi as semesterListApi } from '@/api/semester'
import { listApi as deptListApi } from '@/api/dept'
import { ROLE } from '@/utils/role'
import { getLoginUser } from '@/router'

// ==================== 当前登录人（决定按钮呈现） ====================
const loginUser = getLoginUser() || {}
const role = Number(loginUser.role)
// 生成 / 确认 / 作废 / 调整：中心管理员 + 超级管理员
const isManager = role === ROLE.SUPER_ADMIN || role === ROLE.CENTER_ADMIN
// 发放：财务 + 超级管理员
const isFinance = role === ROLE.FINANCE || role === ROLE.SUPER_ADMIN

// ==================== 结算单状态机（契约 3 状态码） ====================
const STATUS_MAP = {
  0: { text: '待确认', type: 'warning' },
  1: { text: '已确认', type: 'primary' },
  2: { text: '已发放', type: 'success' },
  3: { text: '已作废', type: 'info' }
}
const STATUS_OPTIONS = Object.keys(STATUS_MAP).map((key) => ({
  value: Number(key),
  label: STATUS_MAP[key].text
}))
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// ==================== 搜索条件（SettlementPageQueryDTO{semesterId,month,deptId,status,page,pageSize}） ====================
const searchForm = ref({ semesterId: null, month: '', deptId: null, status: null })

// ==================== 列表数据 ====================
const settlementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 下拉辅助数据（加载失败不阻塞主流程） ====================
const semesterList = ref([])
const deptList = ref([])
const semesterMap = computed(() => {
  const map = {}
  semesterList.value.forEach((item) => {
    map[item.id] = item.name
  })
  return map
})
const deptMap = computed(() => {
  const map = {}
  deptList.value.forEach((item) => {
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
    : row.deptName || deptMap.value[row.deptId] || '部门ID：' + row.deptId

// ==================== 钩子函数 ====================
onMounted(() => {
  loadSemesterList()
  loadDeptList()
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  loading.value = true
  const params = {
    semesterId: searchForm.value.semesterId === null ? undefined : searchForm.value.semesterId,
    month: searchForm.value.month || undefined,
    deptId: searchForm.value.deptId === null ? undefined : searchForm.value.deptId,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      settlementList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '结算单查询失败')
    }
  } catch (e) {
    ElMessage.error('结算单查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { semesterId: null, month: '', deptId: null, status: null }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 下拉数据 ====================
const loadSemesterList = async () => {
  try {
    const res = await semesterListApi()
    if (res.code) {
      semesterList.value = res.data || []
    }
  } catch (e) {
    console.warn('[结算单管理] 学期下拉加载失败', e)
  }
}

const loadDeptList = async () => {
  try {
    const res = await deptListApi()
    if (res.code) {
      deptList.value = res.data || []
    }
  } catch (e) {
    console.warn('[结算单管理] 部门下拉加载失败', e)
  }
}

// ==================== 生成批次（幂等：重复生成由后端拒绝，这里原样提示 msg） ====================
const generateVisible = ref(false)
const generateFormRef = ref()
const generateForm = ref({ semesterId: null, month: '', deptId: null })
const generating = ref(false)

const generateRules = ref({
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  month: [{ required: true, message: '请选择结算月份', trigger: 'change' }]
})

const openGenerate = () => {
  const current = semesterList.value.find((item) => item.status === 1)
  generateForm.value = { semesterId: current ? current.id : null, month: '', deptId: null }
  generateVisible.value = true
  if (generateFormRef.value) {
    generateFormRef.value.clearValidate()
  }
}

const submitGenerate = () => {
  if (!generateFormRef.value) return
  generateFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    generating.value = true
    // SettlementGenerateDTO{semesterId,month,deptId}；deptId 为 null 表示全校
    const payload = {
      semesterId: generateForm.value.semesterId,
      month: generateForm.value.month,
      deptId: generateForm.value.deptId === null ? undefined : generateForm.value.deptId
    }
    try {
      const res = await generateApi(payload)
      if (res.code) {
        const batchNo = res.data && res.data.batchNo ? res.data.batchNo : ''
        ElMessage.success(`结算批次生成成功${batchNo ? '：' + batchNo : ''}`)
        generateVisible.value = false
        currentPage.value = 1
        search()
      } else {
        // 重复生成 / 无可用工时等业务拒绝，直接展示后端中文说明
        ElMessage.error(res.msg || '生成失败')
      }
    } catch (e) {
      ElMessage.error('生成失败')
    } finally {
      generating.value = false
    }
  })
}

// ==================== 明细弹窗（含合计行 + 对账校验） ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
// 明细是否已加载完成（加载中不输出对账结论，避免瞬时误报）
const detailLoaded = ref(false)
const detailRow = ref({})
const detailList = ref([])
const detailSum = ref({ hours: 0, amount: 0 })

// 对账结论：明细合计必须等于结算单总额/总工时（契约 6.7）。
// 注意：作废(3)时后端会解绑工时并删除明细（释放 work_hour_id 唯一键），因此不作对账、改为说明性提示。
const reconcileInfo = computed(() => {
  if (detailRow.value.status === 3) {
    return {
      type: 'info',
      title: '该批次已作废',
      text: '作废时已解绑其下工时并释放明细，故此处没有明细数据；相关工时已回到可结算状态，可重新生成批次。'
    }
  }
  const ok =
    Number(detailSum.value.hours.toFixed(2)) === Number(Number(detailRow.value.totalHours || 0).toFixed(2)) &&
    Number(detailSum.value.amount.toFixed(2)) === Number(Number(detailRow.value.totalAmount || 0).toFixed(2))
  return {
    type: ok ? 'success' : 'error',
    title: ok ? '对账一致' : '对账不一致，请核对数据',
    text:
      '明细合计：' +
      formatHours(detailSum.value.hours) +
      ' 小时 / ' +
      formatMoney(detailSum.value.amount) +
      ' 元；结算单：' +
      formatHours(detailRow.value.totalHours) +
      ' 小时 / ' +
      formatMoney(detailRow.value.totalAmount) +
      ' 元。'
  }
})

const openDetail = async (row) => {
  detailRow.value = { ...row }
  detailList.value = []
  detailSum.value = { hours: 0, amount: 0 }
  detailLoaded.value = false
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await detailApi(row.id)
    if (res.code) {
      detailList.value = res.data || []
      // 合计 + 与结算单总额对账（契约 6.7：∑明细 必须等于 settlement.total_amount / total_hours）
      const sumHours = detailList.value.reduce((acc, item) => acc + Number(item.hours || 0), 0)
      const sumAmount = detailList.value.reduce((acc, item) => acc + Number(item.amount || 0), 0)
      detailSum.value = { hours: sumHours, amount: sumAmount }
    } else {
      ElMessage.error(res.msg || '结算明细查询失败')
    }
  } catch (e) {
    ElMessage.error('结算明细查询失败')
  } finally {
    detailLoading.value = false
    detailLoaded.value = true
  }
}

// el-table 合计行：工时、金额两列求和（保留两位小数）
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

// ==================== 状态机操作：确认 / 发放 / 作废 ====================
// 确认：0 待确认 → 1 已确认（中心 + 超管）
const confirmSettlement = (row) => {
  ElMessageBox.confirm(
    `确认结算单【${row.batchNo}】吗？确认后进入待发放状态（1 已确认）。`,
    '确认结算单',
    { confirmButtonText: '确定确认', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await confirmApi(row.id)
      if (res.code) {
        ElMessage.success('确认成功，等待财务发放')
        search()
      } else {
        ElMessage.error(res.msg || '确认失败')
      }
    } catch (e) {
      ElMessage.error('确认失败')
    }
  }).catch(() => {})
}

// 发放：1 已确认 → 2 已发放（财务 + 超管，发放后锁定不可改）
const paySettlement = (row) => {
  ElMessageBox.confirm(
    `确认发放结算单【${row.batchNo}】吗？总金额 ${formatMoney(row.totalAmount)} 元。\n` +
      '发放后该批次工时与结算单将锁定，不可再调整。',
    '发放结算单',
    { confirmButtonText: '确定发放', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await payApi(row.id)
      if (res.code) {
        ElMessage.success('发放成功')
        search()
      } else {
        ElMessage.error(res.msg || '发放失败')
      }
    } catch (e) {
      ElMessage.error('发放失败')
    }
  }).catch(() => {})
}

// 作废：0/1 → 3 已作废（工时解绑，可重新生成）
const voidSettlement = (row) => {
  ElMessageBox.confirm(
    `确认作废结算单【${row.batchNo}】吗？作废后其下工时会解绑并可重新生成结算。`,
    '作废结算单',
    { confirmButtonText: '确定作废', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await voidApi(row.id)
      if (res.code) {
        ElMessage.success('已作废')
        search()
      } else {
        ElMessage.error(res.msg || '作废失败')
      }
    } catch (e) {
      ElMessage.error('作废失败')
    }
  }).catch(() => {})
}

// ==================== 结算调整（补发 / 扣减） ====================
const adjustVisible = ref(false)
const adjustFormRef = ref()
const adjustForm = ref({ settlementId: null, studentId: null, adjustType: 1, amount: null, reason: '' })
const adjustRow = ref({})
// 学生下拉：取自该批次明细里去重后的学生（不额外拉全量学生）
const adjustStudentOptions = ref([])

const adjustRules = ref({
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  adjustType: [{ required: true, message: '请选择调整类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写调整原因', trigger: 'blur' }]
})

const openAdjust = async (row) => {
  adjustRow.value = { ...row }
  adjustForm.value = { settlementId: row.id, studentId: null, adjustType: 1, amount: null, reason: '' }
  adjustStudentOptions.value = []
  adjustVisible.value = true
  if (adjustFormRef.value) {
    adjustFormRef.value.clearValidate()
  }
  try {
    const res = await detailApi(row.id)
    if (res.code) {
      const seen = {}
      const options = []
      ;(res.data || []).forEach((item) => {
        if (item.studentId === null || item.studentId === undefined) return
        if (seen[item.studentId]) return
        seen[item.studentId] = true
        options.push({
          value: item.studentId,
          label: (item.studentName || '未知学生') + (item.studentNo ? '（' + item.studentNo + '）' : '')
        })
      })
      adjustStudentOptions.value = options
    }
  } catch (e) {
    console.warn('[结算单管理] 调整弹窗学生下拉加载失败', e)
  }
}

const submitAdjust = () => {
  if (!adjustFormRef.value) return
  adjustFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    if (!(Number(adjustForm.value.amount) > 0)) {
      ElMessage.error('调整金额必须大于 0')
      return
    }
    // SettlementAdjustDTO{settlementId,studentId,adjustType,amount,reason}（1 补发 / 2 扣减）
    try {
      const res = await adjustApi({
        settlementId: adjustForm.value.settlementId,
        studentId: adjustForm.value.studentId,
        adjustType: adjustForm.value.adjustType,
        amount: adjustForm.value.amount,
        reason: adjustForm.value.reason
      })
      if (res.code) {
        ElMessage.success('调整成功')
        adjustVisible.value = false
        search()
      } else {
        ElMessage.error(res.msg || '调整失败')
      }
    } catch (e) {
      ElMessage.error('调整失败')
    }
  })
}

// ==================== 导出（blob 下载，文件名 = 批次号.xlsx） ====================
const exporting = ref(false)

const exportSettlement = async (row) => {
  exporting.value = true
  try {
    const res = await exportApi(row.id)
    // responseType 为 blob：成功时拿到文件流；业务失败时后端返回 JSON 也会被包成 Blob，这里解析提示
    if (!(res instanceof Blob)) {
      ElMessage.error((res && res.msg) || '导出失败')
      return
    }
    if (res.type && res.type.indexOf('json') > -1) {
      const text = await res.text()
      let msg = '导出失败'
      try {
        msg = JSON.parse(text).msg || msg
      } catch (e) {
        msg = '导出失败'
      }
      ElMessage.error(msg)
      return
    }
    const url = window.URL.createObjectURL(res)
    const link = document.createElement('a')
    link.href = url
    link.download = (row.batchNo || '结算单') + '.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="settlement-list-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="学期">
          <el-select v-model="searchForm.semesterId" placeholder="全部学期" clearable style="width: 190px">
            <el-option
              v-for="item in semesterList"
              :key="item.id"
              :label="item.name + (item.status === 1 ? '（当前学期）' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker
            v-model="searchForm.month"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="searchForm.deptId" placeholder="全部（含全校）" clearable style="width: 180px">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <!-- 生成批次：仅中心管理员 / 超级管理员 -->
      <el-button v-if="isManager" type="warning" @click="openGenerate">+ 生成结算批次</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="settlementList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="batchNo" label="批次号" min-width="170" align="center" show-overflow-tooltip />
        <el-table-column label="学期" min-width="140" align="center">
          <template #default="scope">{{ semesterLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column prop="month" label="月份" width="100" align="center" />
        <el-table-column label="部门" min-width="120" align="center">
          <template #default="scope">{{ deptLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="总工时(小时)" width="120" align="right">
          <template #default="scope">{{ formatHours(scope.row.totalHours) }}</template>
        </el-table-column>
        <el-table-column label="总金额(元)" width="130" align="right">
          <template #default="scope">{{ formatMoney(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="明细条数" width="100" align="center">
          <template #default="scope">{{ scope.row.detailCount ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看明细</el-button>
            <!-- 0 待确认：确认 / 作废 / 调整（中心 + 超管） -->
            <template v-if="isManager && scope.row.status === 0">
              <el-button link type="success" @click="confirmSettlement(scope.row)">确认</el-button>
              <el-button link type="warning" @click="openAdjust(scope.row)">调整</el-button>
              <el-button link type="danger" @click="voidSettlement(scope.row)">作废</el-button>
            </template>
            <!-- 1 已确认：财务发放 / 中心作废 / 调整 -->
            <template v-else-if="scope.row.status === 1">
              <el-button v-if="isFinance" link type="success" @click="paySettlement(scope.row)">发放</el-button>
              <template v-if="isManager">
                <el-button link type="warning" @click="openAdjust(scope.row)">调整</el-button>
                <el-button link type="danger" @click="voidSettlement(scope.row)">作废</el-button>
              </template>
            </template>
            <!-- 2 已发放：导出（锁定，不可再改） -->
            <template v-else-if="scope.row.status === 2">
              <el-button link type="primary" :loading="exporting" @click="exportSettlement(scope.row)">导出</el-button>
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

    <!-- ==================== 生成批次弹窗 ==================== -->
    <el-dialog v-model="generateVisible" title="生成结算批次" width="560px">
      <el-form :model="generateForm" :rules="generateRules" ref="generateFormRef" label-width="100px">
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="generateForm.semesterId" placeholder="请选择学期" style="width: 100%">
            <el-option
              v-for="item in semesterList"
              :key="item.id"
              :label="item.name + (item.status === 1 ? '（当前学期）' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="结算月份" prop="month">
          <el-date-picker
            v-model="generateForm.month"
            type="month"
            placeholder="请选择月份，如 2026-10"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="用工部门">
          <!-- 留空 = 全校 -->
          <el-select v-model="generateForm.deptId" placeholder="留空表示全校" clearable style="width: 100%">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="只汇总该学期、该月份内「审核通过且未结算」的工时；同一「学期 + 月份 + 部门」重复生成会被拒绝（幂等）。"
        />
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- ==================== 明细弹窗（合计行 + 对账） ==================== -->
    <el-dialog v-model="detailVisible" title="结算明细" width="900px">
      <el-descriptions :column="3" border size="small" class="detail-meta">
        <el-descriptions-item label="批次号">{{ detailRow.batchNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="学期">{{ semesterLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="月份">{{ detailRow.month || '--' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ deptLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="总工时">{{ formatHours(detailRow.totalHours) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ formatMoney(detailRow.totalAmount) }}</el-descriptions-item>
      </el-descriptions>

      <!-- 对账结论：明细合计 与 结算单总额/总工时 一致（作废批次改为说明性提示） -->
      <el-alert
        v-if="detailLoaded"
        class="reconcile-tip"
        :type="reconcileInfo.type"
        :closable="false"
        show-icon
        :title="reconcileInfo.title"
      >
        <template #default>{{ reconcileInfo.text }}</template>
      </el-alert>

      <el-table
        v-loading="detailLoading"
        :data="detailList"
        style="width: 100%"
        show-summary
        :summary-method="summaryMethod"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="学生" min-width="110" align="center">
          <template #default="scope">{{ scope.row.studentName || '--' }}</template>
        </el-table-column>
        <el-table-column label="学号" min-width="120" align="center">
          <template #default="scope">{{ scope.row.studentNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="130" align="center" show-overflow-tooltip>
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
          <el-button
            v-if="isManager && detailRow.status === 0"
            type="success"
            @click="detailVisible = false; confirmSettlement(detailRow)"
          >确认</el-button>
          <el-button
            v-if="isFinance && detailRow.status === 1"
            type="success"
            @click="detailVisible = false; paySettlement(detailRow)"
          >发放</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- ==================== 调整弹窗（补发 / 扣减） ==================== -->
    <el-dialog v-model="adjustVisible" title="结算调整" width="560px">
      <el-form :model="adjustForm" :rules="adjustRules" ref="adjustFormRef" label-width="100px">
        <el-form-item label="批次号">
          <span>{{ adjustRow.batchNo || '--' }}</span>
          <span class="field-tip">当前总金额：{{ formatMoney(adjustRow.totalAmount) }} 元</span>
        </el-form-item>
        <el-form-item label="学生" prop="studentId">
          <el-select v-model="adjustForm.studentId" placeholder="请选择学生" filterable style="width: 100%">
            <el-option
              v-for="item in adjustStudentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整类型" prop="adjustType">
          <el-radio-group v-model="adjustForm.adjustType">
            <el-radio :value="1">补发</el-radio>
            <el-radio :value="2">扣减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="adjustForm.amount" :min="0.01" :max="999999" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="例如：上月漏记 2 小时工时，予以补发"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="adjustVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAdjust">保存</el-button>
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

/* 弹窗内的补充信息 */
.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.detail-meta {
  margin-bottom: 12px;
}

.reconcile-tip {
  margin-bottom: 12px;
}
</style>
