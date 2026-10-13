<!-- 三步定制组件：选款与面料、确认量体、填写收件信息并提交订单。 -->
<script setup lang="ts">
import { reactive, ref, computed, watch, onUnmounted } from 'vue'
import {
  Check,
  ChevronRight,
  ArrowLeft,
  Ruler,
  Leaf,
  RotateCcw,
  LoaderCircle,
  Info,
  Scissors,
} from 'lucide-vue-next'
import ProductImage from './ProductImage.vue'
import { api } from '../api'
import type { Account, Catalog, Customization, Measurements, Quote, Order } from '../types'
const props = defineProps<{ catalog: Catalog; user: Account | null; productId: string }>()
const emit = defineEmits<{ signIn: []; ordered: [order: Order]; accountUpdated: [account: Account] }>()
// 以当前目录构造默认选配，允许父页面指定初始款式。
const defaults: Customization = {
  productId: props.productId || props.catalog.products[0]!.id,
  fabricId: props.catalog.fabrics[0]!.id,
  colorId: props.catalog.fabrics[0]!.colorId,
  fit: 'regular',
  length: 'standard',
  embroidery: false,
  neckline: 'round',
  quantity: 1,
}
// 每次进入定制页从当前款式开始，直接填写选配参数。
const selection = reactive<Customization>({ ...defaults })
const measures = reactive<Measurements>({ height: 165, chest: 86, waist: 68, hip: 92, shoulder: 38 })
const shipping = reactive({ recipient: '', phone: '', address: '', note: '' })
const confirmedMeasures = ref(false)
const step = ref(1)
const busy = ref(false)
const quote = ref<Quote | null>(null)
const quoteBusy = ref(true)
const error = ref('')
const quoteError = ref('')
// 同一次下单及其失败重试共用幂等标识，成功后才生成新标识。
const requestKey = ref(crypto.randomUUID())
const product = computed(() => props.catalog.products.find((p) => p.id === selection.productId)!)
const color = computed(() => props.catalog.colors.find((c) => c.id === selection.colorId)!)
const fabric = computed(() => props.catalog.fabrics.find((f) => f.id === selection.fabricId)!)
const fields = [
  { key: 'height', name: '身高', min: 130, max: 210 },
  { key: 'chest', name: '胸围', min: 60, max: 150 },
  { key: 'waist', name: '腰围', min: 45, max: 150 },
  { key: 'hip', name: '臀围', min: 60, max: 160 },
  { key: 'shoulder', name: '肩宽', min: 25, max: 65 },
] as const
// 登录或账号资料更新后，填入已有尺寸以及收件姓名和手机号。
watch(
  () => props.user,
  (user) => {
    if (user?.measurements) Object.assign(measures, user.measurements)
    if (user) {
      shipping.recipient = user.displayName
      shipping.phone = user.phone
    }
  },
  { immediate: true },
)
watch(
  () => props.productId,
  (id) => {
    if (id) selection.productId = id
  },
)
let timer: ReturnType<typeof setTimeout>
// 递增请求序号用来丢弃过时报价，防止慢响应覆盖新的选配价格。
let quoteSequence = 0
// 选配变化后等待 200 毫秒请求报价；序号保证只显示最新方案的价格。
watch(
  selection,
  () => {
    quoteBusy.value = true
    quoteError.value = ''
    const sequence = ++quoteSequence
    clearTimeout(timer)
    timer = setTimeout(async () => {
      try {
        const result = await api<Quote>('/quotes', { method: 'POST', body: JSON.stringify(selection) })
        if (sequence === quoteSequence) quote.value = result
      } catch (e) {
        if (sequence === quoteSequence) quoteError.value = e instanceof Error ? e.message : '报价暂时不可用'
      } finally {
        if (sequence === quoteSequence) quoteBusy.value = false
      }
    }, 200)
  },
  { deep: true, immediate: true },
)
// 离开页面时取消待发请求的计时器，并让已发出的报价响应失效。
onUnmounted(() => {
  clearTimeout(timer)
  quoteSequence++
})
// 选颜色时同步选择对应面料，优先保留同成分的面料。
function chooseColor(id: string) {
  const matching =
    props.catalog.fabrics.find((f) => f.colorId === id && f.composition === fabric.value.composition) ||
    props.catalog.fabrics.find((f) => f.colorId === id)
  if (matching) {
    selection.fabricId = matching.id
    selection.colorId = id
  }
}
// 面料变化后同步其实际配色，保持报价请求的面料与颜色一致。
watch(
  () => selection.fabricId,
  (id) => {
    const matching = props.catalog.fabrics.find((f) => f.id === id)
    if (matching) selection.colorId = matching.colorId
  },
)
// 恢复默认设计，同时回到第一步并清理表单错误。
function reset() {
  Object.assign(selection, defaults)
  step.value = 1
  error.value = ''
}
// 要求登录后保存独立量体档案，再进入收件信息确认步骤。
async function saveMeasures() {
  if (!props.user) {
    emit('signIn')
    return
  }
  busy.value = true
  error.value = ''
  try {
    emit(
      'accountUpdated',
      await api<Account>('/account/measurements', { method: 'PUT', body: JSON.stringify(measures) }),
    )
    step.value = 3
  } catch (e) {
    error.value = e instanceof Error ? e.message : '量体数据未能保存'
  } finally {
    busy.value = false
  }
}
// 提交选配、尺寸和收件信息；成功后通知父组件显示订单。
async function submit() {
  if (!props.user) {
    emit('signIn')
    return
  }
  busy.value = true
  error.value = ''
  try {
    const order = await api<Order>('/orders', {
      method: 'POST',
      body: JSON.stringify({
        customization: selection,
        measurements: measures,
        ...shipping,
        requestKey: requestKey.value,
      }),
    })
    requestKey.value = crypto.randomUUID()
    emit('ordered', order)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '订单提交失败，请重试'
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>餐饮制服定制</h1>
      <p>按餐饮岗位选款，确认面料、尺寸和收货信息。</p>
    </div>
    <button class="text-button" @click="reset">
      <RotateCcw :size="15" />
      重置方案
    </button>
  </div>
  <div class="studio-layout">
    <!-- 真实照片用于款式展示；无照片时 SVG 根据选配生成示意。 -->
    <section class="studio-preview" aria-label="服装款式预览">
      <div class="preview-top">
        <span class="pill">
          <Leaf :size="14" />
          为你而制
        </span>
        <span>{{ product.imageUrl ? '款式照片' : '款式与配色示意' }}</span>
      </div>
      <ProductImage
        :image-url="product.imageUrl"
        :name="product.name"
        :type="product.previewType"
        :sleeve="product.sleeveType"
        :collar="product.collarType"
        :color="color.hex"
        :neckline="selection.neckline"
        :length="selection.length"
        :fit="selection.fit"
        :embroidery="selection.embroidery"
      />
      <div class="preview-caption">
        <h2>{{ product.name }}</h2>
        <p>{{ color.name }} / {{ fabric.name }}</p>
        <p v-if="product.imageUrl" class="photo-caption">款式照片；配色与细节以所选方案为准</p>
      </div>
      <div class="preview-foot">
        <Scissors :size="16" />
        <span>根据量体数据独立裁剪</span>
      </div>
    </section>
    <section class="studio-controls">
      <nav class="steps" aria-label="定制步骤">
        <button
          v-for="(name, i) in ['选择设计', '填写尺寸', '确认定制']"
          :key="name"
          :disabled="i + 1 > step"
          :class="{ active: step === i + 1, done: step > i + 1 }"
          :aria-current="step === i + 1 ? 'step' : undefined"
          @click="step = i + 1"
        >
          <span>
            <Check v-if="step > i + 1" :size="13" />
            <template v-else>{{ i + 1 }}</template>
          </span>
          {{ name }}
        </button>
      </nav>
      <!-- 第一步选择款式、面料、配色和细节。 -->
      <div v-if="step === 1" class="design-options">
        <div class="field-group">
          <h3>喜欢什么款式？</h3>
          <div class="style-options">
            <button
              v-for="p in catalog.products"
              :key="p.id"
              :aria-label="p.name"
              :title="p.name"
              :class="{ selected: selection.productId === p.id }"
              @click="selection.productId = p.id"
            >
              <ProductImage
                :image-url="p.imageUrl"
                :name="p.name"
                :type="p.previewType"
                :sleeve="p.sleeveType"
                :collar="p.collarType"
                :color="catalog.colors.find((c) => c.id === p.color)?.hex"
              />
              <span>{{ p.name }}</span>
            </button>
          </div>
        </div>
        <div class="field-group">
          <h3>选择面料</h3>
          <div class="fabric-options">
            <button
              v-for="f in catalog.fabrics"
              :key="f.id"
              :class="{ selected: selection.fabricId === f.id }"
              @click="selection.fabricId = f.id"
            >
              <span
                class="mini-swatch"
                :class="f.id"
                :style="{ '--fabric-color': props.catalog.colors.find((c) => c.id === f.colorId)?.hex }"
              ></span>
              <span>
                <strong>{{ f.name }}</strong>
                <small>{{ f.composition }}</small>
              </span>
              <span class="option-price">{{ f.surcharge ? `+ ¥${f.surcharge}` : '已包含' }}</span>
              <Check v-if="selection.fabricId === f.id" :size="16" />
            </button>
          </div>
        </div>
        <div class="field-group">
          <h3>
            选择配色
            <span class="muted">{{ color.name }}</span>
          </h3>
          <div class="color-options">
            <button
              v-for="c in catalog.colors"
              :key="c.id"
              class="color-option"
              :class="{ selected: selection.colorId === c.id }"
              :style="{ '--swatch': c.hex }"
              :aria-label="c.name"
              :aria-pressed="selection.colorId === c.id"
              @click="chooseColor(c.id)"
            >
              <Check v-if="selection.colorId === c.id" :size="17" />
            </button>
          </div>
        </div>
        <div class="option-grid">
          <label>
            版型
            <select v-model="selection.fit" aria-label="版型">
              <option value="regular">自然合身</option>
              <option value="relaxed">宽松自在</option>
            </select>
          </label>
          <label>
            衣长
            <select v-model="selection.length" aria-label="衣长">
              <option value="standard">标准长度</option>
              <option value="long">加长款（+ ¥{{ catalog.optionPrices.longPrice }}）</option>
            </select>
          </label>
          <label>
            数量
            <select v-model.number="selection.quantity" aria-label="数量">
              <option v-for="n in 5" :key="n" :value="n">{{ n }} 件</option>
            </select>
          </label>
        </div>
        <label class="checkbox-line">
          <input v-model="selection.embroidery" type="checkbox" />
          <span>添加枝叶刺绣</span>
          <span class="muted">+ ¥{{ catalog.optionPrices.embroideryPrice }} / 件</span>
        </label>
      </div>
      <!-- 第二步填写并确认厘米单位的量体数据。 -->
      <form v-else-if="step === 2" class="form-stack measure-form" @submit.prevent="step = 3">
        <div>
          <h2>合身，从了解你开始</h2>
          <p class="muted">
            {{
              user?.measurements
                ? '已载入你保存的量体数据，请核对后继续。'
                : '请填写实际量体数据，单位为厘米（cm）。'
            }}
          </p>
        </div>
        <div class="measure-grid">
          <label v-for="f in fields" :key="f.key">
            {{ f.name }}
            <div class="unit-input">
              <input
                v-model.number="measures[f.key]"
                type="number"
                step="0.01"
                :min="f.min"
                :max="f.max"
                required
                :aria-label="f.name"
              />
              <span>cm</span>
            </div>
          </label>
        </div>
        <aside class="measurement-guide">
          <Ruler :size="22" />
          <div>
            <strong>怎样测量更准确？</strong>
            <p>
              自然站立，用软尺贴合身体，不要拉紧。胸围取胸部最丰满处，腰围取腰部最细处，臀围取臀部最丰满处，肩宽测量两侧肩点的距离。
            </p>
          </div>
        </aside>
        <label class="checkbox-line">
          <input v-model="confirmedMeasures" type="checkbox" required />
          我已核对以上量体数据
        </label>
        <p v-if="error" class="error-message" role="alert">{{ error }}</p>
        <div class="inline-actions">
          <button type="button" class="text-button" @click="step = 1">
            <ArrowLeft :size="16" />
            返回设计
          </button>
          <button
            type="button"
            class="text-button"
            :disabled="busy || !confirmedMeasures"
            @click="saveMeasures"
          >
            保存到量体档案
          </button>
          <button class="button primary">
            确认尺寸
            <ChevronRight :size="17" />
          </button>
        </div>
      </form>
      <!-- 第三步确认收件信息并提交，服务端会重新核价。 -->
      <form v-else class="form-stack checkout-form" @submit.prevent="submit">
        <div>
          <h2>让你的方案成为一件衣服</h2>
          <p class="muted">确认收货信息，提交后工作室将安排制作。</p>
        </div>
        <div class="order-summary">
          <strong>{{ product.name }}</strong>
          <p>
            {{ fabric.name }} · {{ color.name }} ·
            {{ selection.fit === 'regular' ? '自然合身' : '宽松自在' }} ·
            {{ selection.length === 'long' ? '加长款' : '标准长度' }} · {{ selection.quantity }} 件
          </p>
          <p>
            身高 {{ measures.height }} / 胸围 {{ measures.chest }} / 腰围 {{ measures.waist }} / 臀围
            {{ measures.hip }} / 肩宽 {{ measures.shoulder }} cm
          </p>
        </div>
        <div class="option-grid">
          <label>
            收货人
            <input
              v-model.trim="shipping.recipient"
              maxlength="30"
              required
              autocomplete="name"
              placeholder="收货人姓名"
            />
          </label>
          <label>
            联系电话
            <input
              v-model="shipping.phone"
              type="tel"
              pattern="1[3-9][0-9]{9}"
              maxlength="11"
              required
              autocomplete="tel"
              placeholder="11 位手机号"
            />
          </label>
        </div>
        <label>
          收货地址
          <textarea
            v-model.trim="shipping.address"
            minlength="5"
            maxlength="200"
            required
            autocomplete="street-address"
            rows="2"
            placeholder="省 / 市 / 区 / 街道及门牌号"
          />
        </label>
        <label>
          定制备注
          <span class="muted">选填</span>
          <textarea
            v-model.trim="shipping.note"
            maxlength="300"
            rows="2"
            placeholder="如穿着习惯、希望调整的细节"
          />
        </label>
        <div class="notice">
          <Info :size="17" />
          <span>此为本地业务演示，提交订单不会扣款。制作进度由管理员更新。</span>
        </div>
        <p v-if="error" class="error-message" role="alert">{{ error }}</p>
        <div class="inline-actions">
          <button type="button" class="text-button" @click="step = 2">
            <ArrowLeft :size="16" />
            修改尺寸
          </button>
          <button class="button primary" :disabled="busy || quoteBusy || !!quoteError">
            <LoaderCircle v-if="busy" class="spin" :size="17" />
            {{ busy ? '正在提交…' : user ? '确认下单' : '登录并下单' }}
            <ChevronRight v-if="!busy" :size="17" />
          </button>
        </div>
      </form>
      <!-- 只展示最新服务端报价，请求进行中或失败时禁用相关提交入口。 -->
      <div class="quote-bar">
        <div>
          <small>
            定制总价
            <span v-if="selection.quantity > 1">/ {{ selection.quantity }} 件</span>
          </small>
          <strong>{{ quoteBusy ? '计算中…' : quote ? `¥${quote.total.toLocaleString()}` : '—' }}</strong>
          <p v-if="quote && !quoteBusy">
            款式 ¥{{ quote.basePrice }} + 面料 ¥{{ quote.fabricPrice }} + 细节 ¥{{ quote.detailPrice }} / 件
          </p>
          <p v-if="quoteError" class="error-message" role="alert">{{ quoteError }}</p>
          <p v-else class="muted">预计 7–12 个工作日制作完成</p>
        </div>
        <button
          v-if="step === 1"
          class="button primary"
          :disabled="quoteBusy || !!quoteError"
          @click="step = 2"
        >
          下一步，填写尺寸
          <ChevronRight :size="18" />
        </button>
      </div>
    </section>
  </div>
</template>
