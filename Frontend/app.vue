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
/* @import url('https://fonts.googleapis.com/css2?family=Teko:wght@300..700&display=swap'); */
@import url('https://fonts.googleapis.com/css2?family=Gemunu+Libre:wght@200..800&family=Teko:wght@300..700&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Gemunu+Libre:wght@200..800&family=Saira+Condensed:wght@100;200;300;400;500;600;700;800;900&family=Teko:wght@300..700&display=swap');
/* Global styles can go here */
:root {
  --primary-50: #00131d;
  --primary-100: #001927; /* Main primary color */
  --primary-200: #054569;
  --primary-300: #5591a9;
  --primary-400: #9ccddc;
  --primary-500: #ced7e0;
  --interaction: #00dfff; /* Interaction color */
  
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
  margin: 0;
  font-family: "Saira Condensed", sans-serif;
  font-weight: 400;
  font-style: normal;
}



</style>