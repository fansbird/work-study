<!--
  学生档案
  ====================================================================
  路由：/system/student    可见角色：超级管理员(0)
  契约依据：第 5 节 StudentController（/student）、第 3 节 student 表、第 7 节前端契约
  写法标杆：src/views/system/user.vue（搜索栏 / 表格+分页 / 弹窗表单 / 行内操作四段式）

  接口对齐：
    GET    /student/page               分页查询 StudentPageQueryDTO{name,studentNo,college,page,pageSize}
    GET    /student/list               查询学生列表（下拉用）
    POST   /student                    新增 StudentDTO{id,userId,studentNo,name,gender,phone,college,major,className,grade,difficultyLevel,bankCard,status}
    PUT    /student                    修改 StudentDTO
    GET    /student/{id}               根据 id 查询
    GET    /student/getByUserId/{userId} 根据关联账号 id 查询
    DELETE /student/{id}               删除
  注意：分页返回的是 StudentVO（Student 全字段 + username 登录账号名），表格里的"登录账号"即 username。

  新增的联动行为（后端 StudentServiceImpl.save，与本页提示文案一致）：
    落学生档案的同时自动创建学生登录账号：username = 学号，role = 1 学生，初始密码 = 123456。
    因此学号在修改态禁止编辑（改了学号不会同步改登录账号，会造成数据不一致）。
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, addApi, updateApi, getByIdApi } from '@/api/student'
import { listApi as dictListApi } from '@/api/dict'

// ==================== 困难等级（契约 3 student.difficulty_level） ====================
// 0 非困难 / 1 一般困难 / 2 特别困难。权威来源是数据字典 dict_code=difficulty_level，
// 字典拉取失败时用内置兜底项，保证下拉与标签始终可用。
const FALLBACK_DIFFICULTY = [
  { value: 0, label: '非困难', tagType: 'info' },
  { value: 1, label: '一般困难', tagType: 'warning' },
  { value: 2, label: '特别困难', tagType: 'danger' }
]
const difficultyOptions = ref(FALLBACK_DIFFICULTY)

const difficultyItem = (level) =>
  difficultyOptions.value.find((item) => item.value === Number(level))
const difficultyLabel = (level) => {
  const item = difficultyItem(level)
  return item ? item.label : '--'
}
const difficultyType = (level) => {
  const item = difficultyItem(level)
  return item ? item.tagType : 'info'
}

// ==================== 性别选项 ====================
const genderOptions = ref(['男', '女'])

// ==================== 搜索条件（StudentPageQueryDTO{name,studentNo,college,page,pageSize}） ====================
const searchForm = ref({ name: '', studentNo: '', college: '' })

// ==================== 列表数据 ====================
const studentList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const formRef = ref()

// StudentDTO{id,userId,studentNo,name,gender,phone,college,major,className,grade,difficultyLevel,bankCard,status}
// userId 由后端联动创建账号后回填，前端不传；故表单里不带该字段
const emptyForm = () => ({
  id: null,
  studentNo: '',
  name: '',
  gender: '',
  phone: '',
  college: '',
  major: '',
  className: '',
  grade: '',
  difficultyLevel: 0,
  bankCard: '',
  status: 1
})
const studentForm = ref(emptyForm())

