# 🚀 GetMeHost (GPanel) Continuous Deployment Guide

This repository is configured to build seamlessly with **GetMeHost GPanel** continuous auto-deployment pipeline for **`testcheckoutsafety2026.com`**.

---

## 📋 Step-by-Step Instructions

### Step 1: Push Project from AI Studio to GitHub
1. In Google AI Studio, click on **Settings** or the **Export/GitHub** button at the top right of the screen.
2. Select **Push to GitHub**.
3. Choose or create your repository (e.g., `testcheckoutsafety2026-android-app`).

---

### Step 2: Configure GPanel for `testcheckoutsafety2026.com`
1. Log into your **GPanel** dashboard for `testcheckoutsafety2026.com`.
2. Open **GIT / GITHUB DEPLOY** from the left sidebar menu.
3. Click **Connect GitHub Account** and select your repository (`testcheckoutsafety2026-android-app`).
4. Select the **Android Native (Gradle APK Pipeline)** preset or fill in:
   - **Build Command:** `./gradlew assembleRelease`
   - **Output Directory:** `app/build/outputs/apk/release`
   - **Production Branch:** `main` (or `master`)
   - **Continuous Auto-Deploy:** Toggle **ON**
5. Save the configuration.

---

### Step 3: Add Webhook in GitHub
1. Copy the **Payload URL** provided by GPanel (e.g. `https://api.getmehost.com/webhooks/git/srv_testcheckoutsafety2026`).
2. Copy the **Secret Token** generated in GPanel.
3. On GitHub, navigate to: `Your Repository -> Settings -> Webhooks -> Add webhook`.
4. Paste the **Payload URL** into the Payload URL field.
5. Set **Content type** to `application/json`.
6. Paste the **Secret Token** in the Secret field.
7. Under "Which events would you like to trigger this webhook?", select **Just the push event**.
8. Click **Add webhook**.

---

## ⚡ How It Works
- Whenever you make changes in AI Studio and push to GitHub, GitHub automatically notifies GetMeHost via the webhook.
- GetMeHost pulls the latest code, runs `./gradlew assembleRelease`, and publishes the build live to `testcheckoutsafety2026.com`.
