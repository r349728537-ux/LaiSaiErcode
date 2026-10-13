<!-- 用户订单和管理员订单共用组件，依据 admin 属性选择接口与可执行操作。 -->
<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Package,
  ChevronRight,
  ChevronLeft,
  RefreshCw,
  LoaderCircle,
  Check,
  Scissors,
  Truck,
  ClipboardCheck,
  Clock,
} from 'lucide-vue-next'
import ProductImage from './ProductImage.vue'
import ModalShell from './ModalShell.vue'
import { api } from '../api'
import type { Catalog, Order, OrderPage, OrderStatus } from '../types'
const props = defineProps<{ catalog: Catalog; admin?: boolean }>()
const emit = defineEmits<{ customize: [] }>()
const result = ref<OrderPage>({ items: [], total: 0, page: 0, pages: 0 })
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const filter = ref('all')
const selected = ref<Order | null>(null)
const labels: Record<OrderStatus, string> = {
  PENDING: '待确认',
  CUTTING: '裁剪中',
  SEWING: '缝制中',
  QUALITY: '质检中',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
// 页面制作流程顺序用于进度展示；实际状态变更仍由服务端校验。
const sequence: OrderStatus[] = ['PENDING', 'CUTTING', 'SEWING', 'QUALITY', 'SHIPPED', 'COMPLETED']
const nextStatus = computed(() => {
  if (!selected.value) return undefined
  const index = sequence.indexOf(selected.value.status)
  return index >= 0 ? sequence[index + 1] : undefined
})
// 状态筛选只作用于当前分页中的订单。
const visible = computed(() =>
  result.value.items.filter((o) => filter.value === 'all' || o.status === filter.value),
)
// 管理员列表使用后台接口，普通用户列表由服务端限制为当前用户。
const endpoint = computed(() => (props.admin ? '/admin/orders' : '/orders'))
// 优先使用现有目录的配色，目录缺失时使用订单响应中的展示信息。
function color(order: Order) {
  return (
    props.catalog.colors.find((c) => c.id === order.customization.colorId) || {
      name: order.colorName,
      hex: order.colorHex,
    }
  )
}
function fabric(order: Order) {
  return (
    props.catalog.fabrics.find((f) => f.id === order.customization.fabricId) || { name: order.fabricName }
  )
}
function date(value: string) {
  return value.slice(0, 16).replace('T', ' ')
}
function openOrder(order: Order) {
  selected.value = order
  error.value = ''
}
function closeOrder() {
  selected.value = null
  error.value = ''
}
// 加载指定页；普通用户和管理员使用各自的订单接口。
async function load(page = result.value.page) {
  loading.value = true
  error.value = ''
  try {
    const data = await api<OrderPage>(`${endpoint.value}?page=${page}`)
    result.value = data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}
// 更新选中订单状态后刷新列表，同时保留服务端返回的最新详情。
async function change(status: OrderStatus) {
  if (!selected.value) return
  busy.value = true
  error.value = ''
  try {
    selected.value = await api<Order>(`${endpoint.value}/${selected.value.id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '更新失败，请重试'
  } finally {
    busy.value = false
  }
}
onMounted(() => load())
// 身份模式改变时清空详情和筛选，再从第一页重新查询。
watch(
  () => props.admin,
  () => {
    selected.value = null
    filter.value = 'all'
    load(0)
  },
)
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>{{ admin ? '工作室管理' : '我的定制订单' }}</h1>
      <p>{{ admin ? '从确认到交付，让每一件定制都有迹可循。' : '一件衣服的诞生，值得慢慢期待。' }}</p>
    </div>
    <button class="button secondary" :disabled="loading" @click="load()">
      <RefreshCw :size="16" />
      刷新订单
    </button>
  </div>
  <div class="orders-toolbar">
    <label class="filter-select">
      订单状态
      <select v-model="filter">
        <option value="all">全部状态</option>
        <option v-for="(label, key) in labels" :key="key" :value="key">{{ label }}</option>
      </select>
    </label>
  </div>
  <p v-if="error" class="error-message" role="alert">{{ error }}</p>
  <div v-if="loading" class="loading-state">
    <LoaderCircle class="spin" />
    正在查找你的订单…
  </div>
  <div v-else-if="!visible.length" class="empty-state">
    <Package :size="45" stroke-width="1.2" />
    <h2>
      {{ result.total ? '没有匹配的订单' : admin ? '工作室还没有收到订单' : '你的第一件定制，正等着开始' }}
    </h2>
    <p>
      {{ result.total ? '请选择其他状态，也可以查看其他页。' : '挑选喜欢的款式，再用面料和配色赋予它个性。' }}
    </p>
    <button v-if="!result.total" class="button primary" @click="emit('customize')">
      开始专属定制
      <ChevronRight :size="17" />
    </button>
  </div>
  <div v-else class="order-list">
    <article v-for="order in visible" :key="order.id" class="order-card">
      <div class="order-card-header">
        <span>{{ order.orderNumber }}</span>
        <small>{{ date(order.createdAt) }}</small>
        <span class="status-pill" :class="order.status.toLowerCase()">{{ labels[order.status] }}</span>
      </div>
      <div class="order-card-body">
        <div class="order-thumbnail" :style="{ backgroundColor: `${color(order)?.hex}20` }">
          <ProductImage
            :image-url="catalog.products.find((p) => p.id === order.customization.productId)?.imageUrl"
            :name="order.productName"
            :type="order.previewType"
            :sleeve="catalog.products.find((p) => p.id === order.customization.productId)?.sleeveType"
            :collar="catalog.products.find((p) => p.id === order.customization.productId)?.collarType"
            :color="color(order)?.hex"
            :embroidery="order.customization.embroidery"
          />
        </div>
        <div class="order-card-info">
          <h3>{{ order.productName }}</h3>
          <p>{{ fabric(order)?.name }} / {{ color(order)?.name }}</p>
          <small>
            {{ order.customization.fit === 'regular' ? '自然合身' : '宽松自在' }} ·
            {{ order.customization.quantity }} 件
            <span v-if="admin">· {{ order.recipient }}</span>
          </small>
        </div>
        <div class="order-price">
          <strong>¥{{ order.total.toLocaleString() }}</strong>
          <button
            class="text-button"
            @click="openOrder(order)"
          >
            {{ admin ? '管理订单' : '查看详情' }}
            <ChevronRight :size="16" />
          </button>
        </div>
      </div>
    </article>
  </div>
  <div v-if="result.pages > 1" class="pagination">
    <button
      class="icon-button"
      :disabled="result.page === 0 || loading"
      aria-label="上一页"
      @click="load(result.page - 1)"
    >
      <ChevronLeft :size="18" />
    </button>
    <span>第 {{ result.page + 1 }} / {{ result.pages }} 页，共 {{ result.total }} 个订单</span>
    <button
      class="icon-button"
      :disabled="result.page + 1 >= result.pages || loading"
      aria-label="下一页"
      @click="load(result.page + 1)"
    >
      <ChevronRight :size="18" />
    </button>
  </div>
  <!-- 详情弹窗展示制作进度、历史明细和当前可执行操作。 -->
  <ModalShell
    v-if="selected"
    :title="admin ? '订单管理' : '定制订单详情'"
    wide
    @close="closeOrder"
  >
    <div class="detail-heading">
      <div>
        <strong>{{ selected.productName }}</strong>
        <p>{{ selected.orderNumber }}</p>
      </div>
      <span class="status-pill" :class="selected.status.toLowerCase()">{{ labels[selected.status] }}</span>
    </div>
    <div v-if="selected.status !== 'CANCELLED'" class="order-timeline">
      <div
        v-for="(status, index) in sequence"
        :key="status"
        :class="{ reached: sequence.indexOf(selected.status) >= index }"
      >
        <span>
          <Check v-if="sequence.indexOf(selected.status) > index" :size="15" />
          <Clock v-else-if="index === 0" :size="15" />
          <Scissors v-else-if="index < 3" :size="15" />
          <ClipboardCheck v-else-if="index === 3" :size="15" />
          <Truck v-else-if="index === 4" :size="15" />
          <Check v-else :size="15" />
        </span>
        <small>{{ labels[status] }}</small>
      </div>
    </div>
    <div v-if="selected.items.length > 1" class="multi-item-details">
      <h3>款式明细</h3>
      <article v-for="item in selected.items" :key="item.id || item.productName">
        <strong>{{ item.productName }}</strong>
        <p>{{ item.fabricName }} / {{ item.colorName }} / {{ item.customization.quantity }} 件</p>
        <p v-if="item.options.length">{{ item.options.map((o) => o.name).join(' / ') }}</p>
        <span>¥{{ item.unitPrice }} × {{ item.customization.quantity }} = ¥{{ item.total }}</span>
      </article>
    </div>
    <dl class="order-details">
      <div>
        <dt>面料与颜色</dt>
        <dd>{{ fabric(selected)?.name }} / {{ color(selected)?.name }}</dd>
      </div>
      <div>
        <dt>定制细节</dt>
        <dd>{{ selected.items[0]?.options.map((o) => o.name).join(' / ') || '未记录定制选项' }}</dd>
      </div>
      <div>
        <dt>量体数据</dt>
        <dd>
          身高 {{ selected.measurements.height }} · 胸围 {{ selected.measurements.chest }} · 腰围
          {{ selected.measurements.waist }} · 臀围 {{ selected.measurements.hip }} · 肩宽
          {{ selected.measurements.shoulder }} cm
        </dd>
      </div>
      <div>
        <dt>收货信息</dt>
        <dd>
          {{ selected.recipient }}　{{ selected.phone }}
          <br />
          {{ selected.address }}
        </dd>
      </div>
      <div v-if="selected.note">
        <dt>定制备注</dt>
        <dd>{{ selected.note }}</dd>
      </div>
      <div>
        <dt>订单金额</dt>
        <dd>
          <strong>¥{{ selected.total.toLocaleString() }}</strong>
          <span v-if="selected.items.length <= 1">
            （¥{{ selected.unitPrice }} × {{ selected.customization.quantity }} 件）
          </span>
          <span v-else>（共 {{ selected.items.length }} 款）</span>
        </dd>
      </div>
      <div>
        <dt>最近更新</dt>
        <dd>{{ date(selected.updatedAt) }}</dd>
      </div>
    </dl>
    <p v-if="error" class="error-message" role="alert">{{ error }}</p>
    <div class="inline-actions detail-actions">
      <button
        v-if="selected.status === 'PENDING'"
        class="button danger"
        :disabled="busy"
        @click="change('CANCELLED')"
      >
        取消订单
      </button>
      <button v-if="admin && nextStatus" class="button primary" :disabled="busy" @click="change(nextStatus!)">
        {{ busy ? '正在更新…' : `更新为${labels[nextStatus]}` }}
      </button>
      <button
        v-else-if="!admin && selected.status === 'SHIPPED'"
        class="button primary"
        :disabled="busy"
        @click="change('COMPLETED')"
      >
        确认收货
      </button>
    </div>
    <p class="detail-note">本地演示订单；进度由工作室维护，未连接实际物流。</p>
  </ModalShell>
</template>
