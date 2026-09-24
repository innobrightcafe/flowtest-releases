# 🚀 Continuous Deployment to Google Cloud Run via GitHub Actions

This repository is pre-configured with a **GitHub Actions CI/CD Pipeline** (`.github/workflows/deploy-cloudrun.yml`) and a **Cloud Run Container** (`Dockerfile` + `server.js`) that automatically builds and deploys both the **Backend Webhook & Secrets Gateway** and the **Latest App Updates (APK)** whenever code is pushed to `main` or `master`.

---

## ⚡ How It Works

1. **Push to GitHub**: Whenever you push code or trigger the workflow, GitHub Actions wakes up.
2. **Builds Android APK**: Compiles the latest application APK and writes an `update-manifest.json` containing the new `versionCode`, commit hash, and release notes.
3. **Deploys to Cloud Run**: Packages `server.js`, `package.json`, and `.build-outputs/` (containing the latest APK) into a lightweight Docker image and deploys it directly to Google Cloud Run.
4. **Instant In-App Updates**:
   - The Cloud Run service provides `/api/v1/app-update/latest` and serves the APK at `/download/apk`.
   - The Android app checks this endpoint and automatically detects the new update, prompting the user once with the update dialog.

---

## 🛠️ Step-by-Step Setup Guide

### Step 1: Enable Google Cloud APIs
In your Google Cloud Console, ensure these 3 APIs are enabled:
```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com storage.googleapis.com
```

---

### Step 2: Set Up GCP Service Account Key
To allow GitHub Actions to build and deploy to your Google Cloud Run service:

1. In **Google Cloud Console**, go to **IAM & Admin** -> **Service Accounts**.
2. Click **Create Service Account** (e.g. `github-actions-deployer`).
3. Grant the following roles:
   - **Cloud Run Admin** (`roles/run.admin`)
   - **Storage Admin** (`roles/storage.admin`) — *Required for uploading source tarball to Cloud Storage during deployment*
   - **Cloud Build Editor** (`roles/cloudbuild.builds.editor`)
   - **Service Account User** (`roles/iam.serviceAccountUser`)
   - **Artifact Registry Admin** (`roles/artifactregistry.admin`)
4. Click on the created Service Account -> **Keys** tab -> **Add Key** -> **Create new key (JSON)**.
5. Download the JSON key file to your computer.

---

### Step 3: Add Secrets in GitHub Repository Settings
1. Go to your **GitHub Repository** -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Click **New repository secret** and add the following:

| Secret Name | Description / Value | Required? |
| :--- | :--- | :--- |
| **`GCP_PROJECT_ID`** | Your Google Cloud Project ID (e.g., `my-project-12345`) | **Yes** |
| **`GCP_SA_KEY`** | Paste the **entire contents** of your Service Account JSON key file | **Yes** |
| **`GCS_BUCKET_NAME`** | (Recommended) Google Cloud Storage bucket name for public APK distribution (e.g. `flowtest-apk-releases`) | Optional |
| **`PAIRGATE_API_KEY`** | Secret API key from Pairgate dashboard | Optional |
| **`MONIEPOINT_WEBHOOK_SECRET`** | Secret key from your Moniepoint Dashboard for HMAC-SHA256 verification | Optional |
| **`DOMAIN`** | Your custom domain (default: `api.flowtest2026.com`) | Optional |
| **`HTTPSMS_API_KEY`** | API key for HttpSMS gateway | Optional |
| **`GMAIL_APP_PASSWORD`** | 16-character Google App Password for email OTPs | Optional |

---

### Step 3b (Optional but Recommended): Public Google Cloud Storage Bucket for High-Speed APK Downloads

To use Step 1 & 2 of the pipeline (uploading APKs directly to Google Cloud Storage):
1. Create a storage bucket in GCP:
   ```bash
   gcloud storage buckets create gs://YOUR_BUCKET_NAME --project YOUR_PROJECT_ID --location=us-central1 --uniform-bucket-level-access
   ```
2. Make the bucket publicly readable for APK downloads:
   ```bash
   gcloud storage buckets add-iam-policy-binding gs://YOUR_BUCKET_NAME --member=allUsers --role=roles/storage.objectViewer
   ```
3. Apply CORS configuration (using the included `gcs-cors.json`):
   ```bash
   gcloud storage buckets update gs://YOUR_BUCKET_NAME --cors-file=gcs-cors.json
   ```
4. Add `GCS_BUCKET_NAME` to your GitHub Repository Secrets with the value `YOUR_BUCKET_NAME`.

---

### Step 4: Trigger Your First Deployment
Once secrets are saved in GitHub:
- Make any commit and push to `main` or `master`:
  ```bash
  git add .
  git commit -m "Deploy backend and app update to Cloud Run"
  git push origin main
  ```
- Or go to GitHub -> **Actions** -> **Build and Deploy Backend & App Updates to Google Cloud Run** -> Click **Run workflow**.

---

## 📡 Live Endpoints Provided by Cloud Run

Once deployed, your Cloud Run service URL will host:

- **Health & Status:** `GET https://<service-url>/health`
- **In-App Update Manifest:** `GET https://<service-url>/api/v1/app-update/latest`
- **Direct APK Download:** `GET https://<service-url>/download/apk`
- **Pairgate Webhook:** `POST https://<service-url>/api/v1/pairgate/webhook`
- **Moniepoint Webhook:** `POST https://<service-url>/api/webhook/moniepoint`
- **HttpSMS Webhook:** `POST https://<service-url>/api/webhook/httpsms`
