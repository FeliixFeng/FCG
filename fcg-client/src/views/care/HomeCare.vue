<script setup>
import BaseLayout from '../../components/common/BaseLayout.vue'
</script>

<template>
  <BaseLayout>
    <div class="care-page">
      <section class="care-card">
        <p class="care-badge">关怀模式 · 首页</p>
        <h2 class="care-title">今日打卡（占位）</h2>
        <p class="care-desc">这里将展示下一条待打卡任务，支持打卡、跳过、上一条/下一条。</p>
      </section>

      <section class="care-card">
        <h3 class="care-subtitle">任务信息占位</h3>
        <p class="care-text">药品名称：阿司匹林</p>
        <p class="care-text">时间段：早间 06:00-10:00</p>
        <p class="care-text">剂量：每次 1 片</p>
      </section>
    </div>
  </BaseLayout>
</template>

<style scoped>
.care-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  gap: 16px;
}

.care-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(45, 95, 93, 0.12);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(45, 95, 93, 0.08);
}

.care-badge {
  display: inline-block;
  font-size: 0.9rem;
  color: #2d5f5d;
  background: rgba(45, 95, 93, 0.1);
  border-radius: 999px;
  padding: 4px 10px;
  margin: 0 0 10px;
}

.care-title {
  margin: 0 0 8px;
  font-size: 1.55rem;
  color: #1f3f3e;
}

.care-subtitle {
  margin: 0 0 10px;
  font-size: 1.2rem;
  color: #1f3f3e;
}

.care-desc,
.care-text {
  margin: 0 0 8px;
  font-size: 1.05rem;
  line-height: 1.7;
  color: #385e5d;
}

@media (max-width: 768px) {
  .care-page {
    padding: 14px;
  }

  .care-card {
    padding: 16px;
  }

  .care-title {
    font-size: 1.35rem;
  }
}
</style>

