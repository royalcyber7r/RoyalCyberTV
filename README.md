# 📺 royalcyberiptv

**Royal Cyber TV** হলো একটি Android IPTV application, যেখানে HLS/M3U8 প্রযুক্তির মাধ্যমে Live TV channel stream করা যায়।

## ✨ Features

* 📺 300+ Live TV Channels
* ▶️ HLS / M3U8 Streaming
* 🔎 Channel Search
* 📱 Android Mobile Support
* 📺 Android TV Support
* ⚡ Fast Channel Loading
* 🔄 Stream Auto-Reconnect
* 🎬 Fullscreen Video Player
* 🌐 Internet-based Live Streaming

---

## 🎥 Current Test Stream

বর্তমানে `MainActivity.kt`-এ পরীক্ষার জন্য নিচের M3U8 stream ব্যবহার করা হয়েছে:

```text
https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8
```

> **নোট:** M3U8 stream-এর availability পরিবর্তিত হতে পারে। Stream বন্ধ বা পরিবর্তন হলে `MainActivity.kt`-এ নতুন বৈধ stream URL দিতে হবে।

---

## 🛠️ Project Technology

* **Language:** Kotlin
* **Platform:** Android
* **Player:** AndroidX Media3 / ExoPlayer
* **Streaming:** HLS / M3U8
* **Build System:** Gradle
* **CI/CD:** GitHub Actions

---

## 📂 Project Structure

