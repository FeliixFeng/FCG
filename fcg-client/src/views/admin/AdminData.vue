<script setup>
</script>

<template>
  <div class="admin-placeholder">
    <div class="placeholder-icon">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <line x1="18" y1="20" x2="18" y2="10"/>
        <line x1="12" y1="20" x2="12" y2="4"/>
        <line x1="6" y1="20" x2="6" y2="14"/>
      </svg>
    </div>
    <h2>数据统计</h2>
    <p>该功能正在筹备中，敬请期待...</p>
  </div>
</template>

<style scoped>
.admin-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  color: #666;
}

.placeholder-icon {
  color: #ddd;
  margin-bottom: 20px;
}

.admin-placeholder h2 {
  margin: 0 0 8px;
  font-size: 1.25rem;
  color: #2d5f5d;
}

.admin-placeholder p {
  margin: 0;
  font-size: 0.9rem;
}
</style>
