/**
 * 路由配置（本项目相对苍穹外卖的核心增量：登录后按角色动态过滤路由 / 菜单）
 *
 * 三部分组成：
 *   1. baseRoutes  基础路由：不需要权限即可访问（登录页 + 兜底重定向）
 *   2. asyncRoutes 异步路由：Layout 子树，每条路由的 meta 里带 { title, icon, roles }
 *      —— roles 是"能看到这条路由的角色码数组"
 *   3. 全局前置守卫：无 token 一律跳 /login；有 token 且还没按当前角色注入过路由时，
 *      按 localStorage 里 LoginVO.role 过滤 asyncRoutes，用 router.addRoute 一次性注入 Layout 父路由
 *      （含过滤后的 children），然后 next({ ...to, replace: true }) 重新匹配目标地址
 *
 * 角色码：0 超级管理员 / 1 学生 / 2 用工部门管理员 / 3 勤工助学中心管理员 / 4 财务
 */
import { createRouter, createWebHashHistory, RouterView } from 'vue-router'
import { h, ref } from 'vue'

// ==================== 视图组件 ====================
// Layout/Login 静态引入（首屏要用）；其余页面全部改成路由懒加载，
// 避免打开登录页时把所有页面 + ECharts 一次性下载下来（首屏从几十秒降到 1~2 秒）。
import LayoutView from '@/views/layout/index.vue'
import LoginView from '@/views/login/index.vue'
const DashboardView = () => import('@/views/dashboard/index.vue')
const SystemUserView = () => import('@/views/system/user.vue')
const SystemDeptView = () => import('@/views/system/dept.vue')
const SystemStudentView = () => import('@/views/system/student.vue')
const SystemSemesterView = () => import('@/views/system/semester.vue')
const SystemDictView = () => import('@/views/system/dict.vue')
const PositionListView = () => import('@/views/position/list.vue')
const PositionAuditView = () => import('@/views/position/audit.vue')
const ApplyPositionsView = () => import('@/views/apply/positions.vue')
const ApplyMyView = () => import('@/views/apply/my.vue')
const ApplyAuditView = () => import('@/views/apply/audit.vue')
const WorkhourFillView = () => import('@/views/workhour/fill.vue')
const WorkhourDeptView = () => import('@/views/workhour/dept.vue')
const WorkhourCenterView = () => import('@/views/workhour/center.vue')
const SalaryRuleView = () => import('@/views/salary/rule.vue')
const SettlementListView = () => import('@/views/settlement/list.vue')
const SettlementMineView = () => import('@/views/settlement/mine.vue')
const ReportIndexView = () => import('@/views/report/index.vue')
const LogIndexView = () => import('@/views/log/index.vue')
const ProfileIndexView = () => import('@/views/profile/index.vue')
const AiChatView = () => import('@/views/ai/index.vue')

/**
 * 目录（分组）路由用的"透传"组件：自身不渲染额外内容，只把子路由页面显示出来，
 * 这样左侧菜单才能用 el-sub-menu 做二级分组。
 */
const MenuGroup = {
  name: 'MenuGroup',
  render: () => h(RouterView)
}

/**
 * 兜底页：地址不存在、或当前角色没有这条路由时展示。
 * 注意：这里刻意使用"组件"而不是 redirect —— 兜底路由一旦写成 redirect: '/dashboard'，
 * 在动态路由注入之前 /dashboard 尚未注册，解析时会再次命中兜底路由，形成无限重定向。
 */
const NotFoundView = {
  name: 'NotFoundView',
  render: () =>
    h(
      'div',
      {
        style:
          'height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;background:#f4f4f5;color:#606266'
      },
      [
        h('div', { style: 'font-size:20px;color:#303133;font-weight:bold' }, '页面不存在或您没有访问权限'),
        h('div', { style: 'font-size:13px' }, '请从左侧菜单进入，或返回工作台'),
        h(
          'button',
          {
            style:
              'margin-top:6px;padding:8px 20px;border:none;border-radius:4px;background:#ffb400;color:#fff;font-size:14px;cursor:pointer',
            onClick: () => router.push('/dashboard')
          },
          '返回工作台'
        )
      ]
    )
}

