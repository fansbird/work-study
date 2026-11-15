<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuRoutes, resetRouter, getLoginUser } from '@/router'
import { createWebSocket } from '@/utils/websocket'
import { roleLabel, roleTagType } from '@/utils/role'

const router = useRouter()

// ==================== 当前登录用户（LoginVO） ====================
const loginUser = ref(getLoginUser() || {})
const userName = computed(() => loginUser.value.name || loginUser.value.username || '未登录')
const roleText = computed(() => roleLabel(loginUser.value.role))
const roleTag = computed(() => roleTagType(loginUser.value.role))

// 左侧菜单：路由全局守卫按当前角色过滤后写入的菜单数组（见 src/router/index.js）
const menus = computed(() => menuRoutes.value)

// ==================== WebSocket 通知 ====================
// 消息类型：1 岗位审批 / 2 报名录用 / 3 工时审核 / 4 结算状态 / 5 定时提醒
const notices = ref([])
const unread = ref(0)
let websocket = null

// 收到后端推送：utils/websocket.js 已经弹过一次 ElMessage，这里只负责沉淀到铃铛列表
const onNotice = (notice) => {
  notices.value.unshift(notice)
  if (notices.value.length > 20) {
    notices.value.pop()
  }
  unread.value += 1
}

onMounted(() => {
  websocket = createWebSocket({ onMessage: onNotice })
  websocket.connect()
})

onUnmounted(() => {
  if (websocket) {
    websocket.disconnect()
  }
})

// 打开铃铛面板即视为已读
const readAll = () => {
  unread.value = 0
}

const clearNotices = () => {
  notices.value = []
  unread.value = 0
}

// ==================== 退出登录 ====================
const logout = () => {
  ElMessageBox.confirm('确认退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 先断开长连接，再清登录信息与动态路由，避免下一次登录残留上一个角色的菜单
    if (websocket) {
      websocket.disconnect()
      websocket = null
    }
    localStorage.removeItem('token')
    resetRouter()
    ElMessage.success('退出登录成功')
    router.push('/login')
  }).catch(() => {
    // 取消操作，无需处理
  })
}
</script>

<template>
  <div class="common-layout">
    <el-container>
      <!-- ==================== 顶部栏：系统名 + 通知铃铛 + 当前用户/角色 + 退出 ==================== -->
      <el-header class="header">
        <div class="header-left">
          <span class="logo">
            <el-icon><School /></el-icon>
          </span>
          <span class="title">校园勤工助学管理系统</span>
          <span class="subtitle">CAMPUS WORK-STUDY MANAGEMENT</span>
        </div>
        <div class="header-right">
          <!-- 通知铃铛：WebSocket 推送的消息在这里沉淀 -->
          <el-popover placement="bottom-end" :width="340" trigger="click" @show="readAll">
            <template #reference>
              <span class="bell-wrap">
                <el-badge :value="unread" :max="99" :hidden="unread === 0">
                  <el-icon class="bell-icon"><Bell /></el-icon>
                </el-badge>
              </span>
            </template>
            <div class="notice-head">
              <span>消息通知</span>
              <el-button link type="primary" size="small" @click="clearNotices">清空</el-button>
            </div>
            <el-empty v-if="notices.length === 0" description="暂无通知" :image-size="60" />
            <div v-else class="notice-list">
              <div v-for="(item, index) in notices" :key="index" class="notice-item">
                <el-tag size="small" :type="item.level">{{ item.typeText }}</el-tag>
                <div class="notice-body">
                  <div class="notice-content">{{ item.content }}</div>
                  <div class="notice-time">{{ item.time }}</div>
                </div>
              </div>
            </div>
          </el-popover>

          <el-dropdown>
            <span class="admin-dropdown">
              {{ userName }}
              <el-tag size="small" :type="roleTag" class="role-tag">{{ roleText }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <!-- ==================== 左侧动态菜单（按角色过滤后的菜单数组） ==================== -->
        <el-aside width="215px" class="aside">
          <el-menu
            router
            background-color="#2B2F3A"
            text-color="#bfcbd9"
            active-text-color="#ffb400"
            :default-active="$route.path"
          >
            <template v-for="item in menus" :key="item.path">
              <!-- 目录：渲染成子菜单 -->
              <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
                <template #title>
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                  <el-icon><component :is="child.icon" /></el-icon>
                  <span>{{ child.title }}</span>
                </el-menu-item>
              </el-sub-menu>
              <!-- 单级菜单 -->
              <el-menu-item v-else :index="item.path">
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-aside>

        <!-- ==================== 主内容区 ==================== -->
        <el-main class="main-content">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.common-layout {
  height: 100vh;
}

/* 顶部栏 */
.header {
  background-color: #ffb400;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  font-size: 26px;
  color: #fff;
  margin-right: 8px;
  display: flex;
  align-items: center;
}

.title {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  margin-right: 8px;
}

.subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

.bell-wrap {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.bell-icon {
  font-size: 20px;
  color: #fff;
}

.admin-dropdown {
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-tag {
  margin-left: 2px;
}

/* 通知面板 */
.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.notice-list {
  max-height: 320px;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.notice-body {
  flex: 1;
}

.notice-content {
  font-size: 13px;
  color: #333;
  line-height: 18px;
}

.notice-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* 左侧深色菜单 */
.aside {
  background-color: #2b2f3a;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.aside :deep(.el-menu-item),
.aside :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

/* 主内容区 */
.main-content {
  background-color: #f4f4f5;
  padding: 16px;
  overflow-y: auto;
}
</style>
