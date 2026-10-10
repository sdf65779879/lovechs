/**
 * 轻量全局提示（不依赖组件库）：在页面顶部居中弹出，自动消失。
 */
let container: HTMLDivElement | null = null

function ensureContainer(): HTMLDivElement {
  if (!container || !document.body.contains(container)) {
    container = document.createElement('div')
    container.className =
      'fixed left-1/2 top-14 z-[9999] flex w-full max-w-sm -translate-x-1/2 flex-col items-center gap-2 px-6 pt-safe'
    document.body.appendChild(container)
  }
  return container
}

/**
 * 弹出一条文字提示。
 *
 * @param message 提示文案
 * @param duration 展示时长（毫秒），默认 2200
 */
export function toast(message: string, duration = 2200): void {
  if (!message) return
  const el = document.createElement('div')
  el.className =
    'animate-pop-in max-w-full break-words rounded-pill bg-cocoa-900/90 px-5 py-2.5 text-center text-sm leading-snug text-white shadow-lg'
  el.textContent = message
  ensureContainer().appendChild(el)
  window.setTimeout(() => {
    el.remove()
  }, duration)
}
