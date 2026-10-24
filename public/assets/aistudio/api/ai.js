// api/ai.js — Vercel Serverless Function (Node) — Google Gemini সংস্করণ (ফ্রি টিয়ারে চলে)
// অ্যাকশন: scanq (অ্যাডমিন, ছবি থেকে প্রশ্ন স্ক্যান) · genq (অ্যাডমিন) · tutor (AI মশাই) · doubt · explain (ভুল উত্তরের ব্যাখ্যা) · aiq_get/aiq_submit (AI ডেইলি কুইজ)
// Env: SUPABASE_URL, SUPABASE_SERVICE_KEY, GEMINI_API_KEY
// ঐচ্ছিক: AI_MODEL_FAST, AI_MODEL_SMART, AI_MODEL_FALLBACK (কমা দিয়ে আলাদা; মূল মডেল ব্যস্ত থাকলে এগুলো চেষ্টা হবে)
const SB = process.env.SUPABASE_URL;
const SK = process.env.SUPABASE_SERVICE_KEY;
const GK = process.env.GEMINI_API_KEY;
const FAST = process.env.AI_MODEL_FAST || 'gemini-3.5-flash-lite';
const SMART = process.env.AI_MODEL_SMART || 'gemini-3.8-flash';
const FALLBACKS = (process.env.AI_MODEL_FALLBACK || 'gemini-3.5-flash-lite,gemini-3.5-flash,gemini-3.8-flash').split(',').map((x) => x.trim()).filter(Boolean);

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
const rpc = (fn, args) => sb('/rest/v1/rpc/' + fn, { method: 'POST', body: JSON.stringify(args) });

async function authUser(req) {
  const t = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  if (!t) return null;
  const r = await fetch(SB + '/auth/v1/user', { headers: { apikey: SK, Authorization: 'Bearer ' + t } });
  if (!r.ok) return null;
  const u = await r.json();
  return u && u.id ? u : null;
}
async function isStaff(id) {
  if (await isAdmin(id)) return true;
  try {
    const r = await sb('/rest/v1/teachers?user_id=eq.' + id + '&status=eq.active&select=user_id');
    return Array.isArray(r) && r.length > 0;
  } catch (e) { return false; }
}
async function isAdmin(id) {
  const r = await sb('/rest/v1/admins?user_id=eq.' + id + '&select=user_id');
  return Array.isArray(r) && r.length > 0;
}

// ---- Gemini কল (আগের claude() এর জায়গায়; ইন্টারফেস একই রাখা হয়েছে) ----
// messages: [{role:'user'|'assistant', content: string | [{type:'text',text}|{type:'image',source:{media_type,data}}]}]
function toParts(content) {
  if (typeof content === 'string') return [{ text: content }];
  return (content || []).map((c) =>
    c.type === 'image'
      ? { inlineData: { mimeType: c.source.media_type || 'image/jpeg', data: c.source.data } }
      : { text: String(c.text || '') }
  );
}
const noThink = new Set(); // যেসব মডেল thinkingConfig নেয় না
async function gcall(model, body) {
  const url = 'https://generativelanguage.googleapis.com/v1beta/models/' + model + ':generateContent';
  const go = (bd) => fetch(url, { method: 'POST', headers: { 'x-goog-api-key': GK, 'content-type': 'application/json' }, body: JSON.stringify(bd) });
  let bd = body;
  if (noThink.has(model)) {
    bd = JSON.parse(JSON.stringify(body));
    delete bd.generationConfig.thinkingConfig;
  }
  let r = await go(bd);
  if (r.status === 400 && bd.generationConfig.thinkingConfig) {
    // কিছু নতুন মডেল thinkingConfig নেয় না — ওটা ছাড়া আবার চেষ্টা
    noThink.add(model);
    const b2 = JSON.parse(JSON.stringify(body));
    delete b2.generationConfig.thinkingConfig;
    b2.generationConfig.maxOutputTokens = Math.max((body.generationConfig.maxOutputTokens || 1000) * 2, 2000);
    r = await go(b2);
  }
  const j = await r.json().catch(() => ({}));
  return { ok: r.ok, status: r.status, j };
}
const retryable = (st, msg) => [404, 429, 500, 502, 503, 504].includes(st) || /no longer available|high demand|overloaded|unavailable|not found|try again later/i.test(msg || '');

