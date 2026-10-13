<!-- 平台入口：加载目录和账号，切换业务页面，处理登录和图片上传。 -->
<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  House,
  Shirt,
  WandSparkles,
  Leaf,
  Package,
  Ruler,
  LayoutDashboard,
  ArrowUpRight,
  ChevronRight,
  Check,
  Scissors,
  Truck,
  LogOut,
  LoaderCircle,
  Upload,
} from 'lucide-vue-next'
import ProductImage from './components/ProductImage.vue'
import AuthDialog from './components/AuthDialog.vue'
import StudioView from './components/StudioView.vue'
import OrdersView from './components/OrdersView.vue'
import { api, ApiError, clearToken, getToken, uploadProductImage } from './api'
import type { Account, Catalog, Measurements, Product, Order } from './types'

type Page = 'home' | 'collection' | 'studio' | 'orders' | 'profile' | 'admin'
const page = ref<Page>('home')
const catalog = ref<Catalog | null>(null)
const user = ref<Account | null>(null)
const loading = ref(true)
const loadError = ref('')
const authOpen = ref(false)
const toast = ref('')
const selectedProduct = ref('')
const category = ref('全部场景')
const imageUploads = ref(new Set<string>())
const measurements = reactive<Measurements>({ height: 165, chest: 86, waist: 68, hip: 92, shoulder: 38 })
const profileBusy = ref(false)
const profileError = ref('')
let pendingPage: Page = 'home'
let toastTimer: ReturnType<typeof setTimeout>

const navItems = [
  { id: 'home', label: '平台首页', icon: House },
  { id: 'collection', label: '餐饮制服', icon: Shirt },
  { id: 'studio', label: '制服定制', icon: WandSparkles },
  { id: 'orders', label: '我的订单', icon: Package },
] as const
const names: Record<Page, string> = {
  home: '平台首页',
  collection: '餐饮制服',
  studio: '制服定制',
  orders: '我的订单',
  profile: '我的量体档案',
  admin: '工作室管理',
}
const categories = computed(() => ['全部场景', ...new Set(catalog.value?.products.map((p) => p.category))])
const products = computed(
  () =>
    catalog.value?.products.filter((p) => category.value === '全部场景' || p.category === category.value) ||
    [],
)
const measureFields = [
  { key: 'height', name: '身高', min: 130, max: 210 },
  { key: 'chest', name: '胸围', min: 60, max: 150 },
  { key: 'waist', name: '腰围', min: 45, max: 150 },
  { key: 'hip', name: '臀围', min: 60, max: 160 },
  { key: 'shoulder', name: '肩宽', min: 25, max: 65 },
] as const

function color(id: string) {
  return catalog.value?.colors.find((c) => c.id === id)?.hex || '#8daf9d'
}

function notify(message: string) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 4000)
}

// 管理员选择照片后提交给后端，成功后更新同一份目录数据。
async function uploadImage(product: Product, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || imageUploads.value.has(product.id)) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    notify('请选择 JPG 或 PNG 图片')
    return
  }
  if (!file.size || file.size > 5 * 1024 * 1024) {
    notify('图片大小需大于 0 且不超过 5 MB')
    return
  }
  imageUploads.value.add(product.id)
  try {
    const updated = await uploadProductImage(product.id, file)
    if (catalog.value) {
      catalog.value.products = catalog.value.products.map((p) =>
        p.id === updated.id ? { ...p, imageUrl: updated.imageUrl } : p,
      )
    }
    notify(product.name + '的图片已更新')
  } catch (e) {
    notify(e instanceof Error ? e.message : '图片上传失败，请重试')
  } finally {
    imageUploads.value.delete(product.id)
  }
}

// 私有页面要求登录；管理员入口还需要 ADMIN 角色。
function navigate(next: Page) {
  if (['orders', 'profile', 'admin'].includes(next) && !user.value) {
    pendingPage = next
    authOpen.value = true
    return
  }
  if (next === 'admin' && user.value?.role !== 'ADMIN') {
    notify('工作室管理需要管理员账号')
    return
  }
  page.value = next
  window.location.hash = next
  if (next === 'profile' && user.value?.measurements) {
    Object.assign(measurements, user.value.measurements)
  }
  window.scrollTo({ top: 0, behavior: 'instant' })
}

