<!--
  账号管理（样板页）
  ====================================================================
  这是本系统的页面写法标杆，后续业务页面请按这里的结构照抄：
    1. 搜索条件区（page-card + el-form inline + 查询/重置 + 右侧新增按钮）
    2. 数据表格区（page-card + el-table + el-pagination 分页条）
    3. 新增/编辑弹窗（el-dialog + el-form + rules 校验 + 保存）
    4. 行内操作（修改 / 启用禁用 / 重置密码，危险操作先 ElMessageBox.confirm）
  接口与字段严格对齐契约第 5 节 SysUserController（/sysUser）。
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, addApi, updateApi, getByIdApi, startOrStopApi, resetPasswordApi } from '@/api/user'
import { listApi as deptListApi } from '@/api/dept'
import { ROLE, ROLE_OPTIONS, roleLabel, roleTagType } from '@/utils/role'

// ==================== 搜索条件（SysUserPageQueryDTO：name, role, page, pageSize） ====================
const searchForm = ref({ name: '', role: null })

// ==================== 列表数据 ====================
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 部门下拉（表格里把 deptId 显示成部门名，表单里选部门） ====================
const deptList = ref([])
const deptMap = computed(() => {
  const map = {}
  deptList.value.forEach((dept) => {
    map[dept.id] = dept.name
  })
  return map
})

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增账号')
const userFormRef = ref()

// SysUserDTO{id,username,name,phone,role,deptId,studentId,avatar,status}
// 说明：studentId 是学生账号与 student 档案的关联字段，等 views/system/student.vue 完成后
//      可把这里改成"选择学生"；当前保持字段原样透传，新增学生账号时暂不填。
const emptyForm = () => ({
  id: null,
  username: '',
  name: '',
  phone: '',
  role: null,
  deptId: null,
  studentId: null,
  avatar: '',
  status: 1
})
const userForm = ref(emptyForm())

// 表单校验规则
const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度应在2到20个字符之间', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  // 只有"用工部门管理员"才会渲染所属部门表单项，隐藏时该规则不会参与校验
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  loadDeptList()
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  const params = {
    name: searchForm.value.name || undefined,
    role: searchForm.value.role === null ? undefined : searchForm.value.role,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      userList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '账号数据查询失败')
    }
  } catch (e) {
    ElMessage.error('账号数据查询失败')
  }
}

// 点击查询按钮：回到第一页再查询
const handleSearch = () => {
  currentPage.value = 1
  search()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = { name: '', role: null }
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

// ==================== 下拉数据 ====================
// 部门列表属于辅助数据，加载失败不阻塞主流程
const loadDeptList = async () => {
  try {
    const res = await deptListApi()
    if (res.code) {
      deptList.value = res.data || []
    }
  } catch (e) {
    console.warn('[账号管理] 部门下拉加载失败，可选择手工填写 deptId', e)
  }
}

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增账号'
  userForm.value = emptyForm()
  dialogVisible.value = true
  if (userFormRef.value) {
    userFormRef.value.clearValidate()
  }
}

const openEdit = async (row) => {
  const res = await getByIdApi(row.id)
  if (res.code) {
    dialogTitle.value = '修改账号'
    // 用后端返回的完整数据回填，避免漏字段（如 studentId / avatar）被覆盖成空
    userForm.value = { ...emptyForm(), ...res.data }
    dialogVisible.value = true
    if (userFormRef.value) {
      userFormRef.value.clearValidate()
    }
  } else {
    ElMessage.error(res.msg || '查询账号信息失败')
  }
}

// 保存（有 id 为修改，没有 id 为新增）
const save = () => {
  if (!userFormRef.value) return
  userFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表单校验不通过')
      return
    }
    // 非部门管理员的账号不带 deptId
    if (userForm.value.role !== ROLE.DEPT_ADMIN) {
      userForm.value.deptId = null
    }
    const isEdit = !!userForm.value.id
    const res = isEdit ? await updateApi(userForm.value) : await addApi(userForm.value)
    if (res.code) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      search()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  })
}

// ==================== 启用 / 禁用 ====================
// 超级管理员 admin 账号不允许禁用
const isAdminAccount = (row) => row.username === 'admin'

const startOrStop = (row) => {
  const status = row.status === 1 ? 0 : 1
  const text = status === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确认要${text}账号【${row.username}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await startOrStopApi(status, row.id)
    if (res.code) {
      ElMessage.success(`${text}成功`)
      search()
    } else {
      ElMessage.error(res.msg || `${text}失败`)
    }
  }).catch(() => {
    // 取消操作，无需处理
  })
}

// ==================== 重置密码 ====================
const resetPassword = (row) => {
  ElMessageBox.prompt(`请输入账号【${row.username}】的新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: '123456',
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度应为 6-20 位',
    type: 'warning'
  }).then(async ({ value }) => {
    // PasswordEditDTO{id,newPassword}
    const res = await resetPasswordApi({ id: row.id, newPassword: value })
    if (res.code) {
      ElMessage.success('密码重置成功')
    } else {
      ElMessage.error(res.msg || '密码重置失败')
    }
  }).catch(() => {
    // 取消操作，无需处理
  })
}
</script>

<template>
  <div class="user-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部角色" clearable style="width: 180px">
            <el-option
              v-for="item in ROLE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="warning" @click="openAdd">+ 新增账号</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="userList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" align="center" />
        <el-table-column prop="name" label="姓名" min-width="100" align="center" />
        <el-table-column prop="phone" label="手机号" min-width="130" align="center" />
        <el-table-column label="角色" min-width="160" align="center">
          <template #default="scope">
            <el-tag :type="roleTagType(scope.row.role)" size="small">{{ roleLabel(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="140" align="center">
          <template #default="scope">
            {{ scope.row.deptId ? (deptMap[scope.row.deptId] || '部门ID：' + scope.row.deptId) : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="scope">
            <span class="status-dot" :class="scope.row.status === 1 ? 'on' : 'off'"></span>
            <span>{{ scope.row.status === 1 ? '启用' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后操作时间" width="180" align="center">
          <template #default="scope">{{ scope.row.updateTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
            <el-button
              link
              type="danger"
              :disabled="isAdminAccount(scope.row)"
              @click="startOrStop(scope.row)"
            >{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button link type="warning" @click="resetPassword(scope.row)">重置密码</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            placeholder="请输入用户名，2-20个字符"
            :disabled="!!userForm.id"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="item in ROLE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <!-- 只有用工部门管理员才需要归属部门（数据归属校验依赖 dept_id） -->
        <el-form-item v-if="userForm.role === ROLE.DEPT_ADMIN" label="所属部门" prop="deptId">
          <el-select v-model="userForm.deptId" placeholder="请选择所属部门" style="width: 100%">
            <el-option
              v-for="dept in deptList"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <!-- 用 el-switch + active-value/inactive-value（各版本 ElementPlus 都稳定支持），
               不用 el-radio 的 label 充当 value（新版已标记废弃并会打印告警） -->
          <el-switch
            v-model="userForm.status"
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

/* ==================== 状态小圆点 ==================== */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.on {
  background-color: #52c41a;
}

.status-dot.off {
  background-color: #c0c4cc;
}
</style>
