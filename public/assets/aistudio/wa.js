// api/wa.js — WhatsApp Cloud API: ফলাফল PDF, সকালের প্রশ্ন + স্ট্রিক, সাপ্তাহিক অভিভাবক রিপোর্ট
// Env: SUPABASE_URL, SUPABASE_SERVICE_KEY, GEMINI_API_KEY, WA_TOKEN, WA_PHONE_ID, CRON_SECRET, SITE_URL
// ঐচ্ছিক: WA_TPL_RESULT, WA_TPL_DAILY, WA_TPL_WEEKLY, WA_LANG, WA_GRAPH_VERSION, AI_MODEL_SMART, AI_MODEL_FALLBACK
const SB = process.env.SUPABASE_URL;
const SK = process.env.SUPABASE_SERVICE_KEY;
const GK = process.env.GEMINI_API_KEY;
const WT = process.env.WA_TOKEN;
const PID = process.env.WA_PHONE_ID;
const GV = process.env.WA_GRAPH_VERSION || 'v23.0';
const LANG = process.env.WA_LANG || 'bn';
const SITE = process.env.SITE_URL || '';
const SMART = process.env.AI_MODEL_SMART || 'gemini-3.8-flash';
const FALLBACKS = (process.env.AI_MODEL_FALLBACK || 'gemini-3.5-flash-lite,gemini-3.5-flash,gemini-3.8-flash').split(',').map((x) => x.trim()).filter(Boolean);
const TPL = {
  result: process.env.WA_TPL_RESULT || 'exam_result_pdf',
  daily: process.env.WA_TPL_DAILY || 'daily_question',
  weekly: process.env.WA_TPL_WEEKLY || 'weekly_report',
};

