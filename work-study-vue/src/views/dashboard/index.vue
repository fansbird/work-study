<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { overviewApi } from '@/api/report'
import { listApi as semesterListApi } from '@/api/semester'
import { getLoginUser } from '@/router'
import { roleLabel, ROLE } from '@/utils/role'

const router = useRouter()

// ==================== 当前登录用户（LoginVO） ====================
const loginUser = ref(getLoginUser() || {})
const userName = computed(() => loginUser.value.name || loginUser.value.username || '同学')
const roleCode = computed(() => Number(loginUser.value.role))
const roleText = computed(() => roleLabel(loginUser.value.role))

// 今天日期，格式：YYYY.MM.DD
const today = ref('')

// 各角色的欢迎语 / 可用功能提示
const ROLE_TIPS = {
  [ROLE.SUPER_ADMIN]: '您拥有系统全部权限：可维护账号、部门、学生档案、学期与字典，审批岗位，查看统计报表与操作日志。',
  [ROLE.STUDENT]: '您可以浏览并报名勤工助学岗位、填报并提交工时、查看自己的薪酬结算情况。',
  [ROLE.DEPT_ADMIN]: '您可以管理本部门的岗位与报名录用，并审核本部门学生填报的工时。',
  [ROLE.CENTER_ADMIN]: '您可以审批全校岗位、复核工时、维护薪酬规则与结算单，并查看统计报表。',
  [ROLE.FINANCE]: '您可以确认与发放结算单，并查看薪酬统计报表。'
}
const roleTip = computed(() => ROLE_TIPS[roleCode.value] || '欢迎使用校园勤工助学管理系统。')

// ==================== 概览卡片 ====================
// 数值来自后端 /report/overview（OverviewVO），后端接口未就绪时保持 null，页面显示 "--"
const overview = ref({
  positionCount: null,
  publishedCount: null,
  applyCount: null,
  hiredCount: null,
  studentCount: null,
  deptCount: null,
  pendingWorkHourCount: null,
  totalHoursMonth: null,
  totalAmountMonth: null
})

// 卡片定义：key 对应 OverviewVO 的字段名
const STAT_CARDS = [
  { key: 'positionCount', label: '岗位总数', unit: '个', icon: 'Briefcase' },
  { key: 'publishedCount', label: '已发布岗位', unit: '个', icon: 'CircleCheck' },
  { key: 'applyCount', label: '报名总数', unit: '人次', icon: 'Document' },
  { key: 'hiredCount', label: '已录用人数', unit: '人', icon: 'UserFilled' },
  { key: 'studentCount', label: '在库学生数', unit: '人', icon: 'Avatar' },
  { key: 'deptCount', label: '用工部门数', unit: '个', icon: 'OfficeBuilding' },
  { key: 'pendingWorkHourCount', label: '待审核工时', unit: '条', icon: 'Timer' },
  { key: 'totalHoursMonth', label: '本月总工时', unit: '小时', icon: 'Clock' },
  { key: 'totalAmountMonth', label: '本月薪酬总额', unit: '元', icon: 'Money' }
]

// 金额（totalAmountMonth）保留两位小数，其余按原值展示
const formatValue = (key, value) => {
  if (value === null || value === undefined || value === '') {
    return '--'
  }
  if (key === 'totalAmountMonth') {
    return Number(value).toFixed(2)
  }
  return value
}

// ==================== 当前学期 ====================
const currentSemester = ref('')

// ==================== 数据加载 ====================
// 概览数据：后端 /report/overview 就绪后这里会自动带出真实数据；未就绪时保持占位，不打扰用户
const loadOverview = async () => {
  try {
    const res = await overviewApi()
    if (res && res.code && res.data) {
      overview.value = { ...overview.value, ...res.data }
    }
  } catch (e) {
    console.warn('[dashboard] 概览数据暂不可用，先展示占位数据', e)
  }
}

// 当前学期（sys_semester.status = 1）
const loadCurrentSemester = async () => {
  try {
    const res = await semesterListApi()
    if (res && res.code && Array.isArray(res.data)) {
      const current = res.data.find((item) => item.status === 1)
      if (current) {
        currentSemester.value = `${current.name}（${current.startDate} ~ ${current.endDate}）`
      }
    }
  } catch (e) {
    console.warn('[dashboard] 学期信息暂不可用', e)
  }
}

onMounted(() => {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  today.value = `${date.getFullYear()}.${month}.${day}`

  loadOverview()
  loadCurrentSemester()
})

