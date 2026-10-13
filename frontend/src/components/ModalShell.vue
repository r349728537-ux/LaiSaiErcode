<!-- 可复用弹窗容器，管理键盘关闭、焦点循环、背景滚动和关闭后的焦点恢复。 -->
<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { X } from 'lucide-vue-next'
const props = defineProps<{ title: string; wide?: boolean }>()
const emit = defineEmits<{ close: [] }>()
const panel = ref<HTMLElement>()
// 记录打开弹窗前的焦点和页面滚动状态。
const previousFocus = document.activeElement as HTMLElement | null
const previousOverflow = document.body.style.overflow
// Escape 关闭弹窗；Tab 与 Shift+Tab 在弹窗内首尾循环，保持键盘操作范围。
function keyboard(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
  if (event.key !== 'Tab' || !panel.value) return
  const elements = Array.from(
    panel.value.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), select, textarea, a[href], [tabindex="0"]',
    ),
  )
  const first = elements[0],
    last = elements.at(-1)
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  }
  if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}
// 挂载后锁定背景滚动，等待内容渲染再把焦点移入弹窗。
onMounted(async () => {
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', keyboard)
  await nextTick()
  panel.value?.querySelector<HTMLElement>('input, button')?.focus()
})
// 移除键盘监听，并恢复原滚动状态和打开弹窗前的焦点。
onUnmounted(() => {
  document.body.style.overflow = previousOverflow
  document.removeEventListener('keydown', keyboard)
  previousFocus?.focus()
})
</script>
<template>
  <!-- 挂载到 body 避免父级样式影响定位；只有点击遮罩本身才触发关闭。 -->
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        ref="panel"
        class="modal-panel"
        :class="{ wide: props.wide }"
        role="dialog"
        aria-modal="true"
        :aria-label="props.title"
      >
        <header class="modal-header">
          <h2>{{ props.title }}</h2>
          <button class="icon-button" aria-label="关闭弹窗" @click="emit('close')"><X :size="20" /></button>
        </header>
        <slot />
      </section>
    </div>
  </Teleport>
</template>
