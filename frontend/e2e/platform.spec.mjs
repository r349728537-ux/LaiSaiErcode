// 浏览器验收覆盖真实接口流程；其中注册和下单用例会产生标注的演示数据。
import { test, expect } from '@playwright/test'

// 验证真实目录与场景筛选，定制页仍保留面料选择。
test('首页、餐饮场景筛选和核心面料选配', async ({ page }) => {
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  const catalog = await (await page.request.get('/api/catalog')).json()
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /为餐饮团队，/ })).toBeVisible()
  await page.getByRole('button', { name: '餐饮制服', exact: true }).click()
  await expect(page.locator('.product-card')).toHaveCount(catalog.products.length)
  const category = catalog.products[0].category
  await page.getByRole('button', { name: category, exact: true }).click()
  await expect(page.locator('.product-card')).toHaveCount(
    catalog.products.filter((p) => p.category === category).length,
  )
  await expect(page.locator('.favorite-button, .collection-filters')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '面料选配', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '全部场景', exact: true }).click()
  await expect(page.locator('.product-card')).toHaveCount(catalog.products.length)
  await page.getByRole('button', { name: '制服定制', exact: true }).click()
  await expect(page.locator('.fabric-options button')).toHaveCount(catalog.fabrics.length)
  expect(errors).toEqual([])
})

// 用不同浏览器上下文隔离用户与管理员，验证报价到收货的完整制作流程。
test('真实登录、报价、下单、管理员推进与用户收货', async ({ page, browser }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '登录 / 注册' }).click()
  await page.getByRole('button', { name: '填入用户账号' }).click()
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page.getByRole('button', { name: '林小姐', exact: true })).toBeVisible()
  const token = await page.evaluate(() => sessionStorage.getItem('lesaire-jwt'))
  expect(token).toMatch(/^[\w-]+\.[\w-]+\.[\w-]+$/)
  expect((await page.request.get('/api/auth/me')).status()).toBe(401)
  expect(
    (await page.request.get('/api/auth/me', { headers: { Authorization: `Bearer ${token}` } })).status(),
  ).toBe(200)
  expect(
    (await page.request.get('/api/admin/orders', { headers: { Authorization: `Bearer ${token}` } })).status(),
  ).toBe(403)
  expect((await page.context().cookies()).some((cookie) => cookie.name === 'JSESSIONID')).toBe(false)
  await page.getByRole('button', { name: '开始制服定制' }).click()
  const catalog = await (await page.request.get('/api/catalog')).json()
  await page.locator('.fabric-options button').nth(1).click()
  await page.getByLabel('衣长', { exact: true }).selectOption('long')
  await page.getByLabel('数量', { exact: true }).selectOption('2')
  await page.getByLabel('添加枝叶刺绣').check()
  const total =
    (catalog.products[0].basePrice +
      catalog.fabrics[1].surcharge +
      catalog.optionPrices.longPrice +
      catalog.optionPrices.embroideryPrice) *
    2
  const expectedPrice = `¥${total.toLocaleString()}`
  await expect(page.locator('.quote-bar strong')).toHaveText(expectedPrice)
  await page.getByRole('button', { name: '下一步，填写尺寸' }).click()
  await page.getByLabel('我已核对以上量体数据').check()
  await page.getByRole('button', { name: '确认尺寸' }).click()
  await page.getByLabel('收货地址', { exact: true }).fill('上海市徐汇区莱赛尔路 88 号（浏览器验收）')
  await page.getByLabel('定制备注', { exact: false }).fill('自动化验收订单，无需实际制作')
  await page.getByRole('button', { name: '确认下单', exact: true }).click()
  await expect(page.getByRole('heading', { name: '我的定制订单' })).toBeVisible()
  const card = page.locator('.order-card').first()
  await expect(card).toContainText(expectedPrice)
  const orderNumber = await card.locator('.order-card-header > span').first().innerText()
  const adminContext = await browser.newContext()
  const admin = await adminContext.newPage()
  await admin.goto(process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173/')
  await admin.getByRole('button', { name: '登录 / 注册' }).click()
  await admin.getByRole('button', { name: '填入管理员账号' }).click()
  await admin.getByRole('button', { name: '登录', exact: true }).click()
  await admin.getByRole('button', { name: '工作室管理', exact: true }).click()
  await admin
    .locator('.order-card')
    .filter({ hasText: orderNumber })
    .getByRole('button', { name: '管理订单' })
    .click()
  for (const state of ['裁剪中', '缝制中', '质检中', '已发货']) {
    await admin.getByRole('button', { name: `更新为${state}`, exact: true }).click()
    await expect(admin.getByRole('dialog').locator('.detail-heading .status-pill')).toHaveText(state)
  }
  await page.getByRole('button', { name: '刷新订单' }).click()
  await card.getByRole('button', { name: '查看详情' }).click()
  await page.getByRole('button', { name: '确认收货', exact: true }).click()
  await expect(page.getByRole('dialog').locator('.detail-heading .status-pill')).toHaveText('已完成')
  await adminContext.close()
})

