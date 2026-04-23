<script setup>
import { computed } from 'vue'
import { useUserStore } from '../../stores/user'
import ProfileHome from '../user/ProfileHome.vue'
import ProfileCare from '../care/ProfileCare.vue'

const userStore = useUserStore()
const isControlledMember = computed(() => userStore.member?.role === 2)
</script>

<template>
  <ProfileCare v-if="isControlledMember" />
  <ProfileHome v-else />
</template>
