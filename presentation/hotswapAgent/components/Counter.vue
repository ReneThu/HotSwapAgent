<script setup lang="ts">
import { ref } from 'vue'

const output = ref('');
const intervalId = ref<number | null>(null);

function fetchOutput() {
    fetch('http://localhost:8081/output')
        .then(response => response.text())
        .then(data => {
            output.value = data;
        })
        .catch(error => console.error('Error fetching data:', error));
}

function startFetching() {
    if (!intervalId.value) {
        fetchOutput(); // Initial fetch
        intervalId.value = setInterval(fetchOutput, 500); // Refresh every second
    }
}

function stopFetching() {
    if (intervalId.value) {
        clearInterval(intervalId.value);
        intervalId.value = null;
    }
}
</script>

<template>
  <div flex="~" w="min" border="~ main rounded-md">
    <button
      border="r main"
      p="2"
      font="mono"
      outline="!none"
      hover:bg="gray-400 opacity-20"
      bg="green-500"
      text="white"
      @click="startFetching"
    >
      Run
    </button>
    <button
      border="l main"
      p="2"
      font="mono"
      outline="!none"
      hover:bg="gray-400 opacity-20"
      bg="red-500"
      text="white"
      @click="stopFetching"
    >
      Stop
    </button>
  </div>
  <div class="pt-4">
    <textarea v-model="output" rows="10" cols="50" readonly></textarea>
  </div>
</template>