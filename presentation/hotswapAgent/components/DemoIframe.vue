<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { onSlideEnter, onSlideLeave } from '@slidev/client'

const props = defineProps<{ url: string }>()

// Reload attempts after a start, giving the agent-hosted server time to come up.
const RELOAD_DELAYS_MS = [1500, 4000, 8000]

const iframeSrc = ref('')
const started = ref(false)
const slideActive = ref(false)
let timers: number[] = []

function load() {
  const separator = props.url.includes('?') ? '&' : '?'
  iframeSrc.value = `${props.url}${separator}t=${Date.now()}`
}

function clear() {
  iframeSrc.value = ''
}

function clearTimers() {
  timers.forEach(clearTimeout)
  timers = []
}

function scheduleReloads() {
  clearTimers()
  timers = RELOAD_DELAYS_MS.map(delay => window.setTimeout(load, delay))
}

function onStarted() {
  started.value = true
  if (slideActive.value) {
    scheduleReloads()
  }
}

function onStopped() {
  started.value = false
  clearTimers()
  clear()
}

onSlideEnter(() => {
  slideActive.value = true
  if (started.value) {
    load()
  }
})

onSlideLeave(() => {
  slideActive.value = false
  clearTimers()
  clear()
})

onMounted(() => {
  window.addEventListener('demo:started', onStarted)
  window.addEventListener('demo:stopped', onStopped)
})

onUnmounted(() => {
  window.removeEventListener('demo:started', onStarted)
  window.removeEventListener('demo:stopped', onStopped)
  clearTimers()
})
</script>

<template>
  <div class="demo-browser">
    <div class="demo-browser__body">
      <iframe v-if="iframeSrc" :src="iframeSrc" class="demo-browser__frame" />
      <div v-else class="demo-browser__placeholder">
        <template v-if="started">
          <div class="demo-browser__spinner" />
          <p>Waiting for the application to start…</p>
        </template>
        <template v-else>
          <p>Press <b>Start</b> to launch the application.</p>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.demo-browser {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  border-radius: 0.8rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: 0 20px 50px -20px rgba(0, 0, 0, 0.55);
  background: #0f172a;
}

.demo-browser__body {
  flex: 1;
  position: relative;
  background: #fff;
}

.demo-browser__frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.demo-browser__placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: #64748b;
  background: #0f172a;
  font-size: 1rem;
}

.demo-browser__spinner {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  border: 3px solid rgba(148, 163, 184, 0.25);
  border-top-color: #38bdf8;
  animation: demo-spin 0.9s linear infinite;
}

@keyframes demo-spin {
  to { transform: rotate(360deg); }
}
</style>
