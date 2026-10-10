/**
 * 生成 15 张暖色菜品占位 SVG → public/images/placeholder/{1..15}.svg
 * 数据驱动：颜色与图形在 PLACEHOLDERS 中维护，离线可用。
 * 运行：node scripts/gen-placeholders.mjs
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const BROWN = '#8D5A3B';
const OUT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../public/images/placeholder');

/** 碗装面食（通用） */
const bowlNoodles = (chopsticks = false) => `
  <path d="M212 202 Q300 292 388 202 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <path d="M232 198 Q258 180 286 198 T340 198 T372 196" fill="none" stroke="#E8A06C" stroke-width="7" stroke-linecap="round"/>
  <path d="M252 152 q8 -14 16 0 M292 144 q8 -14 16 0 M332 152 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>
  ${chopsticks ? `<line x1="330" y1="128" x2="392" y2="214" stroke="#C98A5B" stroke-width="7" stroke-linecap="round"/><line x1="348" y1="122" x2="404" y2="200" stroke="#C98A5B" stroke-width="7" stroke-linecap="round"/>` : ''}`;

/** 饭碗 */
const riceBowl = `
  <path d="M214 208 Q300 288 386 208 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="204" rx="88" ry="24" fill="#FFFDF8" stroke="${BROWN}" stroke-width="7"/>
  <path d="M262 196 q10 -10 20 0 M300 192 q10 -10 20 0 M282 206 q10 -10 20 0" fill="none" stroke="#E3C9A8" stroke-width="5" stroke-linecap="round"/>
  <path d="M262 150 q8 -14 16 0 M300 142 q8 -14 16 0 M338 150 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>`;

/** 可乐鸡翅（鸡腿） */
const drumstick = `
  <line x1="316" y1="180" x2="366" y2="138" stroke="${BROWN}" stroke-width="22" stroke-linecap="round"/>
  <line x1="316" y1="180" x2="366" y2="138" stroke="#FFF6E8" stroke-width="12" stroke-linecap="round"/>
  <ellipse cx="272" cy="222" rx="62" ry="50" fill="#E8985A" stroke="${BROWN}" stroke-width="8" transform="rotate(-28 272 222)"/>
  <path d="M246 208 q14 10 30 4" fill="none" stroke="#C97B46" stroke-width="6" stroke-linecap="round"/>`;

/** 番茄炒蛋 */
const tomatoEgg = `
  <circle cx="262" cy="218" r="48" fill="#FF7058" stroke="${BROWN}" stroke-width="7"/>
  <circle cx="340" cy="216" r="42" fill="#FFD166" stroke="${BROWN}" stroke-width="7"/>
  <path d="M250 176 q12 -16 26 -4 M330 180 q12 -12 24 -2" fill="none" stroke="#7CB342" stroke-width="7" stroke-linecap="round"/>`;

/** 麻婆豆腐 */
const tofu = `
  <rect x="246" y="162" width="108" height="106" rx="12" fill="#FFF7EA" stroke="${BROWN}" stroke-width="8"/>
  <path d="M246 198 h108 M284 162 v106 M320 162 v106" fill="none" stroke="${BROWN}" stroke-width="4" opacity="0.55"/>
  <circle cx="268" cy="222" r="7" fill="#E85D4A"/><circle cx="338" cy="240" r="7" fill="#E85D4A"/><circle cx="306" cy="250" r="6" fill="#5AA469"/>`;

/** 青椒肉丝 */
const shreds = `
  <line x1="236" y1="172" x2="360" y2="238" stroke="#7CB342" stroke-width="15" stroke-linecap="round"/>
  <line x1="244" y1="214" x2="352" y2="168" stroke="#E85D4A" stroke-width="13" stroke-linecap="round"/>
  <line x1="252" y1="250" x2="368" y2="206" stroke="#E8A93C" stroke-width="13" stroke-linecap="round"/>`;

