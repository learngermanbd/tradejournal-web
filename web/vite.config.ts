import { readFileSync } from 'node:fs'
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

function marketingHome(): Plugin {
  return {
    name: 'journaledge-marketing-home',
    transformIndexHtml: {
      order: 'pre',
      handler(html, context) {
        if (context.filename.endsWith('index.html')) {
          return readFileSync(new URL('./marketing.html', import.meta.url), 'utf8')
        }
        return html
      },
    },
  }
}

export default defineConfig({
  plugins: [react(), marketingHome()],
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      input: {
        home: 'index.html',
        app: 'app.html',
        marketing: 'marketing.html',
        privacy: 'privacy.html',
        adsTerms: 'ads-terms.html',
      },
    },
  },
})