async function claude({ model, system, messages, max_tokens = 1200, json = false, think = 0 }) {
  const body = {
    systemInstruction: { parts: [{ text: system }] },
    contents: messages.map((m) => ({ role: m.role === 'assistant' ? 'model' : 'user', parts: toParts(m.content) })),
    generationConfig: {
      maxOutputTokens: max_tokens,
      temperature: json ? 0.7 : 0.5,
      thinkingConfig: { thinkingBudget: think },
      ...(json ? { responseMimeType: 'application/json' } : {}),
    },
  };
  // মূল মডেল ব্যস্ত/অনুপস্থিত হলে নিজে থেকে পরেরটায় যাবে
  const models = [model, ...FALLBACKS].filter((m, i, a) => a.indexOf(m) === i);
  let last = null;
  for (const m of models) {
    const { ok, status, j } = await gcall(m, body);
    if (ok) {
      const c = j.candidates && j.candidates[0];
      const text = ((c && c.content && c.content.parts) || []).map((p) => p.text || '').join('');
      if (text) return text;
      const why = (j.promptFeedback && j.promptFeedback.blockReason) || (c && c.finishReason) || '';
      last = { status: 200, msg: 'AI উত্তর দেয়নি' + (why ? ' (' + why + ')' : '') + ', আবার চেষ্টা করুন' };
      if (why === 'SAFETY' || why === 'PROHIBITED_CONTENT') break;
      continue;
    }
    const msg = (j.error && j.error.message) || 'AI ত্রুটি ' + status;
    last = { status, msg };
    if (!retryable(status, msg)) break;
  }
  if (last && last.status === 429) throw new Error('এখন AI-তে অনেক চাপ (ফ্রি সীমা)। এক মিনিট পরে আবার চেষ্টা করুন।');
  throw new Error((last && last.msg) || 'AI ত্রুটি');
}

const CLS = { 1: 'প্রথম', 2: 'দ্বিতীয়', 3: 'তৃতীয়', 4: 'চতুর্থ', 5: 'পঞ্চম', 6: 'ষষ্ঠ', 7: 'সপ্তম', 8: 'অষ্টম', 9: 'নবম', 10: 'দশম', 11: 'একাদশ', 12: 'দ্বাদশ', 13: 'এডমিশন' };

const TUTOR_SYS = `তুমি "AI মশাই" — "Exam Site by Arghya"-এর স্নেহশীল, ধৈর্যশীল ও বন্ধুসুলভ AI শিক্ষক (মশাই মানে শিক্ষক)। তুমি একটি AI; কেউ জিজ্ঞেস করলে সেটা সত্যি বলবে। বাংলাদেশের NCTB পাঠ্যক্রম অনুযায়ী সহজ বাংলায় শেখাও।
- শিক্ষার্থীকে "তুমি" বলে সম্বোধন করো। আন্তরিক ও উৎসাহব্যঞ্জক থাকো; ভুল করলে বকবে না, ধরিয়ে দিয়ে বুঝিয়ে দেবে।
- আগে ধারণাটা বুঝিয়ে দাও, তারপর উদাহরণ। উত্তর ছোট ও পরিষ্কার রাখো (সাধারণত ২০০ শব্দের মধ্যে)।
- দরকারে ধাপে ধাপে দেখাও। শেষে চাইলে একটি ছোট অনুশীলন প্রশ্ন দিতে পারো।
- পড়াশোনার বাইরের বিষয় হলে বিনয়ের সঙ্গে পড়ার দিকে ফিরিয়ে আনো।
- নিশ্চিত না হলে সেটা বলো; তথ্য বানিয়ে বলো না।`;

