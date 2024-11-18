// nuxt.config.js
export default {
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true },

  plugins: [
    '~/plugins/supertokens.client.js',
    { src: '~/plugins/three.js', mode: 'client' }
  ],

  modules: [
  '@tresjs/nuxt', '@nuxt/ui'],

}