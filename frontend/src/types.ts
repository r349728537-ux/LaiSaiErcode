/**
 * 制服款式展示数据；业务编码用于选择，图片地址可为空。
 */
export interface Product {
  id: string
  name: string
  category: string
  description: string
  basePrice: number
  color: string
  tag: string
  previewType: string
  sleeveType: string
  collarType: string
  imageUrl?: string | null
}
/**
 * 具体面料及其配色，surcharge 表示单件加价。
 */
export interface Fabric {
  id: string
  name: string
  composition: string
  description: string
  surcharge: number
  weight: string
  colorId: string
}
/**
 * 配色目录，稳定标识用于选配，十六进制值用于 SVG 绘制。
 */
export interface Color {
  id: string
  name: string
  hex: string
}
/**
 * 目录接口的完整响应，供首页、款式、面料和定制页面共享。
 */
export interface Catalog {
  products: Product[]
  fabrics: Fabric[]
  colors: Color[]
  optionPrices: { longPrice: number; embroideryPrice: number }
}
/**
 * 量体字段，单位厘米；数值范围及小数位数由表单和后端共同校验。
 */
export interface Measurements {
  height: number
  chest: number
  waist: number
  hip: number
  shoulder: number
}
/**
 * 登录账号资料，角色用于页面入口展示，后端仍独立检查实际权限。
 */
export interface Account {
  id: number
  phone: string
  displayName: string
  role: 'CUSTOMER' | 'ADMIN' | 'DESIGNER' | 'TAILOR'
  measurements: Measurements | null
}
/**
 * 定制选配表单，保存业务标识、版型、衣长、刺绣及数量。
 */
export interface Customization {
  productId: string
  fabricId: string
  colorId: string
  fit: string
  length: string
  embroidery: boolean
  neckline: string
  quantity: number
}
/**
 * 服务端报价，拆分基础价、面料和选项加价，并返回单价与总价。
 */
export interface Quote {
  basePrice: number
  fabricPrice: number
  detailPrice: number
  unitPrice: number
  quantity: number
  total: number
  delivery: string
}
/**
 * 页面制作流程状态，与后端 OrderStatus 枚举保持对应。
 */
export type OrderStatus = 'PENDING' | 'CUTTING' | 'SEWING' | 'QUALITY' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'
/**
 * 订单卡片及详情数据，同时保留完整明细集合。
 */
export interface Order {
  id: number
  orderNumber: string
  productName: string
  customization: Customization
  measurements: Measurements
  recipient: string
  phone: string
  address: string
  note: string
  unitPrice: number
  total: number
  status: OrderStatus
  createdAt: string
  updatedAt: string
  fabricName: string
  colorName: string
  colorHex: string
  previewType: string
  items: OrderItem[]
}
/**
 * 分页结果，page 从 0 开始，页面状态筛选作用于当前 items。
 */
export interface OrderPage {
  items: Order[]
  total: number
  page: number
  pages: number
}

/**
 * 历史订单明细，包含成交价格、量体数据和定制选项快照。
 */
export interface OrderItem {
  id: number | null
  productName: string
  customization: Customization
  measurements: Measurements
  unitPrice: number
  total: number
  fabricName: string
  colorName: string
  colorHex: string
  previewType: string
  options: { group: string; name: string; value: string; priceDelta: number }[]
}