const DOUBT_SYS = `তুমি একজন দক্ষ ডাউট সলভার। শিক্ষার্থীর সমস্যাটি ধাপে ধাপে সমাধান করো।
- প্রথমে কী চাওয়া হয়েছে এক লাইনে লেখো, তারপর ব্যবহৃত সূত্র/ধারণা, তারপর ধাপগুলো।
- শেষ লাইনে **চূড়ান্ত উত্তর** বোল্ডে দাও।
- ছবি থাকলে ছবির লেখা/চিত্র পড়ে সমাধান করো; ছবি অস্পষ্ট বা অসম্পূর্ণ হলে সেটা স্পষ্ট বলো।
- সহজ বাংলায় লেখো; ইংরেজি বিষয়ে প্রয়োজনে ইংরেজি ব্যবহার করো।`;

const GENQ_SYS = `তুমি বাংলাদেশের অভিজ্ঞ প্রশ্নপ্রণেতা। শুধু বৈধ JSON অ্যারে আউটপুট দাও — কোনো ভূমিকা, মার্কডাউন বা ব্যাখ্যা নয়।
প্রতিটি আইটেম: {"q":"প্রশ্ন","o":["অপশন১","অপশন২","অপশন৩","অপশন৪"],"a":সঠিক_অপশনের_ইনডেক্স(0-3),"m":1}
নিয়ম:
- বহুনির্বাচনি (MCQ); ঠিক একটি উত্তর সঠিক; বাকিগুলো বিশ্বাসযোগ্য ভুল (distractor)।
- অপশনে "ক)/খ)" ইত্যাদি লেবেল লিখবে না। প্রশ্ন একটি লাইনে।
- সঠিক উত্তরের অবস্থান (a) এলোমেলো রাখো।
- তথ্যগতভাবে নির্ভুল ও পাঠ্যবইভিত্তিক হবে; অস্পষ্ট বা দ্ব্যর্থক প্রশ্ন নয়।
- বাংলা বিষয়ে বাংলা সংখ্যা, ইংরেজি বিষয়ে ইংরেজিতে লেখো।
- "avoid" তালিকার প্রশ্নের মতো প্রশ্ন আবার করবে না।`;

const SCAN_SYS = `তুমি একজন নির্ভুল OCR ও প্রশ্ন-ডিজিটাইজার। ছবিতে ছাপা MCQ প্রশ্নপত্র আছে (বাংলা-ইংরেজি মেশানো; রসায়ন/গণিতের চিহ্নসহ)। ছবির প্রশ্নগুলো হুবহু তুলে এনে শুধু বৈধ JSON অ্যারে দাও — কোনো ভূমিকা, মার্কডাউন বা ব্যাখ্যা নয়।
প্রতিটি আইটেম: {"n":ছবির_প্রশ্ননম্বর_বা_null,"q":"প্রশ্ন","o":["অপশন১","অপশন২","অপশন৩","অপশন৪"],"a":-1,"m":1}
নিয়ম:
- ছবিতে যত সম্পূর্ণ প্রশ্ন দেখা যায় সব, ছবির ক্রমে। কাটা পড়া/অসম্পূর্ণ প্রশ্ন বাদ দাও।
- প্রশ্ন ও অপশনের ভাষা, বানান ও সংখ্যা হুবহু রাখো; ইংরেজি অংশ ইংরেজিতেই থাকবে। প্রশ্ন নম্বর ("41.") ও অপশনের লেবেল ("a)", "b)", "ক)") লিখবে না; নম্বরটি শুধু "n"-এ দেবে।
- রাসায়নিক সূত্র ও এককে সাবস্ক্রিপ্ট/সুপারস্ক্রিপ্ট ইউনিকোডে লেখো (H₂SO₄, NH₃, V₂O₅, mol⁻¹, °C)।
- হাতের কলমের দাগ, কাটাকুটি বা ছোপ উপেক্ষা করে নিচের ছাপা লেখাটি পড়ো। ছাপা লেখা সত্যিই পড়া না গেলে সেই অপশন "" (খালি) রাখো; অনুমান করে বানিয়ে লিখবে না।
- লাইন ভেঙে যাওয়া প্রশ্ন জোড়া লাগিয়ে এক লাইনে লেখো (হাইফেনে ভাঙা শব্দও জোড়া: "কয়-" + "টি" → "কয়টি")।
- ফাঁকা স্থান ("---") থাকলে "---"ই রাখো।
- ঠিক ৪টি অপশন দাও। ছবিতে কম থাকলে বাকিটা "" রাখো; বেশি থাকলে প্রথম ৪টি।
- উত্তর বের করার চেষ্টা করবে না: "a" সবসময় -1, "m" সবসময় 1।`;

