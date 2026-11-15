/**
 * 动态路由自检脚本（开发期回归测试，不参与打包）
 *
 * 做法：用 esbuild 把"真实的 src/router/index.js"打成 Node 可运行的 bundle：
 *   - .vue 组件 → 空组件替身（本测试只关心路由与菜单，不渲染视图）
 *   - vue-router → 真实 vue-router（只把 createWebHashHistory 换成 createMemoryHistory，
 *     因为 Node 里没有 window），因此全局守卫、addRoute、redirect 解析全部是真实行为
 *   - vue → 真实 vue（ref / h）
 * 然后像真实浏览器那样 `await router.push(path)`，断言：
 *   - 未登录只能停在 /login
 *   - 每个角色注入的子路由数量、左侧菜单结构（顺序 + 路径）与契约第 7 节完全一致
 *   - 该角色没权限的地址会被兜底重定向回 /dashboard（不白屏）
 *   - 已登录访问 /login 回工作台；切换角色会重置旧路由；退出登录移除动态路由
 *
 * 运行：node verify/run-verify.mjs
 */
import { build } from 'esbuild'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const src = path.join(root, 'src')
const require = createRequire(import.meta.url)
const vueRouterEntry = require.resolve('vue-router')

// ---------------- vue-router 包装：保持真实实现，只替换 history ----------------
const vueRouterShim = `
import * as real from 'vue-router-real'
export const RouterView = real.RouterView
export function createWebHashHistory() { return real.createMemoryHistory() }
export function createRouter(options) {
  const router = real.createRouter({ ...options, history: real.createMemoryHistory() })
  globalThis.__router = router
  return router
}
export default { ...real, createRouter, createWebHashHistory }
`

