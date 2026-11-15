<!--
  部门管理
  ====================================================================
  路由：/system/dept    可见角色：超级管理员(0)
  契约依据：第 5 节 DeptController（/dept）、第 3 节 dept 表、第 7 节前端契约
  写法标杆：src/views/system/user.vue（搜索栏 / 表格+分页 / 弹窗表单 / 行内操作四段式）

  接口对齐（下列为契约里的后端真实路径，前端经 utils/request 的 baseURL=/api 转发）：
    GET    /dept/list                查询部门列表（下拉用，仅启用中的）
    GET    /dept/page                分页查询 DeptPageQueryDTO{name,page,pageSize}
    POST   /dept                     新增 DeptDTO{id,name,contactPerson,contactPhone,status}
    PUT    /dept                     修改 DeptDTO
    GET    /dept/{id}                根据 id 查询
    DELETE /dept/{id}                删除（被岗位引用时后端抛业务异常，前端 catch 提示）
    POST   /dept/status/{status}?id= 启用(1)/禁用(0)
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageApi, addApi, updateApi, getByIdApi, startOrStopApi, deleteApi } from '@/api/dept'

// ==================== 搜索条件（DeptPageQueryDTO{name,page,pageSize}） ====================
const searchForm = ref({ name: '' })

// ==================== 列表数据 ====================
const deptList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref()

// DeptDTO{id,name,contactPerson,contactPhone,status}
const emptyForm = () => ({
  id: null,
  name: '',
  contactPerson: '',
  contactPhone: '',
  status: 1
})
const deptForm = ref(emptyForm())

// 表单校验规则（联系电话既可能是手机号也可能是座机，故用"数字+短横线"的宽松校验）
const rules = ref({
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 2, max: 30, message: '部门名称长度应在2到30个字符之间', trigger: 'blur' }
  ],
  contactPerson: [{ max: 20, message: '联系人姓名不能超过20个字符', trigger: 'blur' }],
  contactPhone: [{ pattern: /^[0-9-]{7,20}$/, message: '请输入有效的联系电话（数字或短横线）', trigger: 'blur' }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  const params = {
    name: searchForm.value.name || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      deptList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '部门数据查询失败')
    }
  } catch (e) {
    ElMessage.error('部门数据查询失败')
  }
}

// 点击查询按钮：回到第一页再查询
const handleSearch = () => {
  currentPage.value = 1
  search()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = { name: '' }
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

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增部门'
  deptForm.value = emptyForm()
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openEdit = async (row) => {
  try {
    const res = await getByIdApi(row.id)
    if (!res.code) {
      ElMessage.error(res.msg || '查询部门信息失败')
      return
    }
    dialogTitle.value = '修改部门'
    // 用后端返回的完整数据回填，避免漏字段被覆盖成空
    deptForm.value = { ...emptyForm(), ...res.data }
    dialogVisible.value = true
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  } catch (e) {
    ElMessage.error('查询部门信息失败')
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
      const isEdit = !!deptForm.value.id
      const res = isEdit ? await updateApi(deptForm.value) : await addApi(deptForm.value)
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

// ==================== 启用 / 禁用 ====================
const startOrStop = (row) => {
  const status = row.status === 1 ? 0 : 1
  const text = status === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确认要${text}部门【${row.name}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await startOrStopApi(status, row.id)
      if (res.code) {
        ElMessage.success(`${text}成功`)
        search()
      } else {
        ElMessage.error(res.msg || `${text}失败`)
      }
    } catch (e) {
      ElMessage.error(`${text}失败`)
    }
  }).catch(() => {
    // 取消操作，无需处理
  })
}

// ==================== 删除 ====================
// 部门被岗位引用时后端会拒绝删除，这里把后端返回的中文原因原样提示
const remove = (row) => {
  ElMessageBox.confirm(
    `确认要删除部门【${row.name}】吗？删除后不可恢复。`,
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
  }).catch(() => {
    // 取消操作，无需处理
  })
}
</script>

<template>
  <div class="dept-page">
    <!-- ==================== 搜索栏 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="部门名称">
          <el-input v-model="searchForm.name" placeholder="请输入部门名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="warning" @click="openAdd">+ 新增部门</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="deptList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="部门名称" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column label="联系人" min-width="120" align="center">
          <template #default="scope">{{ scope.row.contactPerson || '--' }}</template>
        </el-table-column>
        <el-table-column label="联系电话" min-width="160" align="center">
          <template #default="scope">{{ scope.row.contactPhone || '--' }}</template>
        </el-table-column>
        <el-table-column label="部门状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" width="180" align="center">
          <template #default="scope">{{ scope.row.updateTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
            <el-button link type="warning" @click="startOrStop(scope.row)">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="deptForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="deptForm.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="deptForm.contactPhone" placeholder="请输入联系电话，如 020-88881001" />
        </el-form-item>
        <el-form-item label="部门状态">
          <!-- 用 el-switch + active-value/inactive-value（各版本 ElementPlus 都稳定支持），
               不用 el-radio 的 label 充当 value（新版已标记废弃并会打印告警） -->
          <el-switch
            v-model="deptForm.status"
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
</style>
