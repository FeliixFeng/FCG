<script setup>
/**
 * AvatarIcon — 根据 relation / role 渲染有辨识性的默认头像
 * 若有真实 avatar URL，直接显示图片
 */
const props = defineProps({
  avatar: { type: String, default: '' },
  relation: { type: String, default: '' },
  role: { type: Number, default: 1 },
  nickname: { type: String, default: '' },
  size: { type: Number, default: 64 },
})

// ── 关键词 → 配置表 ──────────────────────────────────────
const RELATION_MAP = [
  { keys: ['爷爷', '外公', '祖父', '姥爷'], type: 'elder-male',   color: '#4a6fa5', bg: '#e8eef6' },
  { keys: ['奶奶', '外婆', '祖母', '姥姥'], type: 'elder-female', color: '#7c5cbf', bg: '#efe9f8' },
  { keys: ['爸爸', '父亲', '爸',  '父'],   type: 'adult-male',   color: '#2d5f5d', bg: '#e4f0ef' },
  { keys: ['妈妈', '母亲', '妈',  '母'],   type: 'adult-female', color: '#c45c7a', bg: '#f7e8ee' },
  { keys: ['儿子', '哥哥', '弟弟'],        type: 'child-male',   color: '#e07b39', bg: '#faeee4' },
  { keys: ['女儿', '姐姐', '妹妹'],        type: 'child-female', color: '#c47a3a', bg: '#f7eddf' },
  { keys: ['孩子', '小孩', '宝宝'],        type: 'child-male',   color: '#e07b39', bg: '#faeee4' },
  { keys: ['自己', '本人', '我'],          type: 'adult-male',   color: '#3a7abf', bg: '#e5eff8' },
]

const ROLE_FALLBACK = {
  0: { type: 'admin',  color: '#2d5f5d', bg: '#e4f0ef' },  // 管理员
  1: { type: 'member', color: '#5f7d8a', bg: '#e9eff2' },  // 普通成员
  2: { type: 'elder-male', color: '#d4785a', bg: '#f7ece6' }, // 关怀成员
}

function resolveConfig(relation, role) {
  if (relation) {
    for (const entry of RELATION_MAP) {
      if (entry.keys.some(k => relation.includes(k))) return entry
    }
  }
  return ROLE_FALLBACK[role] ?? ROLE_FALLBACK[1]
}

