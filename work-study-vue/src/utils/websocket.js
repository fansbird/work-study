/**
 * WebSocket 工具（后端主动向前端推送业务通知）
 *
 * 作用：岗位审批、报名录用、工时审核、结算状态等动作发生后，后端需要"实时"告诉前端，
 *      普通 http 请求做不到后端主动推送，所以用 WebSocket：连接建立后后端可随时推消息。
 *
 * 使用方式（在页面组件里）：
 *   import { createWebSocket } from '@/utils/websocket'
 *   const ws = createWebSocket({ onMessage: (notice) => { ... } }) // 可选：拿到消息做二次处理（如铃铛列表）
 *   onMounted(() => { ws.connect() })     // 进入页面时连接后端
 *   onUnmounted(() => { ws.disconnect() }) // 离开页面时断开连接
 *
 * 后端端点：/ws/{sid}，vite.config.js 里已把前端的 /ws 代理到 ws://localhost:8080
 * 推送消息格式：{ "type": 1..5, "content": "提示文案" }
 */
import { ElMessage } from 'element-plus'

/**
 * 消息类型 → 展示文案与样式（与后端 WebSocketServer 推送约定一致）
 *   1 岗位审批结果 / 2 报名审核、录用 / 3 工时审核结果 / 4 结算状态 / 5 定时提醒
 */
const NOTICE_TYPES = {
  1: { typeText: '岗位审批', level: 'success' },
  2: { typeText: '报名录用', level: 'success' },
  3: { typeText: '工时审核', level: 'info' },
  4: { typeText: '结算状态', level: 'warning' },
  5: { typeText: '定时提醒', level: 'warning' }
}

/**
 * 创建 WebSocket 连接对象
 * @param {Object} options 可选配置，options.onMessage(notice) 会在收到消息时回调
 *        notice = { type, typeText, level, content, time }
 * @returns {Object} { connect, disconnect }
 */
export function createWebSocket(options = {}) {
  const { onMessage } = options

  // sid：后端用来区分"是哪一条连接"的标识，用时间戳 + 随机数拼一个，保证基本不重复
  const sid = Date.now() + '' + Math.floor(Math.random() * 1000000)

  // 保存 WebSocket 实例（用普通变量保存即可，不需要响应式）
  let ws = null

  /**
   * 建立连接
   */
  const connect = () => {
    // 已经连上、或正在连接中就直接返回，避免重复创建连接导致同一条消息被提示多次
    if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
      return
    }
    // 页面是 https 时要用 wss://，否则用 ws://；location.host 就是当前访问的"域名:端口"
    const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://'
    ws = new WebSocket(`${protocol}${location.host}/ws/${sid}`)

    // 连接建立成功时触发
    ws.onopen = () => {
      console.log('[WebSocket] 连接成功，sid = ' + sid)
    }

    // 收到后端推送的消息时触发（消息内容是 JSON 字符串）
    ws.onmessage = (event) => {
      // 解析后端推送的 JSON，解析失败说明不是约定的消息，直接忽略
      let msg = null
      try {
        msg = JSON.parse(event.data)
      } catch (e) {
        return
      }

      const type = Number(msg.type)
      const config = NOTICE_TYPES[type] || { typeText: '系统通知', level: 'info' }
      const content = msg.content || config.typeText

      // 弹窗提示（Element Plus 消息）
      ElMessage({ message: content, type: config.level, duration: 5 * 1000, showClose: true })

      // 交给调用方（例如 layout 的铃铛列表）
      if (typeof onMessage === 'function') {
        onMessage({
          type,
          typeText: config.typeText,
          level: config.level,
          content,
          time: new Date().toLocaleTimeString('zh-CN', { hour12: false })
        })
      }
    }

    // 连接关闭时触发
    ws.onclose = () => {
      console.log('[WebSocket] 连接已关闭')
    }

    // 连接出错时触发（例如后端没启动）
    ws.onerror = () => {
      console.warn('[WebSocket] 连接出错，请检查后端服务是否已启动（/ws 已代理到 ws://localhost:8080）')
    }
  }

  /**
   * 断开连接（组件卸载 / 退出登录时调用，释放资源）
   */
  const disconnect = () => {
    if (ws) {
      ws.close()
      ws = null
    }
  }

  return { connect, disconnect }
}
