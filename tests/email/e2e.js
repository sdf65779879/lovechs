// Executed by playwright-cli run-code --filename. No @playwright/test or API-based order creation.
async (page) => {
  const base = 'http://127.0.0.1:19080'
  const inboxUrl = 'http://127.0.0.1:19026'
  const steps = []
  const errors = []
  const assert = (condition, message) => { if (!condition) throw new Error(message) }
  const wait = async (condition, message, timeout = 15000) => {
    const deadline = Date.now() + timeout
    while (Date.now() < deadline) {
      if (await condition()) return
      await new Promise(resolve => setTimeout(resolve, 150))
    }
    throw new Error(message)
  }
  const inbox = async () => (await (await page.request.get(inboxUrl + '/messages')).json()).messages
  const monitor = (target) => target.on('pageerror', error => errors.push(error.message))
  monitor(page)
  const health = await (await page.request.get(inboxUrl + '/health')).json()
  assert(health.fixture === 'couple-email-tests', 'E2E requires the isolated SMTP fixture')
  await page.request.post(inboxUrl + '/control', { data: { clear: true } })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto(base + '/admin/login')
  await page.getByRole('textbox', { name: '管理员账号', exact: true }).fill('admin')
  await page.getByRole('textbox', { name: '密码', exact: true }).fill('admin123')
  await page.getByRole('button', { name: /进\s*入\s*工\s*作\s*台/ }).click()
  await page.waitForURL('**/admin/dashboard')
  steps.push('管理员通过真实登录页面进入工作台')

  await page.getByRole('link', { name: '系统设置', exact: true }).click()
  await page.getByRole('heading', { name: '新订单邮件提醒', exact: true }).waitFor()
  const token = await page.evaluate(() => localStorage.getItem('couple_token') || sessionStorage.getItem('couple_token'))
  const adminApi = async (path) => (await (await page.request.get(base + '/api/v1' + path,
    { headers: { Authorization: 'Bearer ' + token } })).json()).data
  assert((await adminApi('/admin/email/settings')).provider === 'SMTP', 'E2E must use real SMTP')
  const recipients = page.getByRole('textbox', { name: /^大厨收件邮箱/ })
  const save = page.getByRole('button', { name: '保存邮件设置', exact: true })
  const test = page.getByRole('button', { name: '发送测试邮件', exact: true })
  await recipients.fill('invalid-recipient')
  assert(await test.isDisabled(), 'Unsaved settings should disable test sending')
  await save.click()
  await wait(async () => (await page.getByRole('alert').textContent())?.includes('邮箱格式不正确'), 'Invalid mailbox must produce a persistent error')
  steps.push('无效收件地址显示持续错误，未保存配置禁止测试')

  await page.getByRole('textbox', { name: 'SMTP 主机', exact: true }).fill('mail')
  await page.getByRole('spinbutton', { name: 'SMTP 端口', exact: true }).fill('2525')
  await page.getByRole('combobox', { name: '加密方式', exact: true }).selectOption('NONE')
  await page.getByRole('textbox', { name: '发件邮箱', exact: true }).fill('sender@example.test')
  await page.getByRole('textbox', { name: 'SMTP 用户名', exact: true }).fill('sender@example.test')
  const password = page.getByRole('textbox', { name: /^邮箱授权码/ })
  await password.fill('fixture-app-password')
  await recipients.fill('e2e-chef@example.test;E2E-CHEF@example.test;second@example.test')
  await page.getByRole('checkbox', { name: '开启新订单邮件通知', exact: true }).check()
  await save.click()
  await wait(() => test.isEnabled(), 'Saved settings should enable test sending')
  assert(await password.inputValue() === '', 'Password must be cleared after successful save')
  assert(await recipients.inputValue() === 'e2e-chef@example.test,second@example.test', 'Recipients must be normalized and deduplicated')
  steps.push('通过界面保存 SMTP、授权码和通知开关；授权码不回显，收件地址去重')
  await test.click()
  await wait(async () => (await inbox()).length === 1, 'SMTP fixture did not receive the UI test message')
  assert((await inbox())[0].subject.includes('通知测试'), 'Wrong test email subject')
  await page.getByRole('button', { name: '刷新记录', exact: true }).click()
  await wait(async () => (await page.getByRole('listitem').first().textContent()).includes('已提交邮件服务'), 'UI should display SMTP acceptance')
  steps.push('点击测试邮件后 SMTP 实际收件，管理页显示成功投递记录')
  await page.screenshot({ path: 'output/playwright/email-e2e-settings.png', fullPage: true })

  const context = await page.context().browser().newContext({ viewport: { width: 390, height: 844 } })
  await context.tracing.start({ screenshots: true, snapshots: true })
  const customer = await context.newPage()
  monitor(customer)
  try {
    await customer.goto(base + '/login')
    await customer.getByRole('textbox', { name: '账号', exact: true }).fill('love')
    await customer.getByRole('textbox', { name: '密码', exact: true }).fill('love123')
    await customer.getByRole('button', { name: /开\s*饭/ }).click()
    await customer.waitForURL('**/home')
    steps.push('独立手机浏览器通过食客登录页面进入菜单')

    const placeOrder = async (remark) => {
      await customer.goto(base + '/home')
      await customer.getByRole('heading', { name: '番茄牛腩拌面', level: 3, exact: true }).locator('xpath=ancestor::button').click()
      await customer.getByRole('heading', { name: '番茄牛腩拌面', level: 2, exact: true }).waitFor()
      await customer.getByRole('button', { name: '微辣', exact: true }).click()
      await customer.getByPlaceholder('比如：不要香菜、少放盐，或者……夸夸大厨').fill('E2E少盐')
      // The existing quantity control has two icon buttons; use its numeric label's parent.
      const stepper = customer.locator('span[class*="min-w-"]').filter({ hasText: /^1$/ }).locator('..')
      await stepper.getByRole('button').nth(1).click()
      await customer.getByRole('button', { name: '加入购物车', exact: true }).last().click()
      await customer.getByRole('heading', { name: '番茄牛腩拌面', level: 2, exact: true }).waitFor({ state: 'hidden' })
      await customer.getByRole('button', { name: '购物车', exact: true }).click()
      await customer.getByRole('button', { name: /去\s*结\s*算/ }).click()
      await customer.waitForURL('**/order-confirm')
      assert((await customer.locator('body').innerText()).includes('微辣 · E2E少盐'), 'Checkout must show taste and item remark')
      await customer.getByRole('button', { name: '指定时间', exact: true }).click()
      await customer.locator('input[type="datetime-local"]').fill('2026-10-13T19:30')
      await customer.getByPlaceholder('比如：今天想吃软一点的米饭……或者：大厨辛苦啦，爱你！').fill(remark)
      const responsePromise = customer.waitForResponse(response => response.url().endsWith('/api/v1/orders') && response.request().method() === 'POST')
      await customer.getByRole('button', { name: /^提交订单/ }).click()
      const response = await responsePromise
      const result = await response.json()
      assert(result.code === 0, 'UI order submission failed')
      assert(response.request().headers()['idempotency-key'], 'Browser must provide an idempotency key')
      await customer.waitForURL(base + '/orders/' + result.data.id)
      await customer.getByRole('heading', { name: '订单详情', exact: true }).waitFor()
      return result.data
    }

    const order = await placeOrder('E2E整单不要葱')
    assert(order.items[0].quantity === 2, 'Quantity change must reach the submitted order')
    steps.push('食客通过菜单选菜、数量加到两份、微辣和备注，购物车结算并指定时间下单')
    await customer.screenshot({ path: 'output/playwright/email-e2e-customer-pending.png', fullPage: true })
    await wait(async () => (await inbox()).some(mail => mail.subject.includes(order.orderNo)), 'No SMTP message for the browser-created order')
    const mails = (await inbox()).filter(mail => mail.subject.includes(order.orderNo))
    assert(mails.length === 1, 'The browser-created order should have exactly one email')
    const mail = mails[0]
    for (const text of ['宝贝', '番茄牛腩拌面 × 2', '微辣', 'E2E少盐', 'E2E整单不要葱', '2026-10-13 19:30']) {
      assert(mail.body.includes(text), 'Email is missing order content: ' + text)
    }
    assert(JSON.stringify(mail.recipients) === JSON.stringify(['e2e-chef@example.test', 'second@example.test']), 'Wrong chef recipients')
    steps.push('SMTP 实际收到该订单唯一邮件，核对顾客、菜品数量、口味、两种备注、时间和收件地址')

    await page.getByRole('link', { name: /^订单管理/ }).click()
    const row = page.getByRole('row').filter({ hasText: order.orderNo })
    await row.waitFor()
    await row.getByRole('button', { name: '处理', exact: true }).click()
    await page.getByRole('button', { name: '接单', exact: true }).waitFor()
    for (const [label, status] of [['接单', 'ACCEPTED'], ['开始制作', 'COOKING'], ['完成订单', 'COMPLETED']]) {
      const promise = page.waitForResponse(response => response.url().endsWith('/admin/orders/' + order.id + '/status') && response.request().method() === 'PUT')
      await page.getByRole('button', { name: label, exact: true }).click()
      const result = await (await promise).json()
      assert(result.code === 0 && result.data.status === status, 'Chef state transition failed: ' + label)
    }
    await page.getByText('订单已结束，无待处理操作', { exact: true }).waitFor()
    await page.screenshot({ path: 'output/playwright/email-e2e-chef-completed.png', fullPage: true })
    steps.push('大厨通过订单表格打开对应订单，接单 → 开始制作 → 完成订单')
    await customer.getByText('完成时间', { exact: true }).waitFor({ timeout: 20000 })
    await customer.getByRole('button', { name: /再来一单/ }).waitFor()
    const size = await customer.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth }))
    assert(size.viewport === size.document, 'Mobile order screen has horizontal overflow')
    await customer.screenshot({ path: 'output/playwright/email-e2e-customer-completed.png', fullPage: true })
    assert((await inbox()).filter(item => item.subject.includes(order.orderNo)).length === 1, 'State transitions must not send another new-order email')
    steps.push('食客端自动轮询显示完成时间和再来一单；手机无横向溢出；状态流转没有重复新单邮件')

    // Visible delivery errors must persist and be safe, even when the SMTP service rejects authentication.
    await page.getByRole('button', { name: '关闭', exact: true }).click()
    await page.getByRole('link', { name: '系统设置', exact: true }).click()
    await password.fill('wrong-fixture-password')
    await save.click()
    await wait(() => test.isEnabled(), 'Failed to save the synthetic invalid credential')
    await test.click()
    await wait(async () => (await adminApi('/admin/email/deliveries'))[0].status === 'FAILED', 'Wrong SMTP credentials should fail')
    await page.getByRole('button', { name: '刷新记录', exact: true }).click()
    await wait(async () => (await page.getByRole('listitem').first().textContent()).includes('认证失败'), 'Authentication failure must remain visible')
    const failedText = await page.getByRole('listitem').first().textContent()
    assert(!failedText.includes('wrong-fixture-password') && !failedText.includes('fixture-private-response'), 'Delivery error leaked credentials or vendor response')
    await page.screenshot({ path: 'output/playwright/email-e2e-auth-failure.png', fullPage: true })
    steps.push('故意使用测试错误授权码，界面展示持续且脱敏的认证失败记录')

    await password.fill('fixture-app-password')
    await page.getByRole('checkbox', { name: '开启新订单邮件通知', exact: true }).uncheck()
    await save.click()
    await wait(() => test.isEnabled(), 'Failed to disable notifications')
    const disabledOrder = await placeOrder('E2E关闭通知后下单')
    assert(!(await adminApi('/admin/email/deliveries')).some(item => item.orderId === disabledOrder.id), 'Disabled notification created a delivery')
    assert((await inbox()).length === 2, 'Disabling notifications should not send another email')
    steps.push('通过界面关闭通知后再次下单成功，没有邮件任务或 SMTP 投递')
    assert(errors.length === 0, 'Browser JavaScript errors: ' + errors.join('; '))
    return { result: 'PASS', steps, orderId: order.id, orderNo: order.orderNo,
      orderStatus: 'COMPLETED', disabledOrderId: disabledOrder.id, smtpMessageCount: 2,
      recipientCount: mail.recipients.length, customerViewport: size, javascriptErrors: errors }
  } finally {
    await context.tracing.stop({ path: 'output/playwright/email-e2e-customer-trace.zip' })
    await context.close()
  }
}
