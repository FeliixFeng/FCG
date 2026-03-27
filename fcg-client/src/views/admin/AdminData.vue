<template>
  <div class="admin-placeholder">
    <div class="page-header">
      <h2>数据统计</h2>
      <p class="page-desc">查看家庭健康数据趋势、成员用药依从率等统计分析图表。</p>
    </div>
    <div class="placeholder-content">
      <div class="placeholder-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <line x1="18" y1="20" x2="18" y2="10"/>
          <line x1="12" y1="20" x2="12" y2="4"/>
          <line x1="6" y1="20" x2="6" y2="14"/>
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