// ==================== 异步路由：Layout 子树 ====================
export const asyncRoutes = [
  {
    path: '/',
    name: 'layout',
    component: LayoutView,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '工作台', icon: 'House', roles: [0, 1, 2, 3, 4] }
      },
      // ---------------- 系统管理（超级管理员） ----------------
      {
        path: 'system',
        name: 'system',
        component: MenuGroup,
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'Setting', roles: [0] },
        children: [
          { path: 'user', name: 'system-user', component: SystemUserView, meta: { title: '账号管理', icon: 'User', roles: [0] } },
          { path: 'dept', name: 'system-dept', component: SystemDeptView, meta: { title: '部门管理', icon: 'OfficeBuilding', roles: [0] } },
          { path: 'student', name: 'system-student', component: SystemStudentView, meta: { title: '学生档案', icon: 'Avatar', roles: [0] } },
          { path: 'semester', name: 'system-semester', component: SystemSemesterView, meta: { title: '学期管理', icon: 'Calendar', roles: [0] } },
          { path: 'dict', name: 'system-dict', component: SystemDictView, meta: { title: '字典管理', icon: 'Collection', roles: [0] } }
        ]
      },
      // ---------------- 岗位管理 ----------------
      {
        path: 'position',
        name: 'position',
        component: MenuGroup,
        meta: { title: '岗位管理', icon: 'Suitcase', roles: [0, 2, 3] },
        children: [
          { path: 'list', name: 'position-list', component: PositionListView, meta: { title: '岗位管理', icon: 'Briefcase', roles: [0, 2, 3] } },
          { path: 'audit', name: 'position-audit', component: PositionAuditView, meta: { title: '岗位审批', icon: 'Check', roles: [0, 3] } }
        ]
      },
      // ---------------- 报名管理 ----------------
      {
        path: 'apply',
        name: 'apply',
        component: MenuGroup,
        meta: { title: '报名管理', icon: 'Memo', roles: [1, 2, 3] },
        children: [
          { path: 'positions', name: 'apply-positions', component: ApplyPositionsView, meta: { title: '岗位浏览报名', icon: 'Search', roles: [1] } },
          { path: 'my', name: 'apply-my', component: ApplyMyView, meta: { title: '我的报名', icon: 'Document', roles: [1] } },
          { path: 'audit', name: 'apply-audit', component: ApplyAuditView, meta: { title: '报名审核', icon: 'Finished', roles: [2, 3] } }
        ]
      },
      // ---------------- 工时管理 ----------------
      {
        path: 'workhour',
        name: 'workhour',
        component: MenuGroup,
        meta: { title: '工时管理', icon: 'Stopwatch', roles: [1, 2, 3] },
        children: [
          { path: 'fill', name: 'workhour-fill', component: WorkhourFillView, meta: { title: '我的工时', icon: 'Timer', roles: [1] } },
          { path: 'dept', name: 'workhour-dept', component: WorkhourDeptView, meta: { title: '工时部门审核', icon: 'EditPen', roles: [2] } },
          { path: 'center', name: 'workhour-center', component: WorkhourCenterView, meta: { title: '工时中心复核', icon: 'Select', roles: [3] } }
        ]
      },
      // ---------------- 薪酬与结算 ----------------
      {
        path: 'salary/rule',
        name: 'salary-rule',
        component: SalaryRuleView,
        meta: { title: '薪酬规则', icon: 'Money', roles: [0, 3] }
      },
      {
        path: 'settlement',
        name: 'settlement',
        component: MenuGroup,
        meta: { title: '结算管理', icon: 'Coin', roles: [0, 1, 3, 4] },
        children: [
          { path: 'list', name: 'settlement-list', component: SettlementListView, meta: { title: '结算单管理', icon: 'Tickets', roles: [0, 3, 4] } },
          { path: 'mine', name: 'settlement-mine', component: SettlementMineView, meta: { title: '我的薪酬', icon: 'Wallet', roles: [1] } }
        ]
      },
      // ---------------- 报表 / 日志 / 个人中心 ----------------
      {
        path: 'report/index',
        name: 'report-index',
        component: ReportIndexView,
        meta: { title: '统计报表', icon: 'TrendCharts', roles: [0, 3, 4] }
      },
      {
        path: 'log/index',
        name: 'log-index',
        component: LogIndexView,
        meta: { title: '操作日志', icon: 'List', roles: [0, 3] }
      },
      {
        path: 'profile/index',
        name: 'profile-index',
        component: ProfileIndexView,
        meta: { title: '个人中心', icon: 'UserFilled', roles: [0, 1, 2, 3, 4] }
      },
      // ---------------- AI 智能助手（LangChain4j，工具调用 + 流式） ----------------
      {
        path: 'ai',
        name: 'ai-chat',
        component: AiChatView,
        meta: { title: 'AI 智能助手', icon: 'ChatDotRound', roles: [0, 1, 2, 3, 4] }
      }
    ]
  }
]

// ==================== 基础路由（不需要权限） ====================
export const baseRoutes = [
  { path: '/login', name: 'login', component: LoginView },
  // 兜底：地址不存在 / 当前角色没有这条路由时展示提示页（不能写成 redirect，否则会在
  // 动态路由注入前反复重定向到尚未注册的 /dashboard）
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
]

