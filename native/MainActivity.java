package com.matibhanga.college;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

    // ===== নেটিভ উপরের বার (লোগোসহ) ও নিচের ট্যাব বার — বাইরের পেজে সবসময় দেখাবে =====
    private LinearLayout topBar, bottomWrap;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Bitmap loadLogo() {
        String[] names = {"public/icon-192.png", "public/icon-512.png", "public/icon.png"};
        for (String n : names) {
            try {
                java.io.InputStream in = getAssets().open(n);
                Bitmap bm = BitmapFactory.decodeStream(in);
                in.close();
                if (bm != null) return bm;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private void setBars(boolean show) {
        int vis = show ? View.VISIBLE : View.GONE;
        if (topBar != null) topBar.setVisibility(vis);
        if (bottomWrap != null) bottomWrap.setVisibility(vis);
    }

    private void updateBars(String url) {
        if (url == null) return;
        if (url.startsWith(HOME)) setBars(false);
        else if (url.startsWith("http")) setBars(true);
    }

    private void goTab(WebView wv, String tab) {
        wv.loadUrl(tab.equals("home") ? HOME : HOME + "#" + tab);
    }

    private void setupNativeBars(final WebView wv) {
        try {
            final ViewGroup parent = (ViewGroup) wv.getParent();
            final int index = parent.indexOfChild(wv);
            parent.removeView(wv);

            final LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);

            // উপরের বার
            topBar = new LinearLayout(this);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0A4D39, 0xFF0F6B4F});
            topBar.setBackground(gd);
            topBar.setPadding(dp(16), dp(10), dp(16), dp(10));

            ImageView logo = new ImageView(this);
            GradientDrawable white = new GradientDrawable();
            white.setColor(Color.WHITE);
            white.setCornerRadius(dp(10));
            logo.setBackground(white);
            logo.setPadding(dp(2), dp(2), dp(2), dp(2));
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Bitmap bm = loadLogo();
            if (bm != null) logo.setImageBitmap(bm);
            topBar.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));

            TextView title = new TextView(this);
            title.setText("মাটিভাঙ্গা ডিগ্রি কলেজ");
            title.setTextColor(Color.WHITE);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            title.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.leftMargin = dp(12);
            topBar.addView(title, tlp);

            // নিচের ট্যাব বার
            bottomWrap = new LinearLayout(this);
            bottomWrap.setOrientation(LinearLayout.VERTICAL);
            bottomWrap.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0A4D39, 0xFF0F6B4F}));
            bottomWrap.setAlpha(1f);
            bottomWrap.setElevation(dp(8));
            View line = new View(this);
            line.setBackgroundColor(0x00000000);
            bottomWrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(4), dp(6), dp(4), dp(6));
            String[][] tabs = {
                {"home", "\uD83C\uDFE0", "হোম"},
                {"results", "\uD83D\uDCCA", "রেজাল্ট"},
                {"classes", "\uD83D\uDCDA", "ক্লাস"},
                {"staff", "\uD83D\uDC69\u200D\uD83C\uDFEB", "শিক্ষক"},
                {"contact", "\uD83D\uDCDE", "যোগাযোগ"}
            };
            for (final String[] t : tabs) {
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                item.setPadding(0, dp(2), 0, dp(2));
                item.setAlpha(1f);
                TextView ic = new TextView(this);
                ic.setText(t[1]);
                ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                ic.setTextColor(0xFF000000); // পুরো গাঢ় রং — ইমোজি আংশিক স্বচ্ছ দেখাত
                ic.setIncludeFontPadding(false);
                ic.setGravity(Gravity.CENTER);
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(Color.WHITE);
                pill.setCornerRadius(dp(14));
                ic.setBackground(pill);
                TextView lb = new TextView(this);
                lb.setText(t[2]);
                lb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                lb.setIncludeFontPadding(true);
                lb.setPadding(0, dp(2), 0, dp(1));
                lb.setSingleLine(true);
                lb.setTextColor(0xFFFFFFFF);
                lb.setTypeface(null, Typeface.BOLD);
                lb.setShadowLayer(3f, 0f, 1f, 0x66000000);
                lb.setAlpha(1f);
                ic.setAlpha(1f);
                lb.setGravity(Gravity.CENTER);
                item.addView(ic, new LinearLayout.LayoutParams(dp(44), dp(28)));
                item.addView(lb, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                item.setOnClickListener(v -> goTab(wv, t[0]));
                row.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            bottomWrap.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(wv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            root.addView(bottomWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            parent.addView(root, index, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // স্ট্যাটাস বার/নেভিগেশন বারের জায়গা (এজ-টু-এজ হলে)
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                topBar.setPadding(dp(16), dp(10) + sb.top, dp(16), dp(10));
                bottomWrap.setPadding(0, 0, 0, sb.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);

            setBars(false); // শুরুতে হোম পেজ — হোমের নিজস্ব বার আছে
        } catch (Throwable t) {
            topBar = null;
            bottomWrap = null;
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

        setupNativeBars(wv);

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
                if (url != null && url.startsWith(HOME)) {
                    clearHistoryNext = true;
                    setBars(false); // হোমে ফেরার শুরুতেই নেটিভ বার লুকানো — নইলে হোমের নিজস্ব বারের সাথে দুইবার দেখায়
                }
            }

            // ব্যাক/হিস্ট্রি দিয়ে হোমে ফিরলেও যেন নেটিভ বার সাথে সাথে লুকায়
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                if (url != null && url.startsWith(HOME)) setBars(false);
            }

            // নতুন পেজ পর্দায় ফুটে ওঠার পরেই বার দেখানো/লুকানো — আগে করলে হোমের বারের সাথে দুইবার দেখায়
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                updateBars(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url == null) return;
                updateBars(url);
                if (url.startsWith(HOME)) {
                    if (clearHistoryNext) { view.clearHistory(); clearHistoryNext = false; }
                } else if (url.startsWith("http")) {
                    view.evaluateJavascript(HIDE_BANNER_JS, null);
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
        public void shareText(final String title, final String text) {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, title);
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, title));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "শেয়ার করা যায়নি", Toast.LENGTH_SHORT).show();
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
