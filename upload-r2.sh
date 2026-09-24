#!/bin/bash
# ==============================================================================
# ☁️ Cloudflare R2 Upload Script for FlowTest APK
# ==============================================================================

set -e

APK_FILE="webapp/FlowTest.apk"
if [ ! -f "$APK_FILE" ]; then
  APK_FILE="FlowTest.apk"
fi
if [ ! -f "$APK_FILE" ]; then
  APK_FILE=".build-outputs/FlowTest.apk"
fi

if [ ! -f "$APK_FILE" ]; then
  echo "❌ Error: Could not find FlowTest.apk in webapp/, root, or .build-outputs/"
  exit 1
fi

R2_BUCKET="${R2_BUCKET_NAME:-flowtest-apk}"

if [ -z "$R2_ACCOUNT_ID" ] || [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
  echo "⚠️ Missing R2 environment credentials. Please set:"
  echo "   export R2_ACCOUNT_ID='<your_cloudflare_account_id>'"
  echo "   export AWS_ACCESS_KEY_ID='<your_r2_access_key_id>'"
  echo "   export AWS_SECRET_ACCESS_KEY='<your_r2_secret_access_key>'"
  echo "   export R2_BUCKET_NAME='flowtest-apk' (optional, defaults to flowtest-apk)"
  exit 1
fi

export AWS_DEFAULT_REGION="auto"
ENDPOINT_URL="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

echo "🚀 Uploading $APK_FILE ($(du -h "$APK_FILE" | cut -f1)) to Cloudflare R2: s3://${R2_BUCKET}/FlowTest.apk..."

aws s3 cp "$APK_FILE" "s3://${R2_BUCKET}/FlowTest.apk" \
  --endpoint-url "$ENDPOINT_URL" \
  --content-type "application/vnd.android.package-archive"

aws s3 cp "$APK_FILE" "s3://${R2_BUCKET}/latest/FlowTest.apk" \
  --endpoint-url "$ENDPOINT_URL" \
  --content-type "application/vnd.android.package-archive"

if [ -f "webapp/update-manifest.json" ]; then
  echo "📄 Uploading update-manifest.json to Cloudflare R2..."
  aws s3 cp "webapp/update-manifest.json" "s3://${R2_BUCKET}/update-manifest.json" \
    --endpoint-url "$ENDPOINT_URL" \
    --content-type "application/json"
fi

echo "✅ Successfully published to Cloudflare R2!"
