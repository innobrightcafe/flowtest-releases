# FlowTest Web Landing Page & APK Distribution Webapp

This directory contains the official web landing page and direct browser download portal for **FlowTest** (`flowtest2026.com` and `api.flowtest2026.com`).

## Features
- **UI UX Pro Max Design**: Built with Tailwind CSS, Lucide icons, dark mode, smooth glow effects, and responsive layout.
- **Direct APK Distribution**: Serves `/download/apk` with streaming downloads and custom package headers.
- **Mobile QR Code Scan**: Instant QR code allowing desktop visitors to scan with their phone camera to download directly.
- **Google Play Protect Guidance**: Interactive step-by-step illustrations showing users how to tap **"More details" (⌵)** -> **"Install anyway"** to bypass Google's direct-sideload alert.
- **BYOD Bulk SMS SaaS Showcase**: Explains the 100% DND bypass architecture (@ ₦7.50 / SMS & ₦5,000/mo BYOD Gateway).
- **Dual-Mode Hosting**:
  1. **Integrated Mode**: Served automatically by `server.js` on Google Cloud Run (`flowtest2026.com` / `api.flowtest2026.com`).
  2. **Standalone Mode**: Can be deployed independently to Vercel, Netlify, Firebase Hosting, Cloudflare Pages, or GetMeHost cPanel `public_html`.

## File Structure
- `index.html` - Complete marketing & features landing page with download links and Play Protect guide.
- `download.html` - Dedicated download page that auto-initiates the APK download with fallback buttons.
- `robots.txt` & `sitemap.xml` - SEO configuration for search indexing.
