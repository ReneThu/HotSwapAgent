<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { onSlideEnter, onSlideLeave, useNav, sharedState } from '@slidev/client'

const props = defineProps<{ url: string }>()

const { isPresenter } = useNav()

// Reload attempts after a start, giving the agent-hosted server time to come up.
const RELOAD_DELAYS_MS = [1500, 4000, 8000]
// Shared backend that relays the demo app's current page across windows (presenter mode).
const NAV_API = 'http://localhost:8081/demo/nav'
const NAV_POLL_MS = 500

const origin = new URL(props.url).origin
const basePath = new URL(props.url).pathname

const iframeSrc = ref('')
const frameEl = ref<HTMLIFrameElement | null>(null)
const started = ref(false)
const slideActive = ref(false)
let timers: number[] = []
let navIntervalId: number | null = null
// While our own iframe's just-navigated path is being published to the backend, suppress
// poll-driven reloads: the backend still holds the previous (stale) path for a moment, and reacting
// to it would yank this window back off the page the user just navigated to.
let posting = false
// The path this window's iframe is currently showing. Updated both when we drive a navigation and
// when the iframe reports its own (a link click inside it) via postMessage — so a window is never
// reloaded onto the page it is already on.
let lastPath = basePath

function load(path?: string) {
  const target = path ? origin + path : props.url
  lastPath = path ?? basePath
  const separator = target.includes('?') ? '&' : '?'
  iframeSrc.value = `${target}${separator}t=${Date.now()}`
}

// Publish the path our own iframe just navigated to, so the other window can follow. We (the parent)
// own this write — not the iframe — so we can hold `posting` until the backend reflects it and thus
// keep pollNav from reloading us back onto the previous page during the in-flight window.
async function publishNav(path: string) {
  posting = true
  try {
    await fetch(NAV_API, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: path,
    })
  } catch {
    // Backend briefly unavailable; the other window simply won't follow this hop.
  } finally {
    posting = false
  }
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
  // Only retry while still on the initial list page; never yank a window back after it has navigated.
  timers = RELOAD_DELAYS_MS.map(delay => window.setTimeout(() => { if (lastPath === basePath) load() }, delay))
}

async function pollNav() {
  if (!started.value || !slideActive.value || posting) return
  try {
    const res = await fetch(NAV_API)
    const path = (await res.text()).trim()
    if (path && path !== lastPath) load(path)
  } catch {
    // Backend briefly unavailable; ignore and retry on the next tick.
  }
}

function startNavPolling() {
  if (navIntervalId == null) navIntervalId = window.setInterval(pollNav, NAV_POLL_MS)
}

function stopNavPolling() {
  if (navIntervalId != null) {
    clearInterval(navIntervalId)
    navIntervalId = null
  }
}

function onDemoMessage(e: MessageEvent) {
  if (e.origin !== origin) return
  const data = e.data as { demoPath?: unknown; demoCursor?: { nx?: unknown; ny?: unknown } }
  // The iframe forwards the presenter's mouse position (the deck can't observe mousemove inside a
  // cross-origin iframe); mirror it into Slidev's shared cursor so the audience keeps seeing it.
  const cursor = data?.demoCursor
  if (cursor && typeof cursor.nx === 'number' && typeof cursor.ny === 'number') {
    updatePresenterCursor(cursor.nx, cursor.ny)
    return
  }
  const path = data?.demoPath
  if (typeof path === 'string' && path) {
    // The app is up and the iframe has loaded a real page: cancel the startup retry reloads so a
    // pending one can't later yank us off a page the user has since navigated to.
    clearTimers()
    // Our iframe reported the page it is now on (e.g. a link was clicked inside it). Record it so we
    // never reload ourselves onto it, and publish it so the other window follows.
    if (path !== lastPath) {
      lastPath = path
      publishNav(path)
    }
  }
}

// Translate a point inside the iframe (normalized 0..1) into a percentage of the slide area and feed
// Slidev's mirrored presenter cursor, matching how presenter.vue computes it against #slide-content.
function updatePresenterCursor(nx: number, ny: number) {
  if (!isPresenter.value || !slideActive.value) return
  const frame = frameEl.value
  if (!frame) return
  const container = frame.closest('#slide-content') as HTMLElement | null
  if (!container) return
  const fr = frame.getBoundingClientRect()
  const sc = container.getBoundingClientRect()
  if (!fr.width || !fr.height || !sc.width || !sc.height) return
  const x = ((fr.left + nx * fr.width) - sc.left) / sc.width * 100
  const y = ((fr.top + ny * fr.height) - sc.top) / sc.height * 100
  if (x < 0 || x > 100 || y < 0 || y > 100) return
  sharedState.cursor = { x, y }
}

function onStarted() {
  started.value = true
  if (slideActive.value) {
    scheduleReloads()
    startNavPolling()
  }
}

function onStopped() {
  started.value = false
  clearTimers()
  stopNavPolling()
  clear()
}

onSlideEnter(async () => {
  slideActive.value = true
  if (started.value) {
    startNavPolling()
    // Resume on whatever page the shared demo is currently showing.
    try {
      const res = await fetch(NAV_API)
      const path = (await res.text()).trim()
      load(path || undefined)
    } catch {
      load()
    }
  }
})

onSlideLeave(() => {
  slideActive.value = false
  clearTimers()
  stopNavPolling()
  clear()
})

onMounted(() => {
  window.addEventListener('demo:started', onStarted)
  window.addEventListener('demo:stopped', onStopped)
  window.addEventListener('message', onDemoMessage)
})

onUnmounted(() => {
  window.removeEventListener('demo:started', onStarted)
  window.removeEventListener('demo:stopped', onStopped)
  window.removeEventListener('message', onDemoMessage)
  clearTimers()
  stopNavPolling()
})
</script>

<template>
  <div class="demo-browser">
    <div class="demo-browser__body">
      <iframe v-if="iframeSrc" ref="frameEl" :src="iframeSrc" class="demo-browser__frame" />
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
