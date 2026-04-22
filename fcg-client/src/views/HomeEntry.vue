<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import Home from './Home.vue'
import HomeCare from './care/HomeCare.vue'

const userStore = useUserStore()
const isControlledMember = computed(() => userStore.member?.role === 2)
</script>

<template>
  <HomeCare v-if="isControlledMember" />
  <Home v-else />
</template>

