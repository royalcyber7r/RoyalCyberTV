# Royal Cyber TV

একটি ১-চ্যানেলের Android HLS/M3U8 test app।

## Stream

বর্তমানে MainActivity.kt-এ এই M3U8 URL সেট করা আছে:

`https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8`

## GitHub থেকে APK

Repository-তে push করার পর:

1. GitHub → **Actions** → **Build APK**
2. Workflow শেষ হলে **Releases** খুলুন।
3. Latest release-এর `RoyalCyberTV.apk` ডাউনলোড করুন।

Stable latest APK link:

`https://github.com/USERNAME/REPOSITORY/releases/latest/download/RoyalCyberTV.apk`

`USERNAME/REPOSITORY` আপনার GitHub repository অনুযায়ী বদলাবেন।

## গুরুত্বপূর্ণ

এই project debug APK তৈরি করে। ব্যক্তিগত/টেস্ট ব্যবহারের জন্য এটি সহজ। Play Store-এর জন্য আলাদা signed release/AAB setup প্রয়োজন।
