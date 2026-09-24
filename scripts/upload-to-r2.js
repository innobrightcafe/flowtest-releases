const { S3Client, PutObjectCommand, HeadObjectCommand } = require('@aws-sdk/client-s3');
const fs = require('fs');
const path = require('path');

const accountId = process.env.R2_ACCOUNT_ID || '422a1cc009f978a11c47618802d0966c';
const accessKeyId = process.env.R2_ACCESS_KEY_ID || process.env.AWS_ACCESS_KEY_ID;
const secretAccessKey = process.env.R2_SECRET_ACCESS_KEY || process.env.AWS_SECRET_ACCESS_KEY;
const bucket = process.env.R2_BUCKET_NAME || 'vibeflowstoraage';

if (!accessKeyId || !secretAccessKey) {
  console.error('Missing R2 credentials');
  process.exit(1);
}

const s3 = new S3Client({
  region: 'auto',
  endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId,
    secretAccessKey,
  },
});

async function uploadFile(filePath, key, contentType) {
  if (!fs.existsSync(filePath)) {
    console.error(`File not found: ${filePath}`);
    return false;
  }
  const fileStream = fs.createReadStream(filePath);
  const stats = fs.statSync(filePath);
  console.log(`Uploading ${filePath} (${(stats.size / 1024 / 1024).toFixed(2)} MB) to s3://${bucket}/${key}...`);

  try {
    const cmd = new PutObjectCommand({
      Bucket: bucket,
      Key: key,
      Body: fileStream,
      ContentType: contentType,
      ContentLength: stats.size,
      CacheControl: 'public, max-age=3600',
    });
    await s3.send(cmd);
    console.log(`✓ Successfully uploaded ${key} to ${bucket}`);
    return true;
  } catch (err) {
    console.error(`Failed to upload ${key}:`, err.message);
    return false;
  }
}

async function main() {
  const apkPath = path.join(__dirname, '..', 'webapp', 'FlowTest.apk');
  const manifestPath = path.join(__dirname, '..', 'webapp', 'update-manifest.json');

  await uploadFile(apkPath, 'FlowTest.apk', 'application/vnd.android.package-archive');
  await uploadFile(apkPath, 'latest/FlowTest.apk', 'application/vnd.android.package-archive');
  if (fs.existsSync(manifestPath)) {
    await uploadFile(manifestPath, 'update-manifest.json', 'application/json');
  }
}

main();
