<!-- <script setup>
import { onMounted } from 'vue';
import SuperTokens from 'supertokens-web-js';
import Session from 'supertokens-web-js/recipe/session';
import EmailPassword from 'supertokens-web-js/recipe/emailpassword';

// Use onMounted to ensure this runs only in the client
onMounted(() => {
    SuperTokens.init({
        appInfo: {
            apiDomain: "http://localhost:3001",
            apiBasePath: "/auth",
            appName: "SpySat",
        },
        recipeList: [
            Session.init(),
            EmailPassword.init(),
        ],
    });
});
</script>

<template>
  <div>
    Your component content
  </div>
</template> -->

<template>
  <div>
    <HeaderComponent />
    <NuxtPage />
    <FooterComponent />
  </div>
</template>

<script>

</script>

<style>
/* Global styles can go here */
</style>