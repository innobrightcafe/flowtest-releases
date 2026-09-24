const http = require('http');
const crypto = require('crypto');

const PORT = 8089;
process.env.PORT = PORT;
process.env.MONIEPOINT_WEBHOOK_SECRET = 'mnp_whsec_test_secret_123';
process.env.HTTPSMS_WEBHOOK_SECRET = 'httpsms_whsec_test_secret_456';

// Start server
require('./server.js');

function postJson(path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request({
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
        ...headers
      }
    }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(raw) });
        } catch (e) {
          resolve({ status: res.statusCode, raw });
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function getJson(path) {
  return new Promise((resolve, reject) => {
    http.get({
      hostname: '127.0.0.1',
      port: PORT,
      path: path,
      headers: { 'Accept': 'application/json' }
    }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(raw) });
        } catch (e) {
          resolve({ status: res.statusCode, raw });
        }
      });
    }).on('error', reject);
  });
}

function computeHmacSha256(data, secret) {
  return crypto.createHmac('sha256', secret).update(data).digest('hex');
}

async function runTests() {
  // Allow server 1s to start
  await new Promise(r => setTimeout(r, 1000));
  console.log('--- STARTING WEBHOOK TESTS ---');

  let passed = 0;
  let failed = 0;

  // 1. Health Check
  try {
    const health = await getJson('/health');
    if (health.status === 200 && health.data.status === 'online') {
      console.log('✅ TEST 1 PASSED: Server health endpoint returns 200 online');
      passed++;
    } else {
      console.error('❌ TEST 1 FAILED:', health);
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 1 ERROR:', e);
    failed++;
  }

  // 2. Moniepoint Webhook - Inbound Transfer with HMAC Signature
  const mnpRef = 'MNP_TEST_TX_' + Date.now();
  const mnpPayload = {
    event: 'PAYMENT_SUCCESSFUL',
    transactionReference: mnpRef,
    amount: 5000.0,
    narration: 'Transfer from Inobright FT-1001',
    senderName: 'Innocent Bright',
    accountNumber: '6666468328'
  };
  const mnpRaw = JSON.stringify(mnpPayload);
  const mnpSig = computeHmacSha256(mnpRaw, 'mnp_whsec_test_secret_123');

  try {
    const mnpRes = await postJson('/api/webhook/moniepoint', mnpPayload, {
      'moniepoint-signature': mnpSig
    });
    if (mnpRes.status === 200 && mnpRes.data.status === 'success') {
      console.log('✅ TEST 2 PASSED: Moniepoint webhook processed successfully with valid HMAC signature');
      passed++;
    } else {
      console.error('❌ TEST 2 FAILED:', mnpRes);
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 2 ERROR:', e);
    failed++;
  }

  // 3. Moniepoint Requery
  try {
    const requeryRes = await getJson(`/api/v1/moniepoint/requery/${mnpRef}`);
    if (requeryRes.status === 200 && requeryRes.data.isFound === true && requeryRes.data.data.amount === 5000.0) {
      console.log('✅ TEST 3 PASSED: Moniepoint transaction requery verified matched event in ledger');
      passed++;
    } else {
      console.error('❌ TEST 3 FAILED:', requeryRes);
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 3 ERROR:', e);
    failed++;
  }

  // 4. HttpSMS Delivery Status Webhook
  const smsMsgId = 'msg_test_' + Date.now();
  const smsDeliveredPayload = {
    type: 'message.delivered',
    data: {
      id: smsMsgId,
      owner: '+2348168290134',
      contact: '+2348103462171',
      content: 'Your FlowTest verification code is 884910',
      status: 'DELIVERED',
      timestamp: new Date().toISOString()
    }
  };
  const smsRaw = JSON.stringify(smsDeliveredPayload);
  const smsSig = computeHmacSha256(smsRaw, 'httpsms_whsec_test_secret_456');

  try {
    const smsRes = await postJson('/api/webhook/httpsms', smsDeliveredPayload, {
      'x-signature': smsSig
    });
    if (smsRes.status === 200 && smsRes.data.status === 'success') {
      console.log('✅ TEST 4 PASSED: HttpSMS delivery webhook acknowledged with 200 OK');
      passed++;
    } else {
      console.error('❌ TEST 4 FAILED:', smsRes);
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 4 ERROR:', e);
    failed++;
  }

  // 5. HttpSMS Incoming Message Webhook
  const inboundSmsPayload = {
    type: 'message.received',
    data: {
      id: 'msg_inbound_' + Date.now(),
      owner: '+2348168290134',
      contact: '+2348103462171',
      content: 'Hello, I need assistance with my VPN subscription',
      status: 'RECEIVED',
      timestamp: new Date().toISOString()
    }
  };

  try {
    const inboundRes = await postJson('/api/v1/httpsms/webhook', inboundSmsPayload, {
      'Authorization': 'Bearer httpsms_whsec_test_secret_456'
    });
    if (inboundRes.status === 200 && inboundRes.data.status === 'success') {
      console.log('✅ TEST 5 PASSED: HttpSMS inbound message webhook accepted successfully');
      passed++;
    } else {
      console.error('❌ TEST 5 FAILED:', inboundRes);
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 5 ERROR:', e);
    failed++;
  }

  // 6. Query Recent Inbound Webhooks
  try {
    const recentRes = await getJson('/api/webhooks/recent');
    const recentSms = await getJson('/api/webhook/httpsms/recent');
    if (recentRes.status === 200 && recentRes.data.count >= 2 && recentSms.status === 200 && recentSms.data.count >= 2) {
      console.log('✅ TEST 6 PASSED: Recent webhooks query returns logged events for Moniepoint & SMS');
      passed++;
    } else {
      console.error('❌ TEST 6 FAILED:', { recentRes, recentSms });
      failed++;
    }
  } catch (e) {
    console.error('❌ TEST 6 ERROR:', e);
    failed++;
  }

  console.log(`\n========================================`);
  console.log(`RESULTS: ${passed} PASSED, ${failed} FAILED`);
  console.log(`========================================\n`);

  process.exit(failed > 0 ? 1 : 0);
}

runTests();