// 页面跳转
const go = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="dashboard">
    <!-- ==================== 欢迎区 ==================== -->
    <div class="page-card welcome-card">
      <div class="welcome-left">
        <div class="welcome-title">
          {{ userName }}，欢迎回来
          <el-tag size="small" class="welcome-role">{{ roleText }}</el-tag>
        </div>
        <div class="welcome-tip">{{ roleTip }}</div>
        <div class="welcome-meta">
          <span>今天：{{ today }}</span>
          <span class="divider">|</span>
          <span>当前学期：{{ currentSemester || '--' }}</span>
        </div>
      </div>
      <div class="welcome-right">
        <el-icon><School /></el-icon>
      </div>
    </div>

    <!-- ==================== 概览卡片（后续接后端 /report/overview） ==================== -->
    <div class="page-card">
      <div class="card-header">
        <span class="card-title">数据概览</span>
        <span class="card-link" @click="go('/report/index')">统计报表 &gt;</span>
      </div>
      <div class="stat-grid">
        <div v-for="card in STAT_CARDS" :key="card.key" class="stat-item">
          <div class="stat-icon">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">
              {{ formatValue(card.key, overview[card.key]) }}
              <span class="stat-unit">{{ card.unit }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 常用入口 ==================== -->
    <div class="page-card">
      <div class="card-header">
        <span class="card-title">常用入口</span>
      </div>
      <div class="entry-row">
        <el-button v-if="roleCode === ROLE.STUDENT" type="warning" @click="go('/apply/positions')">岗位浏览报名</el-button>
        <el-button v-if="roleCode === ROLE.STUDENT" @click="go('/apply/my')">我的报名</el-button>
        <el-button v-if="roleCode === ROLE.STUDENT" @click="go('/workhour/fill')">我的工时</el-button>
        <el-button v-if="roleCode === ROLE.STUDENT" @click="go('/settlement/mine')">我的薪酬</el-button>

        <el-button v-if="roleCode === ROLE.DEPT_ADMIN || roleCode === ROLE.SUPER_ADMIN" type="warning" @click="go('/position/list')">岗位管理</el-button>
        <el-button v-if="roleCode === ROLE.DEPT_ADMIN || roleCode === ROLE.CENTER_ADMIN" @click="go('/apply/audit')">报名审核</el-button>
        <el-button v-if="roleCode === ROLE.DEPT_ADMIN" @click="go('/workhour/dept')">工时部门审核</el-button>

        <el-button v-if="roleCode === ROLE.CENTER_ADMIN || roleCode === ROLE.SUPER_ADMIN" type="warning" @click="go('/position/audit')">岗位审批</el-button>
        <el-button v-if="roleCode === ROLE.CENTER_ADMIN" @click="go('/workhour/center')">工时中心复核</el-button>
        <el-button v-if="roleCode === ROLE.CENTER_ADMIN" @click="go('/salary/rule')">薪酬规则</el-button>

        <el-button v-if="roleCode === ROLE.SUPER_ADMIN" @click="go('/system/user')">账号管理</el-button>
        <el-button v-if="roleCode === ROLE.SUPER_ADMIN" @click="go('/system/semester')">学期管理</el-button>

        <el-button v-if="roleCode === ROLE.FINANCE || roleCode === ROLE.CENTER_ADMIN || roleCode === ROLE.SUPER_ADMIN" type="warning" @click="go('/settlement/list')">结算单管理</el-button>
        <el-button v-if="roleCode === ROLE.FINANCE || roleCode === ROLE.CENTER_ADMIN || roleCode === ROLE.SUPER_ADMIN" @click="go('/report/index')">统计报表</el-button>

        <el-button @click="go('/profile/index')">个人中心</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 欢迎区 ==================== */
.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #fff8e6, #ffffff);
}

.welcome-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  display: flex;
  align-items: center;
}

.welcome-role {
  margin-left: 10px;
  font-weight: normal;
}

.welcome-tip {
  font-size: 13px;
  color: #666;
  margin-top: 10px;
  line-height: 20px;
}

.welcome-meta {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}

.welcome-meta .divider {
  margin: 0 8px;
}

.welcome-right {
  font-size: 64px;
  color: #ffe1a0;
}

/* ==================== 概览卡片 ==================== */
.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.card-link {
  margin-left: auto;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.card-link:hover {
  color: #ffb400;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #f8f8f8;
  border-radius: 4px;
  padding: 16px 18px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background-color: #fff3d6;
  color: #ffb400;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.stat-value {
  font-size: 22px;
  color: #333;
  margin-top: 4px;
}

.stat-unit {
  font-size: 12px;
  color: #999;
  margin-left: 2px;
}

/* ==================== 常用入口 ==================== */
.entry-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
