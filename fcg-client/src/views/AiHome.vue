<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import BaseLayout from '../components/common/BaseLayout.vue'
import { useUserStore } from '../stores/user'
import { chatWithAi, fetchAiContext } from '../utils/api'
import { getToken } from '../utils/storage'

const userStore = useUserStore()
const inputText = ref('')
const isStreaming = ref(false)
const messages = ref([])
const chatBodyRef = ref(null)
const shouldAutoFollow = ref(true)
const abortController = ref(null)
const streamLastUiUpdateAt = ref(0)
const renderQueue = ref([])
const renderLoopActive = ref(false)
const streamStartedAt = ref(0)
const contextCache = ref({ memberId: null, date: '', ts: 0, text: '' })
const renderStats = ref({
  queuedChars: 0,
  paintedChars: 0,
  firstDeltaAt: 0,
  firstPaintAt: 0
})
const RENDER_TICK_MS = 24
const CONTEXT_CACHE_TTL = 30 * 1000

const quickActions = [
  { label: '今天情况总结', prompt: '请结合我今天的健康与用药数据，给我一个简洁总结。' },
  { label: '体征趋势解读', prompt: '请解读我近7天体征趋势，指出重点和风险点。' },
  { label: '用药执行建议', prompt: '请根据我今日待处理/跳过任务，给出可执行的用药建议。' },
  { label: '给家人的提醒', prompt: '请生成一段温和、简短、能直接转发给家人的健康提醒。' }
]

const memberId = computed(() => userStore.member?.id || userStore.member?.userId || null)
const canSend = computed(() => !!inputText.value.trim() && !isStreaming.value)
const memberName = computed(() => userStore.member?.nickname || '你')
const SESSION_KEY_PREFIX = 'fcg_ai_chat_'
const MAX_MESSAGES = 80
const STREAM_ENDPOINT = '/api/ai/chat/stream'
const FOLLOW_THRESHOLD = 72

const systemPrompt = `
你是家庭健康助手，服务于家庭健康管理场景。
回答要求：
1. 先给结论，再给理由，再给可执行建议（最多3条）。
2. 如用户提供了健康数据，必须优先基于数据分析，不要编造不存在的数据。
3. 语气温和、明确，避免恐吓式表述。
4. 非医疗诊断场景请明确“仅供健康管理参考，不替代医生建议”。
`.trim()

const getToday = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const getSessionKey = () => `${SESSION_KEY_PREFIX}${memberId.value || 'unknown'}`

const appendMessage = (msg) => {
  messages.value.push({
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: msg.role,
    content: msg.content || '',
    status: msg.status || 'done',
    ts: Date.now()
  })
  if (messages.value.length > MAX_MESSAGES) {
    messages.value = messages.value.slice(-MAX_MESSAGES)
  }
  persistConversation()
  scrollToBottom()
}

