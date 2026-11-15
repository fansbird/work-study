<!--
  薪酬规则管理
  ====================================================================
  路由：/salary/rule    可见角色：超级管理员(0)、勤工助学中心管理员(3)
  契约依据：第 5 节 SalaryRuleController（/salaryRule）、第 6 节⑦「结算对账 / 规则匹配」

  规则匹配（契约 6.7，由后端结算时执行，本页仅维护数据）：
    · 按 priority 升序（数字小者优先），在「学期 + 岗位类别 + 困难等级」三个维度上命中最具体者生效；
    · 学期 / 岗位类别 / 困难等级留空表示「通用 · 全部」，对所有对象生效；
    · 未命中任何规则时，回退用岗位 hourly_wage、系数 1。

  接口对齐：GET /salaryRule/page（SalaryRulePageQueryDTO{name,semesterId,category,page,pageSize}）
            GET /salaryRule/list  POST /salaryRule  PUT /salaryRule  DELETE /salaryRule/{id}
  说明：启停（status 1 启用 / 0 禁用）无独立接口，按契约用 PUT /salaryRule 整体提交（契约第 5 节无 /status）。
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, addApi, updateApi, deleteApi } from '@/api/salaryRule'
import { listApi as dictListApi } from '@/api/dict'
import { listApi as semesterListApi } from '@/api/semester'

// ==================== 搜索条件（SalaryRulePageQueryDTO{name,semesterId,category,page,pageSize}） ====================
const searchForm = ref({ name: '', semesterId: null, category: null })

// ==================== 列表数据 ====================
const ruleList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// ==================== 下拉辅助数据（加载失败不阻塞主流程） ====================
const semesterList = ref([])
const categoryOptions = ref([])
const difficultyOptions = ref([])

// 学期 id → 名称（表格里把 semesterId 显示成学期名，空值显示「通用」）
const semesterMap = computed(() => {
  const map = {}
  semesterList.value.forEach((item) => {
    map[item.id] = item.name
  })
  return map
})

// 困难等级兜底文案（与 sys_dict.difficulty_level 一致，字典未加载时也能正常展示）
const DIFFICULTY_FALLBACK = { 0: '非困难', 1: '一般困难', 2: '特别困难' }

// 困难等级选项：优先用字典 difficulty_level，字典为空时用兜底
const difficultySelectOptions = computed(() => {
  if (difficultyOptions.value.length > 0) {
    return difficultyOptions.value.map((item) => ({
      value: Number(item.itemValue),
      label: item.itemText
    }))
  }
  return Object.keys(DIFFICULTY_FALLBACK).map((key) => ({
    value: Number(key),
    label: DIFFICULTY_FALLBACK[key]
  }))
})

// ==================== 展示格式化 ====================
// 金额与系数统一保留两位小数（BigDecimal 语义）
const formatMoney = (value) =>
  value === null || value === undefined || value === '' ? '--' : Number(value).toFixed(2)

const semesterLabel = (semesterId) =>
  semesterId === null || semesterId === undefined ? '通用' : semesterMap.value[semesterId] || '学期ID：' + semesterId

const categoryLabel = (category) => (category ? category : '全部类别')

const difficultyLabel = (level) => {
  if (level === null || level === undefined || level === '') return '全部等级'
  const hit = difficultySelectOptions.value.find((item) => item.value === Number(level))
  return hit ? hit.label : DIFFICULTY_FALLBACK[Number(level)] || '全部等级'
}

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增薪酬规则')
const formRef = ref()

// SalaryRuleDTO{id,name,semesterId,category,difficultyLevel,baseWage,coefficient,maxMonthly,minMonthly,priority,status}
const emptyForm = () => ({
  id: null,
  name: '',
  semesterId: null,
  category: null,
  difficultyLevel: null,
  baseWage: null,
  coefficient: 1,
  maxMonthly: null,
  minMonthly: null,
  priority: 100,
  status: 1
})
const ruleForm = ref(emptyForm())

