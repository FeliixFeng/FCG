<script setup>
import BaseLayout from '../components/common/BaseLayout.vue'
</script>

<template>
  <div class="ai-page">
    <BaseLayout>
      <div class="page-content">
        <h1>AI 助手</h1>
        <p>AI 对话功能正在开发中，敬请期待...</p>
      </div>
    </BaseLayout>
  </div>
</template>

<style scoped>
.ai-page {
  min-height: 100vh;
  background: linear-gradient(145deg, #eef5f4 0%, #f0ebe0 45%, #e8f2f0 100%);
}

.page-content {
  padding: 24px;
  text-align: center;
}

.page-content h1 {
  font-size: 1.5rem;
  color: #2d5f5d;
  margin-bottom: 12px;
}

.page-content p {
  color: #666;
  font-size: 1rem;
}
</style>