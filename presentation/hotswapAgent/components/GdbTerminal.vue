<script setup lang="ts">
import { ref } from 'vue'
import { onSlideEnter, onSlideLeave } from '@slidev/client'

const props = defineProps<{ url: string }>()

// The session is started from the terminal itself: entering the slide loads the page, which shows
// the gdb start command and waits for Enter (see GdbLauncher's wrapper). Leaving the slide clears
// the iframe, closing the WebSocket so the backend kills gdb and the slowdebug JVM. No orphans.
const iframeSrc = ref('')

function load() {
  const separator = props.url.includes('?') ? '&' : '?'
  iframeSrc.value = `${props.url}${separator}t=${Date.now()}`
}

onSlideEnter(load)
onSlideLeave(() => { iframeSrc.value = '' })
</script>

<template>
  <div class="gdb-terminal">
    <iframe
      v-if="iframeSrc"
      :src="iframeSrc"
      class="gdb-terminal__frame"
      allow="clipboard-read; clipboard-write"
    />
    <div v-else class="gdb-terminal__placeholder">
      <div class="gdb-terminal__spinner" />
      <p>Loading GDB terminal…</p>
    </div>
  </div>
</template>

<style scoped>
.gdb-terminal {
  position: absolute;
  inset: 0;
  display: flex;
  padding: 8px;
  box-sizing: border-box;
  background: #000;
}

.gdb-terminal__frame {
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.gdb-terminal__placeholder {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: #64748b;
  font-size: 1rem;
}

.gdb-terminal__spinner {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  border: 3px solid rgba(148, 163, 184, 0.25);
  border-top-color: #38bdf8;
  animation: gdb-spin 0.9s linear infinite;
}

@keyframes gdb-spin {
  to { transform: rotate(360deg); }
}
</style>
