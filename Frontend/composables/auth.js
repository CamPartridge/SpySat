// composables/useAuth.js
import { ref } from 'vue';
import Session from "supertokens-web-js/recipe/session";

export function auth() {
  const logout = async () => {
    try {
      await Session.signOut();
      window.location.href = "/auth"; // Redirect after logout
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  return {
    logout
  };
}
