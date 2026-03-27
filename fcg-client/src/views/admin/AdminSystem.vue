<template>
  <div class="admin-placeholder">
    <div class="page-header">
      <h2>系统设置</h2>
      <p class="page-desc">配置家庭基本信息、通知设置、数据导入导出等系统功能。</p>
    </div>
    <div class="placeholder-content">
      <div class="placeholder-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="3"/>
          <path d="M12 1v6m0 6v6m5.2-13.2l-4.2 4.2m-2 2l-4.2 4.2m13.2-5.2l-6 0m-6 0l-6 0m13.2 5.2l-4.2-4.2m-2-2l-4.2-4.2"/>
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
