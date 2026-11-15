<!--
  学期管理
  ====================================================================
  路由：/system/semester    可见角色：超级管理员(0)
  契约依据：第 5 节 SemesterController（/semester）、第 3 节 sys_semester 表、契约 6.11 多学期隔离
  写法标杆：src/views/system/user.vue（搜索栏 / 表格 / 弹窗表单 / 行内操作四段式）

  接口对齐：
    GET    /semester/list            查询全部学期（status=1 的那条即当前学期）
    POST   /semester                 新增 SysSemesterDTO{id,name,startDate,endDate,status}
    PUT    /semester                 修改 SysSemesterDTO
    POST   /semester/setCurrent/{id} 设为当前学期（至多一个 status=1，由后端保证）
    DELETE /semester/{id}            删除

  说明：学期表数据量小，后端只提供 /list（无分页接口），故本页不分页；
        status 不放在表单里编辑，"当前学期"统一走 setCurrent 接口，避免出现多个当前学期。
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApi, addApi, updateApi, setCurrentApi, deleteApi } from '@/api/semester'

// ==================== 列表数据 ====================
const semesterList = ref([])

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增学期')
const formRef = ref()

// SysSemesterDTO{id,name,startDate,endDate,status}
const emptyForm = () => ({
  id: null,
  name: '',
  startDate: '',
  endDate: '',
  // 0 非当前学期（新建学期默认不是当前学期，要切换走"设为当前学期"）
  status: 0
})
const semesterForm = ref(emptyForm())

// 结束日期不得早于开始日期（日期选择器统一 value-format='YYYY-MM-DD'，字符串可直接比较）
const validateEndDate = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择结束日期'))
    return
  }
  if (semesterForm.value.startDate && value < semesterForm.value.startDate) {
    callback(new Error('结束日期不能早于开始日期'))
    return
  }
  callback()
}

const rules = ref({
  name: [
    { required: true, message: '请输入学期名称', trigger: 'blur' },
    { max: 50, message: '学期名称不能超过50个字符', trigger: 'blur' }
  ],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ validator: validateEndDate, trigger: ['blur', 'change'] }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  try {
    const res = await listApi()
    if (res.code) {
      semesterList.value = res.data || []
    } else {
      ElMessage.error(res.msg || '学期数据查询失败')
    }
  } catch (e) {
    ElMessage.error('学期数据查询失败')
  }
}

// 开始日期变化后重新校验结束日期，避免"先填结束日期"时校验结果过期
const handleStartDateChange = () => {
  if (formRef.value && semesterForm.value.endDate) {
    formRef.value.validateField('endDate')
  }
}

// ==================== 新增 / 编辑 ====================
const openAdd = () => {
  dialogTitle.value = '新增学期'
  semesterForm.value = emptyForm()
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 学期没有单独查询详情的接口，直接用表格行数据回填（列表已含全字段）
const openEdit = (row) => {
  dialogTitle.value = '修改学期'
  semesterForm.value = {
    id: row.id,
    name: row.name,
    startDate: row.startDate,
    endDate: row.endDate,
    // 当前学期标记保持原样，改标记请用行内的"设为当前学期"
    status: row.status
  }
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
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
      const isEdit = !!semesterForm.value.id
      const res = isEdit ? await updateApi(semesterForm.value) : await addApi(semesterForm.value)
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

// ==================== 设为当前学期 ====================
const setCurrent = (row) => {
  ElMessageBox.confirm(
    `确认把【${row.name}】设为当前学期吗？设为当前学期后，岗位、工时、结算默认都归属该学期。`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await setCurrentApi(row.id)
      if (res.code) {
        ElMessage.success('已设为当前学期')
        search()
      } else {
        ElMessage.error(res.msg || '设置失败')
      }
    } catch (e) {
      ElMessage.error('设置失败')
    }
  }).catch(() => {
    // 取消操作，无需处理
  })
}

// ==================== 删除 ====================
const remove = (row) => {
  ElMessageBox.confirm(
    `确认要删除学期【${row.name}】吗？删除后不可恢复。`,
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
  <div class="semester-page">
    <!-- ==================== 搜索栏（学期无分页与筛选条件，此处只放新增按钮与统计） ==================== -->
    <div class="page-card search-bar">
      <span class="total-text">共 {{ semesterList.length }} 个学期（同一时间至多一个「当前学期」）</span>
      <el-button type="warning" @click="openAdd">+ 新增学期</el-button>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="semesterList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="name" label="学期名称" min-width="200" align="center" show-overflow-tooltip />
        <el-table-column label="开始日期" min-width="140" align="center">
          <template #default="scope">{{ scope.row.startDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="结束日期" min-width="140" align="center">
          <template #default="scope">{{ scope.row.endDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="当前学期" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '当前学期' : '非当前学期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后操作时间" width="180" align="center">
          <template #default="scope">{{ scope.row.updateTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
            <el-button
              link
              type="success"
              :disabled="scope.row.status === 1"
              @click="setCurrent(scope.row)"
            >设为当前学期</el-button>
            <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ==================== 新增 / 修改弹窗 ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="semesterForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="学期名称" prop="name">
          <el-input v-model="semesterForm.name" placeholder="如：2026-2027学年第一学期" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="semesterForm.startDate"
            type="date"
            placeholder="请选择开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="handleStartDateChange"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="semesterForm.endDate"
            type="date"
            placeholder="请选择结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
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
/* ==================== 顶部条 ==================== */
.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-text {
  font-size: 13px;
  color: #909399;
}
</style>
