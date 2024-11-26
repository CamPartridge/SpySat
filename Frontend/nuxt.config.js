// nuxt.config.js
export default {
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true },

  plugins: [
    '~/plugins/supertokens.client.js',
    { src: '~/plugins/three.js', mode: 'client' },
    '~/plugins/customDropdown.js'
  ],

  modules: [
  '@tresjs/nuxt', '@nuxt/ui'],

}