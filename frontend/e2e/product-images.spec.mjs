import { test, expect } from '@playwright/test'

// 独立 UI 测试数据：通过路由替身模拟上传，不上传或替换真实款式照片。
const png = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==',
  'base64',
)
const fixtureToken = 'photo-ui-test-token'

// 读取实际目录后通过路由替身提供账号和图片响应，隔离真实上传写入。
async function prepare(page, role = 'ADMIN') {
  const catalog = await (await page.request.get('/api/catalog')).json()
  const product = catalog.products[0]
  catalog.products = catalog.products.map((p) => ({ ...p, imageUrl: null }))
  await page.addInitScript((token) => sessionStorage.setItem('lesaire-jwt', token), fixtureToken)
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({
      json: { id: 123, phone: '13800000000', displayName: '图片验收', role, measurements: null },
    }),
  )
  await page.route('**/api/catalog', (route) => route.fulfill({ json: catalog }))
  await page.route('**/uploads/styles/ui-photo.png', (route) =>
    route.fulfill({ contentType: 'image/png', body: png }),
  )
  return { catalog, product }
}

// 拦截上传请求验证请求头和 multipart 内容，再检查多个页面同步展示照片。
test('管理员选择本机照片后更新卡片、首页和定制预览', async ({ page }) => {
  const { catalog, product } = await prepare(page)
  let uploads = 0
  await page.route('**/api/admin/styles/*/image', async (route) => {
    uploads++
    const request = route.request()
    expect(request.method()).toBe('POST')
    expect(request.headers().authorization).toBe(`Bearer ${fixtureToken}`)
    expect(request.headers()['content-type']).toContain('multipart/form-data; boundary=')
    expect(request.postDataBuffer().toString('latin1')).toContain('name="file"; filename="my-uniform.png"')
    const updated = { ...product, imageUrl: '/uploads/styles/ui-photo.png' }
    catalog.products = catalog.products.map((p) => (p.id === product.id ? updated : p))
    await route.fulfill({ json: updated })
  })
  await page.goto('/#collection')
  const card = page
    .locator('.product-card')
    .filter({ has: page.getByRole('heading', { name: product.name, exact: true }) })
  const input = card.getByLabel(`上传款式图片：${product.name}`, { exact: true })
  await expect(input).toHaveAttribute('accept', 'image/jpeg,image/png')
  await input.setInputFiles({
    name: 'not-a-picture.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('test'),
  })
  await expect(page.getByRole('status')).toContainText('请选择 JPG 或 PNG 图片')
  expect(uploads).toBe(0)
  await input.setInputFiles({
    name: 'too-big.png',
    mimeType: 'image/png',
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1),
  })
  await expect(page.getByRole('status')).toContainText('不超过 5 MB')
  expect(uploads).toBe(0)
  await input.setInputFiles({ name: 'my-uniform.png', mimeType: 'image/png', buffer: png })
  await expect(page.getByRole('status')).toContainText('图片已更新')
  expect(uploads).toBe(1)
  await expect(card.getByRole('img', { name: `${product.name}款式照片`, exact: true })).toBeVisible()
  await expect(card.getByLabel(`更换图片：${product.name}`, { exact: true })).toBeAttached()
  await page.reload()
  await expect(card.getByRole('img', { name: `${product.name}款式照片`, exact: true })).toBeVisible()
  await page.getByRole('button', { name: '平台首页', exact: true }).click()
  await expect(
    page.locator('.home-products').getByRole('img', { name: `${product.name}款式照片`, exact: true }),
  ).toBeVisible()
  await page
    .locator('.home-products')
    .getByRole('button', { name: `开始定制${product.name}`, exact: true })
    .click()
  await expect(
    page.locator('.studio-preview').getByRole('img', { name: `${product.name}款式照片`, exact: true }),
  ).toBeVisible()
  await expect(page.locator('.studio-preview')).toContainText('款式照片；配色与细节以所选方案为准')
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.locator('.studio-preview .product-photo')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

// 普通用户隐藏上传入口，图片 404 时显示 SVG 款式示意。
test('普通用户没有上传入口，照片加载失败回退到款式示意', async ({ page }) => {
  const { catalog, product } = await prepare(page, 'CUSTOMER')
  catalog.products[0].imageUrl = '/uploads/styles/missing-ui-photo.png'
  await page.route('**/uploads/styles/missing-ui-photo.png', (route) => route.fulfill({ status: 404 }))
  await page.goto('/#collection')
  await expect(page.locator('.image-upload-control')).toHaveCount(0)
  const card = page
    .locator('.product-card')
    .filter({ has: page.getByRole('heading', { name: product.name, exact: true }) })
  await expect(card.getByRole('img', { name: `${product.name}款式与配色示意`, exact: true })).toBeVisible()
})
