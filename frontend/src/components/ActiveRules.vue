<template>
  <div v-if="rules.length" class="bg-yellow-100 border-2 border-black rounded-lg px-4 py-2 flex gap-3 items-center overflow-hidden">
    <span class="font-fredoka text-sm whitespace-nowrap">📋 Rules:</span>
    <div class="overflow-x-auto flex gap-4 text-sm font-nunito font-semibold">
      <span v-for="(rule, i) in rules" :key="i" class="whitespace-nowrap">
        · {{ rule }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ rules: string[] }>()
</script>