const SOLVE_SYS = `তুমি বাংলাদেশের এইচএসসি/এডমিশন মানের অভিজ্ঞ শিক্ষক। নিচের MCQ-গুলোর সঠিক উত্তর বের করো। শুধু বৈধ JSON অ্যারে দাও: [{"i":প্রশ্নের_ক্রমিক,"a":সঠিক_অপশনের_ইনডেক্স,"c":"high|medium|low"}]
- ইনডেক্স: ০=ক(প্রথম), ১=খ, ২=গ, ৩=ঘ।
- "সত্য নয়", "অংশ নয়", "ভুল" ধরনের নেগেটিভ প্রশ্নে বিশেষ সতর্ক থাকো।
- সংখ্যাভিত্তিক প্রশ্নে হিসাব করে মেলাও। বাংলাদেশের পাঠ্যবইয়ের (NCTB) মান ও মানবিক সূত্র অনুসরণ করো।
- নিশ্চিত না হলে "c":"low" দাও, তবু সেরা অনুমানটি দাও।`;

function cleanScan(arr) {
  return (Array.isArray(arr) ? arr : [])
    .filter((x) => x && typeof x.q === 'string' && x.q.trim() && Array.isArray(x.o))
    .map((x) => {
      const o = x.o.slice(0, 4).map((v) => String(v == null ? '' : v).replace(/\s+/g, ' ').trim());
      while (o.length < 4) o.push('');
      return { n: Number.isInteger(x.n) ? x.n : null, q: x.q.replace(/\s+/g, ' ').trim(), o, a: -1, m: 1 };
    });
}

function parseJSON(txt) {
  const a = txt.indexOf('['), b = txt.lastIndexOf(']');
  if (a < 0 || b <= a) throw new Error('AI সঠিক ফরম্যাটে উত্তর দেয়নি, আবার চেষ্টা করুন');
  return JSON.parse(txt.slice(a, b + 1));
}

const EXPLAIN_SYS = `তুমি "AI মশাই" — স্নেহশীল AI শিক্ষক। শুধু বৈধ JSON অ্যারে দাও, কোনো ভূমিকা/মার্কডাউন নয়: [{"i":প্রশ্নের_নম্বর,"e":"ব্যাখ্যা"}]
প্রতিটি ভুল প্রশ্নের জন্য ২–৩ বাক্যে (৬০ শব্দের মধ্যে) সহজ বাংলায় লেখো: কেন সঠিক উত্তরটি সঠিক, আর শিক্ষার্থীর বাছাই করা উত্তরটি কোথায় ভুল। শেষে চাইলে মনে রাখার একটি ছোট টিপস দাও।
- দেওয়া "সঠিক উত্তর"কে সঠিক ধরেই ব্যাখ্যা করো। তবে সেটি নিশ্চিতভাবে ভুল মনে হলে ব্যাখ্যার শুরুতে "⚠️ উত্তরমালাটি আরেকবার যাচাই করুন।" লিখে কারণ বলো।
- সহানুভূতিশীল ও উৎসাহব্যঞ্জক সুরে লেখো। তথ্য বানিয়ে বলো না।`;

