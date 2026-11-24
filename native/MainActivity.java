package com.matibhanga.college;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;

public class MainActivity extends BridgeActivity {

    private static final String HOME = "https://localhost/";
    private WebView printView; // সাময়িক প্রিন্ট WebView (GC থেকে বাঁচাতে ধরে রাখা)

    private String readAsset(String name) {
        try {
            java.io.InputStream in = getAssets().open(name);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    // অ্যাপের বাইরের (গুগল ইত্যাদি) পেজে উপরে লোগোসহ বার + নিচে হোম ট্যাব (assets/mb-bars.js থেকে)
    private static final String BARS_TPL =
        "(function () {\n" +
        "  if (document.getElementById('mb-top') || !document.body) return;\n" +
        "  var HOME = '__HOME__', LOGO = '__LOGO__';\n" +
        "  var S = document.createElement('style');\n" +
        "  S.id = 'mb-style';\n" +
        "  S.textContent =\n" +
        "    '#mb-top{position:fixed;top:0;left:0;right:0;z-index:2147483647;box-sizing:border-box;' +\n" +
        "      'background:linear-gradient(135deg,#0a4d39,#0f6b4f);color:#fff;' +\n" +
        "      'padding:calc(10px + env(safe-area-inset-top,0px)) 16px 10px;display:flex;align-items:center;gap:12px;' +\n" +
        "      'min-height:56px;font-family:sans-serif}' +\n" +
        "    '#mb-top .l{width:40px;height:40px;border-radius:10px;background:#fff;padding:2px;box-sizing:border-box;flex:none}' +\n" +
        "    '#mb-top img{width:100%;height:100%;object-fit:contain;display:block}' +\n" +
        "    '#mb-top b{font-size:17px;font-weight:700;line-height:1.3}' +\n" +
        "    '#mb-bot{position:fixed;bottom:0;left:0;right:0;z-index:2147483647;box-sizing:border-box;background:#fff;' +\n" +
        "      'border-top:1px solid #e1e9e5;display:flex;padding:6px 4px calc(6px + env(safe-area-inset-bottom,0px));' +\n" +
        "      'font-family:sans-serif}' +\n" +
        "    '#mb-bot button{flex:1;background:none;border:0;display:flex;flex-direction:column;align-items:center;gap:2px;' +\n" +
        "      'color:#6a7b73;font:600 11px sans-serif;padding:4px 0}' +\n" +
        "    '#mb-bot .i{font-size:21px;line-height:1.2}' +\n" +
        "    'html.mb-fit{padding-top:var(--mb-t,60px)!important;padding-bottom:var(--mb-b,64px)!important;box-sizing:border-box!important}' +\n" +
        "    'iframe.mb-fit{top:var(--mb-t,60px)!important;bottom:auto!important;height:calc(100vh - var(--mb-t,60px) - var(--mb-b,64px))!important}' +\n" +
        "    '@media print{#mb-top,#mb-bot{display:none!important}}';\n" +
        "  (document.head || document.documentElement).appendChild(S);\n" +
        "  var top = document.createElement('div');\n" +
        "  top.id = 'mb-top';\n" +
        "  var lg = document.createElement('div'); lg.className = 'l';\n" +
        "  var im = document.createElement('img'); im.src = LOGO; im.alt = '';\n" +
        "  im.onerror = function () { lg.removeChild(im); lg.style.cssText = 'color:#0f6b4f;font:700 22px sans-serif;display:flex;align-items:center;justify-content:center'; lg.textContent = 'ম'; };\n" +
        "  lg.appendChild(im);\n" +
        "  var tt = document.createElement('b'); tt.textContent = 'মাটিভাঙ্গা ডিগ্রি কলেজ';\n" +
        "  top.appendChild(lg); top.appendChild(tt);\n" +
        "  var bot = document.createElement('div');\n" +
        "  bot.id = 'mb-bot';\n" +
        "  var TABS = [['home','🏠','হোম'],['results','📊','রেজাল্ট'],['classes','📚','ক্লাস'],['staff','👩\\u200D🏫','শিক্ষক'],['contact','📞','যোগাযোগ']];\n" +
        "  TABS.forEach(function (t) {\n" +
        "    var b = document.createElement('button');\n" +
        "    var ic = document.createElement('span'); ic.className = 'i'; ic.textContent = t[1];\n" +
        "    b.appendChild(ic);\n" +
        "    b.appendChild(document.createTextNode(t[2]));\n" +
        "    b.onclick = function () { location.href = HOME + (t[0] === 'home' ? '' : '#' + t[0]); };\n" +
        "    bot.appendChild(b);\n" +
        "  });\n" +
        "  document.body.appendChild(top);\n" +
        "  document.body.appendChild(bot);\n" +
        "  function fit() {\n" +
        "    var root = document.documentElement;\n" +
        "    root.style.setProperty('--mb-t', top.offsetHeight + 'px');\n" +
        "    root.style.setProperty('--mb-b', bot.offsetHeight + 'px');\n" +
        "    root.classList.add('mb-fit');\n" +
        "    var fr = document.querySelectorAll('iframe');\n" +
        "    for (var i = 0; i < fr.length; i++) {\n" +
        "      var f = fr[i];\n" +
        "      if (f.classList.contains('mb-fit')) continue;\n" +
        "      try {\n" +
        "        var cs = getComputedStyle(f);\n" +
        "        if (cs.position === 'absolute' || cs.position === 'fixed') {\n" +
        "          var r = f.getBoundingClientRect();\n" +
        "          if (r.width >= innerWidth * 0.9 && r.height >= innerHeight * 0.7) f.classList.add('mb-fit');\n" +
        "        }\n" +
        "      } catch (e) {}\n" +
        "    }\n" +
        "  }\n" +
        "  fit();\n" +
        "  var n = 0, iv = setInterval(function () { fit(); if (++n > 40) clearInterval(iv); }, 500);\n" +
        "  window.addEventListener('resize', fit);\n" +
        "})();\n" +
        "";

    private String barsJs = null;

    private String readAssetBase64(String name) {
        try {
            java.io.InputStream in = getAssets().open(name);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    // গুগলের "This application was created by a Google Apps Script user" ব্যানার লুকানো
    private static final String HIDE_BANNER_JS =
        "(function(){function f(){var all=document.body?document.body.getElementsByTagName('*'):[];" +
        "for(var i=0;i<all.length;i++){var e=all[i];" +
        "if(e.children.length===0&&/created by a Google Apps Script user/i.test(e.textContent||'')){" +
        "var t=e;while(t.parentElement&&!t.parentElement.querySelector('iframe')){t=t.parentElement;}" +
        "t.style.display='none';return true;}}return false;}" +
        "if(!f()){var n=0,iv=setInterval(function(){if(f()||++n>20)clearInterval(iv);},300);}})();";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebView wv = getBridge().getWebView();

        wv.addJavascriptInterface(new NativeHelper(), "MBAndroid");

        String tpl = BARS_TPL;
        {
            String b64 = readAssetBase64("public/icon-192.png");
            String logo = (b64 != null) ? "data:image/png;base64," + b64 : HOME + "icon-192.png";
            barsJs = tpl.replace("__HOME__", HOME).replace("__LOGO__", logo);
        }

        try {
            String js = readAsset("mb-inject.js");
            if (js != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(wv, js, Collections.singleton("*"));
            }
        } catch (Throwable ignored) { }

        // পেজ লোড শেষে: ভাসমান বোতাম বসানো / হোমে ফিরলে হিস্ট্রি মুছে ফেলা
        getBridge().setWebViewClient(new BridgeWebViewClient(getBridge()) {
            private boolean clearHistoryNext = false;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null && url.startsWith(HOME)) clearHistoryNext = true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url == null) return;
                if (url.startsWith(HOME)) {
                    if (clearHistoryNext) { view.clearHistory(); clearHistoryNext = false; }
                } else if (url.startsWith("http")) {
                    view.evaluateJavascript(HIDE_BANNER_JS, null);
                    if (barsJs != null) view.evaluateJavascript(barsJs, null);
                }
            }
        });

        // সাধারণ ফাইল ডাউনলোড (http/https)
        wv.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (url.startsWith("blob:") || url.startsWith("data:")) return; // JS দিয়ে সামলানো হয়
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                String name = URLUtil.guessFileName(url, contentDisposition, mimeType);
                r.setMimeType(mimeType);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) r.addRequestHeader("Cookie", cookie);
                r.addRequestHeader("User-Agent", userAgent);
                r.setTitle(name);
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                Toast.makeText(this, "ডাউনলোড শুরু হয়েছে", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
            }
        });

        // ফোনের ব্যাক: আগে WebView-এর পেছনে যাবে, না থাকলে অ্যাপ বন্ধ
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                WebView w = getBridge().getWebView();
                if (w.canGoBack()) {
                    w.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /** JavaScript থেকে ডাকা নেটিভ ফাংশন */
    public class NativeHelper {

        @JavascriptInterface
        public void printHtml(final String title, final String baseUrl, final String html, final String info) {
            runOnUiThread(() -> {
                try {
                    final WebView pv = new WebView(MainActivity.this);
                    printView = pv;
                    pv.getSettings().setLoadsImagesAutomatically(true);
                    pv.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    final boolean[] printed = {false};
                    final Runnable doPrint = () -> {
                        if (printed[0]) return;
                        printed[0] = true;
                        try {
                            PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                            PrintDocumentAdapter adapter = pv.createPrintDocumentAdapter(title);
                            pm.print(title, adapter, new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "প্রিন্ট শুরু করা যায়নি", Toast.LENGTH_SHORT).show();
                        }
                    };
                    pv.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            // ছবি/ফন্ট বসার জন্য একটু অপেক্ষা (View.postDelayed নয়, কারণ ভিউটি স্ক্রিনে যুক্ত নয়)
                            h.postDelayed(doPrint, 900);
                        }
                    });
                    // নিরাপত্তা: কোনো কারণে onPageFinished না এলেও ৬ সেকেন্ড পরে প্রিন্ট হবে
                    h.postDelayed(doPrint, 6000);
                    pv.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "প্রিন্ট শুরু করা যায়নি", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void saveFile(String name, String mime, String base64) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                String fileName = fixName(name, mime);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(data);
                    os.close();
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    dir.mkdirs();
                    File f = new File(dir, fileName);
                    FileOutputStream os = new FileOutputStream(f);
                    os.write(data);
                    os.close();
                }
                toast("ডাউনলোড হয়েছে: Downloads ফোল্ডারে (" + fileName + ")");
            } catch (Exception e) {
                toast("ফাইল সংরক্ষণ করা যায়নি");
            }
        }

        private String fixName(String name, String mime) {
            if (name == null || name.trim().isEmpty()) name = "download_" + System.currentTimeMillis();
            if (!name.contains(".")) {
                if (mime.contains("pdf")) name += ".pdf";
                else if (mime.contains("png")) name += ".png";
                else if (mime.contains("jpeg") || mime.contains("jpg")) name += ".jpg";
                else if (mime.contains("csv")) name += ".csv";
                else if (mime.contains("spreadsheetml")) name += ".xlsx";
                else if (mime.contains("wordprocessingml")) name += ".docx";
                else if (mime.contains("html")) name += ".html";
                else if (mime.contains("text")) name += ".txt";
            }
            return name;
        }

        private void toast(final String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
        }
    }
}
