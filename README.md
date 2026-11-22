# মাটিভাঙ্গা ডিগ্রি কলেজ — Android অ্যাপ

GitHub নিজেই APK বানিয়ে দেবে। কম্পিউটারে কিছু ইনস্টল করতে হবে না।

## APK পাওয়ার ধাপ

1. [github.com](https://github.com) এ বিনামূল্যে অ্যাকাউন্ট খুলুন (না থাকলে)।
2. **New repository** চাপুন, নাম দিন `matibhanga-app`, তারপর **Create repository**।
3. নতুন রিপোজিটরির পেজে **uploading an existing file** লিংকে চাপুন।
4. ZIP খুলে ভেতরের **সব ফাইল ও ফোল্ডার** (`www`, `assets`, `.github`, `package.json`, `capacitor.config.json` ইত্যাদি) টেনে এনে ছাড়ুন, তারপর **Commit changes**।
   - `.github` ফোল্ডার আপলোড করতে হবে না (ওটা লুকানো)। বদলে নিচের "workflow ফাইল যোগ করা" ধাপটি করুন।
5. উপরের **Actions** ট্যাবে যান। "Build Android APK" চলতে শুরু করবে (৫–১০ মিনিট)। না চললে বাম পাশ থেকে workflow বেছে **Run workflow** চাপুন।
6. সবুজ ✔ হলে ওই রান খুলুন, নিচে **Artifacts** থেকে `matibhanga-college-apk` ডাউনলোড করুন।
7. ZIP খুললে `app-debug.apk` পাবেন। ফোনে পাঠিয়ে ইনস্টল করুন (অজানা সোর্স থেকে ইনস্টলের অনুমতি দিতে হতে পারে)।

## workflow ফাইল যোগ করা (জরুরি)

1. রিপোজিটরির পেজে **Add file → Create new file** চাপুন।
2. নামের ঘরে হুবহু লিখুন: `.github/workflows/build-apk.yml` (স্ল্যাশ `/` টাইপ করলেই ফোল্ডার তৈরি হবে)।
3. `build-apk-yml-copy-this.txt` ফাইলের সবকিছু কপি করে নিচের বড় ঘরে পেস্ট করুন।
4. **Commit changes** চাপুন। এরপর Actions ট্যাবে build শুরু হবে।

## কনটেন্ট বদলাতে চাইলে

- লিংক, লেখা, ফোন নম্বর: `www/index.html` এডিট করুন (`LINKS` অংশে সব লিংক আছে)।
- Commit করলেই নতুন APK আবার তৈরি হবে।
- আইকন বদলাতে `assets/` এর ৩টি ছবি (১০২৪×১০২৪ PNG) পাল্টান: `icon-only.png`, `icon-foreground.png`, `icon-background.png`।

## Play Store এর জন্য

এই APK "debug" সংস্করণ — ফোনে ইনস্টল ও শেয়ার করার জন্য ঠিক আছে। Play Store এ দিতে হলে signed release (.aab) লাগবে; সেটি আলাদা করে সেটআপ করতে হবে।
