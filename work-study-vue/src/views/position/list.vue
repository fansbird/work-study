<!--
  岗位管理
  ====================================================================
  路由：/position/list    可见角色：超级管理员(0)、用工部门管理员(2)、勤工助学中心管理员(3)
  契约依据：第 5 节 PositionController（/position）、第 6 节「状态机 / 名额并发 / 数据归属」、第 7 节前端契约

  数据归属（契约 6.10）：
    · 用工部门管理员：只看得到本部门岗位，新增/编辑时部门锁定为本部门（deptId 由后端按登录角色过滤）；
    · 中心管理员 / 超级管理员：可见全部部门岗位，并可按部门筛选。

  状态机（契约 3 状态码）：
    0 草稿 →(提交) 1 待审批 →（2 已发布 / 3 已驳回）；2 已发布 →(下架) 4 已下架；3 已驳回可编辑后再提交回 1。
    行内操作严格按状态渲染：草稿=编辑/提交/删除，待审批=等待，已发布=下架，已驳回=看原因/编辑/再提交。

  接口对齐：GET /position/page  POST /position  PUT /position  GET /position/{id}
            POST /position/submit/{id}  POST /position/offline/{id}  DELETE /position/{id}
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageApi,
  addApi,
  updateApi,
  getByIdApi,
  submitApi,
  offlineApi,
  deleteApi
} from '@/api/position'
import { listApi as deptListApi } from '@/api/dept'
import { listApi as dictListApi } from '@/api/dict'
import { listApi as semesterListApi } from '@/api/semester'
import { ROLE } from '@/utils/role'
import { getLoginUser } from '@/router'

// ==================== 当前登录人（决定数据归属与可操作范围） ====================
const loginUser = getLoginUser() || {}
const role = Number(loginUser.role)
// 部门管理员：只看本部门、部门锁定
const isDeptAdmin = role === ROLE.DEPT_ADMIN
// 中心 / 超管：可按部门筛选
const canSelectDept = !isDeptAdmin

// ==================== 岗位状态机（契约 3 状态码） ====================
// el-tag type 仅使用 primary/success/info/warning/danger
const STATUS_MAP = {
  0: { text: '草稿', type: 'info' },
  1: { text: '待审批', type: 'warning' },
  2: { text: '已发布', type: 'success' },
  3: { text: '已驳回', type: 'danger' },
  4: { text: '已下架', type: 'info' }
}
const STATUS_OPTIONS = Object.keys(STATUS_MAP).map((key) => ({
  value: Number(key),
  label: STATUS_MAP[key].text
}))
const statusText = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].text : '未知')
const statusType = (status) => (STATUS_MAP[status] ? STATUS_MAP[status].type : 'info')

// 行内操作严格按状态渲染（见模板"操作"列）：
//   0 草稿 = 编辑 / 提交 / 删除；1 待审批 = 等待；2 已发布 = 下架；3 已驳回 = 驳回原因 / 编辑 / 再次提交；4 已下架 = 只读

// ==================== 搜索条件（PositionPageQueryDTO{name,deptId,category,semesterId,status,page,pageSize}） ====================
const searchForm = ref({ name: '', deptId: null, category: null, status: null })

// ==================== 列表数据 ====================
const positionList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 下拉辅助数据 ====================
const deptList = ref([])
const categoryOptions = ref([])
const semesterList = ref([])
// 当前学期 id（契约 6.11 多学期隔离：semester.status=1 的那条），新增岗位时默认带入
const currentSemesterId = ref(null)

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const formRef = ref()

// PositionDTO{id,name,deptId,category,semesterId,quota,hourlyWage,workContent,workLocation,contactPerson,contactPhone,status,rejectReason}
const emptyForm = () => ({
  id: null,
  name: '',
  deptId: isDeptAdmin ? loginUser.deptId : null,
  category: null,
  semesterId: null,
  quota: 1,
  hourlyWage: null,
  workContent: '',
  workLocation: '',
  contactPerson: '',
  contactPhone: '',
  status: 0,
  rejectReason: null
})
const positionForm = ref(emptyForm())