// 验证注册后自动登录、尺寸保存、刷新保留登录以及退出撤销。
test('新用户注册、量体保存和订单空状态', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: '我的订单', exact: true }).click()
  await page.getByRole('button', { name: '创建账号', exact: true }).click()
  await page.getByLabel('你的称呼', { exact: true }).fill('浏览器验收用户')
  await page.getByLabel('手机号', { exact: true }).fill(`13${String(Date.now()).slice(-9)}`)
  await page.getByLabel('密码', { exact: true }).fill('Browser123!')
  await page.getByRole('button', { name: '注册并登录' }).click()
  await expect(page.getByRole('heading', { name: '你的第一件定制，正等着开始' })).toBeVisible()
  const token = await page.evaluate(() => sessionStorage.getItem('lesaire-jwt'))
  expect(token).toMatch(/^[\w-]+\.[\w-]+\.[\w-]+$/)
  await page.getByRole('button', { name: '浏览器验收用户', exact: true }).click()
  await page.getByLabel('身高', { exact: true }).fill('172.25')
  const saveResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/account/measurements') && response.request().method() === 'PUT',
  )
  await page.getByRole('button', { name: '保存量体档案', exact: true }).click()
  expect((await saveResponse).request().headers()['authorization']).toBe(`Bearer ${token}`)
  await expect(page.getByRole('status')).toContainText('量体档案已保存')
  await page.reload()
  await expect(page.getByLabel('身高', { exact: true })).toHaveValue('172.25')
  expect(await page.evaluate(() => sessionStorage.getItem('lesaire-jwt'))).toBe(token)
  await page.getByRole('button', { name: '退出登录', exact: true }).click()
  await expect(page.getByRole('button', { name: '登录 / 注册' })).toBeVisible()
  expect(await page.evaluate(() => sessionStorage.getItem('lesaire-jwt'))).toBeNull()
  expect(
    (await page.request.get('/api/auth/me', { headers: { Authorization: `Bearer ${token}` } })).status(),
  ).toBe(401)
})

// 使用手机视口检查页面宽度与选配操作。
test('手机页面无横向溢出并可完成定制设计', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /为餐饮团队，/ })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('button', { name: '开始制服定制' }).click()
  const catalog = await (await page.request.get('/api/catalog')).json()
  const color = catalog.colors.at(-1)
  await page.getByRole('button', { name: color.name, exact: true }).click()
  await expect(page.locator('.preview-caption')).toContainText(color.name)
  const material =
    catalog.fabrics.find((f) => f.colorId === color.id && f.composition === catalog.fabrics[0].composition) ||
    catalog.fabrics.find((f) => f.colorId === color.id)
  await expect(page.locator('.quote-bar strong')).toHaveText(
    `¥${(catalog.products[0].basePrice + material.surcharge).toLocaleString()}`,
  )
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})

// 验证键盘关闭弹窗后焦点返回原入口。
test('登录弹窗支持键盘退出和焦点恢复', async ({ page }) => {
  await page.goto('/')
  const trigger = page.getByRole('button', { name: '登录 / 注册' })
  await trigger.click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(trigger).toBeFocused()
})
