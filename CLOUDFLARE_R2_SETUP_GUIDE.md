# ☁️ Cloudflare R2 Setup Guide for FlowTest APK Storage & Distribution

## 🎯 Why Cloudflare R2 is the Perfect Solution for FlowTest

1. **Bypasses Cloud Run's 32 MB Request Limit**:
   - Google Cloud Run (and many API gateways) has a hard **32 MB request body limit** (`413 Request Entity Too Large`).
   - By uploading your APK directly to **Cloudflare R2**, the 35+ MB binary never needs to pass through the Cloud Run HTTP ingress.

2. **Zero Egress Fees ($0 Bandwidth Costs)**:
   - Unlike AWS S3 or Google Cloud Storage which charge $0.09–$0.12/GB whenever users download the app, **Cloudflare R2 has $0 egress fees**.
   - You can distribute millions of APK downloads per month for completely free (R2 includes 10 GB storage and 10 million read operations free every month).

3. **Global Edge CDN Performance**:
   - Cloudflare R2 serves files from over 300+ edge locations worldwide, giving fast download speeds across Nigeria, Africa, Europe, and globally.

---

## 🛠️ Step-by-Step 3-Minute Setup

### Step 1: Create an R2 Bucket in Cloudflare
1. Log in to the [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. In the left navigation sidebar, click **R2**.
3. Click **Create bucket**.
4. Name your bucket (e.g., `flowtest-apk`).
5. Choose **Default** location hint, then click **Create Bucket**.

### Step 2: Enable Public Access on the Bucket
To allow users and Android devices to download `FlowTest.apk`:
1. In your `flowtest-apk` bucket page, click on the **Settings** tab.
2. Scroll to the **Public Access** section.
3. Choose one of two options:
   - **Option A (Instant - Free r2.dev subdomain)**:
     - Click **Allow Access** under **R2.dev subdomain**.
     - You will get a public URL like: `https://pub-abcdef123456.r2.dev`
     - Your APK URL will be: `https://pub-abcdef123456.r2.dev/FlowTest.apk`
   - **Option B (Custom Domain - Recommended)**:
     - Click **Connect Domain** (e.g., `apk.flowtest2026.com` or `download.flowtest2026.com`).
     - Cloudflare will configure the DNS and SSL certificate automatically.
     - Your APK URL will be: `https://apk.flowtest2026.com/FlowTest.apk`

### Step 3: Create an R2 S3 API Token (for Automated Uploads)
1. On the main **R2** dashboard page, click **Manage R2 API Tokens** on the right side.
2. Click **Create API token**.
3. Set **Permissions** to **Object Read & Write**.
4. Specify your bucket `flowtest-apk` (or All buckets).
5. Click **Create API Token**.
6. Copy the following credentials:
   - **Account ID** (also shown on your R2 dashboard)
   - **Access Key ID**
   - **Secret Access Key**
   - **Endpoint**: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`

---

## 🚀 How to Upload Your APK to Cloudflare R2

### Method A: Automated via GitHub Actions (Recommended)
Your GitHub Actions workflow (`.github/workflows/deploy-cloudrun.yml`) has already been configured to automatically upload `FlowTest.apk` and `update-manifest.json` to R2 upon every push/build!

Just add these secrets to your GitHub Repository:
1. Go to your GitHub repo -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Add the following repository secrets:
   - `R2_ACCOUNT_ID`: Your Cloudflare Account ID
   - `R2_ACCESS_KEY_ID`: Your R2 S3 Access Key ID
   - `R2_SECRET_ACCESS_KEY`: Your R2 S3 Secret Access Key
   - `R2_BUCKET_NAME`: `flowtest-apk` (or your bucket name)
   - `R2_PUBLIC_URL`: `https://apk.flowtest2026.com/FlowTest.apk` (or your `pub-xxx.r2.dev` URL)

### Method B: Manual One-Click Upload from Cloudflare Web Dashboard
1. Go to your `flowtest-apk` bucket in Cloudflare.
2. Click **Upload** -> **Upload files**.
3. Drag and drop `FlowTest.apk` directly into the bucket.
4. Done! It is instantly live worldwide.

### Method C: Command-Line Upload via AWS CLI
Because R2 is 100% S3-compatible, you can upload using standard AWS CLI:

```bash
export AWS_ACCESS_KEY_ID="your_r2_access_key_id"
export AWS_SECRET_ACCESS_KEY="your_r2_secret_access_key"
export AWS_DEFAULT_REGION="auto"

aws s3 cp webapp/FlowTest.apk s3://flowtest-apk/FlowTest.apk \
  --endpoint-url https://<YOUR_ACCOUNT_ID>.r2.cloudflarestorage.com \
  --content-type "application/vnd.android.package-archive"
```

---

## 🔄 How the App & Backend Use Cloudflare R2

1. **`server.js` Integration**:
   - The backend checks for `process.env.R2_PUBLIC_URL` or `CLOUDFLARE_R2_URL`.
   - When a user visits `https://api.flowtest2026.com/download/apk` or `https://api.flowtest2026.com/download/FlowTest.apk`, the server automatically redirects (302) to your high-speed Cloudflare R2 CDN URL.
   - The `/api/version` endpoint returns the R2 URL as `downloadUrl` and `r2DownloadUrl`.

2. **In-App Auto Updater**:
   - The Android app queries `/api/version` upon launch.
   - When an update is detected, the app downloads directly from the Cloudflare R2 URL at maximum edge speed without burdening your Cloud Run instance.
