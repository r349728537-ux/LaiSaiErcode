import type { Product } from './types'

// 当前标签页使用 sessionStorage 保存登录令牌，刷新页面后仍可复用。
const TOKEN_KEY = 'lesaire-jwt'
// 读取令牌，未登录时返回空字符串。
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}
// 登录成功后保存服务端签发的令牌。
export function saveToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token)
}
// 退出或认证失效时清理本地令牌。
export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}
// 保留 HTTP 状态码，便于页面区分认证失败和其他业务错误。
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message)
  }
}
// 统一请求入口：附加 Bearer 令牌、解析 JSON 响应，并转换服务端错误提示。
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  // 普通请求使用 JSON；FormData 的 multipart 边界由浏览器自动生成。
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const response = await fetch(`/api${path}`, { ...options, headers, credentials: 'omit' })
  const payload = await response.json().catch(() => ({}))
  if (!response.ok) {
    // 受保护请求认证失效时通知根组件重新打开登录弹窗。
    if (response.status === 401 && path !== '/auth/login') {
      clearToken()
      if (path !== '/auth/me') window.dispatchEvent(new Event('lesaire-auth-expired'))
    }
    throw new ApiError(payload.message || '请求未完成，请稍后重试', response.status)
  }
  return payload as T
}

// 图片上传使用 multipart 表单，并编码款式标识以构造正确路径。
export async function uploadProductImage(productId: string, file: File): Promise<Product> {
  const form = new FormData()
  form.append('file', file)
  return api<Product>(`/admin/styles/${encodeURIComponent(productId)}/image`, { method: 'POST', body: form })
}
