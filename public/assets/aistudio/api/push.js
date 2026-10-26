// api/push.js — অ্যাপে পুশ নোটিফিকেশন (Firebase Cloud Messaging HTTP v1)
// Env: SUPABASE_URL, SUPABASE_SERVICE_KEY, FIREBASE_SERVICE_ACCOUNT (সার্ভিস অ্যাকাউন্ট JSON, পুরোটা), CRON_SECRET, SITE_URL
// অ্যাকশন (POST, লগইন লাগবে): register · unregister · test (নিজেকে) · stats (অ্যাডমিন) · send (অ্যাডমিন)
// Cron (GET, Authorization: Bearer CRON_SECRET): ?job=daily
const crypto = require('crypto');
const SB = process.env.SUPABASE_URL;
const SK = process.env.SUPABASE_SERVICE_KEY;
const SITE = (process.env.SITE_URL || '').replace(/\/$/, '');

async function sb(path, opt = {}) {
  const r = await fetch(SB + path, {
    ...opt,
    headers: { apikey: SK, ...(String(SK).startsWith('eyJ') ? { Authorization: 'Bearer ' + SK } : {}), 'Content-Type': 'application/json', ...(opt.headers || {}) },
  });
  const t = await r.text();
  let j = null;
  try { j = t ? JSON.parse(t) : null; } catch (e) {}
  if (!r.ok) throw new Error((j && (j.message || j.error)) || 'DB ' + r.status);
  return j;
}
async function authUser(req) {
  const t = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  if (!t) return null;
  const r = await fetch(SB + '/auth/v1/user', { headers: { apikey: SK, Authorization: 'Bearer ' + t } });
  if (!r.ok) return null;
  const u = await r.json();
  return u && u.id ? u : null;
}
async function isAdmin(id) {
  const r = await sb('/rest/v1/admins?user_id=eq.' + id + '&select=user_id');
  return Array.isArray(r) && r.length > 0;
}