/** 蒜蓉西兰花 */
const broccoli = `
  <path d="M286 214 L276 252 M312 210 L318 254 M299 226 L300 260" stroke="${BROWN}" stroke-width="8" stroke-linecap="round"/>
  <circle cx="276" cy="192" r="28" fill="#7CB342" stroke="${BROWN}" stroke-width="6"/>
  <circle cx="322" cy="184" r="28" fill="#66BB6A" stroke="${BROWN}" stroke-width="6"/>
  <circle cx="300" cy="218" r="28" fill="#81C784" stroke="${BROWN}" stroke-width="6"/>`;

/** 玉米排骨汤 */
const cornSoup = `
  <path d="M212 204 Q300 286 388 204 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="202" rx="88" ry="22" fill="#FFE9C7" stroke="${BROWN}" stroke-width="6"/>
  <circle cx="268" cy="200" r="9" fill="#FFD166" stroke="#E8A93C" stroke-width="3"/>
  <circle cx="292" cy="204" r="9" fill="#FFD166" stroke="#E8A93C" stroke-width="3"/>
  <circle cx="316" cy="200" r="9" fill="#FFD166" stroke="#E8A93C" stroke-width="3"/>
  <circle cx="340" cy="204" r="8" fill="#E8D3B8" stroke="#C9A980" stroke-width="3"/>
  <path d="M262 150 q8 -14 16 0 M300 142 q8 -14 16 0 M338 150 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>`;

/** 紫菜蛋花汤 */
const seaweedSoup = `
  <path d="M212 204 Q300 286 388 204 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="202" rx="88" ry="22" fill="#E8D3B8" stroke="${BROWN}" stroke-width="6"/>
  <path d="M252 202 q14 -12 28 0 t28 0 t28 0" fill="none" stroke="#FFD166" stroke-width="7" stroke-linecap="round"/>
  <path d="M262 196 q10 8 20 0 M312 208 q10 8 20 0" fill="none" stroke="#6D4C41" stroke-width="5" stroke-linecap="round"/>
  <path d="M262 150 q8 -14 16 0 M300 142 q8 -14 16 0 M338 150 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>`;

/** 双皮奶 */
const pudding = `
  <path d="M256 192 L344 192 L330 262 L270 262 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="192" rx="44" ry="12" fill="#FFF3E4" stroke="${BROWN}" stroke-width="6"/>
  <circle cx="300" cy="168" r="14" fill="#FF6B81" stroke="${BROWN}" stroke-width="5"/>
  <path d="M296 156 q-8 -10 4 -14 M304 156 q8 -10 -4 -14" fill="none" stroke="#7CB342" stroke-width="5" stroke-linecap="round"/>`;

/** 草莓奶冻（草莓） */
const strawberry = `
  <path d="M300 262 C248 226 248 184 282 184 C295 184 300 196 300 200 C300 196 305 184 318 184 C352 184 352 226 300 262 Z" fill="#FF6B81" stroke="${BROWN}" stroke-width="7" stroke-linejoin="round"/>
  <path d="M278 182 q22 -16 44 0 l-8 10 q-14 -8 -28 0 Z" fill="#7CB342" stroke="${BROWN}" stroke-width="5" stroke-linejoin="round"/>
  <ellipse cx="283" cy="214" rx="3" ry="5" fill="#FFE9A8"/><ellipse cx="317" cy="214" rx="3" ry="5" fill="#FFE9A8"/><ellipse cx="300" cy="236" rx="3" ry="5" fill="#FFE9A8"/><ellipse cx="300" cy="206" rx="3" ry="5" fill="#FFE9A8"/>`;

/** 蜂蜜柚子茶（杯） */
const cup = `
  <path d="M262 166 L338 166 L330 266 L270 266 Z" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="168" rx="38" ry="10" fill="#FFD54F" stroke="${BROWN}" stroke-width="5"/>
  <line x1="322" y1="166" x2="348" y2="128" stroke="#E8A93C" stroke-width="8" stroke-linecap="round"/>
  <circle cx="286" cy="214" r="6" fill="#FFB74D" opacity="0.8"/><circle cx="312" cy="232" r="6" fill="#FFB74D" opacity="0.8"/>`;

