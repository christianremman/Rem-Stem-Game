<template>
  <div class="flex flex-col items-center gap-1">
    <QrcodeVue :value="url" :size="size" :margin="1" />
    <span class="font-fredoka text-xl tracking-widest">{{ code }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import QrcodeVue from 'qrcode.vue'

const props = defineProps<{ code: string; size?: number }>()
const url = computed(() => `${window.location.origin}/play/${props.code}`)
</script>
