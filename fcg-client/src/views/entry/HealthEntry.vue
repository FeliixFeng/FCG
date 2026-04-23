<script setup>
import { computed } from 'vue'
import { useUserStore } from '../../stores/user'
import HealthHome from '../user/HealthHome.vue'
import HealthCare from '../care/HealthCare.vue'

const userStore = useUserStore()
const isControlledMember = computed(() => userStore.member?.role === 2)
</script>

<template>
  <HealthCare v-if="isControlledMember" />
  <HealthHome v-else />
</template>
