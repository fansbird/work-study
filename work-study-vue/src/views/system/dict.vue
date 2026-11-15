<!--
  字典管理
  ====================================================================
  路由：/system/dict    可见角色：超级管理员(0)
  契约依据：第 5 节 DictController（/dict）、第 3 节 sys_dict 表（索引 idx_dict_code）、第 7 节前端契约
  写法标杆：src/views/system/user.vue（弹窗表单 / 行内操作 / 危险操作先 ElMessageBox.confirm 的写法）

  接口对齐：
    GET    /dict/list?dictCode=  查询字典项（dictCode 不传即查全部，本页用它做"按 dictCode 分组浏览"）
    POST   /dict                 新增 SysDictDTO{id,dictName,dictCode,itemText,itemValue,sort,status}
    PUT    /dict                 修改 SysDictDTO
    DELETE /dict/{id}            删除

  说明：字典没有"根据 id 查询详情"的接口，修改态直接用表格行数据回填（列表已含全字段）。
  ====================================================================
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApi, addApi, updateApi, deleteApi } from '@/api/dict'

// ==================== 原始数据与分组 ====================
// 全部字典项（一次性拉全量，再在前端按 dictCode 分组）
const allDict = ref([])
// 分组关键字过滤（按字典名称 / 字典编码匹配）
const filterText = ref('')
// 当前选中的分组 dictCode
const activeCode = ref('')

// 按 dictCode 分组：[{ dictCode, dictName, items:[...] }]，组内按 sort 升序
const groups = computed(() => {
  const map = new Map()
  allDict.value.forEach((item) => {
    const code = item.dictCode || '未分类'
    if (!map.has(code)) {
      map.set(code, { dictCode: code, dictName: item.dictName || code, items: [] })
    }
    map.get(code).items.push(item)
  })
  const list = Array.from(map.values())
  list.forEach((group) => {
    group.items.sort((a, b) => (a.sort || 0) - (b.sort || 0))
  })
  list.sort((a, b) => a.dictCode.localeCompare(b.dictCode))
  return list
})

// 分组列表（套一层关键字过滤）
const filteredGroups = computed(() => {
  const keyword = filterText.value.trim().toLowerCase()
  if (!keyword) {
    return groups.value
  }
  return groups.value.filter(
    (group) =>
      group.dictCode.toLowerCase().includes(keyword) ||
      (group.dictName || '').toLowerCase().includes(keyword)
  )
})

// 当前分组（选中项失效时回退到第一个分组）
const activeGroup = computed(() => {
  const list = filteredGroups.value
  return list.find((group) => group.dictCode === activeCode.value) || list[0] || null
})
// 右侧表格数据
const itemList = computed(() => (activeGroup.value ? activeGroup.value.items : []))

const selectGroup = (group) => {
  activeCode.value = group.dictCode
}

// ==================== 新增 / 编辑弹窗 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('新增字典项')
const formRef = ref()

// SysDictDTO{id,dictName,dictCode,itemText,itemValue,sort,status}
const emptyForm = () => ({
  id: null,
  dictName: '',
  dictCode: '',
  itemText: '',
  itemValue: '',
  sort: 0,
  status: 1
})
const dictForm = ref(emptyForm())

