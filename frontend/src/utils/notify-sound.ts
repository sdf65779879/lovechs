/**
 * Web Audio 提示音：无需音频文件，合成两声清脆“叮咚”。
 */
let audioCtx: AudioContext | null = null

function getContext(): AudioContext | null {
  if (typeof window === 'undefined') return null
  const Ctor =
    window.AudioContext ||
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!Ctor) return null
  if (!audioCtx) {
    audioCtx = new Ctor()
  }
  return audioCtx
}

/**
 * 播放一个音符。
 *
 * @param ctx 音频上下文
 * @param frequency 频率（Hz）
 * @param startAt 开始时间（秒，ctx 时间轴）
 * @param duration 时长（秒）
 */
function tone(ctx: AudioContext, frequency: number, startAt: number, duration: number) {
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = 'sine'
  osc.frequency.value = frequency
  // 简单包络，避免爆音
  gain.gain.setValueAtTime(0.0001, startAt)
  gain.gain.exponentialRampToValueAtTime(0.25, startAt + 0.02)
  gain.gain.exponentialRampToValueAtTime(0.0001, startAt + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(startAt)
  osc.stop(startAt + duration + 0.05)
}

/** 播放“叮咚”提示音；在无用户手势时浏览器可能挂起，会尝试 resume */
export async function playDing(): Promise<void> {
  const ctx = getContext()
  if (!ctx) return
  if (ctx.state === 'suspended') {
    try {
      await ctx.resume()
    } catch {
      // 忽略：部分浏览器需用户手势后才能出声
    }
  }
  const now = ctx.currentTime
  tone(ctx, 880, now, 0.18)
  tone(ctx, 660, now + 0.16, 0.28)
}
