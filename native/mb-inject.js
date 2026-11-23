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

  function snapshot(doc) {
    var clone = doc.documentElement.cloneNode(true);

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

    // স্ক্রিপ্ট ও CSP সরানো (স্ন্যাপশটে JS চলবে না)
    var rm = clone.querySelectorAll('script,meta[http-equiv="Content-Security-Policy"],meta[http-equiv="content-security-policy"]');
    for (var n = 0; n < rm.length; n++) rm[n].parentNode.removeChild(rm[n]);

    // প্রিন্টে লেখা যেন কেটে না যায়/লুকানো না থাকে
    var st = doc.createElement('style');
    st.textContent = 'html,body{height:auto!important;min-height:0!important;overflow:visible!important;}' +
      'body{visibility:visible!important;opacity:1!important;}';
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

  function doPrint(doc) {
    doc = doc || document;
    var base = location.href;
    try { if (doc.location && doc.location.href && doc.location.href !== 'about:blank') base = doc.location.href; } catch (e) {}
    var title = (doc.title || document.title || 'document');
    snapshot(doc).then(function (html) {
      A.printHtml(title, base, html);
    }).catch(function () {
      try { A.printHtml(title, base, '<!DOCTYPE html>' + doc.documentElement.outerHTML); } catch (e) {}
    });
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
