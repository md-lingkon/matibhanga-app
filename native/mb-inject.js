(function () {
  if (window.__mbInit || !window.MBAndroid) return;
  window.__mbInit = true;
  var A = window.MBAndroid;

  /* ---------- প্রিন্ট: পেজের স্ন্যাপশট নিয়ে নেটিভ প্রিন্টে পাঠানো ---------- */
  function toDataUrl(blob) {
    return new Promise(function (res) {
      var r = new FileReader();
      r.onloadend = function () { res(String(r.result)); };
      r.readAsDataURL(blob);
    });
  }

  var STRIP_PRINT_CSS = false; // সাইটের নিজস্ব প্রিন্ট-CSS রাখা হবে
  var stripped = 0;

  function isPrintOnly(mt) { return /print/i.test(mt) && !/screen|all/i.test(mt); }

  function snapshot(doc) {
    stripped = 0;
    var clone = doc.documentElement.cloneNode(true);

    // এনিমেশন/ফেড-ইন করা উপাদান: স্ক্রিনে এখন যেমন দেখাচ্ছে (শেষ অবস্থা) সেটাই স্থির করে দেওয়া
    try {
      var liveAll = doc.querySelectorAll('*');
      var cloneAll = clone.querySelectorAll('*');
      var win2 = doc.defaultView || window;
      if (liveAll.length === cloneAll.length) {
        for (var li = 0; li < liveAll.length; li++) {
          var cs0 = win2.getComputedStyle(liveAll[li]);
          if (cs0.animationName && cs0.animationName !== 'none') {
            cloneAll[li].style.cssText += ';animation:none!important;opacity:' + cs0.opacity +
              '!important;transform:' + cs0.transform + '!important;visibility:' + cs0.visibility + '!important';
          }
        }
      }
    } catch (e) {}

    // ফর্মের মান ধরে রাখা
    var s = doc.querySelectorAll('input,textarea,select');
    var d = clone.querySelectorAll('input,textarea,select');
    for (var i = 0; i < s.length; i++) {
      var a = s[i], b = d[i];
      if (!b) continue;
      try {
        if (a.tagName === 'TEXTAREA') b.textContent = a.value;
        else if (a.tagName === 'SELECT') {
          for (var j = 0; j < a.options.length; j++) {
            if (a.options[j].selected) b.options[j].setAttribute('selected', 'selected');
            else b.options[j].removeAttribute('selected');
          }
        } else if (a.type === 'checkbox' || a.type === 'radio') {
          if (a.checked) b.setAttribute('checked', 'checked'); else b.removeAttribute('checked');
        } else b.setAttribute('value', a.value);
      } catch (e) {}
    }

    // canvas (চার্ট/QR/ব্যারকোড) → ছবি
    var sc = doc.querySelectorAll('canvas');
    var dc = clone.querySelectorAll('canvas');
    for (var k = 0; k < sc.length; k++) {
      try {
        var im = doc.createElement('img');
        im.src = sc[k].toDataURL('image/png');
        im.style.cssText = 'width:' + sc[k].clientWidth + 'px;height:' + sc[k].clientHeight + 'px';
        dc[k].parentNode.replaceChild(im, dc[k]);
      } catch (e) {}
    }

    // CSS রুল (CSSOM) সহ স্টাইল ধরে রাখা — JS দিয়ে বসানো স্টাইলও আসবে
    try {
      var os = doc.querySelectorAll('style'), cs = clone.querySelectorAll('style');
      for (var q = 0; q < os.length; q++) {
        var sh = os[q].sheet;
        if (sh && cs[q]) {
          try {
            var t = '', rules = sh.cssRules;
            for (var z = 0; z < rules.length; z++) {
              var ru = rules[z];
              if (STRIP_PRINT_CSS && ru.type === 4 && isPrintOnly(ru.media.mediaText)) { stripped++; continue; }
              t += ru.cssText + '\n';
            }
            if (t) cs[q].textContent = t;
          } catch (e) {}
        }
      }
      if (doc.adoptedStyleSheets && doc.adoptedStyleSheets.length) {
        var t2 = '';
        for (var y = 0; y < doc.adoptedStyleSheets.length; y++) {
          try {
            var rr = doc.adoptedStyleSheets[y].cssRules;
            for (var x = 0; x < rr.length; x++) t2 += rr[x].cssText + '\n';
          } catch (e) {}
        }
        if (t2) {
          var as = doc.createElement('style'); as.textContent = t2;
          var hd = clone.querySelector('head'); if (hd) hd.appendChild(as);
        }
      }
    } catch (e) {}

    // শুধু-প্রিন্ট মিডিয়ার স্টাইল/লিংক বাদ (সাইটের প্রিন্ট-CSS পেজ লুকিয়ে ফেললে ফাঁকা আসে)
    var pm = clone.querySelectorAll('style[media],link[media]');
    for (var m2 = 0; m2 < pm.length; m2++) {
      if (STRIP_PRINT_CSS && isPrintOnly(pm[m2].getAttribute('media') || '')) { pm[m2].parentNode.removeChild(pm[m2]); stripped++; }
    }
    // ইনলাইন ইভেন্ট হ্যান্ডলার (onload ইত্যাদি) সরানো
    var all = clone.querySelectorAll('*');
    for (var e2 = 0; e2 < all.length; e2++) {
      var at = all[e2].attributes;
      for (var f2 = at.length - 1; f2 >= 0; f2--) {
        if (/^on/i.test(at[f2].name)) all[e2].removeAttribute(at[f2].name);
      }
    }

    // অ্যাপের উপরের বার/নিচের হোম ট্যাব প্রিন্টে যাবে না
    ['mb-top','mb-bot','mb-style'].forEach(function (id) {
      var el = clone.querySelector('#' + id);
      if (el && el.parentNode) el.parentNode.removeChild(el);
    });
    clone.classList.remove('mb-fit');
    var mf = clone.querySelectorAll('.mb-fit');
    for (var mi = 0; mi < mf.length; mi++) mf[mi].classList.remove('mb-fit');

    // স্ক্রিপ্ট ও CSP সরানো (স্ন্যাপশটে JS চলবে না)
    var rm = clone.querySelectorAll('script,meta[http-equiv="Content-Security-Policy"],meta[http-equiv="content-security-policy"]');
    for (var n = 0; n < rm.length; n++) rm[n].parentNode.removeChild(rm[n]);

    // প্রিন্টে লেখা যেন কেটে না যায়/লুকানো না থাকে
    var st = doc.createElement('style');
    st.textContent = 'html,body{height:auto!important;min-height:0!important;overflow:visible!important;}' +
      'body{visibility:visible!important;opacity:1!important;}' +
      '*{animation-duration:0s!important;animation-delay:0s!important;animation-fill-mode:both!important;transition:none!important;}';
    var head = clone.querySelector('head');
    if (head) head.appendChild(st);

    // blob: ছবি → data: (নতুন ভিউতে blob চলে না)
    var tasks = [];
    var imgs = clone.querySelectorAll('img[src^="blob:"]');
    imgs.forEach(function (img) {
      tasks.push(fetch(img.getAttribute('src')).then(function (r) { return r.blob(); })
        .then(toDataUrl).then(function (u) { img.setAttribute('src', u); }).catch(function () {}));
    });

    return Promise.all(tasks).then(function () {
      return '<!DOCTYPE html>' + clone.outerHTML;
    });
  }

  function textLen(d) {
    try { return ((d.body && d.body.innerText) || '').replace(/\s+/g, '').length; } catch (e) { return 0; }
  }

  // নিজের লেখা কম হলে ভিতরের (একই-অরিজিন) ফ্রেমগুলো থেকে সবচেয়ে বেশি লেখাওয়ালাটি নেওয়া
  function bestDoc(doc) {
    var best = doc, bestLen = textLen(doc);
    if (bestLen >= 30) return best;
    var fr = doc.querySelectorAll('iframe');
    for (var i = 0; i < fr.length; i++) {
      try {
        var d = fr[i].contentDocument;
        if (d && d.body) {
          var r = bestDoc(d), l = textLen(r);
          if (l > bestLen) { best = r; bestLen = l; }
        }
      } catch (e) {}
    }
    return best;
  }

  function doPrint(doc) {
    doc = doc || document;
    var win = doc.defaultView || window;
    // সাইট প্রিন্টের ঠিক আগে কন্টেন্ট বসিয়ে পরে সরিয়ে ফেলে, তাই এখনই (সিঙ্ক্রোনাসলি) কপি নিতে হবে
    try { win.dispatchEvent(new Event('beforeprint')); } catch (e) {}
    var target = bestDoc(doc);
    var base = location.href;
    try { if (target.location && target.location.href && target.location.href !== 'about:blank') base = target.location.href; } catch (e) {}
    var title = (target.title || document.title || 'document');
    var info = 'লেখা=' + textLen(target) + (target !== doc ? ' (ফ্রেম)' : '');
    snapshot(target).then(function (html) {
      A.printHtml(title, base, html, info + ' প্রিন্ট-CSS বাদ=' + stripped);
    }).catch(function () {
      try { A.printHtml(title, base, '<!DOCTYPE html>' + target.documentElement.outerHTML, info); } catch (e) {}
    });
    try { win.dispatchEvent(new Event('afterprint')); } catch (e) {}
  }

  window.print = function () { doPrint(document); };

  /* ---------- window.open('') দিয়ে প্রিন্ট-পপআপ বানানো সাইটের জন্য ---------- */
  var nativeOpen = window.open;
  window.open = function (u) {
    if (!u || u === 'about:blank') {
      var fr = document.createElement('iframe');
      fr.style.cssText = 'position:fixed;left:-10000px;top:0;width:800px;height:600px;border:0;visibility:hidden';
      (document.body || document.documentElement).appendChild(fr);
      var w = fr.contentWindow;
      try {
        w.print = function () { doPrint(fr.contentDocument); };
        w.focus = function () {};
        w.close = function () { setTimeout(function () { try { fr.parentNode.removeChild(fr); } catch (e) {} }, 60000); };
      } catch (e) {}
      return w;
    }
    return nativeOpen.apply(window, arguments);
  };

  /* ---------- blob/data ডাউনলোড ---------- */
  var blobs = {};
  var oc = URL.createObjectURL;
  URL.createObjectURL = function (o) { var u = oc.call(URL, o); blobs[u] = o; return u; };

  function save(name, blob) {
    toDataUrl(blob).then(function (d) {
      A.saveFile(name || '', blob.type || 'application/octet-stream', d.substring(d.indexOf(',') + 1));
    });
  }

  function handle(a) {
    var h = a.href || '';
    var n = a.getAttribute('download') || '';
    if (h.indexOf('blob:') === 0) {
      var b = blobs[h];
      if (b) save(n, b);
      else fetch(h).then(function (x) { return x.blob(); }).then(function (bb) { save(n, bb); });
      return true;
    }
    if (h.indexOf('data:') === 0 && a.hasAttribute('download')) {
      fetch(h).then(function (x) { return x.blob(); }).then(function (bb) { save(n, bb); });
      return true;
    }
    return false;
  }

  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest ? e.target.closest('a') : null;
    if (a && handle(a)) { e.preventDefault(); e.stopPropagation(); }
  }, true);

  var ac = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    if (handle(this)) return;
    return ac.apply(this, arguments);
  };
})();
