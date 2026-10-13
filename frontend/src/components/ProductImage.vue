<!-- 统一款式图片组件：优先显示照片，缺少照片或加载失败时回退到 SVG 示意。 -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import GarmentPreview from './GarmentPreview.vue'

const props = withDefaults(
  defineProps<{
    imageUrl?: string | null
    name?: string
    type?: string
    color?: string
    neckline?: string
    length?: string
    fit?: string
    embroidery?: boolean
    sleeve?: string
    collar?: string
  }>(),
  { name: '制服款式', type: 'shirt', color: '#8daf9d' },
)
// 记录当前图片加载失败，避免反复展示不可用地址。
const failed = ref(false)
// 图片地址更新后允许重新尝试加载新照片。
watch(
  () => props.imageUrl,
  () => {
    failed.value = false
  },
)
</script>

<template>
  <!-- 真实照片保持原图外观，SVG 回退才根据颜色和细节绘制预览。 -->
  <div class="product-visual" :class="{ 'has-photo': props.imageUrl && !failed }">
    <img
      v-if="props.imageUrl && !failed"
      class="product-photo"
      :src="props.imageUrl"
      :alt="`${props.name}款式照片`"
      decoding="async"
      @error="failed = true"
    />
    <GarmentPreview
      v-else
      :type="props.type"
      :color="props.color"
      :neckline="props.neckline"
      :length="props.length"
      :fit="props.fit"
      :embroidery="props.embroidery"
      :sleeve="props.sleeve"
      :collar="props.collar"
      :label="`${props.name}款式与配色示意`"
    />
  </div>
</template>
