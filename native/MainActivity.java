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

        String tpl = readAsset("mb-bars.js");
        if (tpl != null) {
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
