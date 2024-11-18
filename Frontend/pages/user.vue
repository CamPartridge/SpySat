<template>
        <NavBar>
      <!-- Left Slot -->
      <template #left>
        Home
      </template>
      
      <!-- Center Slot -->
      <template #center>
        <ul class="nav-items">
        </ul>
      </template>
      
      <!-- Right Slot -->
      <template #right>
        <button @click="logout">Logout</button>
      </template>
    </NavBar>
    <div>
      <h1>User</h1>
    </div>
    <div>

    </div>
  </template>
  
  <script>
import { auth } from '@/composables/auth'; // Adjust path if necessary
import NavBar from '@@/component/NavBar.vue';

export default {
  name: "UserPage",
  components: { NavBar },
  setup() {
    const { logout } = auth(); // Use the composable

    return {
      logout
    };
  }
};
  </script>
  