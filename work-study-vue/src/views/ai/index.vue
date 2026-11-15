<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion, RefreshRight } from '@element-plus/icons-vue'
import { streamChat } from '@/api/ai'

// 对话消息列表：{ role: 'user' | 'ai', content, streaming }
const messages = ref([])
const input = ref('')
const sending = ref(false)
const listRef = ref(null)
let controller = null

const quickQuestions = [
  '现在有哪些在招的勤工助学岗位？',
  '图书馆那个岗位还招人吗？时薪多少？',
  '勤工助学的工时申报上限是多少？',
  '薪酬规则是怎么定的？'
]

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const el = listRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

// 发送
const send = async (text) => {
  const content = (text !== undefined ? text : input.value).trim()
  if (!content) {
    ElMessage.warning('请输入问题')
    return
  }
  if (sending.value) return

  input.value = ''
  sending.value = true
  messages.value.push({ role: 'user', content })
  const aiMessage = { role: 'ai', content: '', streaming: true }
  messages.value.push(aiMessage)
  await scrollToBottom()

  controller = new AbortController()
  try {
    await streamChat(content, {
      signal: controller.signal,
      onDelta: (delta) => {
        aiMessage.content += delta
        scrollToBottom()
      }
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      aiMessage.content += '\n\n（已停止）'
    } else {
      aiMessage.content = aiMessage.content || `抱歉，出错了：${error.message}`
      ElMessage.error(error.message)
    }
  } finally {
    aiMessage.streaming = false
    sending.value = false
    controller = null
    scrollToBottom()
  }
}

// 停止生成
const stop = () => {
  if (controller) {
    controller.abort()
  }
}

// 清空对话（同时清掉浏览器侧展示；Redis 里的记忆按用户保留，刷新不影响）
const clear = () => {
  if (sending.value) {
    ElMessage.warning('正在生成中，请先停止')
    return
  }
  messages.value = []
}
</script>

<template>
  <div class="ai-page">
    <el-card shadow="never" class="ai-card">
      <template #header>
        <div class="card-header">
          <div class="title">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 智能助手</span>
            <el-tag size="small" type="success" effect="plain">LangChain4j</el-tag>
          </div>
          <div class="actions">
            <el-button :icon="RefreshRight" text @click="clear">清空对话</el-button>
          </div>
        </div>
      </template>

      <!-- 消息区 -->
      <div ref="listRef" class="message-list">
        <el-empty v-if="messages.length === 0" description="问我岗位、工时、薪酬相关的问题">
          <div class="quick-list">
            <el-button
              v-for="q in quickQuestions"
              :key="q"
              size="small"
              round
              @click="send(q)"
            >{{ q }}</el-button>
          </div>
        </el-empty>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-row"
          :class="msg.role === 'user' ? 'is-user' : 'is-ai'"
        >
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <span class="text">{{ msg.content }}</span>
            <span v-if="msg.streaming" class="cursor">▍</span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="例如：现在有哪些在招岗位？图书馆岗位时薪多少？（Enter 发送，Shift + Enter 换行）"
          :disabled="sending"
          @keydown.enter.exact.prevent="send()"
        />
        <div class="input-actions">
          <el-button v-if="sending" type="danger" plain @click="stop">停止生成</el-button>
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="send()">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.ai-page {
  padding: 4px;
}
.ai-card {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}
.ai-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-header .title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}
.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 640px;
}
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.message-row.is-user {
  flex-direction: row-reverse;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;
  background: #909399;
}
.is-user .avatar {
  background: #ffb400;
}
.is-ai .avatar {
  background: #409eff;
}
.bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f4f4f5;
  color: #303133;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.is-user .bubble {
  background: #fff7e6;
}
.cursor {
  animation: blink 1s steps(2, start) infinite;
  color: #409eff;
}
@keyframes blink {
  to {
    visibility: hidden;
  }
}
.input-area {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>