/** 热可可（马克杯） */
const mug = `
  <rect x="248" y="178" width="84" height="84" rx="12" fill="#FFFFFF" stroke="${BROWN}" stroke-width="8"/>
  <path d="M332 198 q36 2 36 24 q0 22 -36 24" fill="none" stroke="${BROWN}" stroke-width="8"/>
  <ellipse cx="290" cy="182" rx="38" ry="10" fill="#8D5A3B"/>
  <path d="M272 152 q8 -14 16 0 M300 144 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>`;

/** 芝士泡面（小锅） */
const potNoodles = `
  <path d="M238 198 L362 198 L346 272 L254 272 Z" fill="#FFB74D" stroke="${BROWN}" stroke-width="8" stroke-linejoin="round"/>
  <ellipse cx="300" cy="198" rx="62" ry="18" fill="#FFF3D6" stroke="${BROWN}" stroke-width="7"/>
  <path d="M258 196 q12 -10 24 0 t24 0 t24 0" fill="none" stroke="#E8A06C" stroke-width="6" stroke-linecap="round"/>
  <path d="M286 178 L300 160 L314 178 Z M306 182 L320 164 L334 182 Z" fill="#FFD54F" stroke="${BROWN}" stroke-width="4" stroke-linejoin="round"/>
  <path d="M262 150 q8 -14 16 0 M300 142 q8 -14 16 0 M338 150 q8 -14 16 0" fill="none" stroke="#B07850" stroke-width="6" stroke-linecap="round"/>`;

const PLACEHOLDERS = [
  { c1: '#FFD0A8', c2: '#FF8A65', glyph: bowlNoodles(true) },
  { c1: '#FFE3B3', c2: '#FFB74D', glyph: riceBowl },
  { c1: '#FFF0D9', c2: '#FFCC80', glyph: bowlNoodles(false) },
  { c1: '#FFD6C2', c2: '#FF8A65', glyph: drumstick },
  { c1: '#FFE0B0', c2: '#FF7043', glyph: tomatoEgg },
  { c1: '#FFEDD8', c2: '#FFAB91', glyph: tofu },
  { c1: '#EAF5DC', c2: '#AED581', glyph: shreds },
  { c1: '#E4F2DD', c2: '#81C784', glyph: broccoli },
  { c1: '#FFE9CC', c2: '#FFB366', glyph: cornSoup },
  { c1: '#FFF3E0', c2: '#FFCC80', glyph: seaweedSoup },
  { c1: '#FFE4EC', c2: '#FF8FA3', glyph: pudding },
  { c1: '#FFD9E2', c2: '#FF6B81', glyph: strawberry },
  { c1: '#FFF1C9', c2: '#FFD54F', glyph: cup },
  { c1: '#FCE4D6', c2: '#D7A98C', glyph: mug },
  { c1: '#FFE0B8', c2: '#FF9E57', glyph: potNoodles },
];

const wrap = (c1, c2, glyph) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 400" width="600" height="400">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${c1}"/>
      <stop offset="1" stop-color="${c2}"/>
    </linearGradient>
  </defs>
  <rect width="600" height="400" rx="28" fill="url(#bg)"/>
  <circle cx="92" cy="82" r="60" fill="#ffffff" opacity="0.18"/>
  <circle cx="520" cy="332" r="82" fill="#ffffff" opacity="0.14"/>
  <circle cx="478" cy="72" r="26" fill="#ffffff" opacity="0.20"/>
  <circle cx="300" cy="206" r="122" fill="#FFF9F4" opacity="0.92"/>
  ${glyph}
</svg>
`;

mkdirSync(OUT_DIR, { recursive: true });
PLACEHOLDERS.forEach((item, index) => {
  const file = resolve(OUT_DIR, `${index + 1}.svg`);
  writeFileSync(file, wrap(item.c1, item.c2, item.glyph), 'utf8');
  console.log('generated', file);
});
console.log(`done: ${PLACEHOLDERS.length} placeholders -> ${OUT_DIR}`);
