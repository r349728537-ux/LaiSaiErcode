<!-- 未上传照片时，显示餐饮衬衫的款式与配色示意。 -->
<script setup lang="ts">
import { computed, useId } from 'vue'

// 保留图片组件传入的参数；当前目录只有衬衫，不再绘制其他服装。
const props = withDefaults(
  defineProps<{
    type?: string
    color?: string
    neckline?: string
    length?: string
    fit?: string
    embroidery?: boolean
    label?: string
    sleeve?: string
    collar?: string
  }>(),
  {
    type: 'shirt',
    color: '#8daf9d',
    neckline: 'round',
    length: 'standard',
    fit: 'regular',
    embroidery: false,
    label: '制服款式与配色示意',
    sleeve: 'long',
    collar: 'shirt',
  },
)

// 每个预览使用独立的阴影编号，避免多个 SVG 互相引用。
const shadowId = `shirt-shadow-${useId().replace(/:/g, '')}`
const hem = computed(() => (props.length === 'long' ? 93 : 73))

// 三种实际款式由短袖、长袖以及衬衫领、立领组合而成。
const outline = computed(() =>
  props.sleeve === 'short'
    ? `M111 66l-32 13-35 47 28 21 22-36v71l-5 ${hem.value}q49 16 102 0l-5-${hem.value}v-71l22 36 28-21-35-47-32-13z`
    : `M111 66l-32 13-31 45-11 107 28 5 18-103 11 49-5 ${hem.value}q49 16 102 0l-5-${hem.value} 11-49 18 103 28-5-11-107-31-45-32-13z`,
)
</script>

<template>
  <svg class="garment-svg" viewBox="0 0 280 360" role="img" :aria-label="props.label">
    <defs>
      <filter :id="shadowId" x="-30%" y="-20%" width="160%" height="160%">
        <feDropShadow dx="0" dy="9" stdDeviation="7" flood-color="#344e42" flood-opacity=".12" />
      </filter>
    </defs>
    <ellipse cx="140" cy="338" rx="70" ry="8" fill="#263e33" opacity=".055" />
    <path
      d="M141 39v-8c0-11 16-10 15 0-1 7-10 8-15 8M141 39l-61 31q-6 7 4 7h112q10 0 4-7z"
      fill="none"
      stroke="#9c998e"
      stroke-width="2"
      stroke-linecap="round"
    />

    <!-- 宽松版型横向放大；衣长、袖长和领型由当前方案决定。 -->
    <g
      :fill="props.color"
      :filter="`url(#${shadowId})`"
      :transform="props.fit === 'relaxed' ? 'translate(-8 0) scale(1.06 1)' : ''"
      stroke="#32483e"
      stroke-opacity=".3"
      stroke-width="1.15"
      stroke-linejoin="round"
    >
      <path :d="outline" />
      <path v-if="props.collar === 'stand'" d="M111 66q29 12 58 0l-3 15q-26 10-52 0z" />
      <path v-else d="M111 66l-9 23 20 18 18-30 18 30 20-18-9-23" fill="none" />
      <path :d="`M140 79v${103 + hem}`" fill="none" />
      <path v-if="props.sleeve !== 'short'" d="M48 221l18 5M214 226l18-5" fill="none" />
      <path d="M106 128h22v25q-10 9-22 0z" fill="none" />
      <g fill="#f3f0e7" stroke="none">
        <circle v-for="y in [117, 145, 173, 201, 228]" :key="y" cx="145" :cy="y" r="2" />
      </g>
      <path
        v-if="props.embroidery"
        d="M161 124v16m0-7q-13-11-10-1 2 5 10 1m0-2q13-11 10-1-2 5-10 1"
        fill="none"
        stroke="#f3eedc"
        stroke-width="2"
        stroke-opacity=".95"
      />
    </g>
  </svg>
</template>