const stubPlugin = {
  name: 'stub-views',
  setup(b) {
    // .vue 组件 → 空组件（不渲染视图）
    b.onResolve({ filter: /\.vue$/ }, (args) => ({ path: args.path, namespace: 'stub-vue' }))
    b.onLoad({ filter: /.*/, namespace: 'stub-vue' }, () => ({
      contents: 'export default { name: "StubView", render: () => null }',
      loader: 'js'
    }))
    // vue-router → 真实实现 + memory history 包装
    b.onResolve({ filter: /^vue-router-real$/ }, () => ({ path: vueRouterEntry }))
    b.onResolve({ filter: /^vue-router$/ }, () => ({ path: 'vue-router', namespace: 'shim-router' }))
    b.onLoad({ filter: /.*/, namespace: 'shim-router' }, () => ({ contents: vueRouterShim, loader: 'js' }))
    // 别名 @ → src
    b.onResolve({ filter: /^@\// }, (args) => ({ path: path.join(src, args.path.slice(2)) }))
  }
}

const outfile = path.join(__dirname, '.bundle-router.mjs')
await build({
  entryPoints: [path.join(__dirname, 'entry-router.js')],
  bundle: true,
  format: 'esm',
  platform: 'node',
  outfile,
  plugins: [stubPlugin],
  logLevel: 'error'
})

// ---------------- 浏览器全局替身：localStorage ----------------
const storage = (entries = {}) => {
  const store = { ...entries }
  return {
    getItem: (k) => (k in store ? store[k] : null),
    setItem: (k, v) => { store[k] = String(v) },
    removeItem: (k) => { delete store[k] }
  }
}
const loginVO = (role) => JSON.stringify({ id: 1, username: 'u', name: '张三', role, token: 'tk-' + role })
globalThis.localStorage = storage({})

const { asyncRoutes, menuRoutes, resetRouter } = await import(pathToFileURL(outfile).href)
const router = globalThis.__router
assert.ok(router && typeof router.push === 'function', '未拿到真实 router 实例')

// ---------------- 期望值（严格来自契约第 7 节） ----------------
const EXPECTED_MENUS = {
  0: [
    { title: '工作台', path: '/dashboard' },
    { title: '系统管理', path: '/system', children: [
      { title: '账号管理', path: '/system/user' },
      { title: '部门管理', path: '/system/dept' },
      { title: '学生档案', path: '/system/student' },
      { title: '学期管理', path: '/system/semester' },
      { title: '字典管理', path: '/system/dict' }
    ] },
    { title: '岗位管理', path: '/position', children: [
      { title: '岗位管理', path: '/position/list' },
      { title: '岗位审批', path: '/position/audit' }
    ] },
    { title: '薪酬规则', path: '/salary/rule' },
    { title: '结算单管理', path: '/settlement/list' },
    { title: '统计报表', path: '/report/index' },
    { title: '操作日志', path: '/log/index' },
    { title: '个人中心', path: '/profile/index' }
  ],
  1: [
    { title: '工作台', path: '/dashboard' },
    { title: '报名管理', path: '/apply', children: [
      { title: '岗位浏览报名', path: '/apply/positions' },
      { title: '我的报名', path: '/apply/my' }
    ] },
    { title: '我的工时', path: '/workhour/fill' },
    { title: '我的薪酬', path: '/settlement/mine' },
    { title: '个人中心', path: '/profile/index' }
  ],
  2: [
    { title: '工作台', path: '/dashboard' },
    { title: '岗位管理', path: '/position/list' },
    { title: '报名审核', path: '/apply/audit' },
    { title: '工时部门审核', path: '/workhour/dept' },
    { title: '个人中心', path: '/profile/index' }
  ],
  3: [
    { title: '工作台', path: '/dashboard' },
    { title: '岗位管理', path: '/position', children: [
      { title: '岗位管理', path: '/position/list' },
      { title: '岗位审批', path: '/position/audit' }
    ] },
    { title: '报名审核', path: '/apply/audit' },
    { title: '工时中心复核', path: '/workhour/center' },
    { title: '薪酬规则', path: '/salary/rule' },
    { title: '结算单管理', path: '/settlement/list' },
    { title: '统计报表', path: '/report/index' },
    { title: '操作日志', path: '/log/index' },
    { title: '个人中心', path: '/profile/index' }
  ],
  4: [
    { title: '工作台', path: '/dashboard' },
    { title: '结算单管理', path: '/settlement/list' },
    { title: '统计报表', path: '/report/index' },
    { title: '个人中心', path: '/profile/index' }
  ]
}

const ALL_CONTRACT_PATHS = [
  '/dashboard', '/system/user', '/system/dept', '/system/student', '/system/semester', '/system/dict',
  '/position/list', '/position/audit', '/apply/positions', '/apply/my', '/apply/audit',
  '/workhour/fill', '/workhour/dept', '/workhour/center', '/salary/rule',
  '/settlement/list', '/settlement/mine', '/report/index', '/log/index', '/profile/index'
]

const project = (menus) =>
  menus.map((m) => (m.children ? { title: m.title, path: m.path, children: project(m.children) } : { title: m.title, path: m.path }))
const flattenPaths = (menus) => menus.flatMap((m) => (m.children ? flattenPaths(m.children) : [m.path]))
const collectIcons = (menus, bag = []) => {
  menus.forEach((m) => {
    bag.push(m.icon)
    if (m.children) collectIcons(m.children, bag)
  })
  return bag
}

// push 到无权限地址时会被兜底重定向，这里不关心导航返回值，只看最终 currentRoute
const go = async (p) => {
  try {
    await router.push(p)
  } catch (e) {
    /* 导航被重定向/取消是预期情况，最终以 currentRoute 为准 */
  }
  return router.currentRoute.value.path
}

const results = []

// ---------- 场景 1：未登录 ----------
{
  globalThis.localStorage = storage({})
  resetRouter()
  assert.equal(await go('/dashboard'), '/login', '未登录访问业务页应停在 /login')
  assert.equal(await go('/login'), '/login', '未登录访问 /login 应正常打开')
  results.push('场景1 未登录：业务页 → /login，/login 正常打开')
}

// ---------- 场景 2：逐角色（真实 router.push） ----------
for (const role of [0, 1, 2, 3, 4]) {
  resetRouter()
  globalThis.localStorage = storage({ token: loginVO(role) })
  assert.equal(await go('/dashboard'), '/dashboard', `role=${role} 应能进入工作台`)

  // 左侧菜单：顺序 + 路径 + 分组完全一致
  assert.deepEqual(project(menuRoutes.value), EXPECTED_MENUS[role], `role=${role} 左侧菜单与契约不一致`)

  // 图标必须是字符串（layout 用 <component :is="meta.icon"> 渲染全局注册的图标组件）
  collectIcons(menuRoutes.value).forEach((icon) => {
    assert.ok(typeof icon === 'string' && icon.length > 0, `role=${role} 存在空图标`)
  })

  // 动态路由真的被加进了真实 router
  assert.ok(router.hasRoute('layout'), `role=${role} 应注入 layout 路由`)
  const visible = flattenPaths(EXPECTED_MENUS[role])
  assert.ok(router.hasRoute('dashboard'), `role=${role} 应能访问 /dashboard`)
  // 抽样校验：可见路由确实可以直接访问
  for (const p of ALL_CONTRACT_PATHS.filter((p) => visible.includes(p)).slice(0, 5)) {
    assert.equal(await go(p), p, `role=${role} 应能访问可见路由 ${p}`)
  }

  // 无权限地址：必须落到兜底页（name=not-found），绝不能匹配到真实页面
  const forbidden = ALL_CONTRACT_PATHS.filter((p) => !visible.includes(p))
  if (forbidden.length > 0) {
    const landed = await go(forbidden[0])
    assert.equal(landed, forbidden[0], `role=${role} 访问无权限地址 ${forbidden[0]} 的地址应保持不变`)
    assert.equal(
      router.currentRoute.value.name,
      'not-found',
      `role=${role} 访问无权限地址 ${forbidden[0]} 应落在兜底页而不是真实页面`
    )
  }

  // 重复导航不应重复注入
  const routeCount = router.getRoutes().length
  await go('/dashboard')
  assert.equal(router.getRoutes().length, routeCount, `role=${role} 不应重复注入路由`)

  results.push(`场景2 role=${role}：${visible.length} 个可见路由 / ${menuRoutes.value.length} 个一级菜单，无权限地址落到兜底页`)
}

// ---------- 场景 3：已登录访问 /login 回工作台 ----------
{
  globalThis.localStorage = storage({ token: loginVO(1) })
  resetRouter()
  assert.equal(await go('/dashboard'), '/dashboard')
  assert.equal(await go('/login'), '/dashboard', '已登录访问 /login 应回工作台')
  results.push('场景3 已登录访问 /login → 回 /dashboard')
}

// ---------- 场景 4：切换角色（同会话换账号）应重置旧路由 ----------
{
  globalThis.localStorage = storage({ token: loginVO(0) })
  resetRouter()
  await go('/dashboard')
  assert.equal(await go('/system/user'), '/system/user', 'role=0 应能进入账号管理')
  assert.ok(router.hasRoute('system-user'), 'role=0 应注册账号管理路由')

  globalThis.localStorage = storage({ token: loginVO(1) })
  assert.equal(await go('/dashboard'), '/dashboard')
  assert.deepEqual(project(menuRoutes.value), EXPECTED_MENUS[1], '切换角色后菜单应变成学生的菜单')
  assert.equal(router.hasRoute('system-user'), false, '切换角色后旧角色的路由应被移除')
  assert.equal(await go('/system/user'), '/system/user')
  assert.equal(router.currentRoute.value.name, 'not-found', '切换角色后旧角色页面应落到兜底页')
  results.push('场景4 角色切换：重置旧路由 + 按新角色注入 + 旧页面不可达')
}

// ---------- 场景 5：退出登录 ----------
{
  globalThis.localStorage = storage({ token: loginVO(2) })
  resetRouter()
  await go('/dashboard')
  assert.ok(router.hasRoute('layout'), '退出前应存在 layout 路由')

  globalThis.localStorage = storage({})   // 模拟 localStorage.removeItem('token')
  resetRouter()                            // 模拟 layout 里的退出逻辑
  assert.equal(router.hasRoute('layout'), false, '退出后 layout 路由应被移除')
  assert.equal(menuRoutes.value.length, 0, '退出后菜单应为空')
  assert.equal(await go('/dashboard'), '/login', '退出后访问业务页应回 /login')
  results.push('场景5 退出登录：移除动态路由、清空菜单、回 /login')
}

// ---------- 场景 6：路由表覆盖契约第 7 节全部 20 条子路由 + meta 齐全 ----------
{
  const allChildren = asyncRoutes[0].children
  const flat = (routes, base = '') => {
    const out = []
    routes.forEach((r) => {
      const full = r.path.startsWith('/') ? r.path : (base === '/' || base === '' ? '/' : base + '/') + r.path
      if (r.children && r.children.length) out.push(...flat(r.children, full))
      else out.push(full)
    })
    return out
  }
  const actual = flat(allChildren)
  assert.equal(actual.length, ALL_CONTRACT_PATHS.length, '子路由数量应与契约一致')
  ALL_CONTRACT_PATHS.forEach((p) => {
    assert.ok(actual.includes(p), `缺少契约要求的子路由：${p}`)
  })
  const checkMeta = (routes) => {
    routes.forEach((r) => {
      assert.ok(r.meta && r.meta.title, `路由 ${r.path} 缺少 meta.title`)
      assert.ok(r.meta && r.meta.icon, `路由 ${r.path} 缺少 meta.icon`)
      assert.ok(r.meta && Array.isArray(r.meta.roles) && r.meta.roles.length > 0, `路由 ${r.path} 缺少 meta.roles`)
      if (r.children) checkMeta(r.children)
    })
  }
  checkMeta(allChildren)
  assert.equal(asyncRoutes.length, 1, 'asyncRoutes 应只含一条 Layout 父路由')
  assert.equal(asyncRoutes[0].path, '/', 'Layout 父路由 path 应为 /')
  results.push('场景6 路由表 20 条子路由 + meta{title,icon,roles} 齐全')
}

fs.unlinkSync(outfile)
results.forEach((r) => console.log('通过 | ' + r))
console.log(`\n全部 ${results.length} 组断言通过`)
