/**
 * 浏览器标题闪烁：新订单到来时在标题前循环提示，直到调用 stop。
 */
let timer: number | undefined
let originalTitle = ''
let toggle = false

/**
 * 开始标题闪烁。
 *
 * @param text 闪烁文案，默认“🔔 有新订单”
 */
export function startTitleFlash(text = '🔔 有新订单'): void {
  if (timer) return
  originalTitle = document.title
  timer = window.setInterval(() => {
    toggle = !toggle
    document.title = toggle ? text : originalTitle
  }, 800)
}

/** 停止闪烁并恢复原标题 */
export function stopTitleFlash(): void {
  if (timer) {
    window.clearInterval(timer)
    timer = undefined
  }
  if (originalTitle) {
    document.title = originalTitle
  }
  toggle = false
}
