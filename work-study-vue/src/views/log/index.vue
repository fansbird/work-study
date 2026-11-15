<!--
  操作日志
  ====================================================================
  路由：/log/index    可见角色：超级管理员(0)、勤工助学中心管理员(3)
  契约依据：第 5 节 OperLogController（/operLog）、第 3 节 sys_oper_log 表（索引 idx_oper_time）、第 6 节 AOP 操作日志
  写法标杆：src/views/system/user.vue（搜索栏 / 表格+分页 / 详情弹窗三段式）

  接口对齐（已按后端 OperLogController 的真实签名逐项核对）：
    GET /operLog/page  分页查询，查询参数：
         title        模块标题（模糊）
         begin / end  操作日期范围，格式 yyyy-MM-dd（后端是 LocalDate + @DateTimeFormat("yyyy-MM-dd")，
                      所以日期选择器必须用 daterange + value-format="YYYY-MM-DD"，传日期时间会绑定失败）
         page / pageSize 分页
       （后端还支持 operUserName 操作人、status 1正常0异常，本页按要求只做"模块 + 时间范围"两个条件）
    GET /operLog/{id}  根据 id 查询详情（列表里的 request_param / error_msg 可能被截断，详情以本接口为准）

  字段（SysOperLog）：title 模块标题 / method 方法名 / requestMethod 请求方式 / requestUrl 请求URL /
    requestParam 请求参数 / operUserId 操作人id / operUserName 操作人姓名 / operIp 操作IP /
    operTime 操作时间 / costTime 耗时(毫秒) / status 1正常0异常 / errorMsg 异常信息
  ====================================================================
-->
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageApi, getByIdApi } from '@/api/operLog'

// ==================== 搜索条件 ====================
// title 模块标题（对应 sys_oper_log.title，即 @Log(title="模块名") 标注的名字）
// dateRange 操作日期范围，el-date-picker 的 daterange 数组 [开始, 结束]，格式 yyyy-MM-dd
const searchForm = ref({ title: '', dateRange: [] })

// ==================== 列表数据 ====================
const logList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailRow = ref({})

// ==================== 钩子函数 ====================
onMounted(() => {
  search()
})

// ==================== 列表查询 ====================
const search = async () => {
  // 后端签名：title / begin / end（yyyy-MM-dd）/ page / pageSize
  const params = {
    title: searchForm.value.title || undefined,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  const range = searchForm.value.dateRange || []
  if (range[0]) {
    params.begin = range[0]
  }
  if (range[1]) {
    params.end = range[1]
  }
  try {
    const res = await pageApi(params)
    if (res.code) {
      // 分页返回 PageResult{total, records}
      logList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '操作日志查询失败')
    }
  } catch (e) {
    ElMessage.error('操作日志查询失败')
  }
}

// 点击查询按钮：回到第一页再查询
const handleSearch = () => {
  currentPage.value = 1
  search()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.value = { title: '', dateRange: [] }
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

// ==================== 查看详情 ====================
// 列表数据可能被截断，详情统一按 id 重新查询
const openDetail = async (row) => {
  try {
    const res = await getByIdApi(row.id)
    if (!res.code) {
      ElMessage.error(res.msg || '查询日志详情失败')
      return
    }
    detailRow.value = res.data || row
    detailVisible.value = true
  } catch (e) {
    // 详情接口异常时退化为展示列表行数据，保证弹窗仍可用
    detailRow.value = row
    detailVisible.value = true
  }
}
</script>

<template>
  <div class="log-page">
    <!-- ==================== 搜索栏（模块 / 时间范围） ==================== -->
    <div class="page-card search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="操作模块">
          <el-input v-model="searchForm.title" placeholder="请输入模块名，如：部门管理" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="操作日期">
          <!-- 后端 begin/end 是 LocalDate（yyyy-MM-dd），必须用 daterange + 日期格式，不能带时间 -->
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="query-btn" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ==================== 数据表格 ==================== -->
    <div class="page-card">
      <el-table :data="logList" style="width: 100%">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="title" label="操作模块" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column label="请求方式" width="100" align="center">
          <template #default="scope">{{ scope.row.requestMethod || '--' }}</template>
        </el-table-column>
        <el-table-column label="请求URL" min-width="200" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.requestUrl || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="110" align="center">
          <template #default="scope">{{ scope.row.operUserName || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作IP" min-width="130" align="center" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.operIp || '--' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="scope">
            {{ scope.row.costTime === null || scope.row.costTime === undefined ? '--' : scope.row.costTime + ' ms' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="180" align="center">
          <template #default="scope">{{ scope.row.operTime || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看详情</el-button>
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

    <!-- ==================== 日志详情弹窗（含请求参数与异常信息） ==================== -->
    <el-dialog v-model="detailVisible" title="日志详情" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作模块">{{ detailRow.title || '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailRow.status === 1 ? 'success' : 'danger'" size="small">
            {{ detailRow.status === 1 ? '正常' : '异常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detailRow.requestMethod || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detailRow.operUserName || (detailRow.operUserId ? '账号ID：' + detailRow.operUserId : '--') }}
        </el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ detailRow.requestUrl || '--' }}</el-descriptions-item>
        <el-descriptions-item label="后端方法" :span="2">{{ detailRow.method || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detailRow.operIp || '--' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detailRow.costTime === null || detailRow.costTime === undefined ? '--' : detailRow.costTime + ' ms' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailRow.operTime || '--' }}</el-descriptions-item>
      </el-descriptions>

      <div class="block-title">请求参数</div>
      <pre class="code-block">{{ detailRow.requestParam || '无' }}</pre>

      <template v-if="detailRow.status === 0">
        <div class="block-title error-title">异常信息</div>
        <pre class="code-block error-block">{{ detailRow.errorMsg || '无' }}</pre>
      </template>

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

/* ==================== 详情里的参数/异常文本块 ==================== */
.block-title {
  font-size: 13px;
  font-weight: bold;
  color: #303133;
  margin: 14px 0 6px;
}

.error-title {
  color: #f56c6c;
}

.code-block {
  margin: 0;
  padding: 10px 12px;
  max-height: 220px;
  overflow: auto;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 18px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-block {
  background-color: #fef0f0;
  color: #f56c6c;
}
</style>
