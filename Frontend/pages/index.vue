<template>
  <div>
    <h1>Welcome to SpySat</h1>
  </div>
  <div>
    <button @click="logout">
      Logout
    </button>
  </div>
</template>

<script>
import Session from "supertokens-web-js/recipe/session";

export default {
  name: 'HomePage',
  methods: {
    async logout() {
      try {
        await Session.signOut();
        window.location.href = "/auth"; // redirect after logout
      } catch (error) {
        console.error("Logout failed:", error);
      }
    }
  }
};
</script>