const rules = ref({
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择用工部门', trigger: 'change' }],
  category: [{ required: true, message: '请选择岗位类别', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择所属学期', trigger: 'change' }],
  quota: [{ required: true, message: '请输入招聘名额', trigger: 'blur' }],
  hourlyWage: [{ required: true, message: '请输入时薪', trigger: 'blur' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }]
})

// ==================== 展示格式化 ====================
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)
const remainQuota = (row) => {
  if (row.remainQuota !== null && row.remainQuota !== undefined) {
    return row.remainQuota
  }
  return (row.quota || 0) - (row.hired || 0)
}
const categoryLabel = (row) => row.categoryName || row.category || '--'

// ==================== 钩子 ====================
onMounted(() => {
  loadDictList()
  loadSemesterList()
  if (canSelectDept) {
    loadDeptList()
  }
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  const params = {
    name: searchForm.value.name || undefined,
    deptId: searchForm.value.deptId === null ? undefined : searchForm.value.deptId,
    category: searchForm.value.category || undefined,
    status: searchForm.value.status === null ? undefined : searchForm.value.status,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      positionList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '岗位数据查询失败')
    }
  } catch (e) {
    ElMessage.error('岗位数据查询失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { name: '', deptId: null, category: null, status: null }
  currentPage.value = 1
  search()
}

const handleSizeChange = () => {
  search()
}

const handleCurrentChange = () => {
  search()
}

// ==================== 辅助下拉（失败不阻塞主流程） ====================
const loadDeptList = async () => {
  try {
    const res = await deptListApi()
    if (res.code) {
      deptList.value = res.data || []
    }
  } catch (e) {
    console.warn('[岗位管理] 部门下拉加载失败', e)
  }
}

// 岗位类别取自数据字典 dict_code = position_category
const loadDictList = async () => {
  try {
    const res = await dictListApi('position_category')
    if (res.code) {
      categoryOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('[岗位管理] 岗位类别字典加载失败', e)
  }
}

// 学期下拉：status=1 的那条是当前学期，默认选中
const loadSemesterList = async () => {
  try {
    const res = await semesterListApi()
    if (res.code) {
      semesterList.value = res.data || []
      const current = semesterList.value.find((item) => item.status === 1)
      if (current) {
        currentSemesterId.value = current.id
      }
    }
  } catch (e) {
    console.warn('[岗位管理] 学期下拉加载失败', e)
  }
}

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增岗位'
  positionForm.value = emptyForm()
  if (!positionForm.value.semesterId && currentSemesterId.value) {
    positionForm.value.semesterId = currentSemesterId.value
  }
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openEdit = async (row) => {
  try {
    const res = await getByIdApi(row.id)
    if (!res.code) {
      ElMessage.error(res.msg || '查询岗位信息失败')
      return
    }
    dialogTitle.value = '修改岗位'
    // 用后端返回的完整数据回填，避免漏字段被覆盖成空
    positionForm.value = { ...emptyForm(), ...res.data }
    dialogVisible.value = true
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  } catch (e) {
    ElMessage.error('查询岗位信息失败')
  }
}

// 保存：有 id 为修改，没有 id 为新增（新增即存草稿 status=0）
const save = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    // 部门管理员强制归属本部门（数据归属校验依赖 dept_id）
    if (isDeptAdmin) {
      positionForm.value.deptId = loginUser.deptId
    }
    try {
      const isEdit = !!positionForm.value.id
      const res = isEdit ? await updateApi(positionForm.value) : await addApi(positionForm.value)
      if (res.code) {
        ElMessage.success(isEdit ? '修改成功' : '已保存为草稿')
        dialogVisible.value = false
        search()
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } catch (e) {
      ElMessage.error('保存失败')
    }
  })
}

// ==================== 状态机操作 ====================
// 提交审批：0 草稿 / 3 已驳回 → 1 待审批
const submit = (row) => {
  ElMessageBox.confirm(`确认提交岗位【${row.name}】进行中心审批吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await submitApi(row.id)
      if (res.code) {
        ElMessage.success('已提交，等待中心审批')
        search()
      } else {
        ElMessage.error(res.msg || '提交失败')
      }
    } catch (e) {
      ElMessage.error('提交失败')
    }
  }).catch(() => {})
}

// 下架：2 已发布 → 4 已下架
const offline = (row) => {
  ElMessageBox.confirm(
    `确认下架岗位【${row.name}】吗？下架后学生将无法再看到并报名该岗位。`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await offlineApi(row.id)
      if (res.code) {
        ElMessage.success('已下架')
        search()
      } else {
        ElMessage.error(res.msg || '下架失败')
      }
    } catch (e) {
      ElMessage.error('下架失败')
    }
  }).catch(() => {})
}

// 删除：仅草稿可删除
const remove = (row) => {
  ElMessageBox.confirm(`确认删除草稿岗位【${row.name}】吗？删除后不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteApi(row.id)
      if (res.code) {
        ElMessage.success('删除成功')
        search()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 查看驳回原因（已驳回 / 已下架的岗位详情）
const detailVisible = ref(false)
const detailRow = ref({})
const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

// 待审批状态只提示"等待"，不渲染任何可执行按钮
const waitingTip = (row) => {
  ElMessage.info(`岗位【${row.name}】已提交，等待中心审批`)
}
</script>

<template>
  <div class="position-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="岗位名称">
          <el-input v-model="searchForm.name" placeholder="请输入岗位名称" clearable style="width: 170px" />
        </el-form-item>
        <el-form-item v-if="canSelectDept" label="用工部门">
          <el-select v-model="searchForm.deptId" placeholder="全部部门" clearable style="width: 170px">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
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
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="warning" @click="openAdd">+ 新增岗位</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="positionList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="岗位名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="用工部门" min-width="120" align="center">
          <template #default="scope">
            {{ scope.row.deptName || (scope.row.deptId ? '部门ID：' + scope.row.deptId : '--') }}
          </template>
        </el-table-column>
        <el-table-column label="岗位类别" min-width="110" align="center">
          <template #default="scope">{{ categoryLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="名额(已录/总)" width="120" align="center">
          <template #default="scope">
            <span class="quota-text">{{ scope.row.hired || 0 }} / {{ scope.row.quota }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余名额" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="remainQuota(scope.row) <= 0" type="danger" size="small">已满</el-tag>
            <span v-else>{{ remainQuota(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时薪(元)" width="100" align="center">
          <template #default="scope">{{ formatMoney(scope.row.hourlyWage) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="270" align="center">
          <template #default="scope">
            <!-- 0 草稿：编辑 / 提交 / 删除 -->
            <template v-if="scope.row.status === 0">
              <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
              <el-button link type="success" @click="submit(scope.row)">提交</el-button>
              <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
            </template>
            <!-- 1 待审批：等待（不渲染任何写操作） -->
            <template v-else-if="scope.row.status === 1">
              <el-button link type="info" @click="waitingTip(scope.row)">等待审批</el-button>
            </template>
            <!-- 2 已发布：下架 -->
            <template v-else-if="scope.row.status === 2">
              <el-button link type="warning" @click="offline(scope.row)">下架</el-button>
            </template>
            <!-- 3 已驳回：看原因 / 编辑 / 再提交 -->
            <template v-else-if="scope.row.status === 3">
              <el-button link type="danger" @click="openDetail(scope.row)">驳回原因</el-button>
              <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
              <el-button link type="success" @click="submit(scope.row)">再次提交</el-button>
            </template>
            <!-- 4 已下架：只读 -->
            <template v-else>
              <el-button link type="primary" @click="openDetail(scope.row)">查看</el-button>
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

    <!-- ==================== 新增 / 编辑弹窗 ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form :model="positionForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="positionForm.name" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="用工部门" prop="deptId">
          <!-- 部门管理员归属本部门，禁止改选（数据归属校验依赖 dept_id） -->
          <el-select
            v-model="positionForm.deptId"
            placeholder="请选择用工部门"
            :disabled="isDeptAdmin"
            style="width: 100%"
          >
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别" prop="category">
          <el-select v-model="positionForm.category" placeholder="请选择岗位类别" style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.itemText"
              :value="item.itemValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属学期" prop="semesterId">
          <el-select v-model="positionForm.semesterId" placeholder="请选择所属学期" style="width: 100%">
            <el-option
              v-for="item in semesterList"
              :key="item.id"
              :label="item.name + (item.status === 1 ? '（当前学期）' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="招聘名额" prop="quota">
          <el-input-number v-model="positionForm.quota" :min="1" :max="9999" :precision="0" />
        </el-form-item>
        <el-form-item label="时薪(元)" prop="hourlyWage">
          <el-input-number v-model="positionForm.hourlyWage" :min="0.01" :max="9999" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="工作内容" prop="workContent">
          <el-input
            v-model="positionForm.workContent"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请描述岗位工作内容"
          />
        </el-form-item>
        <el-form-item label="工作地点" prop="workLocation">
          <el-input v-model="positionForm.workLocation" placeholder="请输入工作地点" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="positionForm.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="positionForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- ==================== 详情弹窗（驳回原因 / 只读查看） ==================== -->
    <el-dialog v-model="detailVisible" title="岗位详情" width="640px">
      <el-descriptions :column="2" border>
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

.quota-text {
  color: #333333;
  font-weight: 500;
}

.reject-text {
  color: #f56c6c;
}
</style>
