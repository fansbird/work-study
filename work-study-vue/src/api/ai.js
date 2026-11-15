/**
 * AI 助手流式接口
 *
 * 为什么不用 axios：后端返回的是 text/event-stream（SSE 流），
 * axios 默认会等整个响应体下载完才 resolve，拿不到“逐字输出”的效果；
 * 这里用 fetch + ReadableStream 边读边解析，才能做到打字机效果。
 */
export async function streamChat(message, { onDelta, signal } = {}) {
  let loginUser = null
  try {
    loginUser = JSON.parse(localStorage.getItem('token') || 'null')
  } catch (e) {
    loginUser = null
  }

  const url = `/api/ai/chat?message=${encodeURIComponent(message)}`
  const response = await fetch(url, {
    method: 'GET',
    headers: loginUser && loginUser.token ? { token: loginUser.token } : {},
    signal
  })

  if (response.status === 401) {
    throw new Error('登录已过期，请重新登录')
  }
  if (!response.ok) {
    throw new Error(`请求失败（${response.status}）`)
  }

  // 后端异常时返回的是 JSON（Result{code,msg}），不是 SSE，这里单独处理，避免出现空回复
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('text/event-stream')) {
    const text = await response.text()
    let msg = 'AI 服务异常，请稍后再试'
    try {
      const json = JSON.parse(text)
      msg = json.msg || msg
    } catch (e) {
      /* 非 JSON 就保留默认提示 */
    }
    throw new Error(msg)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const consume = (block) => {
    for (const line of block.split('\n')) {
      if (line.startsWith('data:')) {
        onDelta && onDelta(line.slice(5))
      }
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // SSE 以空行分隔事件，逐个事件解析 data: 行
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const event of events) {
      consume(event)
    }
  }

  // 流结束时缓冲区里可能还剩最后一小段没有空行结尾，补一次
  if (buffer.trim()) {
    consume(buffer)
  }
}