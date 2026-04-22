<script setup>
import BaseLayout from '../../components/common/BaseLayout.vue'
</script>

<template>
  <BaseLayout>
    <div class="care-page">
      <section class="care-card">
        <p class="care-badge">关怀模式 · 健康</p>
        <h2 class="care-title">快捷录入（占位）</h2>
        <p class="care-desc">这里将保留血压、血糖、体重三项快速录入，去掉复杂图表和周报区域。</p>
      </section>

      <section class="care-grid">
        <article class="quick-card">血压录入</article>
        <article class="quick-card">血糖录入</article>
        <article class="quick-card">体重录入</article>
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

.care-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  min-height: 88px;
  border-radius: 14px;
  border: 1px dashed rgba(45, 95, 93, 0.3);
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: #2d5f5d;
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

.care-desc {
  margin: 0;
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

  .care-grid {
    grid-template-columns: 1fr;
  }
}
</style>

