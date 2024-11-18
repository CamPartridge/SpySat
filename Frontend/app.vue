<script>

</script>

<template>
  <div class="app-container">
    <HeaderComponent />
    <NavBar id="navBar" >
      <slot name="navContent"></slot>
    </NavBar>
    <NuxtPage />
    <UNotifications />
    <FooterComponent />
  </div>
</template>


<style>
/* Global styles can go here */
:root {
  --primary-50: #011724;
  --primary-100: #062c43; /* Main primary color */
  --primary-200: #054569;
  --primary-300: #5591a9;
  --primary-400: #9ccddc;
  --primary-500: #ced7e0;
  --interaction: #3df4fa; /* Interaction color */
  
  --highlight-satelliteGreen: #7ed957;
  --highlight-rocketOrange: #ff914d;
  --highlight-debrisRed: #ff3131;
  --highlight-unknownYellow: #ffde59;
}

.app-container {
  background-color: var(--primary-100); /* Set to your desired color */
  min-height: 100vh; /* Ensure the background covers the full height */
  display: flex;
  flex-direction: column;
}

/* Optional: Set body styles */
body {
  margin: 0; /* Remove default margin */
}
</style>