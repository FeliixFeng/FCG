<template>
  <div class="app-transition-root">
    <router-view v-slot="{ Component, route }">
      <transition :name="route.meta.transition || 'page-fade'">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>
</template>

<style>
.app-transition-root {
  position: relative;
  min-height: 100vh;
  /* 不设 overflow-x:hidden —— 否则会触发 BFC 把滚动条从 html 抢走 */
}

/* 进出页面都 absolute 叠放，旧页面消失时不塌陷文档高度 */
.page-fade-enter-active,
.page-fade-leave-active {
  position: absolute;
  inset: 0;
  width: 100%;
}

.page-fade-enter-active {
  transition: opacity 0.2s ease;
  z-index: 1;
}
.page-fade-leave-active {
  transition: opacity 0.15s ease;
  z-index: 0;
}

/* 纯淡入淡出，无位移 */
.page-fade-enter-from { opacity: 0; }
.page-fade-enter-to   { opacity: 1; }
.page-fade-leave-from { opacity: 1; }
.page-fade-leave-to   { opacity: 0; }
</style>