const rules = ref({
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  baseWage: [{ required: true, message: '请输入基础时薪', trigger: 'blur' }],
  coefficient: [{ required: true, message: '请输入系数', trigger: 'blur' }],
  priority: [{ required: true, message: '请输入优先级', trigger: 'blur' }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  loadSemesterList()
  loadCategoryOptions()
  loadDifficultyOptions()
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  loading.value = true
  const params = {
    name: searchForm.value.name || undefined,
    semesterId: searchForm.value.semesterId === null ? undefined : searchForm.value.semesterId,
    category: searchForm.value.category || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      ruleList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '薪酬规则查询失败')
    }
  } catch (e) {
    ElMessage.error('薪酬规则查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  search()
}

const handleReset = () => {
  searchForm.value = { name: '', semesterId: null, category: null }
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
    console.warn('[薪酬规则] 学期下拉加载失败', e)
  }
}

// 岗位类别取自数据字典 dict_code = position_category
const loadCategoryOptions = async () => {
  try {
    const res = await dictListApi('position_category')
    if (res.code) {
      categoryOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('[薪酬规则] 岗位类别字典加载失败', e)
  }
}

// 困难等级取自数据字典 dict_code = difficulty_level
const loadDifficultyOptions = async () => {
  try {
    const res = await dictListApi('difficulty_level')
    if (res.code) {
      difficultyOptions.value = res.data || []
    }
  } catch (e) {
    console.warn('[薪酬规则] 困难等级字典加载失败', e)
  }
}

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增薪酬规则'
  ruleForm.value = emptyForm()
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 编辑：直接用行数据回填（DTO 字段齐全，避免额外一次请求）
const openEdit = (row) => {
  dialogTitle.value = '修改薪酬规则'
  ruleForm.value = { ...emptyForm(), ...row }
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 只提交 DTO 字段，避免把 createTime / updateTime 等审计字段回传
const toPayload = (rule) => ({
  id: rule.id,
  name: rule.name,
  semesterId: rule.semesterId,
  category: rule.category,
  difficultyLevel: rule.difficultyLevel,
  baseWage: rule.baseWage,
  coefficient: rule.coefficient,
  maxMonthly: rule.maxMonthly,
  minMonthly: rule.minMonthly,
  priority: rule.priority,
  status: rule.status
})

// 保存（有 id 为修改，没有 id 为新增）
const save = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    const { maxMonthly, minMonthly } = ruleForm.value
    // 封顶不能低于保底（两者都填时才校验）
    if (
      maxMonthly !== null &&
      maxMonthly !== undefined &&
      minMonthly !== null &&
      minMonthly !== undefined &&
      Number(maxMonthly) < Number(minMonthly)
    ) {
      ElMessage.error('月封顶金额不能小于月保底金额')
      return
    }
    try {
      const isEdit = !!ruleForm.value.id
      const payload = toPayload(ruleForm.value)
      const res = isEdit ? await updateApi(payload) : await addApi(payload)
      if (res.code) {
        ElMessage.success('保存成功')
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

// ==================== 启用 / 禁用（PUT /salaryRule 整体提交，契约无独立 status 接口） ====================
const toggleStatus = (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const text = nextStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确认要${text}规则【${row.name}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await updateApi(toPayload({ ...row, status: nextStatus }))
      if (res.code) {
        ElMessage.success(`${text}成功`)
        search()
      } else {
        ElMessage.error(res.msg || `${text}失败`)
      }
    } catch (e) {
      ElMessage.error(`${text}失败`)
    }
  }).catch(() => {})
}

// ==================== 删除 ====================
const remove = (row) => {
  ElMessageBox.confirm(
    `确认删除规则【${row.name}】吗？删除后不可恢复，已生成的结算单不受影响。`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
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
</script>

<template>
  <div class="salary-rule-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.name" placeholder="请输入规则名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="适用学期">
          <el-select v-model="searchForm.semesterId" placeholder="全部学期" clearable style="width: 190px">
            <el-option
              v-for="item in semesterList"
              :key="item.id"
              :label="item.name + (item.status === 1 ? '（当前学期）' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别">
          <el-select v-model="searchForm.category" placeholder="全部类别" clearable style="width: 160px">
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
      <el-button type="warning" @click="openAdd">+ 新增规则</el-button>
    </div>

    <!-- ==================== 匹配顺序说明 ==================== -->
    <el-alert
      class="rule-tip"
      type="info"
      :closable="false"
      show-icon
      title="规则匹配顺序（结算时由后端执行）"
    >
      <template #default>
        按「优先级」升序（数字小者优先）匹配：学期 / 岗位类别 / 困难等级三个维度命中最具体的一条规则生效；
        三者留空表示「通用 · 全部对象」。未命中任何规则时，回退按岗位时薪、系数 1 计算。
      </template>
    </el-alert>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table v-loading="loading" :data="ruleList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="规则名称" min-width="170" align="center" show-overflow-tooltip />
        <el-table-column label="适用学期" min-width="120" align="center">
          <template #default="scope">{{ semesterLabel(scope.row.semesterId) }}</template>
        </el-table-column>
        <el-table-column label="岗位类别" min-width="110" align="center">
          <template #default="scope">{{ categoryLabel(scope.row.category) }}</template>
        </el-table-column>
        <el-table-column label="困难等级" min-width="110" align="center">
          <template #default="scope">{{ difficultyLabel(scope.row.difficultyLevel) }}</template>
        </el-table-column>
        <el-table-column label="基础时薪(元)" width="120" align="right">
          <template #default="scope">{{ formatMoney(scope.row.baseWage) }}</template>
        </el-table-column>
        <el-table-column label="系数" width="90" align="right">
          <template #default="scope">{{ formatMoney(scope.row.coefficient) }}</template>
        </el-table-column>
        <el-table-column label="月封顶(元)" width="110" align="right">
          <template #default="scope">{{ formatMoney(scope.row.maxMonthly) }}</template>
        </el-table-column>
        <el-table-column label="月保底(元)" width="110" align="right">
          <template #default="scope">{{ formatMoney(scope.row.minMonthly) }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
            <el-button link type="warning" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
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

    <!-- ==================== 新增 / 修改弹窗 ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form :model="ruleForm" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="例如：困难学生时薪上浮规则" maxlength="100" />
        </el-form-item>
        <el-form-item label="适用学期">
          <!-- 留空 = 通用（对所有学期生效） -->
          <el-select v-model="ruleForm.semesterId" placeholder="留空表示通用" clearable style="width: 100%">
            <el-option
              v-for="item in semesterList"
              :key="item.id"
              :label="item.name + (item.status === 1 ? '（当前学期）' : '')"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位类别">
          <!-- 留空 = 全部类别 -->
          <el-select v-model="ruleForm.category" placeholder="留空表示全部类别" clearable style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item.id"
              :label="item.itemText"
              :value="item.itemValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="困难等级">
          <!-- 留空 = 全部等级 -->
          <el-select v-model="ruleForm.difficultyLevel" placeholder="留空表示全部等级" clearable style="width: 100%">
            <el-option
              v-for="item in difficultySelectOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="基础时薪(元)" prop="baseWage">
          <el-input-number v-model="ruleForm.baseWage" :min="0.01" :max="9999" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="系数" prop="coefficient">
          <el-input-number v-model="ruleForm.coefficient" :min="0.01" :max="99" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="月封顶(元)">
          <el-input-number v-model="ruleForm.maxMonthly" :min="0" :max="999999" :precision="2" :step="50" />
          <span class="field-tip">留空表示不封顶</span>
        </el-form-item>
        <el-form-item label="月保底(元)">
          <el-input-number v-model="ruleForm.minMonthly" :min="0" :max="999999" :precision="2" :step="50" />
          <span class="field-tip">留空表示不保底</span>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="ruleForm.priority" :min="1" :max="9999" :precision="0" />
          <span class="field-tip">数字越小越优先</span>
        </el-form-item>
        <el-form-item label="规则状态">
          <!-- 用 el-switch + active-value/inactive-value（各版本 ElementPlus 都稳定支持），
               不用 el-radio 的 label 充当 value -->
          <el-switch
            v-model="ruleForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
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

/* 匹配顺序说明 */
.rule-tip {
  margin-bottom: 16px;
}

/* 表单里的补充说明文字 */
.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