// 表单校验规则
const rules = ref({
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { max: 30, message: '学号不能超过30个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }],
  bankCard: [{ pattern: /^[0-9\s-]{8,30}$/, message: '请输入有效的银行卡号（数字）', trigger: 'blur' }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  loadDifficultyOptions()
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  const params = {
    name: searchForm.value.name || undefined,
    studentNo: searchForm.value.studentNo || undefined,
    college: searchForm.value.college || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      studentList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '学生数据查询失败')
    }
  } catch (e) {
    ElMessage.error('学生数据查询失败')
  }
}

// 点击查询按钮：回到第一页再查询
const handleSearch = () => {
  currentPage.value = 1
  search()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = { name: '', studentNo: '', college: '' }
  currentPage.value = 1
  search()
}

// 每页展示记录数变化
const handleSizeChange = () => {
  search()
}

// 页码变化
const handleCurrentChange = () => {
  search()
}

// ==================== 困难等级下拉（字典优先，失败兜底） ====================
const loadDifficultyOptions = async () => {
  try {
    const res = await dictListApi('difficulty_level')
    if (res.code && res.data && res.data.length > 0) {
      const options = res.data
        .filter((item) => item.status === 1)
        .sort((a, b) => (a.sort || 0) - (b.sort || 0))
        .map((item) => ({
          value: Number(item.itemValue),
          label: item.itemText,
          // 字典里没有定义 el-tag 颜色，按等级沿用内置配色
          tagType:
            (FALLBACK_DIFFICULTY.find((it) => it.value === Number(item.itemValue)) || {}).tagType || 'info'
        }))
      if (options.length > 0) {
        difficultyOptions.value = options
      }
    }
  } catch (e) {
    console.warn('[学生档案] 困难等级字典加载失败，使用内置兜底项', e)
  }
}

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增学生'
  studentForm.value = emptyForm()
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openEdit = async (row) => {
  try {
    const res = await getByIdApi(row.id)
    if (!res.code) {
      ElMessage.error(res.msg || '查询学生信息失败')
      return
    }
    dialogTitle.value = '修改学生'
    // 用后端返回的完整数据回填，避免漏字段被覆盖成空
    studentForm.value = { ...emptyForm(), ...res.data }
    dialogVisible.value = true
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  } catch (e) {
    ElMessage.error('查询学生信息失败')
  }
}

// 保存（有 id 为修改，没有 id 为新增）
const save = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    try {
      const isEdit = !!studentForm.value.id
      // userId 由后端联动维护（新增时回填、修改时忽略），前端不传
      delete studentForm.value.userId
      const res = isEdit ? await updateApi(studentForm.value) : await addApi(studentForm.value)
      if (res.code) {
        dialogVisible.value = false
        search()
        if (isEdit) {
          ElMessage.success('保存成功')
        } else {
          // 新增会联动创建登录账号（后端 StudentServiceImpl.save）
          ElMessageBox.alert(
            `学生档案已保存。系统已自动创建该学生的登录账号：账号“${studentForm.value.studentNo}”，初始密码“123456”，请提醒学生首次登录后及时修改密码。`,
            '新增成功',
            { confirmButtonText: '知道了', type: 'success' }
          ).catch(() => {})
        }
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } catch (e) {
      ElMessage.error('保存失败')
    }
  })
}
</script>

<template>
  <div class="student-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="searchForm.studentNo" placeholder="请输入学号" clearable style="width: 170px" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="searchForm.college" placeholder="请输入学院" clearable style="width: 170px" />
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="warning" @click="openAdd">+ 新增学生</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="studentList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="studentNo" label="学号" min-width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="100" align="center" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="scope">{{ scope.row.gender || '--' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130" align="center">
          <template #default="scope">{{ scope.row.phone || '--' }}</template>
        </el-table-column>
        <el-table-column label="学院" min-width="130" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.college || '--' }}</template>
        </el-table-column>
        <el-table-column label="专业" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.major || '--' }}</template>
        </el-table-column>
        <el-table-column label="班级" min-width="110" align="center">
          <template #default="scope">{{ scope.row.className || '--' }}</template>
        </el-table-column>
        <el-table-column label="年级" width="100" align="center">
          <template #default="scope">{{ scope.row.grade || '--' }}</template>
        </el-table-column>
        <el-table-column label="困难等级" width="110" align="center">
          <template #default="scope">
            <el-tag :type="difficultyType(scope.row.difficultyLevel)" size="small">
              {{ difficultyLabel(scope.row.difficultyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="银行卡号" min-width="180" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.bankCard || '--' }}</template>
        </el-table-column>
        <el-table-column label="登录账号" min-width="130" align="center" show-overflow-tooltip>
          <!-- StudentVO 的 username：新增学生时后端联动创建的登录账号名 = 学号 -->
          <template #default="scope">{{ scope.row.username || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
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
      <el-form :model="studentForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="学号" prop="studentNo">
          <!-- 学号与登录账号一一对应，修改态禁止编辑（改学号不会同步改登录账号） -->
          <el-input
            v-model="studentForm.studentNo"
            placeholder="请输入学号，新增后即为登录账号"
            :disabled="!!studentForm.id"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="studentForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="studentForm.gender" placeholder="请选择性别" clearable style="width: 100%">
            <el-option v-for="item in genderOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="studentForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-input v-model="studentForm.college" placeholder="请输入学院" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="studentForm.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="班级" prop="className">
          <el-input v-model="studentForm.className" placeholder="请输入班级" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="studentForm.grade" placeholder="请输入年级，如 2024" />
        </el-form-item>
        <el-form-item label="困难等级" prop="difficultyLevel">
          <el-select v-model="studentForm.difficultyLevel" placeholder="请选择困难等级" style="width: 100%">
            <el-option
              v-for="item in difficultyOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="银行卡号" prop="bankCard">
          <el-input v-model="studentForm.bankCard" placeholder="请输入用于发放薪酬的银行卡号" />
        </el-form-item>
        <el-form-item label="档案状态">
          <el-switch
            v-model="studentForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>

      <!-- 底部按钮 -->
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
</style>
