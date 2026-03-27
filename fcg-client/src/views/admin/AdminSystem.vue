<script setup>
</script>

<template>
  <div class="admin-placeholder">
    <div class="placeholder-icon">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="12" cy="12" r="3"/>
        <path d="M12 1v6m0 6v6m5.2-13.2l-4.2 4.2m-2 2l-4.2 4.2m13.2-5.2l-6 0m-6 0l-6 0m13.2 5.2l-4.2-4.2m-2-2l-4.2-4.2"/>
      </svg>
    </div>
    <h2>系统设置</h2>
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