function routeFromHash() {
  const next = location.hash.slice(1) as Page
  if (Object.hasOwn(names, next)) navigate(next)
}

function customize(product?: Product) {
  if (!catalog.value?.products.length || !catalog.value.fabrics.length) {
    notify('暂时没有可定制的款式或面料')
    return
  }
  selectedProduct.value = product?.id || ''
  navigate('studio')
}

function signIn() {
  pendingPage = page.value
  authOpen.value = true
}

function authExpired() {
  user.value = null
  signIn()
}

function authenticated(account: Account) {
  user.value = account
  authOpen.value = false
  notify('欢迎回来，' + account.displayName)
  navigate(pendingPage)
}

async function logout() {
  let revoked = true
  try {
    if (getToken()) await api('/auth/logout', { method: 'POST' })
  } catch (e) {
    revoked = e instanceof ApiError && e.status === 401
  }
  clearToken()
  user.value = null
  authOpen.value = false
  navigate('home')
  notify(revoked ? '已退出登录' : '已清除本机登录状态，令牌注销请求未完成')
}

// 首次加载公开目录；没有登录的用户也可以浏览和设计。
async function load() {
  loading.value = true
  loadError.value = ''
  try {
    catalog.value = await api<Catalog>('/catalog')
    user.value = await api<Account>('/auth/me').catch((e) => {
      if (e instanceof ApiError && e.status === 401) return null
      throw e
    })
    routeFromHash()
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '平台暂时不可用，请确认后台服务已启动'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  profileBusy.value = true
  profileError.value = ''
  try {
    user.value = await api<Account>('/account/measurements', {
      method: 'PUT',
      body: JSON.stringify(measurements),
    })
    notify('量体档案已保存')
  } catch (e) {
    profileError.value = e instanceof Error ? e.message : '保存失败，请重试'
  } finally {
    profileBusy.value = false
  }
}

function ordered(order: Order) {
  notify('定制订单 ' + order.orderNumber + ' 已提交')
  navigate('orders')
}

onMounted(() => {
  load()
  window.addEventListener('hashchange', routeFromHash)
  window.addEventListener('lesaire-auth-expired', authExpired)
})
onUnmounted(() => {
  clearTimeout(toastTimer)
  window.removeEventListener('hashchange', routeFromHash)
  window.removeEventListener('lesaire-auth-expired', authExpired)
})
</script>

<template>
  <div class="app-shell">
    <!-- 主导航使用统一的 navigate 方法切换页面。 -->
    <aside class="sidebar">
      <a class="brand" href="#home" aria-label="莱赛尔首页">
        <span class="brand-mark"><Leaf :size="25" stroke-width="1.6" /></span>
        <span>
          <strong>莱赛尔</strong>
          <small>LESAIRE</small>
        </span>
      </a>
      <p class="sidebar-caption">餐饮制服，舒适上岗</p>
      <nav class="main-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.id"
          :class="{ active: page === item.id }"
          :aria-current="page === item.id ? 'page' : undefined"
          @click="navigate(item.id)"
        >
          <component :is="item.icon" :size="19" stroke-width="1.7" />
          <span>{{ item.label }}</span>
          <span v-if="item.id === 'studio'" class="nav-dot"></span>
        </button>
        <button
          v-if="user?.role === 'ADMIN'"
          :class="{ active: page === 'admin' }"
          @click="navigate('admin')"
        >
          <LayoutDashboard :size="19" />
          工作室管理
        </button>
      </nav>
      <div class="sidebar-bottom">
        <div class="sidebar-note">
          <Leaf :size="19" />
          <strong>为餐饮团队而制</strong>
          <p>
            从门店场景出发，
            <br />
            选择合适的制服。
          </p>
          <button @click="customize()">
            选择面料开始定制
            <ArrowUpRight :size="15" />
          </button>
        </div>
        <button class="side-secondary" :class="{ active: page === 'profile' }" @click="navigate('profile')">
          <Ruler :size="18" />
          我的量体档案
        </button>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <div class="breadcrumb">
          <span>莱赛尔餐饮制服</span>
          <ChevronRight :size="14" />
          <strong>{{ names[page] }}</strong>
        </div>
        <div class="topbar-actions">
          <span class="topbar-slogan">
            <span></span>
            让团队，舒适上岗
          </span>
          <button v-if="user" class="account-button" @click="navigate('profile')">
            <span class="avatar" aria-hidden="true">{{ user.displayName.slice(0, 1) }}</span>
            {{ user.displayName }}
          </button>
          <button v-else class="button small secondary" @click="signIn">登录 / 注册</button>
          <button v-if="user" class="icon-button" aria-label="退出登录" @click="logout">
            <LogOut :size="17" />
          </button>
        </div>
      </header>
      <!-- 先显示加载或错误状态，目录可用后再显示各业务页面。 -->
      <main id="main-content">
        <div v-if="loading" class="loading-state initial">
          <LoaderCircle class="spin" />
          正在打开莱赛尔餐饮制服…
        </div>
        <div v-else-if="loadError" class="empty-state">
          <Leaf :size="45" />
          <h1>暂时无法打开工作室</h1>
          <p>{{ loadError }}</p>
          <button class="button primary" @click="load">重新加载</button>
        </div>
        <template v-else-if="catalog">
          <!-- 首页展示场景款式及定制入口。 -->
          <template v-if="page === 'home'">
            <section class="hero">
              <div class="hero-copy">
                <div class="season-label">
                  <span></span>
                  莱赛尔餐饮制服定制
                </div>
                <h1>
                  为餐饮团队，
                  <br />
                  定制舒适制服。
                </h1>
                <p>
                  咖啡厅、酒店西餐厅、烘焙工坊。
                  <br />
                  按门店场景选款，用量体数据确认尺寸。
                </p>
                <div class="hero-actions">
                  <button class="button primary" @click="customize()">
                    开始制服定制
                    <ArrowUpRight :size="19" />
                  </button>
                  <button class="text-button" @click="navigate('collection')">
                    查看餐饮制服
                    <ChevronRight :size="17" />
                  </button>
                </div>
                <div class="hero-points">
                  <span>
                    <Check :size="14" />
                    按身形裁剪
                  </span>
                  <span>
                    <Check :size="14" />
                    自由选择配色
                  </span>
                  <span>
                    <Check :size="14" />
                    制作全程可追踪
                  </span>
                </div>
              </div>
              <div class="hero-art" aria-label="莱赛尔餐饮制服展示">
                <div class="art-oval"></div>
                <div class="art-line line-one"></div>
                <div class="art-line line-two"></div>
                <div class="hero-jacket">
                  <ProductImage
                    :image-url="catalog.products[1]?.imageUrl"
                    :name="catalog.products[1]?.name"
                    :type="catalog.products[1]?.previewType || 'shirt'"
                    :sleeve="catalog.products[1]?.sleeveType"
                    :collar="catalog.products[1]?.collarType"
                    :color="color(catalog.fabrics[1]?.colorId || catalog.products[0]?.color || 'cloud')"
                  />
                </div>
                <div class="hero-dress">
                  <ProductImage
                    :image-url="catalog.products[0]?.imageUrl"
                    :name="catalog.products[0]?.name"
                    :type="catalog.products[0]?.previewType || 'shirt'"
                    :sleeve="catalog.products[0]?.sleeveType"
                    :collar="catalog.products[0]?.collarType"
                    :color="color(catalog.fabrics[0]?.colorId || 'cloud')"
                  />
                </div>
                <div class="material-tag">
                  <span class="fabric-tag-swatch"></span>
                  <div>
                    <strong>{{ catalog.fabrics[0]?.name || '莱赛尔面料' }}</strong>
                    <small>{{ catalog.fabrics[0]?.weight || '自然触感' }}</small>
                  </div>
                  <Leaf :size="18" />
                </div>
                <span class="art-caption">为每一个认真服务的餐饮团队</span>
              </div>
            </section>
            <div class="service-strip">
              <div>
                <Scissors :size="24" />
                <span>
                  <strong>专属版型</strong>
                  <small>根据你的量体数据制作</small>
                </span>
              </div>
              <div>
                <Leaf :size="24" />
                <span>
                  <strong>触感先行</strong>
                  <small>按触感与配色找到偏好</small>
                </span>
              </div>
              <div>
                <Truck :size="24" />
                <span>
                  <strong>安心等待</strong>
                  <small>订单进度，随时查看</small>
                </span>
              </div>
            </div>
            <section class="home-products">
              <div class="section-heading">
                <div>
                  <h2>按餐饮场景选择制服</h2>
                  <p>沿用现有制服款式，搭配适合岗位的面料与尺寸。</p>
                </div>
                <button class="text-button" @click="navigate('collection')">
                  查看全部场景
                  <ChevronRight :size="16" />
                </button>
              </div>
              <div class="product-grid">
                <article v-for="p in catalog.products.slice(0, 3)" :key="p.id" class="product-card">
                  <div class="product-image" :class="p.previewType">
                    <span class="product-tag">{{ p.tag }}</span>
                    <button class="product-art-button" :aria-label="`定制${p.name}`" @click="customize(p)">
                      <ProductImage
                        :image-url="p.imageUrl"
                        :name="p.name"
                        :type="p.previewType"
                        :sleeve="p.sleeveType"
                        :collar="p.collarType"
                        :color="color(p.color)"
                      />
                    </button>
                  </div>
                  <div class="product-info">
                    <div>
                      <h3>{{ p.name }}</h3>
                      <p>{{ p.description }}</p>
                    </div>
                    <div class="product-bottom">
                      <span>
                        <strong>¥{{ p.basePrice }}</strong>
                        <small>起 / 制服定制</small>
                      </span>
                      <button class="round-button" :aria-label="`开始定制${p.name}`" @click="customize(p)">
                        <ArrowUpRight :size="19" />
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </section>
            <section class="home-bottom">
              <div class="fabric-story">
                <div class="thread-art">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div>
                  <span class="pill">面料选配</span>
                  <h2>
                    服务时的自在，
                    <br />
                    从舒适的制服开始。
                  </h2>
                  <button class="text-button" @click="customize()">
                    选择面料开始定制
                    <ChevronRight :size="17" />
                  </button>
                </div>
              </div>
              <div class="process-story">
                <h2>餐饮制服定制，分三步</h2>
                <ol>
                  <li>
                    <span>1</span>
                    <div>
                      <strong>选择岗位与款式</strong>
                      <small>按门店场景选择制服和面料</small>
                    </div>
                  </li>
                  <li>
                    <span>2</span>
                    <div>
                      <strong>留下你的尺寸</strong>
                      <small>记录身形，让剪裁更合身</small>
                    </div>
                  </li>
                  <li>
                    <span>3</span>
                    <div>
                      <strong>期待它的诞生</strong>
                      <small>确认订单，跟随制作进度</small>
                    </div>
                  </li>
                </ol>
              </div>
            </section>
          </template>
          <!-- 按餐饮场景显示制服；管理员可以上传款式照片。 -->
          <template v-else-if="page === 'collection'">
            <div class="page-heading">
              <div>
                <h1>餐饮制服款式</h1>
                <p>咖啡厅、酒店西餐厅与烘焙工坊，选择合适的工作制服。</p>
              </div>
            </div>
            <div class="collection-toolbar">
              <div class="category-tabs" aria-label="餐饮场景筛选">
                <button
                  v-for="c in categories"
                  :key="c"
                  :class="{ active: category === c }"
                  @click="category = c"
                >
                  {{ c }}
                </button>
              </div>
            </div>
            <p class="result-count">共 {{ products.length }} 款作品</p>
            <p v-if="user?.role === 'ADMIN'" class="image-upload-hint">
              在款式卡片上传你自己的照片，支持 JPG、PNG，每张不超过 5 MB。
            </p>
            <div v-if="!products.length" class="empty-state">
              <Shirt :size="42" />
              <h2>没有匹配的餐饮制服</h2>
              <p>请选择其他餐饮场景，或返回全部场景。</p>
              <button class="button secondary" @click="category = '全部场景'">浏览全部场景</button>
            </div>
            <div v-else class="product-grid collection-grid">
              <article v-for="p in products" :key="p.id" class="product-card">
                <div class="product-image" :class="p.previewType">
                  <span class="product-tag">{{ p.tag }}</span>
                  <button class="product-art-button" :aria-label="`定制${p.name}`" @click="customize(p)">
                    <ProductImage
                      :image-url="p.imageUrl"
                      :name="p.name"
                      :type="p.previewType"
                      :sleeve="p.sleeveType"
                      :collar="p.collarType"
                      :color="color(p.color)"
                    />
                  </button>
                  <label
                    v-if="user?.role === 'ADMIN'"
                    class="image-upload-control"
                    :class="{ busy: imageUploads.has(p.id) }"
                  >
                    <LoaderCircle v-if="imageUploads.has(p.id)" class="spin" :size="14" />
                    <Upload v-else :size="14" />
                    <span>
                      {{ imageUploads.has(p.id) ? '上传中…' : p.imageUrl ? '更换图片' : '上传款式图片' }}
                    </span>
                    <input
                      class="visually-hidden"
                      type="file"
                      accept="image/jpeg,image/png"
                      :aria-label="`${p.imageUrl ? '更换图片' : '上传款式图片'}：${p.name}`"
                      :disabled="imageUploads.has(p.id)"
                      @change="uploadImage(p, $event)"
                    />
                  </label>
                </div>
                <div class="product-info">
                  <h3>{{ p.name }}</h3>
                  <p>{{ p.description }}</p>
                  <div class="product-bottom">
                    <span>
                      <strong>¥{{ p.basePrice }}</strong>
                      <small>起 / 制服定制</small>
                    </span>
                    <button class="round-button" :aria-label="`开始定制${p.name}`" @click="customize(p)">
                      <ArrowUpRight :size="19" />
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </template>
          <!-- 定制组件通过事件请求登录、更新账号资料和通知下单完成。 -->
          <StudioView
            v-else-if="page === 'studio' && catalog.products.length && catalog.fabrics.length"
            :catalog="catalog"
            :user="user"
            :product-id="selectedProduct"
            @sign-in="signIn"
            @ordered="ordered"
            @account-updated="user = $event"
          />

          <!-- 用户或页面身份变化时重新创建订单组件，清理上一身份的列表状态。 -->
          <OrdersView
            :key="`${page}-${user?.id}`"
            v-else-if="page === 'orders' || page === 'admin'"
            :catalog="catalog"
            :admin="page === 'admin'"
            @customize="customize()"
          />
          <template v-else-if="page === 'profile' && user">
            <div class="page-heading">
              <div>
                <h1>我的量体档案</h1>
                <p>保存量体数据，为餐饮制服的合身剪裁做准备。</p>
              </div>
            </div>
            <section class="profile-layout">
              <div class="profile-intro">
                <span class="profile-avatar">{{ user.displayName.slice(0, 1) }}</span>
                <h2>{{ user.displayName }}</h2>
                <p>{{ user.phone }}</p>
                <span class="pill">{{ user.role === 'ADMIN' ? '工作室管理员' : '餐饮客户' }}</span>
                <Ruler :size="90" stroke-width=".7" />
                <p>
                  量体数据仅用于你的服装定制。
                  <br />
                  身形变化时，可以随时更新。
                </p>
              </div>
              <form class="form-stack profile-form" @submit.prevent="saveProfile">
                <h2>我的尺寸</h2>
                <p class="muted">自然站立，软尺贴合身体。所有尺寸以厘米为单位。</p>
                <div class="measure-grid">
                  <label v-for="f in measureFields" :key="f.key">
                    {{ f.name }}
                    <div class="unit-input">
                      <input
                        v-model.number="measurements[f.key]"
                        :aria-label="f.name"
                        type="number"
                        step="0.01"
                        :min="f.min"
                        :max="f.max"
                        required
                      />
                      <span>cm</span>
                    </div>
                  </label>
                </div>
                <aside class="measurement-guide">
                  <Ruler :size="22" />
                  <div>
                    <strong>量体小贴士</strong>
                    <p>
                      测量胸围和臀围时取最丰满处，腰围取最细处；肩宽为两侧肩点间的距离。请让家人或朋友协助测量，并复测一次。
                    </p>
                  </div>
                </aside>
                <p v-if="profileError" class="error-message" role="alert">{{ profileError }}</p>
                <button class="button primary" :disabled="profileBusy">
                  {{ profileBusy ? '正在保存…' : '保存量体档案' }}
                  <Check :size="17" />
                </button>
              </form>
            </section>
          </template>
        </template>
      </main>
      <footer class="footer">
        <span>莱赛尔 LESAIRE</span>
        <span>为餐饮团队，定制舒适制服。</span>
      </footer>
    </div>
    <!-- 登录弹窗与业务页面共享账号状态。 -->
    <AuthDialog v-if="authOpen" @close="authOpen = false" @authenticated="authenticated" />

    <Transition name="toast">
      <div v-if="toast" class="toast-message" role="status">
        <Check :size="18" />
        {{ toast }}
      </div>
    </Transition>
  </div>
</template>