```text
RoyalCyberTV/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── live/
│           │       └── royalcyber/
│           │           └── tv/
│           │               └── MainActivity.kt
│           │
│           ├── res/
│           │   ├── drawable/
│           │   ├── layout/
│           │   ├── mipmap/
│           │   └── values/
│           │
│           └── AndroidManifest.xml
│
├── .github/
│   └── workflows/
│       └── build-apk.yml
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

# 🚀 Build APK with GitHub Actions

Repository-তে project push করার পর GitHub Actions ব্যবহার করে APK build করা যাবে।

### Step 1 — GitHub Repository খুলুন

আপনার **RoyalCyberTV** repository খুলুন।

### Step 2 — Actions খুলুন

উপরের **Actions** tab-এ যান।

### Step 3 — Build APK চালু করুন

**Build APK** অথবা **Build RoyalCyberTV APK** workflow নির্বাচন করুন।

তারপর:

```text
Run workflow
```

চাপুন।

### Step 4 — Build সম্পূর্ণ হওয়ার অপেক্ষা করুন

Workflow সফলভাবে শেষ হলে APK তৈরি হবে।

### Step 5 — Releases খুলুন

Repository-এর:

```text
Releases
```

এ যান।

তারপর সর্বশেষ release খুলুন।

### Step 6 — APK Download করুন

সেখানে:

```text
RoyalCyberTV.apk
```

ফাইলটি ডাউনলোড করুন।

---

# 📦 Stable Latest APK

যদি repository হয়:

```text
royalcyber7r/RoyalCyberTV
```

তাহলে সর্বশেষ release-এর APK-এর direct link:

```text
https://github.com/royalcyber7r/RoyalCyberTV/releases/latest/download/RoyalCyberTV.apk
```

> এই link কাজ করার জন্য GitHub Release-এর asset-এর নাম অবশ্যই `RoyalCyberTV.apk` হতে হবে।

---

# 📱 Android Installation

APK ডাউনলোড করার পর Android device-এ APK install করুন।

যদি Android security warning দেখায়, তাহলে আপনার device-এর অনুমতি অনুযায়ী **Install unknown apps** permission চালু করতে হতে পারে।

---

# 📺 HLS / M3U8 Streaming

Royal Cyber TV-এর Live TV player HLS/M3U8 stream ব্যবহার করে।

উদাহরণ:

```text
https://example.com/live/channel/index.m3u8
```

M3U8 stream সাধারণত Internet connection-এর মাধ্যমে Live video playback-এর জন্য ব্যবহার করা হয়।

---

# 🔄 Channel Update

নতুন channel যোগ করতে অথবা existing channel-এর stream পরিবর্তন করতে `MainActivity.kt`-এর channel configuration/update করতে হবে।

উদাহরণ:

```kotlin
Channel(
    name = "Test Channel",
    url = "https://example.com/live/channel/index.m3u8"
)
```

তারপর পরিবর্তনগুলো GitHub repository-তে push করলে GitHub Actions দিয়ে নতুন APK build করা যাবে।

---

# ⚡ GitHub Actions Workflow

এই project-এর APK build করার জন্য workflow ব্যবহার করা হয়:

```text
.github/workflows/build-apk.yml
```

Workflow-এর কাজ:

1. Repository checkout করা
2. JDK setup করা
3. Gradle setup করা
4. Android project build করা
5. APK তৈরি করা
6. APK-এর নাম `RoyalCyberTV.apk` করা
7. Build artifact তৈরি করা
8. GitHub Release তৈরি করা

---

# 🔐 APK Build Type

বর্তমান project **Debug APK** তৈরি করে।

Debug APK মূলত:

* Development
* Testing
* Personal use
* Android phone testing
* Android TV testing

এর জন্য ব্যবহার করা যায়।

---

# 🏪 Google Play Store

Google Play Store-এ প্রকাশ করার জন্য আলাদা **Release Build** প্রয়োজন।

সাধারণত প্রয়োজন হবে:

* Signed Release APK অথবা AAB
* Release Keystore
* Proper application signing
* Production build configuration
* Google Play Console setup

বর্তমান GitHub Actions workflow শুধুমাত্র testing-এর জন্য Debug APK তৈরি করে।

---

# 📺 Android TV Support

Android TV-তে অ্যাপ চালানোর জন্য project-এ Android TV compatibility এবং TV-friendly UI configuration সঠিকভাবে থাকতে হবে।

TV device-এ testing করার সময়:

* Landscape layout
* Remote/D-pad navigation
* Focus handling
* Large buttons
* TV-compatible launcher configuration

সঠিকভাবে পরীক্ষা করা উচিত।

---

# 🌐 Internet Permission

Live HLS/M3U8 stream চালানোর জন্য Android application-এ Internet permission থাকতে হবে।

`AndroidManifest.xml`-এ:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

থাকা প্রয়োজন।

---

# ⚠️ Important

M3U8 stream-এর URL, server availability, authorization এবং access policy যেকোনো সময় পরিবর্তিত হতে পারে।

কোনো stream কাজ না করলে প্রথমে stream URL এবং server availability পরীক্ষা করুন।

শুধুমাত্র আপনার মালিকানাধীন, অনুমোদিত অথবা বৈধভাবে ব্যবহারের অধিকার থাকা TV stream ব্যবহার করুন।

---

# 👨‍💻 Project Information

**Project Name:** Royal Cyber TV

**Package Name:**

```text
live.royalcyber.tv
```

**Platform:** Android

**Language:** Kotlin

**Streaming:** HLS / M3U8

**Player:** AndroidX Media3 / ExoPlayer

**Channels:** 300+

**Build:** Gradle

**Automation:** GitHub Actions

---

# 📥 Latest APK

**RoyalCyberTV.apk**

```text
https://github.com/royalcyber7r/RoyalCyberTV/releases/latest/download/RoyalCyberTV.apk
```

---

## 📄 License / Usage

এই project-এর source code, application branding, channel list এবং streaming resources ব্যবহারের ক্ষেত্রে সংশ্লিষ্ট মালিকানা ও অনুমতির বিষয়গুলো মেনে চলুন।

---

# ⭐ Royal Cyber TV

**Live TV • Sports • Cricket • Football • Movies • Drama • Web Series**

**Royal Cyber IPTV — Your Entertainment Platform**
