<template>
  <div class="flex flex-col gap-3">
    <p class="font-fredoka text-lg text-center">Did they do it?</p>
    <div class="flex gap-3 justify-center">
      <button @click="$emit('vote', 'DONE')"
              class="flex-1 bg-wheel-green text-white font-fredoka text-xl py-4 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] min-h-[56px] active:translate-y-0.5">
        ✅ Done
      </button>
      <button @click="$emit('vote', 'REFUSED')"
              class="flex-1 bg-wheel-red text-white font-fredoka text-xl py-4 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] min-h-[56px] active:translate-y-0.5">
        ❌ Refused
      </button>
    </div>
    <button @click="$emit('vote', 'BOO')"
            class="w-full bg-wheel-orange text-white font-fredoka text-xl py-3 rounded-xl border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] active:translate-y-0.5">
      👎 BOO
    </button>
  </div>
</template>

<script setup lang="ts">
defineEmits<{ vote: [vote: string] }>()
</script>