// ── SVG 路径库 ────────────────────────────────────────────
// 每种类型返回 SVG <path> + 可选装饰，坐标基于 100×100 画布
function getPaths(type) {
  switch (type) {
    case 'elder-male':
      return `
        <!-- 头发（稀疏） -->
        <ellipse cx="50" cy="30" rx="18" ry="16" fill="currentColor" opacity="0.15"/>
        <!-- 头部 -->
        <ellipse cx="50" cy="33" rx="16" ry="17" fill="currentColor" opacity="0.9"/>
        <!-- 皱纹暗示 -->
        <path d="M40 33 Q50 31 60 33" stroke="currentColor" stroke-width="1.2" fill="none" opacity="0.3"/>
        <!-- 身体 -->
        <path d="M28 78 Q30 58 50 56 Q70 58 72 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 拐杖 -->
        <line x1="68" y1="78" x2="64" y2="58" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
        <ellipse cx="64" cy="57" rx="3" ry="2" fill="currentColor" opacity="0.4"/>
      `
    case 'elder-female':
      return `
        <!-- 头部 -->
        <ellipse cx="50" cy="33" rx="16" ry="17" fill="currentColor" opacity="0.9"/>
        <!-- 白发 -->
        <ellipse cx="50" cy="22" rx="14" ry="6" fill="white" opacity="0.5"/>
        <!-- 皱纹 -->
        <path d="M41 33 Q50 31 59 33" stroke="currentColor" stroke-width="1.2" fill="none" opacity="0.3"/>
        <!-- 身体（裙形） -->
        <path d="M31 78 Q34 56 50 54 Q66 56 69 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 领口装饰 -->
        <ellipse cx="50" cy="56" rx="8" ry="3" fill="white" opacity="0.3"/>
      `
    case 'adult-male':
      return `
        <!-- 头部 -->
        <ellipse cx="50" cy="32" rx="16" ry="18" fill="currentColor" opacity="0.9"/>
        <!-- 头发 -->
        <path d="M34 28 Q36 16 50 15 Q64 16 66 28 Q62 20 50 20 Q38 20 34 28Z" fill="currentColor" opacity="0.5"/>
        <!-- 身体（宽肩） -->
        <path d="M26 78 Q28 55 50 52 Q72 55 74 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 领带/领口 -->
        <path d="M47 52 L50 64 L53 52" fill="white" opacity="0.25"/>
      `
    case 'adult-female':
      return `
        <!-- 头部 -->
        <ellipse cx="50" cy="32" rx="15" ry="18" fill="currentColor" opacity="0.9"/>
        <!-- 长发 -->
        <path d="M35 30 Q32 48 36 60" stroke="currentColor" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.45"/>
        <path d="M65 30 Q68 48 64 60" stroke="currentColor" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.45"/>
        <!-- 身体 -->
        <path d="M30 78 Q34 56 50 53 Q66 56 70 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 领口 -->
        <ellipse cx="50" cy="55" rx="7" ry="3" fill="white" opacity="0.25"/>
      `
    case 'child-male':
      return `
        <!-- 头部（大） -->
        <ellipse cx="50" cy="35" rx="18" ry="19" fill="currentColor" opacity="0.9"/>
        <!-- 发型 -->
        <path d="M32 30 Q34 18 50 17 Q66 18 68 30 Q62 22 50 22 Q38 22 32 30Z" fill="currentColor" opacity="0.5"/>
        <!-- 身体（小） -->
        <path d="M32 78 Q34 58 50 56 Q66 58 68 78 Z" fill="currentColor" opacity="0.75"/>
      `
    case 'child-female':
      return `
        <!-- 头部（大） -->
        <ellipse cx="50" cy="35" rx="17" ry="19" fill="currentColor" opacity="0.9"/>
        <!-- 双马尾 -->
        <ellipse cx="30" cy="28" rx="7" ry="5" fill="currentColor" opacity="0.5"/>
        <ellipse cx="70" cy="28" rx="7" ry="5" fill="currentColor" opacity="0.5"/>
        <!-- 身体 -->
        <path d="M31 78 Q34 58 50 56 Q66 58 69 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 裙子 -->
        <path d="M33 70 Q50 66 67 70 L70 78 Q50 74 30 78 Z" fill="white" opacity="0.2"/>
      `
    case 'admin':
      return `
        <!-- 头部 -->
        <ellipse cx="50" cy="32" rx="16" ry="18" fill="currentColor" opacity="0.9"/>
        <!-- 发型 -->
        <path d="M34 28 Q36 16 50 15 Q64 16 66 28 Q62 20 50 20 Q38 20 34 28Z" fill="currentColor" opacity="0.5"/>
        <!-- 身体 -->
        <path d="M26 78 Q28 55 50 52 Q72 55 74 78 Z" fill="currentColor" opacity="0.75"/>
        <!-- 领章 -->
        <polygon points="50,52 53,59 60,59 54,63 57,71 50,66 43,71 46,63 40,59 47,59" fill="white" opacity="0.35"/>
      `
    default: // member
      return `
        <!-- 头部 -->
        <ellipse cx="50" cy="32" rx="16" ry="18" fill="currentColor" opacity="0.9"/>
        <!-- 发型 -->
        <path d="M34 28 Q36 16 50 15 Q64 16 66 28 Q62 20 50 20 Q38 20 34 28Z" fill="currentColor" opacity="0.5"/>
        <!-- 身体 -->
        <path d="M28 78 Q30 56 50 53 Q70 56 72 78 Z" fill="currentColor" opacity="0.75"/>
      `
  }
}

const cfg = resolveConfig(props.relation, props.role)
</script>

<template>
  <!-- 有真实头像：显示图片 -->
  <div
    v-if="avatar"
    class="avatar-wrap"
    :style="{ width: size + 'px', height: size + 'px' }"
  >
    <img :src="avatar" :alt="nickname" class="avatar-img" />
  </div>

  <!-- 无头像：渲染 SVG 默认头像 -->
  <div
    v-else
    class="avatar-wrap"
    :style="{
      width: size + 'px',
      height: size + 'px',
      background: cfg.bg,
    }"
  >
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 100 100"
      :style="{ color: cfg.color }"
      xmlns="http://www.w3.org/2000/svg"
      v-html="getPaths(cfg.type)"
    />
  </div>
</template>

<style scoped>
.avatar-wrap {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
