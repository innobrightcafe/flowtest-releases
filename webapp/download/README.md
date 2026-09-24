# FlowTest APK Download Directory

This folder contains the compiled Android Application Packages (APKs) for **FlowTest** and the direct download landing page.

## Contents
- **`FlowTest.apk`** — Latest production build (Version 1.3.4, Build 8) with WireGuard VPN engine, VTU vending, and dual-auth SMS gateway.
- **`app-debug.apk`** — Signed debug binary for sideloading and testing.
- **`app-release.apk`** — Production release binary mirror.
- **`index.html`** — Standalone browser download page with 1-second auto-redirect, download retry buttons, and Google Play Protect bypass instructions.

## Static Hosting Compatibility
If hosting `webapp/` statically (GitHub Pages, Cloudflare Pages, Netlify, Apache, Nginx, or cPanel):
- Users visiting `/download/` or `/download/index.html` can download `FlowTest.apk` directly from this directory.
- The download link resolves to `./FlowTest.apk`, ensuring direct file downloads without requiring an active Node.js backend.
