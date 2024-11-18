<!-- NavBar.vue -->
<template>
    <nav :style="{ height: '4.5vw', backgroundColor: 'var(--primary-50)', color: 'var(--primary-100)' }" class="navbar">
      <div class="nav-left">
        <slot name="left"></slot>
      </div>
      
      <div class="nav-center">
        <slot name="center"></slot>
      </div>
      
      <div class="nav-right">
        <slot name="right"></slot>
      </div>
    </nav>
  </template>

<script setup>
  import {text} from './State.js'
  import { defineProps } from 'vue';

  const {message} = defineProps(["message"])
</script>
  
  <style scoped>
  .navbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 1rem;
  }
  
  .nav-left {
    /* Ensure the left section is aligned to the left */
    flex: 0 1 auto;
  }
  
  .nav-center {
    /* Ensures the center section aligns after the left section */
    flex: 0 1 auto;
    margin-left: 1rem; /* Add some space between left and center */
  }
  
  .nav-right {
    /* Ensure the right section is aligned to the right */
    flex: 0 1 auto;
    margin-left: auto; /* Pushes the right section to the far right */
  }
  </style>
  