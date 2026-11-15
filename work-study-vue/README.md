# work-study-vue —— 校园勤工助学管理系统（前端）

Vue3 + Vite + ElementPlus + VueRouter(hash) + Axios 前端工程，严格对齐《冻结契约规格》第 5、7 节。
工程结构与写法参照 `D:\JAVA\springproject\sky-take-out-vue`（只读参照，未做任何改动）。

## 一、本次交付范围（"地基 + 样板"分片）

已完整实现：

| 文件 | 说明 |
| --- | --- |
| `package.json` / `vite.config.js` / `index.html` | 与苍穹一致的依赖与代理（`/api`→`http://localhost:8080` 并 rewrite 去掉 `/api`；`/ws`→`ws://localhost:8080`，`ws:true`） |
| `src/main.js` / `src/App.vue` / `src/assets/main.css` | 注册 ElementPlus 中文包 + 全部图标 + 路由 |
| `src/utils/request.js` | 请求头 `token`（取 localStorage 里 LoginVO 的 `.token`）；响应直接返回 `response.data`；401 提示并跳 `/login` |
| `src/utils/websocket.js` | 连接 `/ws/{sid}`，收到 `{type,content}` 按 type=1..5 用 ElMessage 提示，并回调给调用方 |
| `src/utils/role.js` | 角色码常量、角色选项、角色中文名 / el-tag 类型（全系统共用，勿再硬编码） |
| `src/router/index.js` | **核心增量**：完整路由表（Layout 子树、`meta:{title,icon,roles}`）、`asyncRoutes`、全局守卫按角色动态 `addRoute` |
| `src/views/login/index.vue` | 登录，`LoginVO` 整体存 localStorage（key=`token`），按角色进入 `/dashboard` |
| `src/views/layout/index.vue` | 顶部系统名/用户名/角色标签/通知铃铛/退出；左侧按角色过滤后的动态菜单；主区 `router-view`；挂载-卸载 WebSocket |
| `src/views/dashboard/index.vue` | 全角色欢迎页 + 概览占位卡片（后端 `/report/overview` 就绪后自动带出真实数据）+ 按角色的常用入口 |
| `src/views/system/user.vue` | **样板页**：账号管理（表格 + 分页 + 新增/编辑对话框 + 启用禁用 + 重置密码） |
| `src/api/*.js` | 14 个 api 模块，函数路径/方式严格对齐契约第 5 节 |

**占位页（18 个）**：`views/system/{dept,student,semester,dict}.vue`、`views/position/{list,audit}.vue`、
`views/apply/{positions,my,audit}.vue`、`views/workhour/{fill,dept,center}.vue`、`views/salary/rule.vue`、
`views/settlement/{list,mine}.vue`、`views/report/index.vue`、`views/log/index.vue`、`views/profile/index.vue`。

> 占位页只保证"路由表完整 + 构建通过"，内容是 `el-empty`。**业务实现请直接整体替换这些文件**，
> 照 `src/views/system/user.vue` 的写法（搜索栏 / 表格 / 分页 / 弹窗 / 确认框），api 模块已经写好，直接 import 即可。
> 占位文件头部注释写明了该页的路由、可见角色、要用的 api 文件与状态机要点。

## 二、动态路由 / 动态菜单实现说明

1. **路由表分级**
   - `baseRoutes`：`/login` + 兜底 `/:pathMatch(.*)*`（重定向到 `/dashboard`，避免访问无权限地址白屏）。
   - `asyncRoutes`：只有一条 —— `path:'/'`、`name:'layout'`、`component: LayoutView`，其 `children` 才是全部业务页。
     每个 children 都带 `meta:{title,icon,roles}`；`roles` 是"能看到它的角色码数组"。
     需要二级菜单的用目录路由包一层（`component: MenuGroup`，内部只渲染 `<router-view>`）：
     `system`、`position`、`apply`、`workhour`、`settlement`。
