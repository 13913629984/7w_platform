import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Vite 6 + http-proxy can choke on `Connection: close` from upstream;
        // rewriting it avoids "Data after Connection: close" parse errors.
        headers: { Connection: 'keep-alive' },
      },
    },
  },
})