async function sb(path, opt = {}) {
  const r = await fetch(SB + path, {
    ...opt,
    headers: { apikey: SK, Authorization: 'Bearer ' + SK, 'Content-Type': 'application/json', ...(opt.headers || {}) },
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

const bn = (n) => String(n).replace(/\d/g, (d) => '০১২৩৪৫৬৭৮৯'[d]);
const flat = (s, max = 900) => String(s == null ? '' : s).replace(/[\r\n\t]+/g, ' ').replace(/ {2,}/g, ' ').trim().slice(0, max);
const dkey = (ms) => new Date(ms + 6 * 3600e3).toISOString().slice(0, 10); // ঢাকা সময়ের তারিখ
const today = () => dkey(Date.now());
const CLS = { 1: 'প্রথম', 2: 'দ্বিতীয়', 3: 'তৃতীয়', 4: 'চতুর্থ', 5: 'পঞ্চম', 6: 'ষষ্ঠ', 7: 'সপ্তম', 8: 'অষ্টম', 9: 'নবম', 10: 'দশম', 11: 'একাদশ', 12: 'দ্বাদশ', 13: 'এডমিশন' };
const SUBJ = ['সাধারণ জ্ঞান', 'গণিত', 'বিজ্ঞান', 'ইংরেজি', 'বাংলা', 'বাংলাদেশ ও বিশ্বপরিচয়', 'গণিত']; // রবি→শনি (UTC weekday 0=রবি)

async function pool(items, n, fn) {
  const out = [];
  let i = 0;
  await Promise.all(
    Array.from({ length: Math.min(n, items.length) }, async () => {
      while (i < items.length) {
        const k = i++;
        out[k] = await fn(items[k]).catch((e) => ({ error: String((e && e.message) || e) }));
      }
    })
  );
  return out;
}

async function wlog(user_id, kind, ok, info) {
  await sb('/rest/v1/wa_log', { method: 'POST', body: JSON.stringify({ user_id, kind, ok, info: String(info || '').slice(0, 300) }) }).catch(() => {});
}

async function waPost(path, opt) {
  const r = await fetch(`https://graph.facebook.com/${GV}/${PID}/${path}`, {
    ...opt,
    headers: { Authorization: 'Bearer ' + WT, ...(opt.headers || {}) },
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error((j.error && (j.error.error_user_msg || j.error.message)) || 'WhatsApp ত্রুটি ' + r.status);
  return j;
}
async function sendTemplate(to, name, params, header, lang) {
  const components = [];
  if (header) components.push({ type: 'header', parameters: [header] });
  if (params && params.length) components.push({ type: 'body', parameters: params.map((t) => ({ type: 'text', text: flat(t) || '-' })) });
  return waPost('messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messaging_product: 'whatsapp', to, type: 'template', template: { name, language: { code: lang || LANG }, components } }),
  });
}
async function uploadPdf(b64) {
  const fd = new FormData();
  fd.append('messaging_product', 'whatsapp');
  fd.append('type', 'application/pdf');
  fd.append('file', new Blob([Buffer.from(b64, 'base64')], { type: 'application/pdf' }), 'answer-sheet.pdf');
  const j = await waPost('media', { method: 'POST', body: fd });
  return j.id;
}

// Gemini দিয়ে লেখা তৈরি (ফ্রি টিয়ারে চলে); মূল মডেল ব্যস্ত থাকলে পরেরটায় যায়
const noThink = new Set();
async function claudeText(system, user, max_tokens = 800) {
  if (!GK) throw new Error('GEMINI_API_KEY সেট করা নেই');
  const body = {
    systemInstruction: { parts: [{ text: system }] },
    contents: [{ role: 'user', parts: [{ text: user }] }],
    generationConfig: { maxOutputTokens: max_tokens, temperature: 0.6, responseMimeType: 'application/json', thinkingConfig: { thinkingBudget: 0 } },
  };
  const models = [SMART, ...FALLBACKS].filter((m, i, a) => a.indexOf(m) === i);
  let lastErr = 'AI ত্রুটি';
  for (const m of models) {
    const url = 'https://generativelanguage.googleapis.com/v1beta/models/' + m + ':generateContent';
    const go = (bd) => fetch(url, { method: 'POST', headers: { 'x-goog-api-key': GK, 'content-type': 'application/json' }, body: JSON.stringify(bd) });
    let bd = body;
    if (noThink.has(m)) { bd = JSON.parse(JSON.stringify(body)); delete bd.generationConfig.thinkingConfig; }
    let r = await go(bd);
    if (r.status === 400 && bd.generationConfig.thinkingConfig) {
      noThink.add(m);
      const b2 = JSON.parse(JSON.stringify(body));
      delete b2.generationConfig.thinkingConfig;
      b2.generationConfig.maxOutputTokens = Math.max(max_tokens * 2, 2000);
      r = await go(b2);
    }
    const j = await r.json().catch(() => ({}));
    if (r.ok) {
      const c = j.candidates && j.candidates[0];
      const t = ((c && c.content && c.content.parts) || []).map((p) => p.text || '').join('');
      if (t) return t;
      lastErr = 'AI উত্তর দেয়নি';
      continue;
    }
    lastErr = (j.error && j.error.message) || 'AI ত্রুটি ' + r.status;
    if (![404, 429, 500, 502, 503, 504].includes(r.status) && !/no longer available|high demand|overloaded|unavailable|not found/i.test(lastErr)) break;
  }
  throw new Error(lastErr);
}

// ---- সকালের প্রশ্ন (শ্রেণি + তারিখ অনুযায়ী একবার তৈরি হয়ে জমা থাকে) ----
async function dailyQ(cls, day, create) {
  const got = await sb(`/rest/v1/daily_wa?class_level=eq.${cls}&day=eq.${day}&select=q`);
  if (got && got[0]) return got[0].q;
  if (!create) return null;
  const subj = SUBJ[new Date(day + 'T00:00:00Z').getUTCDay()];
  const txt = await claudeText(
    'তুমি বাংলাদেশের NCTB পাঠ্যক্রমের প্রশ্নপ্রণেতা। শুধু একটি JSON অবজেক্ট দাও, আর কিছু নয়: {"q":"প্রশ্ন","o":["","","",""],"a":0-3,"s":"বিষয়"}। প্রশ্ন এক লাইনে, অপশনে ক/খ লেবেল নেই, ঠিক একটি উত্তর সঠিক, তথ্য নির্ভুল।',
    `শ্রেণি: ${CLS[cls] || cls}। বিষয়: ${subj}। একটি মাঝারি মানের MCQ বানাও। প্রশ্ন ও অপশন ছোট রাখো (মোট ২৫০ অক্ষরের মধ্যে)।`
  );
  const a = txt.indexOf('{'), b = txt.lastIndexOf('}');
  const q = JSON.parse(txt.slice(a, b + 1));
  if (!q.q || !Array.isArray(q.o) || q.o.length !== 4 || !(q.a >= 0 && q.a <= 3)) throw new Error('দৈনিক প্রশ্ন ফরম্যাট ভুল');
  q.s = q.s || subj;
  await sb('/rest/v1/daily_wa', { method: 'POST', headers: { Prefer: 'resolution=ignore-duplicates' }, body: JSON.stringify({ class_level: cls, day, q }) });
  return q;
}
const LET = ['ক', 'খ', 'গ', 'ঘ'];

// ---- অ্যাক্টিভিটি/স্ট্রিক ----
async function activity(uid, days) {
  const since = new Date(Date.now() - days * 864e5).toISOString();
  const rs = await sb(`/rest/v1/results?select=score,total,created_at,exams(title)&user_id=eq.${uid}&created_at=gte.${since}&order=created_at.desc&limit=500`);
  const dr = await sb(`/rest/v1/daily_runs?select=created_at&user_id=eq.${uid}&created_at=gte.${since}&limit=300`).catch(() => []);
  const set = new Set();
  (rs || []).forEach((r) => set.add(dkey(Date.parse(r.created_at))));
  (dr || []).forEach((r) => set.add(dkey(Date.parse(r.created_at))));
  let t = Date.now(), cur = 0;
  if (!set.has(dkey(t))) t -= 864e5;
  while (set.has(dkey(t))) { cur++; t -= 864e5; }
  return { rs: rs || [], streak: cur, doneToday: set.has(today()) };
}
async function userInfo(uid) {
  const r = await fetch(`${SB}/auth/v1/admin/users/${uid}`, { headers: { apikey: SK, Authorization: 'Bearer ' + SK } });
  const u = await r.json().catch(() => ({}));
  const m = u.user_metadata || {};
  return { name: m.full_name || (u.email || '').split('@')[0] || 'শিক্ষার্থী', cls: parseInt(m.class_level) || null };
}

async function jobDaily(dry) {
  const day = today();
  const f = encodeURIComponent(`(last_daily.is.null,last_daily.lt.${day})`);
  const prefs = (await sb(`/rest/v1/wa_prefs?daily_on=eq.true&wa_number=not.is.null&or=${f}&select=user_id,wa_number,class_level&limit=2000`)) || [];
  if (dry) return { dry: true, would_send: prefs.length };
  const infos = await pool(prefs, 6, async (p) => ({ p, i: await userInfo(p.user_id) }));
  const classes = [...new Set(infos.filter((x) => x && x.p).map((x) => x.p.class_level || x.i.cls || 8))];
  const qmap = {};
  for (const c of classes) { try { qmap[c] = await dailyQ(c, day, true); } catch (e) { qmap[c] = null; } }
  const yday = dkey(Date.now() - 864e5);
  const ymap = {};
  for (const c of classes) { ymap[c] = await dailyQ(c, yday, false).catch(() => null); }
  let sent = 0, failed = 0;
  await pool(infos.filter((x) => x && x.p), 5, async ({ p, i }) => {
    const cls = p.class_level || i.cls || 8;
    const q = qmap[cls];
    if (!q) { failed++; await wlog(p.user_id, 'daily', false, 'প্রশ্ন তৈরি হয়নি'); return; }
    try {
      const act = await activity(p.user_id, 60);
      const qt = `${q.q} ` + q.o.map((o, k) => `(${LET[k]}) ${o}`).join(' ');
      const yq = ymap[cls];
      const ya = yq ? `(${LET[yq.a]}) ${yq.o[yq.a]}` : 'আজই প্রথম প্রশ্ন';
      const sl = act.streak > 0
        ? `🔥 তোমার ${bn(act.streak)} দিনের স্ট্রিক চলছে — আজ একটি পরীক্ষা দিয়ে ধরে রাখো!`
        : '🔥 আজ একটি পরীক্ষা দিয়ে নতুন স্ট্রিক শুরু করো!';
      await sendTemplate(p.wa_number, TPL.daily, [i.name, q.s || 'সাধারণ', qt, ya, sl, SITE || 'আমাদের সাইট']);
      await sb(`/rest/v1/wa_prefs?user_id=eq.${p.user_id}`, { method: 'PATCH', body: JSON.stringify({ last_daily: day }) });
      await wlog(p.user_id, 'daily', true, 'ok');
      sent++;
    } catch (e) { failed++; await wlog(p.user_id, 'daily', false, e.message); }
  });
  return { sent, failed, total: prefs.length };
}

async function jobWeekly(dry) {
  const day = today();
  const lim = dkey(Date.now() - 5 * 864e5);
  const f = encodeURIComponent(`(last_weekly.is.null,last_weekly.lt.${lim})`);
  const prefs = (await sb(`/rest/v1/wa_prefs?weekly_on=eq.true&guardian_number=not.is.null&or=${f}&select=user_id,guardian_number&limit=2000`)) || [];
  if (dry) return { dry: true, would_send: prefs.length };
  let sent = 0, failed = 0;
  await pool(prefs, 5, async (p) => {
    try {
      const i = await userInfo(p.user_id);
      const act = await activity(p.user_id, 7);
      const pcs = act.rs.filter((r) => r.total > 0).map((r) => Math.round((r.score / r.total) * 100));
      const n = act.rs.length;
      const avg = pcs.length ? Math.round(pcs.reduce((a, b) => a + b, 0) / pcs.length) : 0;
      const best = pcs.length ? Math.max(...pcs) : 0;
      const cm = !n ? 'এই সপ্তাহে কোনো পরীক্ষা দেওয়া হয়নি — নিয়মিত অনুশীলনে উৎসাহ দিন।'
        : avg >= 80 ? 'চমৎকার ফল! এভাবেই চালিয়ে যেতে উৎসাহ দিন।'
        : avg >= 50 ? 'ভালো চলছে; দুর্বল অংশগুলো আবার দেখলে ফল আরও ভালো হবে।'
        : 'আরও অনুশীলন দরকার; প্রতিদিন অল্প সময় পড়ার অভ্যাস গড়তে সাহায্য করুন।';
      await sendTemplate(p.guardian_number, TPL.weekly, [i.name, bn(n), bn(avg) + '%', bn(best) + '%', bn(act.streak), cm]);
      await sb(`/rest/v1/wa_prefs?user_id=eq.${p.user_id}`, { method: 'PATCH', body: JSON.stringify({ last_weekly: day }) });
      await wlog(p.user_id, 'weekly', true, 'ok');
      sent++;
    } catch (e) { failed++; await wlog(p.user_id, 'weekly', false, e.message); }
  });
  return { sent, failed, total: prefs.length };
}

// ---- গেম জোনের নতুন প্রশ্ন (Gemini দিয়ে প্রতিদিন; আগের সব প্রশ্ন যেমন আছে তেমনই থাকে) ----
const GTHEMES = [
  'বাংলাদেশের ইতিহাস ও মুক্তিযুদ্ধ', 'বিজ্ঞান ও প্রকৃতি', 'গণিত ও যুক্তি', 'ভূগোল ও বিশ্বের দেশ-রাজধানী',
  'প্রাণী, পাখি ও গাছপালা', 'বাংলা ভাষা-সাহিত্য ও সংস্কৃতি', 'খেলাধুলা, প্রযুক্তি ও দৈনন্দিন জীবন',
  'মানবদেহ ও স্বাস্থ্য', 'রসায়ন ও পদার্থের ধর্ম', 'বাংলাদেশের নদী, পাহাড় ও জলবায়ু', 'বিশ্ব ইতিহাস ও আবিষ্কার',
  'ইংরেজি শব্দ ও ব্যাকরণ', 'পরিবেশ ও জলবায়ু পরিবর্তন', 'মহাকাশ ও পৃথিবী', 'বাংলাদেশের অর্থনীতি ও সংবিধান',
  'কম্পিউটার ও প্রযুক্তির মূলনীতি', 'ধর্ম-সংস্কৃতি ও উৎসব', 'কৃষি ও খাদ্য', 'বিখ্যাত মানুষ ও তাদের কাজ',
];
// সংখ্যার ধাঁধার নিয়মের ধরন — প্রতিবার একটি বাছা হয়, যাতে প্রশ্নের ছাঁচ একঘেয়ে না হয়
const SEQ_RULES = [
  'ধ্রুব যোগ বা বিয়োগের ধারা', 'ধ্রুব গুণের ধারা', 'বর্গ বা ঘনের ধারা', 'আগের দুটির যোগফলের ধারা (ফিবোনাচ্চি-ধরন)',
  'বিকল্পে যোগ ও গুণের মিশ্র ধারা', 'ক্রমবর্ধমান পার্থক্যের ধারা (১, ২, ৩ ...)', 'দুটি আলাদা ধারার মিশ্রণ (জোড় ও বিজোড় স্থান আলাদা নিয়মে)',
];
const gnorm = (x) => String(x == null ? '' : x).replace(/\s+/g, ' ').trim().toLowerCase();
function gshuf(a) { const r = a.slice(); for (let i = r.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [r[i], r[j]] = [r[j], r[i]]; } return r; }
function gjson(txt) {
  const a = txt.indexOf('{'), b = txt.lastIndexOf('}');
  const j = JSON.parse(txt.slice(a, b + 1));
  return Array.isArray(j.items) ? j.items : [];
}
// সংখ্যার ধাঁধার উত্তর আমরা নিজেরা যাচাই করি — এআই ভুল করলে সেটি বাদ যায়
function seqOk(t) {
  if (!Array.isArray(t) || t.length !== 6 || !t.every((x) => Number.isInteger(x) && Math.abs(x) <= 100000)) return false;
  if (new Set(t).size < 4) return false;
  const d = (a) => a.slice(1).map((x, i) => x - a[i]);
  const same = (a) => a.length > 1 && a.every((x) => x === a[0]);
  const d1 = d(t), d2 = d(d1), d3 = d(d2);
  if (same(d1) && d1[0] !== 0) return true;
  if (same(d2) && d2[0] !== 0) return true;
  if (same(d3) && d3[0] !== 0) return true;
  if (t[0] !== 0 && t.slice(1).every((x, i) => x * t[0] === t[i] * t[1]) && t[1] !== t[0]) return true;
  if (t.slice(2).every((x, i) => x === t[i] + t[i + 1])) return true;
  return false;
}
async function genSeq(day, avoid) {
  const rule = gshuf(SEQ_RULES)[0];
  const txt = await claudeText(
    'তুমি স্কুলের শিক্ষার্থীদের জন্য সংখ্যার ধাঁধার প্রশ্নপ্রণেতা। শুধু JSON দাও: {"items":[{"t":[৬টি পূর্ণসংখ্যা]}]}। প্রতিটি t-তে ঠিক ৬টি সংখ্যা, একটি স্পষ্ট নিয়মে চলে (যোগ/বিয়োগ, গুণ, বর্গ, ঘন, ফিবোনাচ্চি-ধরন)। কোনো ব্যাখ্যা নয়।',
    `তারিখ ${day}। ৮টি ধাঁধা দাও; এদের বেশিরভাগ নিয়ম হোক: ${rule}। সহজ থেকে একটু কঠিন, সংখ্যা ছোট (সর্বোচ্চ ৫ অঙ্ক)। এগুলোর মতো (একই ধারা) দিও না: ${avoid}`, 900);
  return gjson(txt).filter((x) => seqOk(x.t)).map((x) => {
    const t = x.t, ans = t[5], last = t[5] - t[4];
    const cand = gshuf([ans + 1, ans - 1, ans + 2, ans - 2, ans + (last || 3), ans - (last || 3), ans + 10, ans * 2].filter((v, i, a) => v !== ans && a.indexOf(v) === i)).slice(0, 3);
    const opts = gshuf([ans, ...cand]);
    const f = (n) => bn(n).replace('-', '−');
    return { game: 'numseq', q: t.slice(0, 5).map(f).join(', ') + ', ?', opts: opts.map(f), ans: opts.indexOf(ans) };
  });
}
async function genTF(day, theme, avoid) {
  const txt = await claudeText(
    'তুমি বাংলাদেশের ৬ষ্ঠ–১০ম শ্রেণির শিক্ষার্থীদের জন্য "সত্য নাকি মিথ্যা" প্রশ্নপ্রণেতা। শুধু JSON দাও: {"items":[{"q":"বিবৃতি","a":true বা false}]}। বিবৃতি এক লাইনে (১৪০ অক্ষরের মধ্যে), তথ্য নিশ্চিতভাবে নির্ভুল ও বিতর্কহীন; সত্য ও মিথ্যা প্রায় সমান সংখ্যায়। অনিশ্চিত তথ্য দিও না।',
    `তারিখ ${day}। বিষয়-ভাবনা: ${theme}। ১২টি নতুন বিবৃতি দাও। এগুলো আগে দেওয়া হয়েছে, পুনরাবৃত্তি করো না: ${avoid}`, 1800);
  return gjson(txt).filter((x) => typeof x.q === 'string' && x.q.length >= 8 && x.q.length <= 160 && typeof x.a === 'boolean')
    .map((x) => ({ game: 'tf', q: flat(x.q, 160), opts: ['সত্য', 'মিথ্যা'], ans: x.a ? 0 : 1 }));
}
async function genPic(day, theme, avoid) {
  const txt = await claudeText(
    'তুমি শিশু-কিশোরদের জন্য "ছবির ধাঁধা" বানাও। শুধু JSON দাও: {"items":[{"q":"১–৪টি ইমোজি","o":["","","",""],"a":0-3}]}। q-তে শুধু ইমোজি; o-তে ৪টি আলাদা ছোট বাংলা শব্দ (প্রতিটি ২০ অক্ষরের মধ্যে); ঠিক একটি উত্তর স্পষ্টভাবে সঠিক, বাকিগুলো বিভ্রান্তিকর হলেও ভুল। ইমোজির অর্থ নিয়ে দ্ব্যর্থতা রাখবে না।',
    `তারিখ ${day}। বিষয়-ভাবনা: ${theme} (ইমোজি দিয়ে প্রকাশ করা যায় এমন)। ৮টি নতুন ধাঁধা দাও। এগুলো আগে দেওয়া হয়েছে: ${avoid}`, 1500);
  return gjson(txt).filter((x) => typeof x.q === 'string' && x.q.length >= 1 && x.q.length <= 24 && /[^\x00-\x7F]/.test(x.q) && Array.isArray(x.o) && x.o.length === 4
    && new Set(x.o.map(gnorm)).size === 4 && x.o.every((o) => typeof o === 'string' && o.length > 0 && o.length <= 24) && x.a >= 0 && x.a <= 3)
    .map((x) => { const right = x.o[x.a], o = gshuf(x.o); return { game: 'pic', q: x.q.trim(), opts: o, ans: o.indexOf(right) }; });
}
async function genPairs(day, theme, avoid) {
  const txt = await claudeText(
    'তুমি কার্ড মেলানো খেলার জোড়া বানাও। শুধু JSON দাও: {"items":[{"a":"কার্ড ১","b":"কার্ড ২"}]}। প্রতিটি জোড়া নিশ্চিতভাবে সঠিক। উদাহরণের ধরন: ইংরেজি শব্দ ↔ বাংলা অর্থ, দেশ ↔ রাজধানী, প্রাণী ↔ ডাক/বাসস্থান, বিখ্যাত ব্যক্তি ↔ পরিচয়। প্রতিটি কার্ডের লেখা ১৬ অক্ষরের মধ্যে।',
    `তারিখ ${day}। বিষয়-ভাবনা: ${theme}। ৮টি নতুন জোড়া দাও, একই ধরনের না হয়ে মিশ্র হোক। এই কার্ডগুলো আগে দেওয়া হয়েছে, এগুলো আবার দিও না: ${avoid}`, 900);
  return gjson(txt).filter((x) => typeof x.a === 'string' && typeof x.b === 'string' && x.a.trim() && x.b.trim() && x.a.length <= 20 && x.b.length <= 20 && gnorm(x.a) !== gnorm(x.b))
    .map((x) => ({ a: flat(x.a, 20), b: flat(x.b, 20) }));
}
// ===== নতুন গেম: জোড়া মেলাও (match) ও স্লাইডার (slider) — প্রতিদিন AI কন্টেন্ট =====
const XG_THEMES = ['ধাতু ও রসায়ন', 'বাংলাদেশের ইতিহাস', 'গণিত ও জ্যামিতি', 'বিজ্ঞান ও প্রকৃতি', 'ভূগোল ও পরিবেশ', 'সাধারণ জ্ঞান'];
function xgValidMatch(x) {
  if (!x || typeof x.title !== 'string' || !Array.isArray(x.pairs)) return null;
  const pr = x.pairs
    .filter((p) => p && typeof p.a === 'string' && typeof p.b === 'string' && p.a.trim() && p.b.trim() && p.a.length <= 40 && p.b.length <= 40)
    .map((p) => ({ a: flat(p.a, 40), b: flat(p.b, 40) }));
  const sa = new Set(pr.map((p) => gnorm(p.a))), sb2 = new Set(pr.map((p) => gnorm(p.b)));
  if (pr.length < 3 || pr.length > 5 || sa.size !== pr.length || sb2.size !== pr.length) return null;
  return { title: flat(x.title, 60), pairs: pr };
}
function xgValidSlider(x) {
  if (!x || typeof x.q !== 'string' || !x.q.trim() || x.q.length > 220) return null;
  const mn = Number(x.min), mx = Number(x.max), st = Number(x.step || 1), tg = Number(x.target), tl = Number(x.tol || 0);
  if (![mn, mx, st, tg, tl].every((v) => Number.isInteger(v))) return null;
  if (!(mn < mx) || mx - mn > 100000 || st <= 0 || st > mx - mn) return null;
  if (tg < mn || tg > mx || (tg - mn) % st !== 0) return null;
  if (tl < 0 || tl > Math.max(1, (mx - mn) / 10)) return null;
  return { q: flat(x.q, 220), min: mn, max: mx, step: st, target: tg, tol: tl, unit: typeof x.unit === 'string' ? flat(x.unit, 20) : '' };
}
async function genXgMatch(day, theme) {
  const txt = await claudeText(
    'তুমি স্কুলের শিক্ষার্থীদের জন্য জোড়া মেলানোর খেলার প্রশ্নপ্রণেতা। শুধু JSON দাও, কোনো ব্যাখ্যা নয়: {"items":[{"title":"শিরোনাম","pairs":[{"a":"বাম","b":"ডান"}]}]}। প্রতি সেটে ৩–৪টি জোড়া; প্রতিটি জোড়া নিশ্চিতভাবে সঠিক; ডান পাশের প্রতিটি লেখা আলাদা; প্রতিটি লেখা ৪০ অক্ষরের মধ্যে।',
    `তারিখ ${day}। বিষয়-ভাবনা: ${theme}। ৩টি নতুন সেট দাও; সাধারণ উদাহরণের মতো না হয়ে নতুন ধরনের হোক।`, 1400);
  return gjson(txt).map(xgValidMatch).filter(Boolean);
}
async function genXgSlider(day, theme) {
  const txt = await claudeText(
    'তুমি স্কুলের শিক্ষার্থীদের জন্য স্লাইডার প্রশ্নপ্রণেতা। শুধু JSON দাও: {"items":[{"q":"প্রশ্ন","min":0,"max":100,"step":1,"target":60,"tol":2,"unit":"একক"}]}। min, max, step, target, tol সব পূর্ণসংখ্যা; target অবশ্যই min থেকে শুরু করে step-এর গুণিতক ধাপে থাকবে; সঠিক উত্তর নিশ্চিতভাবে সঠিক হতে হবে।',
    `তারিখ ${day}। বিষয়-ভাবনা: ${theme}। ৩টি নতুন প্রশ্ন দাও (গণনা, বিজ্ঞানের মান, ভূগোলের সংখ্যা ইত্যাদি)।`, 1200);
  return gjson(txt).map(xgValidSlider).filter(Boolean);
}
async function jobXgExt(dry) {
  const day = today();
  const theme = XG_THEMES[new Date(day + 'T00:00:00Z').getUTCDay() % XG_THEMES.length];
  const [mt, sl] = await Promise.all([genXgMatch(day, theme).catch(() => []), genXgSlider(day, theme).catch(() => [])]);
  const out = { ok: true, day, theme, match: mt.length, slider: sl.length };
  if (dry) return { ...out, dry: true };
  if (!mt.length && !sl.length) throw new Error('এআই থেকে কোনো বৈধ নতুন গেম পাওয়া যায়নি');
  // ১৪ দিনের পুরনো কন্টেন্ট বন্ধ — নতুনগুলো চালু থাকবে, তাই গেম প্রতিদিন নতুন দেখায়
  const cut = new Date(Date.now() - 14 * 86400000).toISOString();
  await sb('/rest/v1/xg_ext?created_at=lt.' + encodeURIComponent(cut), { method: 'PATCH', headers: { Prefer: 'return=minimal' }, body: JSON.stringify({ active: false }) }).catch(() => {});
  const rows = [
    ...mt.map((p) => ({ game: 'match', payload: p, source: 'ai' })),
    ...sl.map((p) => ({ game: 'slider', payload: p, source: 'ai' })),
  ];
  await sb('/rest/v1/xg_ext', { method: 'POST', headers: { Prefer: 'return=minimal' }, body: JSON.stringify(rows) });
  return out;
}
async function jobGames(dry) {
  const day = today();
  const theme = gshuf(GTHEMES)[0];
  const oldP = (await sb('/rest/v1/game_puzzles?select=game,q&order=id.desc&limit=2000')) || [];
  const oldM = (await sb('/rest/v1/game_pairs?select=a,b&order=id.desc&limit=2000')) || [];
  const haveQ = new Set(oldP.map((x) => x.game + '|' + gnorm(x.q)));
  const haveA = new Set(oldM.flatMap((x) => [gnorm(x.a), gnorm(x.b)]));
  // এআই-কে শেষ ১৫০টি প্রশ্ন/জোড়া দেখানো হয়, যাতে নতুন কিছু দেয়
  const avoid = (g) => oldP.filter((x) => x.game === g).slice(0, 150).map((x) => x.q).join(' | ') || 'কিছু নেই';
  const avoidPairs = oldM.slice(0, 150).map((x) => x.a).join(' | ') || 'কিছু নেই';
  const [seq, tf, pic, pr] = await Promise.all([
    genSeq(day, avoid('numseq')).catch(() => []), genTF(day, theme, avoid('tf')).catch(() => []),
    genPic(day, theme, avoid('pic')).catch(() => []), genPairs(day, theme, avoidPairs).catch(() => []),
  ]);
  const seen = new Set();
  const puz = [...seq, ...tf, ...pic].filter((x) => { const k = x.game + '|' + gnorm(x.q); if (haveQ.has(k) || seen.has(k)) return false; seen.add(k); return true; });
  const seenA = new Set();
  const prs = pr.filter((x) => { const a = gnorm(x.a), b = gnorm(x.b); if (haveA.has(a) || haveA.has(b) || seenA.has(a) || seenA.has(b)) return false; seenA.add(a); seenA.add(b); return true; });
  const cnt = (g) => puz.filter((x) => x.game === g).length;
  const out = { ok: true, day, theme, numseq: cnt('numseq'), tf: cnt('tf'), pic: cnt('pic'), pairs: prs.length };
  if (dry) return { ...out, dry: true };
  if (!puz.length && !prs.length) throw new Error('এআই থেকে কোনো বৈধ প্রশ্ন পাওয়া যায়নি — একটু পরে আবার চেষ্টা করুন');
  if (puz.length) await sb('/rest/v1/game_puzzles', { method: 'POST', headers: { Prefer: 'return=minimal' }, body: JSON.stringify(puz) });
  if (prs.length) await sb('/rest/v1/game_pairs', { method: 'POST', headers: { Prefer: 'return=minimal' }, body: JSON.stringify(prs) });
  return out;
}

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (!SB || !SK) return res.status(500).json({ error: 'SUPABASE_URL / SUPABASE_SERVICE_KEY সেট করা নেই' });

    // Vercel Cron (GET) — Authorization: Bearer CRON_SECRET
    if (req.method === 'GET') {
      const cs = process.env.CRON_SECRET;
      if (!cs || (req.headers.authorization || '') !== 'Bearer ' + cs) return res.status(401).json({ error: 'unauthorized' });
      const job = (req.query && req.query.job) || '';
      if (job === 'games') return res.json(await jobGames(false));
      if (job === 'xgext') return res.json(await jobXgExt(false));
      if (!WT || !PID) return res.status(500).json({ error: 'WA_TOKEN / WA_PHONE_ID সেট করা নেই' });
      if (job === 'daily') return res.json(await jobDaily(false));
      if (job === 'weekly') return res.json(await jobWeekly(false));
      return res.status(400).json({ error: 'job=daily|weekly|games|xgext দিন' });
    }
    if (req.method !== 'POST') return res.status(405).json({ error: 'method' });

    const u = await authUser(req);
    if (!u) return res.status(401).json({ error: 'লগইন করুন' });
    const b = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    if ((!WT || !PID) && !(b.action === 'run' && (b.job === 'games' || b.job === 'xgext'))) return res.status(500).json({ error: 'সার্ভারে WA_TOKEN / WA_PHONE_ID সেট করা নেই' });

    // স্টুডেন্ট: জমা দেওয়া মাত্র নিজের WhatsApp-এ উত্তরপত্র PDF
    if (b.action === 'result') {
      if (u.is_anonymous || !u.email) return res.status(403).json({ error: 'লগইন প্রয়োজন' });
      const pr = await sb(`/rest/v1/wa_prefs?user_id=eq.${u.id}&select=wa_number,result_on`);
      const p = pr && pr[0];
      if (!p || !p.result_on || !p.wa_number) return res.status(400).json({ error: 'WhatsApp অটো-সেন্ড চালু নেই' });
      const used = await sb(`/rest/v1/usage_log?user_id=eq.${u.id}&kind=eq.wa&day=eq.${today()}&select=id`);
      if ((used || []).length >= 15) return res.status(429).json({ error: 'আজকের সীমা শেষ' });
      const pdf = String(b.pdf || '');
      if (!pdf || pdf.length > 4_000_000) return res.status(400).json({ error: 'PDF ঠিক নেই বা বেশি বড়' });
      const i = await userInfo(u.id);
      try {
        const mid = await uploadPdf(pdf);
        await sendTemplate(
          p.wa_number, TPL.result,
          [i.name, flat(b.exam_title, 120), flat(b.score, 40)],
          { type: 'document', document: { id: mid, filename: 'answer-sheet.pdf' } }
        );
        await sb('/rest/v1/usage_log', { method: 'POST', body: JSON.stringify({ user_id: u.id, kind: 'wa' }) });
        await wlog(u.id, 'result', true, 'ok');
        return res.json({ ok: true });
      } catch (e) { await wlog(u.id, 'result', false, e.message); throw e; }
    }

    // অ্যাডমিন টুলস
    if (!(await isAdmin(u.id))) return res.status(403).json({ error: 'শুধু অ্যাডমিন' });
    if (b.action === 'run') {
      if (b.job === 'daily') return res.json(await jobDaily(!!b.dry));
      if (b.job === 'weekly') return res.json(await jobWeekly(!!b.dry));
      if (b.job === 'games') return res.json(await jobGames(!!b.dry));
      if (b.job === 'xgext') return res.json(await jobXgExt(!!b.dry));
      return res.status(400).json({ error: 'job ভুল' });
    }
    if (b.action === 'test') {
      let to = String(b.to || '').replace(/\D/g, '');
      if (/^01\d{9}$/.test(to)) to = '88' + to;
      if (!/^8801\d{9}$/.test(to)) return res.status(400).json({ error: 'সঠিক নম্বর দিন' });
      await sendTemplate(to, 'hello_world', [], null, 'en_US'); // Meta-র তৈরি করা ডিফল্ট টেমপ্লেট
      return res.json({ ok: true });
    }
    return res.status(400).json({ error: 'অজানা action' });
  } catch (e) {
    return res.status(500).json({ error: String((e && e.message) || e) });
  }
};
