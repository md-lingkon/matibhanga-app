// Service worker: অ্যাপের কাঠামো (shell) অফলাইনে চালু রাখে।
// ডেটা/রেজাল্ট সবসময় লাইভ ইন্টারনেট থেকে আসে (ক্যাশ হয় না)।
const CACHE = "matibhanga-v1";
const SHELL = ["./", "./index.html", "./manifest.json", "./icon-192.png", "./icon-512.png", "./icon-maskable-512.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== location.origin) return; // বাইরের সাইট (Google Script ইত্যাদি) সরাসরি নেটওয়ার্কে

  // নেটওয়ার্ক-ফার্স্ট, ব্যর্থ হলে ক্যাশ
  e.respondWith(
    fetch(req)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(req, copy));
        return res;
      })
      .catch(() => caches.match(req).then((r) => r || caches.match("./index.html")))
  );
});