// ---- Firebase সার্ভিস অ্যাকাউন্ট → অ্যাক্সেস টোকেন (বাইরের লাইব্রেরি ছাড়া) ----
function getSA() {
  let raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) throw new Error('সার্ভারে FIREBASE_SERVICE_ACCOUNT সেট করা নেই');
  raw = raw.trim();
  if (raw[0] !== '{') raw = Buffer.from(raw, 'base64').toString('utf8');
  const sa = JSON.parse(raw);
  if (!sa.client_email || !sa.private_key || !sa.project_id) throw new Error('FIREBASE_SERVICE_ACCOUNT ঠিক নেই');
  return sa;
}
let _tok = null, _exp = 0;
async function fcmAccess(sa) {
  const now = Math.floor(Date.now() / 1000);
  if (_tok && _exp > now + 60) return _tok;
  const b64u = (x) => Buffer.from(x).toString('base64url');
  const h = b64u(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const c = b64u(JSON.stringify({ iss: sa.client_email, scope: 'https://www.googleapis.com/auth/firebase.messaging', aud: 'https://oauth2.googleapis.com/token', iat: now, exp: now + 3600 }));
  const sig = crypto.createSign('RSA-SHA256').update(h + '.' + c).sign(sa.private_key).toString('base64url');
  const r = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'grant_type=' + encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer') + '&assertion=' + h + '.' + c + '.' + sig,
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok || !j.access_token) throw new Error('Firebase টোকেন পাওয়া যায়নি: ' + (j.error_description || j.error || r.status));
  _tok = j.access_token;
  _exp = now + (j.expires_in || 3600);
  return _tok;
}
async function sendOne(sa, at, token, m) {
  const message = {
    token,
    notification: { title: m.title, body: m.body },
    android: { priority: 'HIGH', notification: { channel_id: 'exam_alerts', sound: 'default' } },
  };
  if (m.url) message.data = { url: m.url };
  const r = await fetch('https://fcm.googleapis.com/v1/projects/' + sa.project_id + '/messages:send', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + at, 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  });
  if (r.ok) return { ok: true };
  const j = await r.json().catch(() => ({}));
  const det = JSON.stringify((j.error && j.error.details) || '') + ((j.error && j.error.status) || '');
  return { ok: false, dead: r.status === 404 || /UNREGISTERED/.test(det), err: (j.error && j.error.message) || String(r.status) };
}
async function sendMany(tokens, m) {
  const sa = getSA();
  const at = await fcmAccess(sa);
  let sent = 0, failed = 0, firstErr = '';
  const dead = [];
  for (let i = 0; i < tokens.length; i += 10) {
    const part = tokens.slice(i, i + 10);
    const res = await Promise.all(part.map((t) => sendOne(sa, at, t, m).catch((e) => ({ ok: false, err: e.message }))));
    res.forEach((x, k) => {
      if (x.ok) sent++;
      else { failed++; if (!firstErr) firstErr = x.err || ''; if (x.dead) dead.push(part[k]); }
    });
  }
  for (const t of dead) { try { await sb('/rest/v1/push_tokens?token=eq.' + encodeURIComponent(t), { method: 'DELETE' }); } catch (e) {} }
  return { total: tokens.length, sent, failed, removed: dead.length, error: firstErr || undefined };
}
async function allTokens(userIds) {
  const out = [];
  let from = 0;
  const filt = userIds ? '&user_id=in.(' + userIds.join(',') + ')' : '';
  for (let p = 0; p < 40; p++) {
    const rows = await sb('/rest/v1/push_tokens?select=token,user_id' + filt + '&order=created_at.asc&limit=1000&offset=' + from);
    if (!Array.isArray(rows) || !rows.length) break;
    rows.forEach((r) => out.push(r));
    if (rows.length < 1000) break;
    from += 1000;
  }
  return out;
}
async function userIdByEmail(email) {
  const want = String(email || '').trim().toLowerCase();
  for (let page = 1; page <= 10; page++) {
    const r = await fetch(SB + '/auth/v1/admin/users?page=' + page + '&per_page=1000', {
      headers: { apikey: SK, ...(String(SK).startsWith('eyJ') ? { Authorization: 'Bearer ' + SK } : {}) },
    });
    if (!r.ok) break;
    const j = await r.json();
    const us = (j && j.users) || [];
    const hit = us.find((x) => (x.email || '').toLowerCase() === want);
    if (hit) return hit.id;
    if (us.length < 1000) break;
  }
  return null;
}
const clean = (s, n) => String(s == null ? '' : s).replace(/[\u0000-\u001f]/g, ' ').trim().slice(0, n);
const okUrl = (u) => { u = String(u || '').trim(); return SITE && u.startsWith(SITE) ? u : (SITE ? SITE + '/' : ''); };

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (!SB || !SK) return res.status(500).json({ error: 'সার্ভারে SUPABASE_URL / SUPABASE_SERVICE_KEY সেট করা নেই' });

    // Vercel Cron
    if (req.method === 'GET') {
      const cs = process.env.CRON_SECRET;
      if (!cs || (req.headers.authorization || '') !== 'Bearer ' + cs) return res.status(401).json({ error: 'unauthorized' });
      const job = (req.query && req.query.job) || '';
      if (job === 'daily') {
        const rows = await allTokens();
        const r = await sendMany(rows.map((x) => x.token), {
          title: '🌞 আজকের ডেইলি চ্যালেঞ্জ এসেছে!',
          body: 'স্ট্রিক ধরে রাখতে আজকের চ্যালেঞ্জটা এখনই করে ফেলো।',
          url: okUrl(''),
        });
        return res.json(r);
      }
      return res.status(400).json({ error: 'job=daily দিন' });
    }
    if (req.method !== 'POST') return res.status(405).json({ error: 'POST দিন' });

    const u = await authUser(req);
    if (!u) return res.status(401).json({ error: 'লগইন করুন' });
    const b = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};

    if (b.action === 'register') {
      if (u.is_anonymous) return res.json({ ok: true, skipped: true });
      const token = clean(b.token, 400);
      if (token.length < 20) return res.status(400).json({ error: 'টোকেন ঠিক নেই' });
      await sb('/rest/v1/push_tokens?on_conflict=token', {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=minimal' },
        body: JSON.stringify({ token, user_id: u.id, platform: 'android', updated_at: new Date().toISOString() }),
      });
      return res.json({ ok: true });
    }
    if (b.action === 'unregister') {
      const token = clean(b.token, 400);
      if (token) await sb('/rest/v1/push_tokens?token=eq.' + encodeURIComponent(token) + '&user_id=eq.' + u.id, { method: 'DELETE' });
      return res.json({ ok: true });
    }
    if (b.action === 'test') {
      const rows = await allTokens([u.id]);
      if (!rows.length) return res.status(400).json({ error: 'এই অ্যাকাউন্টে অ্যাপের নোটিফিকেশন চালু নেই। অ্যাপে লগইন করে কিছুক্ষণ অপেক্ষা করুন।' });
      return res.json(await sendMany(rows.map((x) => x.token), { title: '🔔 টেস্ট নোটিফিকেশন', body: 'সব ঠিক আছে — এই বার্তা আপনার ফোনে এসেছে।', url: okUrl('') }));
    }

    if (!(await isAdmin(u.id))) return res.status(403).json({ error: 'শুধু অ্যাডমিন' });

    if (b.action === 'stats') {
      const rows = await allTokens();
      return res.json({ ok: true, devices: rows.length, users: new Set(rows.map((x) => x.user_id)).size, firebase: !!process.env.FIREBASE_SERVICE_ACCOUNT });
    }
    if (b.action === 'send') {
      const title = clean(b.title, 60), body = clean(b.body, 200);
      if (!title || !body) return res.status(400).json({ error: 'শিরোনাম ও বার্তা দিন' });
      let rows;
      if (b.audience === 'self') rows = await allTokens([u.id]);
      else if (b.audience === 'email') {
        const id = await userIdByEmail(b.email);
        if (!id) return res.status(404).json({ error: 'এই ইমেইলের অ্যাকাউন্ট পাওয়া যায়নি' });
        rows = await allTokens([id]);
      } else rows = await allTokens();
      if (!rows.length) return res.status(400).json({ error: 'কোনো ডিভাইস পাওয়া যায়নি (কেউ অ্যাপে লগইন করেনি)' });
      return res.json({ ok: true, ...(await sendMany(rows.map((x) => x.token), { title, body, url: okUrl(b.url) })) });
    }
    return res.status(400).json({ error: 'action ভুল' });
  } catch (e) {
    return res.status(500).json({ error: String((e && e.message) || e) });
  }
};