const dhakaDay = () => new Date(Date.now() + 6 * 3600e3).toISOString().slice(0, 10);
const cleanQs = (arr) => (Array.isArray(arr) ? arr : [])
  .filter((x) => x && typeof x.q === 'string' && Array.isArray(x.o) && x.o.length === 4 && x.o.every((o) => typeof o === 'string' && o.trim()) && Number.isInteger(x.a) && x.a >= 0 && x.a <= 3)
  .map((x) => ({ q: x.q.replace(/\s+/g, ' ').trim(), o: x.o.map((o) => o.replace(/\s+/g, ' ').trim()), a: x.a, m: 1 }));

// আজকের AI কুইজ (শ্রেণি অনুযায়ী দিনে একটি সেট; প্রথমজন চাইলে তৈরি হয়ে জমা থাকে)
async function getQuiz(cls, day, create) {
  const got = await sb(`/rest/v1/ai_daily_quiz?class_level=eq.${cls}&day=eq.${day}&select=q`);
  if (got && got[0] && Array.isArray(got[0].q) && got[0].q.length) return got[0].q;
  if (!create) return null;
  let n = 5;
  try {
    const c = await sb('/rest/v1/ai_config?key=eq.aiq_count&select=val');
    if (c && c[0]) n = Math.max(1, Math.min(10, parseInt(c[0].val) || 5));
  } catch (e) {}
  const subj = cls >= 13 ? 'সাধারণ জ্ঞান, গণিত, ইংরেজি, বাংলা (এডমিশন পরীক্ষার মান)' : 'শ্রেণির মূল বিষয়গুলো (গণিত, বিজ্ঞান, ইংরেজি, বাংলা, বাংলাদেশ ও বিশ্বপরিচয় ইত্যাদি) থেকে মিশিয়ে';
  const prompt = `শ্রেণি: ${CLS[cls] || cls}\nবিষয়: ${subj}\nকাঠিন্য: মাঝারি\nপ্রশ্নসংখ্যা: ${n}\nতারিখ: ${day} (প্রতিদিন নতুন প্রশ্ন দাও)\nবিভিন্ন বিষয় থেকে একটি করে প্রশ্ন দাও, একই বিষয়ের পুনরাবৃত্তি কম রাখো।\navoid: []`;
  let qs = [];
  for (let k = 0; k < 2 && qs.length < n; k++) {
    try {
      const out = await claude({ model: SMART, system: GENQ_SYS, messages: [{ role: 'user', content: prompt }], max_tokens: 4000, json: true });
      cleanQs(parseJSON(out)).forEach((x) => { if (qs.length < n && !qs.some((y) => y.q === x.q)) qs.push(x); });
    } catch (e) { if (k === 1 && !qs.length) throw e; }
  }
  if (qs.length < Math.min(3, n)) throw new Error('আজকের কুইজ তৈরি হয়নি, একটু পরে আবার চেষ্টা করুন');
  await sb('/rest/v1/ai_daily_quiz', { method: 'POST', headers: { Prefer: 'resolution=ignore-duplicates' }, body: JSON.stringify({ class_level: cls, day, q: qs }) }).catch(() => {});
  const again = await sb(`/rest/v1/ai_daily_quiz?class_level=eq.${cls}&day=eq.${day}&select=q`);
  return (again && again[0] && again[0].q) || qs;
}

