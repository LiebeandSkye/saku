import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vite'

const apkRedirectPlugin = (): Plugin => ({
  name: 'apk-redirect',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      if (req.url === '/Saku.apk') {
        res.writeHead(302, {
          Location: 'https://github.com/LiebeandSkye/saku/releases/latest/download/Saku.apk',
        })
        res.end()
        return
      }
      next()
    })
  },
  configurePreviewServer(server) {
    server.middlewares.use((req, res, next) => {
      if (req.url === '/Saku.apk') {
        res.writeHead(302, {
          Location: 'https://github.com/LiebeandSkye/saku/releases/latest/download/Saku.apk',
        })
        res.end()
        return
      }
      next()
    })
  },
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), apkRedirectPlugin()],
})