2. **守卫逻辑**（`router.beforeEach`）
   - 无 `token` → 一律 `next('/login')`；已登录再访问 `/login` → `next('/dashboard')`。
   - 有 `token` 且"当前已注入的角色 === LoginVO.role" → 直接放行。
   - 否则：按 `role` 过滤 `asyncRoutes[0].children`（`meta.roles.indexOf(role) > -1`；目录路由递归过滤，
     只剩 0 个子路由的整组丢弃）→ `router.addRoute({...asyncRoutes[0], children})` **一次性注入 Layout 父路由** →
     `next({ path: to.path, query: to.query, hash: to.hash, replace: true })` 重新匹配目标地址。
   - **注意**：这里不能写成常见的 `next({ ...to, replace: true })`。注入前目标地址还没匹配上，`to.name` 会是
     兜底路由的 `not-found`，而 `resolve()` 优先按 `name` 解析，spread 写法会把用户又送回兜底页（例如登录后
     应进 `/dashboard` 却显示"页面不存在"）。只带 path/query/hash 才是安全的重新匹配方式。
   - 角色变化（同一次会话换账号）时先 `resetRouter()` 再注入，避免残留上一个角色的菜单。
3. **兜底路由**：`/:pathMatch(.*)*` → `NotFoundView`（地址不存在 / 当前角色无权限时展示提示页 + 「返回工作台」按钮）。
   **必须用组件，不能用 `redirect`**：动态路由注入前 `/dashboard` 尚未注册，`redirect: '/dashboard'` 会被兜底路由
   再次命中而无限重定向，导致未登录用户连 `/login` 都打不开。
3. **菜单数据**：守卫同时把过滤结果转成菜单数组 `menuRoutes`（`export const menuRoutes = ref([])`），
   layout 直接消费；只剩一个可见子菜单的目录会被"提升"为一级菜单项，避免出现只有一个子项的下拉。
   图标用 `meta.icon` 字符串 + `<component :is="item.icon" />`（main.js 已全局注册全部 ElementPlus 图标）。
4. **退出登录**：`localStorage.removeItem('token')` + `resetRouter()`（移除已注入路由并清空菜单）+ `router.push('/login')`。
5. **路由被拦**：后端返回 401 时，`utils/request.js` 会清掉失效 token 再跳 `/login`，防止守卫把用户又送回工作台。

## 三、各角色可见菜单（由 `meta.roles` 推导）

| 角色码 | 角色 | 可见菜单 |
| --- | --- | --- |
| 0 | 超级管理员 | 全部（10 组） |
| 1 | 学生 | 工作台、岗位浏览报名、我的报名、我的工时、我的薪酬、个人中心 |
| 2 | 用工部门管理员 | 工作台、岗位管理、报名审核、工时部门审核、个人中心 |
| 3 | 勤工助学中心管理员 | 工作台、岗位管理、岗位审批、报名审核、工时中心复核、薪酬规则、结算单管理、统计报表、操作日志、个人中心 |
| 4 | 财务 | 工作台、结算单管理、统计报表、个人中心 |

## 四、接口约定（写页面时必须对齐）

- 统一返回 `Result<T>{code,msg,data}`，`code=1` 成功；分页返回 `PageResult{total,records}`。
- 所有请求走 `src/utils/request.js`，`baseURL='/api'`，由 vite 代理去掉 `/api` 前缀；后端真实路径见各 api 文件注释。
- POST 且参数是"若干标量"的接口（`/position/audit`、`/apply/audit`、`/workHour/deptAudit`、
  `/workHour/centerAudit`、`/apply`）统一用**查询参数**（query）传递，后端用 `@RequestParam` 接收；
  `rejectReason` 为可选参数，不传即不拼接。
- 需要带 DTO 的写操作（新增/修改/审核以外的业务数据）用 `@RequestBody` 接收，前端直接传对象。
- 导出类接口（`/settlement/export/{id}`）用 `responseType:'blob'`。

## 五、动态路由回归自检

`verify/run-verify.mjs` 用 esbuild 把**真实的 `src/router/index.js`** 打包成 Node 可运行模块
（只把 `.vue` 换成空组件、`createWebHashHistory` 换成 `createMemoryHistory`，vue-router 本身是真实实现），
然后像浏览器一样 `router.push()`，断言 5 个角色各自的菜单结构与契约完全一致、无权限地址落到兜底页、
切换角色会重置旧路由、退出登录会移除动态路由：

```bash
node verify/run-verify.mjs
```

## 六、本地运行

```bash
npm install
npm run dev     # 开发，默认 http://localhost:5173，需后端 8080 已启动
npm run build   # 构建到 dist/
```
