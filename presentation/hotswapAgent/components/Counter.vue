<script setup lang="ts">
import { ref, computed, nextTick, onUnmounted } from 'vue'
import { onSlideEnter, onSlideLeave } from '@slidev/client'

const BASE_URL = 'http://localhost:8081'
const POLL_INTERVAL_MS = 500

type Status = 'stopped' | 'starting' | 'running' | 'stopping' | 'error'

const status = ref<Status>('stopped')
const output = ref('')
const errorMessage = ref('')
const outputEl = ref<HTMLElement | null>(null)

// While a Start/Stop click is in flight we show the transient label until the backend confirms;
// otherwise `status` simply mirrors the shared backend, so every window (presenter + projected
// audience view in presenter mode) converges on the same state regardless of which one clicked.
type Pending = 'start' | 'stop' | null
const pending = ref<Pending>(null)
const backendRunning = ref(false)
let intervalId: number | null = null

const canStart = computed(() => status.value === 'stopped' || status.value === 'error')
const canStop = computed(() => status.value === 'running' || status.value === 'error')

const statusLabel = computed(() => ({
  stopped: 'Stopped',
  starting: 'Starting…',
  running: 'Running',
  stopping: 'Stopping…',
  error: 'Error',
}[status.value]))

async function fetchOutput() {
  try {
    const response = await fetch(`${BASE_URL}/output`)
    output.value = await response.text()
    await nextTick()
    if (outputEl.value) {
      outputEl.value.scrollTop = outputEl.value.scrollHeight
    }
  } catch {
    // Output is briefly unavailable while the process boots; ignore transient failures.
  }
}

async function fetchStatus() {
  let running: boolean
  try {
    const response = await fetch(`${BASE_URL}/status`)
    running = (await response.text()).trim() === 'running'
  } catch {
    // Ignore transient backend blips; keep the last known state rather than flickering to error.
    return
  }

  // Reflect observed backend transitions so this window's DemoIframe reacts — even when the click
  // that caused the transition happened in another window (presenter mode).
  if (running && !backendRunning.value) {
    window.dispatchEvent(new CustomEvent('demo:started'))
  } else if (!running && backendRunning.value) {
    window.dispatchEvent(new CustomEvent('demo:stopped'))
  }
  backendRunning.value = running

  if (pending.value === 'start') {
    if (running) { pending.value = null; status.value = 'running' }
  } else if (pending.value === 'stop') {
    if (!running) { pending.value = null; status.value = 'stopped' }
  } else if (status.value !== 'error') {
    status.value = running ? 'running' : 'stopped'
  }
}

async function poll() {
  await fetchStatus()
  await fetchOutput()
}

function startPolling() {
  if (intervalId == null) {
    poll()
    intervalId = window.setInterval(poll, POLL_INTERVAL_MS)
  }
}

function stopPolling() {
  if (intervalId != null) {
    clearInterval(intervalId)
    intervalId = null
  }
}

async function startApp() {
  if (!canStart.value) return
  errorMessage.value = ''
  status.value = 'starting'
  pending.value = 'start'
  try {
    const response = await fetch(`${BASE_URL}/start`)
    if (!response.ok && response.status !== 409) {
      throw new Error(`HTTP ${response.status}`)
    }
    await poll()
  } catch {
    pending.value = null
    status.value = 'error'
    errorMessage.value = 'Could not reach the backend on :8081'
  }
}

async function stopApp() {
  if (!canStop.value) return
  errorMessage.value = ''
  status.value = 'stopping'
  pending.value = 'stop'
  try {
    const response = await fetch(`${BASE_URL}/stop`)
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    await poll()
  } catch {
    pending.value = null
    status.value = 'error'
    errorMessage.value = 'Could not reach the backend on :8081'
  }
}

onSlideEnter(startPolling)
onSlideLeave(stopPolling)
onUnmounted(stopPolling)
</script>

<template>
  <div class="control-panel">
    <div class="control-panel__header">
      <span class="control-panel__title">Application control</span>
      <span class="status" :class="`status--${status}`">
        <span class="status__dot" />
        {{ statusLabel }}
      </span>
    </div>

    <div class="control-panel__actions">
      <button class="btn btn--start" :disabled="!canStart" @click="startApp">
        <span class="btn__glyph">▶</span> Start
      </button>
      <button class="btn btn--stop" :disabled="!canStop" @click="stopApp">
        <span class="btn__glyph">■</span> Stop
      </button>
    </div>

    <p v-if="errorMessage" class="control-panel__error">{{ errorMessage }}</p>

    <div class="terminal">
      <pre ref="outputEl" class="terminal__body">{{ output || '— no output yet —' }}</pre>
    </div>
  </div>
</template>

<style scoped>
.control-panel {
  width: 100%;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  font-family: ui-sans-serif, system-ui, sans-serif;
}

.control-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.control-panel__title {
  font-weight: 600;
  font-size: 0.95rem;
  opacity: 0.75;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  border: 1px solid transparent;
}

.status__dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 999px;
  background: currentColor;
}

.status--stopped { color: #64748b; background: rgba(100, 116, 139, 0.12); border-color: rgba(100, 116, 139, 0.25); }
.status--starting,
.status--stopping { color: #d97706; background: rgba(217, 119, 6, 0.12); border-color: rgba(217, 119, 6, 0.25); }
.status--running { color: #16a34a; background: rgba(22, 163, 74, 0.12); border-color: rgba(22, 163, 74, 0.25); }
.status--running .status__dot { animation: pulse 1.2s ease-in-out infinite; }
.status--error { color: #dc2626; background: rgba(220, 38, 38, 0.12); border-color: rgba(220, 38, 38, 0.25); }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

.control-panel__actions {
  display: flex;
  gap: 0.6rem;
}

.btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.55rem 0.9rem;
  border: none;
  border-radius: 0.6rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: transform 0.08s ease, box-shadow 0.2s ease, opacity 0.2s ease;
  box-shadow: 0 6px 16px -8px rgba(0, 0, 0, 0.5);
}

.btn__glyph { font-size: 0.75rem; }
.btn:not(:disabled):hover { transform: translateY(-1px); }
.btn:not(:disabled):active { transform: translateY(0); }
.btn:disabled { opacity: 0.4; cursor: not-allowed; box-shadow: none; }

.btn--start { background: linear-gradient(135deg, #22c55e, #16a34a); }
.btn--stop { background: linear-gradient(135deg, #ef4444, #dc2626); }

.control-panel__error {
  margin: 0;
  font-size: 0.8rem;
  color: #dc2626;
}

.terminal {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 0.7rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: #0f172a;
  box-shadow: 0 10px 30px -12px rgba(0, 0, 0, 0.6);
}

.terminal__body {
  flex: 1 1 auto;
  min-height: 0;
  margin: 0;
  padding: 0.75rem 0.9rem;
  overflow-y: auto;
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
  font-size: 0.85rem;
  line-height: 1.5;
  color: #4ade80;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
