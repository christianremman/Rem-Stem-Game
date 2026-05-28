<template>
  <div class="relative flex flex-col items-center">
    <!-- Fixed pointer at top -->
    <svg width="30" height="24" class="z-10 drop-shadow-md" style="margin-bottom:-4px">
      <polygon points="15,22 0,0 30,0" fill="#1F2937" stroke="black" stroke-width="1"/>
    </svg>

    <div class="relative rounded-full overflow-hidden border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
         :style="{ width: size + 'px', height: size + 'px' }">
      <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
        <!-- Rotate the whole wheel group -->
        <g :style="wheelStyle">
          <g v-for="(seg, i) in segments" :key="i">
            <path :d="segmentPath(i)" :fill="seg.color" stroke="black" stroke-width="2"/>
            <text
              :transform="labelTransform(i)"
              fill="white"
              :font-size="fontSize"
              font-family="Fredoka One, cursive"
              text-anchor="middle"
              dominant-baseline="middle"
            >{{ truncate(seg.label) }}</text>
          </g>
        </g>
        <!-- Center hub -->
        <circle :cx="cx" :cy="cy" :r="hubR" fill="white" stroke="black" stroke-width="2"/>
        <text :x="cx" :y="cy" text-anchor="middle" dominant-baseline="middle"
              font-size="20">🎡</text>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface Segment {
  label: string
  color: string
}

const props = withDefaults(defineProps<{
  segments: Segment[]
  size?: number
  spinning?: boolean
  targetAngle?: number
}>(), {
  size: 320,
  spinning: false,
  targetAngle: 0
})

const currentRotation = ref(0)
const animRotation = ref(0)
const isAnimating = ref(false)

const cx = computed(() => props.size / 2)
const cy = computed(() => props.size / 2)
const r = computed(() => props.size / 2 - 2)
const hubR = computed(() => props.size * 0.1)
const fontSize = computed(() => Math.max(10, props.size / 22))

function segmentPath(index: number): string {
  const n = props.segments.length
  const anglePerSeg = (2 * Math.PI) / n
  const startAngle = index * anglePerSeg - Math.PI / 2
  const endAngle = startAngle + anglePerSeg
  const x1 = cx.value + r.value * Math.cos(startAngle)
  const y1 = cy.value + r.value * Math.sin(startAngle)
  const x2 = cx.value + r.value * Math.cos(endAngle)
  const y2 = cy.value + r.value * Math.sin(endAngle)
  const largeArc = n <= 2 ? 1 : 0
  return `M ${cx.value} ${cy.value} L ${x1} ${y1} A ${r.value} ${r.value} 0 ${largeArc} 1 ${x2} ${y2} Z`
}

function labelTransform(index: number): string {
  const n = props.segments.length
  const anglePerSeg = (2 * Math.PI) / n
  const midAngle = index * anglePerSeg - Math.PI / 2 + anglePerSeg / 2
  const lx = cx.value + (r.value * 0.65) * Math.cos(midAngle)
  const ly = cy.value + (r.value * 0.65) * Math.sin(midAngle)
  const deg = (midAngle * 180) / Math.PI + 90
  return `translate(${lx}, ${ly}) rotate(${deg})`
}

function truncate(s: string): string {
  return s.length > 10 ? s.slice(0, 9) + '…' : s
}

const wheelStyle = computed(() => ({
  transform: `rotate(${animRotation.value}deg)`,
  transformOrigin: `${cx.value}px ${cy.value}px`,
  transition: isAnimating.value
    ? 'transform 4s cubic-bezier(0.2, 0, 0.2, 1)'
    : 'none'
}))

watch(() => props.spinning, (val) => {
  if (val) {
    isAnimating.value = true
    // spin 5+ full rotations, land on target
    const extra = 5 * 360 + (360 - props.targetAngle)
    animRotation.value = currentRotation.value + extra
    currentRotation.value = animRotation.value
    setTimeout(() => { isAnimating.value = false }, 4200)
  }
})
</script>