const rules = ref({
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  itemText: [{ required: true, message: '请输入字典项文本', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入字典项值', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
})

// ==================== 钩子函数 ====================
onMounted(() => {
  loadDict()
})

// ==================== 查询 ====================
const loadDict = async () => {
  try {
    // 不传 dictCode = 查询全部字典项
    const res = await listApi()
    if (res.code) {
      allDict.value = res.data || []
      // 选中项不存在时（首次进入 / 该分组被删空）回退到第一个分组
      if (!groups.value.some((group) => group.dictCode === activeCode.value)) {
        activeCode.value = groups.value.length > 0 ? groups.value[0].dictCode : ''
      }
    } else {
      ElMessage.error(res.msg || '字典数据查询失败')
    }
  } catch (e) {
    ElMessage.error('字典数据查询失败')
  }
}

// ==================== 新增 / 编辑 ====================
// 新增：默认继承当前选中的分组（字典名称 + 字典编码），也可以手改成新的分组
const openAdd = () => {
  dialogTitle.value = '新增字典项'
  dictForm.value = emptyForm()
  if (activeGroup.value) {
    dictForm.value.dictName = activeGroup.value.dictName
    dictForm.value.dictCode = activeGroup.value.dictCode
  }
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openEdit = (row) => {
  dialogTitle.value = '修改字典项'
  dictForm.value = {
    id: row.id,
    dictName: row.dictName,
    dictCode: row.dictCode,
    itemText: row.itemText,
    itemValue: row.itemValue,
    sort: row.sort === null || row.sort === undefined ? 0 : row.sort,
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
      const isEdit = !!dictForm.value.id
      const res = isEdit ? await updateApi(dictForm.value) : await addApi(dictForm.value)
      if (res.code) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        // 新增/改编码后把右侧切到目标分组，便于立刻看到结果
        activeCode.value = dictForm.value.dictCode
        loadDict()
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } catch (e) {
      ElMessage.error('保存失败')
    }
  })
}

// ==================== 删除 ====================
const remove = (row) => {
  ElMessageBox.confirm(
    `确认要删除字典项【${row.itemText}】吗？删除后不可恢复。`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      const res = await deleteApi(row.id)
      if (res.code) {
        ElMessage.success('删除成功')
        loadDict()
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
  <div class="dict-page">
    <!-- ==================== 顶部条：分组过滤 + 新增 ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true">
        <el-form-item label="字典分组">
          <el-input
            v-model="filterText"
            placeholder="按字典名称 / 编码筛选"
            clearable
            style="width: 240px"
          />
        </el-form-item>
      </el-form>
      <el-button type="warning" @click="openAdd">+ 新增字典项</el-button>
    </div>

    <!-- ==================== 左：分组列表；右：该分组的字典项 ==================== -->
    <div class="dict-body">
      <div class="page-card group-panel">
        <div class="panel-title">字典分组（{{ filteredGroups.length }}）</div>
        <el-empty v-if="filteredGroups.length === 0" description="暂无字典分组" :image-size="60" />
        <ul v-else class="group-list">
          <li
            v-for="group in filteredGroups"
            :key="group.dictCode"
            class="group-item"
            :class="{ active: activeGroup && activeGroup.dictCode === group.dictCode }"
            @click="selectGroup(group)"
          >
            <div class="group-name">{{ group.dictName }}</div>
            <div class="group-code">{{ group.dictCode }}（{{ group.items.length }}）</div>
          </li>
        </ul>
      </div>

      <div class="page-card item-panel">
        <div class="panel-title">
          {{ activeGroup ? activeGroup.dictName + '（' + activeGroup.dictCode + '）' : '字典项' }}
        </div>
        <el-table :data="itemList" style="width: 100%">
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column prop="itemText" label="字典项文本" min-width="160" align="center" show-overflow-tooltip />
          <el-table-column prop="itemValue" label="字典项值" min-width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="90" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="openEdit(scope.row)">修改</el-button>
              <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ==================== 新增 / 修改弹窗 ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="dictForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictForm.dictName" placeholder="如：岗位类别" />
        </el-form-item>
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="dictForm.dictCode" placeholder="如：position_category" />
        </el-form-item>
        <el-form-item label="字典项文本" prop="itemText">
          <el-input v-model="dictForm.itemText" placeholder="页面上展示的文字，如：行政助理" />
        </el-form-item>
        <el-form-item label="字典项值" prop="itemValue">
          <el-input v-model="dictForm.itemValue" placeholder="业务实际存库的值，如：行政助理 或 0" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="dictForm.sort" :min="0" :max="9999" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <!-- 用 el-switch + active-value/inactive-value，不用 el-radio 的 label 充当 value -->
          <el-switch
            v-model="dictForm.status"
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
/* ==================== 顶部条 ==================== */
.search-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

/* ==================== 左右分栏 ==================== */
.dict-body {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.group-panel {
  width: 250px;
  flex: none;
  max-height: 620px;
  overflow-y: auto;
}

.item-panel {
  flex: 1;
  min-width: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}

/* ==================== 分组列表 ==================== */
.group-list {
  list-style: none;
}

.group-item {
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  border-left: 3px solid transparent;
  margin-bottom: 4px;
}

.group-item:hover {
  background-color: #f5f7fa;
}

.group-item.active {
  background-color: #fdf3dc;
  border-left-color: #ffb400;
}

.group-name {
  font-size: 14px;
  color: #303133;
}

.group-code {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  word-break: break-all;
}
</style>
