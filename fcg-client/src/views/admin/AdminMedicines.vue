<script setup>
</script>

<template>
  <div class="admin-placeholder">
    <div class="page-header">
      <h2>药品管理</h2>
      <p class="page-desc">批量管理家庭药品库，查看所有成员的用药计划，清理过期药品。</p>
    </div>
    <div class="placeholder-content">
      <div class="placeholder-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
        </svg>
      </div>
      <p>该功能正在筹备中，敬请期待...</p>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 1.25rem;
  color: #2d5f5d;
}

.page-desc {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.placeholder-content {
  background: white;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(45, 95, 93, 0.08);
}

.placeholder-icon {
  color: #ddd;
  margin-bottom: 20px;
}

.placeholder-content p {
  margin: 0;
  font-size: 0.9rem;
  color: #999;
}
</style>