const router = createRouter({
  // 使用哈希模式，URL 形如 localhost/#/dashboard
  history: createWebHashHistory(),
  routes: [...baseRoutes]
})

// ==================== 动态路由：按角色过滤 + 注入 ====================

/**
 * 按角色过滤路由表：只有 meta.roles 包含当前 role 的路由才保留
 * - 叶子路由：meta.roles 为空、或包含 role → 保留
 * - 目录路由：递归过滤 children，至少剩一个可见子路由才保留整个分组
 * @param {Array} routes 路由数组
 * @param {Number} role 当前登录用户的角色码
 */
const filterRoutesByRole = (routes, role) => {
  const result = []
  routes.forEach((route) => {
    if (route.children && route.children.length > 0) {
      const children = filterRoutesByRole(route.children, role)
      if (children.length > 0) {
        result.push({ ...route, children })
      }
      return
    }
    const roles = route.meta && route.meta.roles
    if (!roles || roles.indexOf(role) > -1) {
      result.push({ ...route })
    }
  })
  return result
}

/** 拼接父子路由的绝对路径（'system' + 'user' → '/system/user'） */
const resolvePath = (basePath, path) => {
  if (path.indexOf('/') === 0) {
    return path
  }
  if (!basePath || basePath === '/') {
    return '/' + path
  }
  return basePath.replace(/\/$/, '') + '/' + path
}

/**
 * 把过滤后的路由表转换成左侧菜单需要的数据结构：[{ title, icon, path, children? }]
 * 说明：只剩一个可见子菜单的分组会被"提升"为一级菜单项，避免左侧出现只有一个子项的下拉。
 */
const buildMenus = (routes, basePath = '') => {
  const menus = []
  routes.forEach((route) => {
    const fullPath = resolvePath(basePath, route.path)
    const meta = route.meta || {}
    if (route.children && route.children.length > 0) {
      const children = buildMenus(route.children, fullPath)
      if (children.length === 0) {
        return
      }
      if (children.length === 1) {
        menus.push(children[0])
        return
      }
      menus.push({ title: meta.title, icon: meta.icon, path: fullPath, children })
      return
    }
    menus.push({ title: meta.title, icon: meta.icon, path: fullPath })
  })
  return menus
}

/** 当前已注入动态路由的角色码，null 表示还没注入 */
let injectedRole = null

/** 左侧菜单数据（按角色过滤后的菜单数组），layout 组件直接消费 */
export const menuRoutes = ref([])

/** 读取 localStorage 里的登录信息（LoginVO），解析失败按未登录处理 */
export const getLoginUser = () => {
  try {
    return JSON.parse(localStorage.getItem('token') || 'null')
  } catch (e) {
    return null
  }
}

/** 清空动态路由（退出登录 / 角色变化时调用） */
export const resetRouter = () => {
  if (router.hasRoute('layout')) {
    router.removeRoute('layout')
  }
  injectedRole = null
  menuRoutes.value = []
}

// ==================== 全局前置守卫 ====================
router.beforeEach((to, from, next) => {
  const loginUser = getLoginUser()

  // 1. 没有登录信息：只放行登录页
  if (!loginUser || !loginUser.token) {
    if (to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return
  }

  const role = Number(loginUser.role)

  // 旧 token（没有 role 字段）会得到 NaN；NaN !== NaN，会让下面的"已注入"判断永远为 false，
  // 导致守卫反复 next() 造成无限重定向。这里直接当成未登录处理。
  if (!Number.isFinite(role)) {
    localStorage.removeItem('token')
    next('/login')
    return
  }

  // 2. 已按当前角色注入过动态路由：直接放行（已登录又访问登录页则回工作台）
  if (injectedRole !== null && injectedRole === role) {
    if (to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 3. 首次进入、或登录角色发生变化：过滤出该角色可见的 children，一次性注入 Layout 父路由
  if (injectedRole !== null) {
    resetRouter()
  }
  const children = filterRoutesByRole(asyncRoutes[0].children, role)
  menuRoutes.value = buildMenus(children)
  router.addRoute({ ...asyncRoutes[0], children })
  injectedRole = role

  // 动态路由刚注入，原目标可能还没匹配上，用 replace 重新匹配一次。
  // 注意：根路径 '/' 要直接给 '/dashboard'。如果继续 next({ path: '/' })，会再次命中
  // Layout 路由的 redirect: '/dashboard'，形成「守卫→redirect→守卫」的循环。
  // 这里显式只带 path/query/hash：不能直接 spread `to`，因为 `to.name` 可能是兜底路由的
  // 'not-found'（注入前目标地址都没匹配上），按 name 重新解析会再次落到兜底页。
  const target = to.path === '/' ? '/dashboard' : to.path
  next({ path: target, query: to.query, hash: to.hash, replace: true })
})

export default router