module.exports = async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') return res.status(405).json({ error: 'POST দিন' });
  try {
    if (!SB || !SK) return res.status(500).json({ error: 'সার্ভারে SUPABASE_URL / SUPABASE_SERVICE_KEY সেট করা নেই' });
    const u = await authUser(req);
    if (!u) return res.status(401).json({ error: 'লগইন করুন' });
    const b = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
    const act = b.action;

    if (act === 'health') {
      let okAdmin = false;
      try { okAdmin = await isAdmin(u.id); } catch (e) { return res.status(500).json({ error: 'ডেটাবেস পড়া যায়নি (SUPABASE_SERVICE_KEY ভুল?): ' + e.message }); }
      if (!okAdmin) return res.status(403).json({ error: 'শুধু অ্যাডমিন — এই লগইন: ' + (u.email || 'ইমেইল নেই') + ' (' + String(u.id).slice(0, 8) + ')। যদি এটা অ্যাডমিন ইমেইলই হয় তবে SUPABASE_SERVICE_KEY ভুল।' });
      const has = (k) => !!process.env[k];
      return res.json({
        ok: true,
        env: Object.fromEntries(
          ['SUPABASE_URL', 'SUPABASE_SERVICE_KEY', 'GEMINI_API_KEY', 'WA_TOKEN', 'WA_PHONE_ID', 'CRON_SECRET', 'SITE_URL'].map((k) => [k, has(k)])
        ),
      });
    }

    if (!GK) return res.status(500).json({ error: 'সার্ভারে GEMINI_API_KEY সেট করা নেই' });

    if (act === 'genq') {
      if (!(await isStaff(u.id))) return res.status(403).json({ error: 'শুধু অ্যাডমিন বা অনুমোদিত শিক্ষক' });
      const n = Math.max(1, Math.min(8, parseInt(b.count) || 6));
      const avoid = (b.avoid || []).slice(-40).map((s) => String(s).slice(0, 80));
      const prompt = `শ্রেণি: ${CLS[b.class_level] || b.class_level || 'উল্লেখ নেই'}
বিষয়: ${String(b.subject || '').slice(0, 80)}
অধ্যায়/টপিক: ${String(b.chapter || '').slice(0, 120)}
কাঠিন্য: ${String(b.difficulty || 'বার্ষিক পরীক্ষার মান').slice(0, 40)}
প্রশ্নসংখ্যা: ${n}
${b.notes ? 'এই নোট/টেক্সটের ভিত্তিতে বানাও:\n' + String(b.notes).slice(0, 6000) : ''}
avoid: ${JSON.stringify(avoid)}`;
      const out = await claude({ model: SMART, system: GENQ_SYS, messages: [{ role: 'user', content: prompt }], max_tokens: 6000, json: true });
      const arr = parseJSON(out)
        .filter((x) => x && typeof x.q === 'string' && Array.isArray(x.o) && x.o.length === 4 && x.o.every((o) => typeof o === 'string' && o.trim()) && Number.isInteger(x.a) && x.a >= 0 && x.a <= 3)
        .map((x) => ({ q: x.q.replace(/\s+/g, ' ').trim(), o: x.o.map((o) => o.replace(/\s+/g, ' ').trim()), a: x.a, m: Math.max(1, parseInt(x.m) || 1) }));
      return res.json({ questions: arr });
    }

    if (act === 'scanq') {
      if (!(await isStaff(u.id))) return res.status(403).json({ error: 'শুধু অ্যাডমিন বা অনুমোদিত শিক্ষক' });
      const img = typeof b.image === 'string' ? b.image.replace(/^data:[^,]+,/, '') : '';
      if (!img) return res.status(400).json({ error: 'ছবি দিন' });
      if (img.length > 4_000_000) return res.status(413).json({ error: 'ছবি বেশি বড়, ছোট করে আবার দিন' });
      const hint = (b.class_level ? `\nশ্রেণি: ${CLS[b.class_level] || b.class_level}` : '') + (b.subject ? `\nবিষয়: ${String(b.subject).slice(0, 80)}` : '');
      const out = await claude({
        model: SMART, system: SCAN_SYS, max_tokens: 12000, json: true,
        messages: [{ role: 'user', content: [{ type: 'image', source: { media_type: 'image/jpeg', data: img } }, { type: 'text', text: 'এই ছবির সব MCQ প্রশ্ন JSON-এ দাও।' + hint }] }],
      });
      const arr = cleanScan(parseJSON(out));
      if (!arr.length) return res.status(422).json({ error: 'ছবিতে কোনো প্রশ্ন পাওয়া যায়নি। ছবিটি সোজা ও স্পষ্ট করে আবার তুলুন।' });
      let solved = false, note = '';
      if (b.solve !== false) {
        try {
          const sp = arr.map((x, i) => `${i}. ${x.q}\n` + x.o.map((o, k) => `(${'কখগঘ'[k]}) ${o}`).join('\n')).join('\n\n') + hint;
          const so = await claude({ model: SMART, system: SOLVE_SYS, messages: [{ role: 'user', content: sp }], max_tokens: 12000, json: true, think: 2048 });
          parseJSON(so).forEach((r) => {
            if (r && Number.isInteger(r.i) && arr[r.i] && Number.isInteger(r.a) && r.a >= 0 && r.a <= 3) {
              arr[r.i].a = r.a;
              arr[r.i].c = ['high', 'medium', 'low'].includes(r.c) ? r.c : 'medium';
            }
          });
          solved = arr.some((x) => x.a >= 0);
        } catch (e) { note = 'উত্তর বের করা যায়নি: ' + e.message; }
      }
      return res.json({ questions: arr, solved, note });
    }

    if (u.is_anonymous || !u.email) return res.status(403).json({ error: 'এই ফিচারের জন্য ইমেইল/Google অ্যাকাউন্টে লগইন করুন' });

    if (act === 'tutor') {
      let msgs = (b.messages || []).slice(-10).map((m) => ({
        role: m.role === 'assistant' ? 'assistant' : 'user',
        content: String(m.content || '').slice(0, 2000),
      })).filter((m) => m.content);
      while (msgs.length && msgs[0].role !== 'user') msgs.shift();
      if (!msgs.length || msgs[msgs.length - 1].role !== 'user') return res.status(400).json({ error: 'প্রশ্ন লিখুন' });
      const how = await rpc('ai_charge', { p_user: u.id, p_kind: 'tutor' });
      if (how === 'none') return res.status(402).json({ error: 'আজকের ফ্রি মেসেজ শেষ। কয়েন শপ থেকে "মশাই বুস্ট" কিনুন অথবা কাল আবার আসুন।' });
      try {
        const sys = TUTOR_SYS + (b.class_level ? `\nশিক্ষার্থীর শ্রেণি: ${CLS[b.class_level] || b.class_level}` : '');
        const reply = await claude({ model: FAST, system: sys, messages: msgs, max_tokens: 1000 });
        return res.json({ reply, how });
      } catch (e) {
        await rpc('ai_refund', { p_user: u.id, p_kind: 'tutor', p_how: how }).catch(() => {});
        throw e;
      }
    }

    if (act === 'doubt') {
      const q = String(b.question || '').trim().slice(0, 3000);
      const img = typeof b.image === 'string' ? b.image : '';
      if (!q && !img) return res.status(400).json({ error: 'প্রশ্ন লিখুন বা ছবি দিন' });
      if (img.length > 3_000_000) return res.status(413).json({ error: 'ছবি বেশি বড়' });
      const how = await rpc('ai_charge', { p_user: u.id, p_kind: 'doubt' });
      if (how === 'none') return res.status(402).json({ error: 'আজকের ফ্রি ডাউট শেষ এবং যথেষ্ট কয়েন নেই। কয়েন শপ থেকে ডাউট প্যাক কিনুন।' });
      try {
        const content = [];
        if (img) content.push({ type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: img } });
        content.push({ type: 'text', text: (b.subject ? 'বিষয়: ' + String(b.subject).slice(0, 60) + '\n' : '') + (q || 'ছবির প্রশ্নটি সমাধান করো।') });
        const ans = await claude({ model: SMART, system: DOUBT_SYS, messages: [{ role: 'user', content }], max_tokens: 2500 });
        await sb('/rest/v1/doubts', {
          method: 'POST',
          body: JSON.stringify({ user_id: u.id, subject: b.subject || null, question: q || '(ছবি)', answer: ans, had_image: !!img }),
        }).catch(() => {});
        return res.json({ answer: ans, how });
      } catch (e) {
        await rpc('ai_refund', { p_user: u.id, p_kind: 'doubt', p_how: how }).catch(() => {});
        throw e;
      }
    }

    if (act === 'explain') {
      const raw = Array.isArray(b.items) ? b.items.slice(0, 15) : [];
      const items = raw.map((x, i) => ({
        i,
        body: String((x && x.body) || '').slice(0, 500),
        options: (Array.isArray(x && x.options) ? x.options : []).slice(0, 6).map((o) => String(o).slice(0, 200)),
        correct: Number.isInteger(x && x.correct) ? x.correct : -1,
        chosen: Number.isInteger(x && x.chosen) ? x.chosen : -1,
      })).filter((x) => x.body && x.options.length >= 2 && x.correct >= 0 && x.correct < x.options.length);
      if (!items.length) return res.status(400).json({ error: 'ব্যাখ্যা করার মতো প্রশ্ন পাওয়া যায়নি' });
      const how = await rpc('ai_charge', { p_user: u.id, p_kind: 'explain' });
      if (how === 'none') return res.status(402).json({ error: 'আজকের ফ্রি ব্যাখ্যা শেষ এবং যথেষ্ট কয়েন নেই। পরীক্ষা দিয়ে কয়েন জমান, অথবা কাল আবার আসুন।' });
      try {
        const prompt = items.map((x) => `প্রশ্ন ${x.i}: ${x.body}\n` + x.options.map((o, k) => `(${k + 1}) ${o}`).join('\n') + `\nসঠিক উত্তর: (${x.correct + 1}) ${x.options[x.correct]}\nশিক্ষার্থীর উত্তর: ` + (x.chosen >= 0 && x.chosen < x.options.length ? `(${x.chosen + 1}) ${x.options[x.chosen]}` : 'দেওয়া হয়নি')).join('\n\n');
        const out = await claude({ model: SMART, system: EXPLAIN_SYS, messages: [{ role: 'user', content: prompt }], max_tokens: 4000, json: true });
        const arr = parseJSON(out).filter((x) => x && Number.isInteger(x.i) && typeof x.e === 'string' && x.e.trim()).map((x) => ({ i: x.i, e: x.e.trim() }));
        if (!arr.length) throw new Error('ব্যাখ্যা তৈরি হয়নি, আবার চেষ্টা করুন');
        return res.json({ explanations: arr, how });
      } catch (e) {
        await rpc('ai_refund', { p_user: u.id, p_kind: 'explain', p_how: how }).catch(() => {});
        throw e;
      }
    }

    if (act === 'aiq_get' || act === 'aiq_submit') {
      const cls = parseInt(b.class_level);
      if (!(cls >= 1 && cls <= 13)) return res.status(400).json({ error: 'শ্রেণি বেছে নিন' });
      const day = dhakaDay();
      if (act === 'aiq_get') {
        const done = await sb(`/rest/v1/ai_daily_done?user_id=eq.${u.id}&day=eq.${day}&select=score,total`);
        if (done && done[0]) return res.json({ done: true, score: done[0].score, total: done[0].total });
        const quiz = await getQuiz(cls, day, true);
        return res.json({ done: false, day, questions: quiz.map((x) => ({ q: x.q, o: x.o })) });
      }
      const quiz = await getQuiz(cls, day, false);
      if (!quiz) return res.status(400).json({ error: 'আগে আজকের কুইজটি খুলুন' });
      const ans = Array.isArray(b.answers) ? b.answers : [];
      let score = 0;
      quiz.forEach((x, i) => { if (Number.isInteger(ans[i]) && ans[i] === x.a) score++; });
      const coins = await rpc('ai_quiz_award', { p_user: u.id, p_day: day, p_cls: cls, p_score: score, p_total: quiz.length });
      if (coins === -1) return res.status(409).json({ error: 'আজকের কুইজ আগেই জমা দেওয়া হয়েছে। কাল নতুন কুইজ আসবে!' });
      return res.json({ score, total: quiz.length, coins, keys: quiz.map((x) => x.a), questions: quiz.map((x) => ({ q: x.q, o: x.o })) });
    }

    return res.status(400).json({ error: 'অজানা action' });
  } catch (e) {
    return res.status(500).json({ error: String((e && e.message) || e) });
  }
};
