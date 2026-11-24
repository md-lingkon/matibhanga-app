(function () {
  if (document.getElementById('mb-top') || !document.body) return;
  var HOME = '__HOME__', LOGO = '__LOGO__';

  var S = document.createElement('style');
  S.id = 'mb-style';
  S.textContent =
    '#mb-top{position:fixed;top:0;left:0;right:0;z-index:2147483647;box-sizing:border-box;' +
      'background:linear-gradient(135deg,#0a4d39,#0f6b4f);color:#fff;' +
      'padding:calc(10px + env(safe-area-inset-top,0px)) 16px 10px;display:flex;align-items:center;gap:12px;' +
      'min-height:56px;font-family:sans-serif}' +
    '#mb-top .l{width:40px;height:40px;border-radius:10px;background:#fff;padding:2px;box-sizing:border-box;flex:none}' +
    '#mb-top img{width:100%;height:100%;object-fit:contain;display:block}' +
    '#mb-top b{font-size:17px;font-weight:700;line-height:1.3}' +
    '#mb-bot{position:fixed;bottom:0;left:0;right:0;z-index:2147483647;box-sizing:border-box;background:#fff;' +
      'border-top:1px solid #e1e9e5;display:flex;padding:6px 4px calc(6px + env(safe-area-inset-bottom,0px));' +
      'font-family:sans-serif}' +
    '#mb-bot button{flex:1;background:none;border:0;display:flex;flex-direction:column;align-items:center;gap:2px;' +
      'color:#6a7b73;font:600 11px sans-serif;padding:4px 0}' +
    '#mb-bot .i{font-size:21px;line-height:1.2}' +
    'html.mb-fit{padding-top:var(--mb-t,60px)!important;padding-bottom:var(--mb-b,64px)!important;box-sizing:border-box!important}' +
    'iframe.mb-fit{top:var(--mb-t,60px)!important;bottom:auto!important;height:calc(100vh - var(--mb-t,60px) - var(--mb-b,64px))!important}' +
    '@media print{#mb-top,#mb-bot{display:none!important}}';
  (document.head || document.documentElement).appendChild(S);

  var top = document.createElement('div');
  top.id = 'mb-top';
  var lg = document.createElement('div'); lg.className = 'l';
  var im = document.createElement('img'); im.src = LOGO; im.alt = '';
  im.onerror = function () { if (im.src !== HOME + 'icon-192.png') im.src = HOME + 'icon-192.png'; };
  lg.appendChild(im);
  var tt = document.createElement('b'); tt.textContent = 'মাটিভাঙ্গা ডিগ্রি কলেজ';
  top.appendChild(lg); top.appendChild(tt);

  var bot = document.createElement('div');
  bot.id = 'mb-bot';
  var TABS = [['home','🏠','হোম'],['results','📊','রেজাল্ট'],['classes','📚','ক্লাস'],['staff','👩\u200D🏫','শিক্ষক'],['contact','📞','যোগাযোগ']];
  TABS.forEach(function (t) {
    var b = document.createElement('button');
    b.innerHTML = '<span class="i">' + t[1] + '</span>' + t[2];
    b.onclick = function () { location.href = HOME + (t[0] === 'home' ? '' : '#' + t[0]); };
    bot.appendChild(b);
  });

  document.body.appendChild(top);
  document.body.appendChild(bot);

  function fit() {
    var root = document.documentElement;
    root.style.setProperty('--mb-t', top.offsetHeight + 'px');
    root.style.setProperty('--mb-b', bot.offsetHeight + 'px');
    root.classList.add('mb-fit');
    // পুরো স্ক্রিন জুড়ে থাকা (absolute/fixed) ফ্রেম — যেমন Google Apps Script-এর ফ্রেম — নিচে নামানো
    var fr = document.querySelectorAll('iframe');
    for (var i = 0; i < fr.length; i++) {
      var f = fr[i];
      if (f.classList.contains('mb-fit')) continue;
      try {
        var cs = getComputedStyle(f);
        if (cs.position === 'absolute' || cs.position === 'fixed') {
          var r = f.getBoundingClientRect();
          if (r.width >= innerWidth * 0.9 && r.height >= innerHeight * 0.7) f.classList.add('mb-fit');
        }
      } catch (e) {}
    }
  }
  fit();
  var n = 0, iv = setInterval(function () { fit(); if (++n > 40) clearInterval(iv); }, 500);
  window.addEventListener('resize', fit);
})();
