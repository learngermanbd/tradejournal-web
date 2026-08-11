import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      input: {
        home: 'index.html',
        app: 'app.html',
        marketing: 'marketing.html',
        preview: 'preview-all-pages.html',
      },
    },
  },
})