const loadConversation = () => {
  if (!memberId.value) {
    messages.value = []
    return
  }
  try {
    const raw = sessionStorage.getItem(getSessionKey())
    const parsed = raw ? JSON.parse(raw) : []
    messages.value = Array.isArray(parsed) ? parsed : []
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

const persistConversation = () => {
  if (!memberId.value) return
  sessionStorage.setItem(getSessionKey(), JSON.stringify(messages.value.slice(-MAX_MESSAGES)))
}

const clearConversation = () => {
  messages.value = []
  if (memberId.value) {
    sessionStorage.removeItem(getSessionKey())
  }
}

const isNearBottom = () => {
  const el = chatBodyRef.value
  if (!el) return true
  return el.scrollHeight - (el.scrollTop + el.clientHeight) <= FOLLOW_THRESHOLD
}

const handleChatScroll = () => {
  shouldAutoFollow.value = isNearBottom()
}

const scrollToBottom = async (force = false) => {
  await nextTick()
  const el = chatBodyRef.value
  if (!el) return
  if (!force && !shouldAutoFollow.value) return
  el.scrollTop = el.scrollHeight
}

const safeText = (v) => (v == null || v === '' ? '-' : String(v))
const normalizeAvatar = (raw) => {
  if (!raw) return ''
  const text = String(raw).trim()
  if (!text) return ''
  if (
    text.startsWith('http://') ||
    text.startsWith('https://') ||
    text.startsWith('data:image/') ||
    text.startsWith('/avatars/') ||
    text.startsWith('/uploads/') ||
    text.startsWith('/')
  ) {
    return text
  }
  return ''
}

const currentUserAvatar = computed(() => normalizeAvatar(userStore.member?.avatar))
const aiAvatar = 'AI'

const formatTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

const renderMarkdown = (content) => {
  if (!content) return ''
  return marked.parse(content, { gfm: true, breaks: true })
}

const formatVitals = (list) => {
  if (!Array.isArray(list) || list.length === 0) return '今日体征：暂无'
  const lines = list.map(item => {
    const type = item.type || 'unknown'
    if (type === 'blood_pressure') {
      return `- 血压：${safeText(item.valueSystolic)}/${safeText(item.valueDiastolic)} mmHg`
    }
    if (type === 'blood_sugar_fasting') {
      return `- 空腹血糖：${safeText(item.value)} mmol/L`
    }
    if (type === 'blood_sugar_postmeal') {
      return `- 餐后血糖：${safeText(item.value)} mmol/L`
    }
    if (type === 'weight') {
      return `- 体重：${safeText(item.value)} kg`
    }
    return `- ${type}：${safeText(item.value)}`
  })
  return ['今日体征：', ...lines].join('\n')
}

const formatWeeklyTrend = (weeklyMap) => {
  const typeLabel = {
    blood_pressure: '血压',
    blood_sugar_fasting: '空腹血糖',
    blood_sugar_postmeal: '餐后血糖',
    weight: '体重'
  }
  const lines = Object.entries(typeLabel).map(([type, label]) => {
    const list = Array.isArray(weeklyMap[type]) ? weeklyMap[type] : []
    if (!list.length) return `- ${label}：近7天暂无记录`
    const latest = list[0]
    if (type === 'blood_pressure') {
      return `- ${label}：近7天${list.length}条，最新${safeText(latest.valueSystolic)}/${safeText(latest.valueDiastolic)} mmHg`
    }
    const unit = type === 'weight' ? 'kg' : 'mmol/L'
    return `- ${label}：近7天${list.length}条，最新${safeText(latest.value)} ${unit}`
  })
  return ['近7天体征趋势：', ...lines].join('\n')
}

const formatPlanRecords = (records) => {
  if (!Array.isArray(records) || records.length === 0) return '今日用药任务：暂无'
  const total = records.length
  const taken = records.filter(r => Number(r.status) === 1).length
  const skipped = records.filter(r => Number(r.status) === 2).length
  const pending = total - taken - skipped
  const detailLines = records.slice(0, 10).map((r, idx) => {
    const statusMap = { 0: '待处理', 1: '已打卡', 2: '已跳过' }
    const status = statusMap[Number(r.status)] || '未知'
    const med = safeText(r.medicineName || r.planMedicineName || r.name)
    const dosage = safeText(r.planDosage || r.dosage)
    const unit = safeText(r.planUnit || r.unit)
    const time = safeText(r.scheduledTime || r.planTime)
    return `- ${idx + 1}. ${med} ${dosage}${unit === '-' ? '' : unit}（${time}，${status}）`
  })
  return [`今日用药任务：共${total}条，已打卡${taken}条，已跳过${skipped}条，待处理${pending}条`, ...detailLines].join('\n')
}

const getContextSummary = async () => {
  if (!memberId.value) return '当前无成员上下文'
  const today = getToday()
  const now = Date.now()
  if (
    contextCache.value.memberId === memberId.value &&
    contextCache.value.date === today &&
    now - contextCache.value.ts < CONTEXT_CACHE_TTL &&
    contextCache.value.text
  ) {
    return contextCache.value.text
  }
  const res = await fetchAiContext({ userId: memberId.value, date: today })
  const text = res?.data?.contextText || ''
  if (!text) {
    console.warn('[AI context] empty contextText from backend, fallback simple context')
    return `当前成员：${safeText(userStore.member?.nickname)}（userId=${memberId.value}）\n日期：${today}`
  }
  contextCache.value = { memberId: memberId.value, date: today, ts: now, text }
  console.log('[AI context] loaded from backend, length=', text.length, 'memberId=', memberId.value)
  return text
}

const prefetchContext = async () => {
  if (!memberId.value) return
  try {
    await getContextSummary()
  } catch (e) {
    console.warn('[AI context] prefetch failed:', e?.message || e)
  }
}

const parseSseBlock = (block) => {
  const lines = block.split('\n')
  const dataLines = []
  for (const line of lines) {
    const cleanLine = line.replace(/\r$/, '')
    if (cleanLine.startsWith('data:')) {
      dataLines.push(cleanLine.slice(5).trim())
    }
  }
  if (!dataLines.length) return null
  const text = dataLines.join('\n').trim()
  if (!text) return null
  try {
    const parsed = JSON.parse(text)
    // 兼容后端可能返回被字符串包裹的一层 JSON（例如 "{\"type\":\"delta\"...}"）
    if (typeof parsed === 'string') {
      try {
        return JSON.parse(parsed)
      } catch {
        return null
      }
    }
    return parsed
  } catch {
    return null
  }
}

const streamChat = async (payload, onDelta, signal) => {
  const token = getToken()
  const response = await fetch(STREAM_ENDPOINT, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!response.ok) {
    throw new Error(`流式接口请求失败（HTTP ${response.status}）`)
  }
  if (!response.body) {
    throw new Error('流式响应不可读')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let deltaCount = 0
  let readCount = 0
  const startedAt = Date.now()
  let firstDeltaAt = 0
  const handleDataLine = (line) => {
    const clean = line.replace(/\r$/, '')
    if (!clean.startsWith('data:')) return
    const raw = clean.slice(5).trim()
    if (!raw) return
    const event = parseSseBlock(`data: ${raw}`)
    if (!event) return
    if (event.type === 'delta' && event.content) {
      deltaCount += 1
      if (!firstDeltaAt) firstDeltaAt = Date.now()
      onDelta(event.content)
      return true
    }
    if (event.type === 'error') {
      throw new Error(event.content || 'AI流式服务异常')
    }
    return false
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    readCount += 1
    buffer += decoder.decode(value, { stream: true })
    if (readCount % 20 === 0) {
      console.debug('[AI stream] read chunks=', readCount, 'bufferLen=', buffer.length, 'deltaCount=', deltaCount)
    }
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    for (const line of lines) {
      const consumed = handleDataLine(line)
      // 即使底层被缓冲成大块，也要主动让出一帧，确保“可见流式”
      if (consumed && deltaCount % 4 === 0) {
        await new Promise((resolve) => requestAnimationFrame(() => resolve()))
      }
    }
  }

  // 处理流结束时残留的最后一行（避免尾包丢失）
  if (buffer.trim()) {
    handleDataLine(buffer)
  }
  console.log('[AI stream] totalDeltas=', deltaCount, 'firstDeltaMs=', firstDeltaAt ? (firstDeltaAt - startedAt) : -1, 'totalMs=', Date.now() - startedAt, 'readChunks=', readCount)
}

const appendDelta = (assistantMsg, delta) => {
  if (!delta) return
  if (!renderStats.value.firstDeltaAt) {
    renderStats.value.firstDeltaAt = Date.now()
  }
  renderStats.value.queuedChars += delta.length
  renderQueue.value.push(...delta.split(''))
  if (!renderLoopActive.value) {
    startRenderLoop(assistantMsg)
  }
}

const startRenderLoop = (assistantMsg) => {
  if (renderLoopActive.value) return
  renderLoopActive.value = true

  const tick = () => {
    if (!renderQueue.value.length) {
      renderLoopActive.value = false
      return
    }

    // 固定节奏逐字渲染：后端即使突发返回，也能保持可见流式
    const backlog = renderQueue.value.length
    const charsPerTick = backlog > 220 ? 4 : backlog > 120 ? 3 : backlog > 50 ? 2 : 1
    const part = renderQueue.value.splice(0, charsPerTick).join('')
    assistantMsg.content += part
    renderStats.value.paintedChars += part.length
    if (!renderStats.value.firstPaintAt && part.length > 0) {
      renderStats.value.firstPaintAt = Date.now()
    }

    const now = Date.now()
    if (now - streamLastUiUpdateAt.value > 120) {
      streamLastUiUpdateAt.value = now
      persistConversation()
      scrollToBottom()
    }

    setTimeout(tick, RENDER_TICK_MS)
  }

  setTimeout(tick, RENDER_TICK_MS)
}

const waitRenderDrain = () => new Promise((resolve) => {
  const check = () => {
    if (!renderQueue.value.length && !renderLoopActive.value) {
      resolve()
      return
    }
    setTimeout(check, 20)
  }
  check()
})

const sendMessage = async (text) => {
  const question = (text || inputText.value).trim()
  if (!question || isStreaming.value) return

  appendMessage({ role: 'user', content: question, status: 'done' })
  inputText.value = ''
  shouldAutoFollow.value = true

  // 用 reactive 包裹，确保流式过程中 content/status 变更是响应式更新
  const assistantMsg = reactive({
    id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: 'assistant',
    content: '',
    status: 'streaming',
    ts: Date.now()
  })
  messages.value.push(assistantMsg)
  persistConversation()
  scrollToBottom(true)

  isStreaming.value = true
  streamStartedAt.value = Date.now()
  streamLastUiUpdateAt.value = 0
  renderStats.value = { queuedChars: 0, paintedChars: 0, firstDeltaAt: 0, firstPaintAt: 0 }
  abortController.value = new AbortController()

  try {
    const contextText = await getContextSummary()
    const payload = {
      systemPrompt,
      userPrompt: [
        `用户问题：${question}`,
        '',
        '【系统已注入健康上下文（必须优先使用，不允许忽略）】',
        contextText,
        '',
        '如果上下文里某项为“暂无记录”，请明确指出“该项暂无数据”，并基于已有项给出建议。'
      ].join('\n')
    }
    let receivedDelta = false

    await streamChat(
      payload,
      (delta) => {
        receivedDelta = true
        appendDelta(assistantMsg, delta)
      },
      abortController.value.signal
    )
    await waitRenderDrain()
    assistantMsg.status = 'done'
    console.log(
      '[AI render] queuedChars=',
      renderStats.value.queuedChars,
      'paintedChars=',
      renderStats.value.paintedChars,
      'send->firstDelta(ms)=',
      renderStats.value.firstDeltaAt ? (renderStats.value.firstDeltaAt - streamStartedAt.value) : -1,
      'delta->paint(ms)=',
      renderStats.value.firstDeltaAt && renderStats.value.firstPaintAt
        ? (renderStats.value.firstPaintAt - renderStats.value.firstDeltaAt)
        : -1
    )
    persistConversation()
    scrollToBottom()

    if (!receivedDelta && !assistantMsg.content.trim()) {
      console.warn('[AI stream] no delta received, fallback to non-stream chat')
      const fallback = await chatWithAi(payload)
      assistantMsg.content = fallback?.data || '暂无可用回复，请稍后重试。'
    }
  } catch (err) {
    if (err?.name === 'AbortError') {
      assistantMsg.status = 'done'
      if (!assistantMsg.content.trim()) {
        assistantMsg.content = '已停止生成'
      }
    } else {
      assistantMsg.status = 'error'
      const msg = err?.message || 'AI回复失败，请稍后重试'
      assistantMsg.content = assistantMsg.content || msg
      ElMessage.error(msg)
    }
  } finally {
    isStreaming.value = false
    abortController.value = null
    if (!isStreaming.value && !renderLoopActive.value) {
      renderQueue.value = []
    }
    persistConversation()
  }
}

const handleSend = async () => {
  if (!canSend.value) return
  await sendMessage(inputText.value)
}

const stopStream = () => {
  if (abortController.value) {
    abortController.value.abort()
  }
}

watch(memberId, () => {
  contextCache.value = { memberId: null, date: '', ts: 0, text: '' }
  loadConversation()
  prefetchContext()
}, { immediate: true })

onMounted(() => {
  loadConversation()
  prefetchContext()
})
</script>

<template>
  <div class="ai-page">
    <BaseLayout>
      <section class="ai-container">
        <div class="chat-shell">
          <header class="ai-header">
            <div class="header-main">
              <h1>🤖 AI 健康助手</h1>
              <p>和我聊聊健康问题，我会结合当前成员数据给你建议</p>
            </div>
            <div class="header-actions">
              <button class="ghost-btn" @click="clearConversation">清空对话</button>
            </div>
          </header>

          <div ref="chatBodyRef" class="chat-body" @scroll.passive="handleChatScroll">
            <div v-if="messages.length === 0" class="empty-state">
              <div class="empty-logo">AI</div>
              <h3>欢迎提问，{{ memberName }}</h3>
              <p>你可以直接提问，也可以点击下面的快捷问题开始。</p>
              <div class="quick-title">⚡ 快捷提问</div>
              <div class="empty-quick-list">
                <button
                  v-for="item in quickActions"
                  :key="item.label"
                  class="quick-btn"
                  :disabled="isStreaming"
                  @click="sendMessage(item.prompt)"
                >
                  <span class="quick-dot"></span>
                  <span>{{ item.label }}</span>
                </button>
              </div>
            </div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="msg-row"
              :class="msg.role === 'user' ? 'user' : 'assistant'"
            >
              <div class="msg-avatar">
                <img
                  v-if="msg.role === 'user' && currentUserAvatar"
                  :src="currentUserAvatar"
                  alt="用户头像"
                  class="avatar-img"
                />
                <span v-else>{{ msg.role === 'assistant' ? aiAvatar : '我' }}</span>
              </div>
              <div class="msg-main">
                <div v-if="msg.role === 'assistant'" class="msg-meta">健康助手 · {{ formatTime(msg.ts) }}</div>
                <div v-else class="msg-meta user-meta">{{ formatTime(msg.ts) }}</div>
                <div class="msg-bubble" :class="{ error: msg.status === 'error' }">
                  <pre v-if="msg.role === 'user' && msg.content">{{ msg.content }}</pre>
                  <pre
                    v-else-if="msg.role === 'assistant' && msg.status === 'streaming'"
                    class="streaming-text"
                  >{{ msg.content }}</pre>
                  <div
                    v-else-if="msg.role === 'assistant' && msg.status === 'streaming' && !msg.content"
                    class="stream-placeholder"
                  >
                    正在整理你的健康数据并生成回复...
                  </div>
                  <div
                    v-else-if="msg.role === 'assistant' && msg.content"
                    class="ai-markdown"
                    v-html="renderMarkdown(msg.content)"
                  ></div>
                  <div v-if="msg.status === 'streaming'" class="typing-dots">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <footer class="input-wrap">
            <div class="composer">
              <textarea
                v-model="inputText"
                :disabled="isStreaming"
                placeholder="请输入健康问题..."
                @keydown.enter.exact.prevent="handleSend"
                @keydown.enter.shift.exact.stop
              />
              <div class="composer-actions">
                <button v-if="isStreaming" class="stop-btn" @click="stopStream">停止</button>
                <button class="send-btn" :disabled="!canSend" @click="handleSend">发送</button>
              </div>
            </div>
            <div class="input-hint">
              Enter 发送，Shift+Enter 换行
            </div>
          </footer>
        </div>
      </section>
    </BaseLayout>
  </div>
</template>

<style scoped>
.ai-page {
  min-height: 100vh;
  background: linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%);
  position: relative;
}

.ai-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(700px 260px at 8% -2%, rgba(45, 95, 93, 0.12), transparent 60%),
    radial-gradient(640px 260px at 100% 0, rgba(112, 164, 158, 0.12), transparent 62%);
}

.ai-container {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: calc(100vh - 88px);
  height: calc(100vh - 88px);
  display: flex;
}

.chat-shell {
  width: 100%;
  height: 100%;
  min-height: 0;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 22px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 56px rgba(22, 52, 50, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(45, 95, 93, 0.08);
  background: linear-gradient(180deg, rgba(248, 252, 251, 0.95), rgba(248, 252, 251, 0.62));
}

.header-main {
  min-width: 0;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ai-header h1 {
  margin: 0;
  font-size: 1.18rem;
  color: #173f3d;
}

.ai-header p {
  margin: 2px 0 0;
  color: #5e7574;
  font-size: 0.84rem;
}

.ghost-btn {
  border: 1px solid #d0e0df;
  background: #ffffff;
  color: #355f5d;
  border-radius: 10px;
  padding: 6px 11px;
  cursor: pointer;
  transition: all 0.16s ease;
  font-size: 0.88rem;
}

.ghost-btn:hover {
  background: #f4faf9;
  border-color: #bcd3d1;
}

.quick-btn {
  background: #f4faf9;
  color: #2b5654;
  border-radius: 999px;
  padding: 7px 11px;
  cursor: pointer;
  font-size: 0.82rem;
  border: 1px solid #d5e3e2;
  transition: all 0.18s ease;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  justify-content: center;
  white-space: nowrap;
}

.quick-btn:hover {
  background: #e9f4f2;
  border-color: #c8dddd;
  box-shadow: 0 2px 8px rgba(31, 83, 79, 0.08);
}

.quick-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (hover: none) {
  .quick-btn:hover {
    background: #f4faf9;
    border-color: #d5e3e2;
    box-shadow: none;
  }
}

.chat-body {
  padding: 12px 14px;
  overflow-y: auto;
  flex: 1;
  min-height: 240px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), rgba(248, 252, 251, 0.72));
}

.chat-body::-webkit-scrollbar {
  width: 8px;
}

.chat-body::-webkit-scrollbar-thumb {
  background: rgba(86, 130, 126, 0.28);
  border-radius: 999px;
}

.empty-state {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: #6e8483;
  padding: 18px;
}

.empty-logo {
  width: 62px;
  height: 62px;
  border-radius: 20px;
  background: linear-gradient(145deg, #2d5f5d, #4f8a86);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.empty-state h3 {
  margin: 0;
  color: #1b4442;
  font-size: 1.08rem;
}

.empty-state p {
  margin: 8px 0 12px;
  font-size: 0.9rem;
}

.quick-title {
  width: auto;
  text-align: center;
  color: #7a8f8e;
  font-size: 0.76rem;
  margin: 0 0 8px;
}

.empty-quick-list {
  width: min(680px, 100%);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.quick-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3f7a77;
  flex-shrink: 0;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.msg-row.user {
  justify-content: flex-end;
  flex-direction: row-reverse;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  font-size: 0.76rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  background: linear-gradient(145deg, #2e6865, #4f8a86);
  box-shadow: 0 7px 15px rgba(42, 94, 90, 0.2);
  overflow: hidden;
}

.msg-row.user .msg-avatar {
  background: linear-gradient(145deg, #4b8f8b, #2d5f5d);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.msg-main {
  max-width: min(96ch, 90%);
}

.msg-meta {
  font-size: 0.76rem;
  color: #7e9493;
  margin: 0 4px 4px;
}

.msg-row.user .msg-main {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: auto;
}

.msg-row.user .msg-meta {
  text-align: right;
  margin: 0 2px 4px 0;
}

.user-meta {
  opacity: 0.75;
}

.msg-bubble {
  border-radius: 16px;
  padding: 10px 13px;
  background: #f4fbfa;
  border: 1px solid #d6e7e5;
  box-shadow: 0 10px 20px rgba(47, 93, 88, 0.05);
}

.msg-row.user .msg-bubble {
  border-top-right-radius: 8px;
}

.msg-row.assistant .msg-bubble {
  border-top-left-radius: 8px;
}

.msg-row.user .msg-bubble {
  background: linear-gradient(145deg, #2d5f5d, #3b7572);
  border-color: transparent;
  box-shadow: 0 12px 24px rgba(45, 95, 93, 0.24);
  margin-left: auto;
}

.msg-row.user .msg-bubble pre {
  color: #fff;
}

.msg-bubble.error {
  border-color: #e4b0aa;
  background: #fff6f5;
}

.msg-bubble pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.55;
  color: #244847;
  font-size: 0.92rem;
  font-family: inherit;
}

.streaming-text {
  margin-bottom: 6px !important;
}

.stream-placeholder {
  color: #5f7d7c;
  font-size: 0.9rem;
  margin-bottom: 6px;
}

.ai-markdown {
  color: #244847;
  font-size: 0.92rem;
  line-height: 1.65;
}

.ai-markdown :deep(p) {
  margin: 0 0 8px;
}

.ai-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-markdown :deep(ul),
.ai-markdown :deep(ol) {
  margin: 8px 0 8px 20px;
}

.ai-markdown :deep(li) {
  margin: 4px 0;
}

.ai-markdown :deep(code) {
  background: rgba(45, 95, 93, 0.08);
  padding: 1px 5px;
  border-radius: 6px;
  font-size: 0.86rem;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 1px;
}

.typing-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(47, 92, 88, 0.55);
  animation: typingPulse 1s infinite ease-in-out;
}

.typing-dots span:nth-child(2) { animation-delay: 0.12s; }
.typing-dots span:nth-child(3) { animation-delay: 0.24s; }

@keyframes typingPulse {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.45; }
  40% { transform: translateY(-2px); opacity: 1; }
}

.input-wrap {
  padding: 8px 10px 10px;
  border-top: 1px solid rgba(45, 95, 93, 0.09);
  background: rgba(248, 252, 251, 0.82);
  flex-shrink: 0;
}

.composer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-wrap textarea {
  width: 100%;
  min-height: 56px;
  max-height: 150px;
  border: 1px solid #d2e2e1;
  border-radius: 12px;
  padding: 10px;
  resize: none;
  font-family: inherit;
  font-size: 0.9rem;
  background: rgba(255, 255, 255, 0.95);
  flex: 1;
}

.input-wrap textarea:focus {
  outline: none;
  border-color: #2d5f5d;
  box-shadow: 0 0 0 3px rgba(45, 95, 93, 0.14);
}

.composer-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  width: 80px;
}

.stop-btn,
.send-btn {
  border: none;
  border-radius: 10px;
  padding: 7px 8px;
  cursor: pointer;
  width: 100%;
  min-height: 34px;
}

.stop-btn {
  background: #f3e5e3;
  color: #9a3d34;
}

.send-btn {
  background: linear-gradient(145deg, #2d5f5d, #3d7a77);
  color: #fff;
  box-shadow: 0 10px 20px rgba(45, 95, 93, 0.24);
}

.send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.input-hint {
  margin-top: 6px;
  font-size: 0.72rem;
  color: #7f9796;
  padding-left: 2px;
  line-height: 1.2;
}

@media (max-width: 768px) {
  .ai-container {
    min-height: calc(100dvh - 116px);
    height: calc(100dvh - 116px);
  }

  .ai-header {
    gap: 6px;
    padding: 8px 10px;
  }

  .header-actions {
    width: auto;
    justify-content: flex-end;
  }

  .chat-body {
    min-height: 0;
    padding: 8px 10px;
  }

  .empty-state {
    min-height: 280px;
    padding: 10px;
  }

  .empty-quick-list {
    width: 100%;
    flex-wrap: nowrap;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .empty-quick-list::-webkit-scrollbar {
    display: none;
  }

  .quick-btn {
    flex: 0 0 auto;
  }

  .msg-bubble {
    max-width: 100%;
  }

  .msg-main {
    max-width: 92%;
  }

  .input-wrap {
    padding: 6px 8px 8px;
  }

  .composer {
    flex-direction: row;
    align-items: flex-end;
  }

  .composer-actions {
    width: auto;
    flex-direction: column;
    justify-content: flex-end;
    gap: 6px;
  }

  .stop-btn,
  .send-btn {
    width: 76px;
    min-width: 76px;
    min-height: 36px;
    padding: 7px 8px;
  }

  .input-wrap textarea {
    min-height: 48px;
    max-height: 118px;
    padding: 8px;
    font-size: 0.88rem;
  }

  .ai-header h1 {
    font-size: 1.08rem;
  }

  .ai-header p {
    font-size: 0.8rem;
    margin-top: 2px;
  }

  .input-hint {
    display: none;
  }
}
</style>
