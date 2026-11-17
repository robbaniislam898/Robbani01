# Imo AI Auto Reply (Android starter)

## কীভাবে কাজ করে
- Android `NotificationListenerService` দিয়ে notification পড়ে।
- notification-এ free-form `RemoteInput` Reply action থাকলে সেটি ব্যবহার করে।
- মেসেজ ও sender-কে আপনার HTTPS AI backend-এ পাঠায়।
- backend থেকে `{"reply":"..."}` পেলে inline reply পাঠানোর চেষ্টা করে।
- একই notification/message ১০ মিনিটের মধ্যে দ্বিতীয়বার প্রসেস না করার জন্য in-memory duplicate guard আছে।

## গুরুত্বপূর্ণ সীমাবদ্ধতা
1. এটি Imo-তে স্ক্রিনে গিয়ে মেসেজে ক্লিক করে না। Notification-এর Reply action ব্যবহার করে।
2. Imo-এর আপনার ইনস্টল করা সংস্করণে inline Reply action না থাকলে এটি উত্তর পাঠাতে পারবে না।
3. Android/Imo আপডেট, notification privacy, chat type বা account state অনুযায়ী কাজ নাও করতে পারে।
4. প্রথমে নিজের/বিশ্বস্ত পরীক্ষামূলক চ্যাটে পরীক্ষা করুন। Auto Reply চালু থাকলে ভুল বা অসম্পূর্ণ AI উত্তর পাঠাতে পারে।
5. Notification-এ সংবেদনশীল মেসেজ আসতে পারে। নিজস্ব HTTPS backend ব্যবহার করুন; লগে মেসেজ সংরক্ষণ করবেন না।
6. অ্যাপটি কেবল আপনার অনুমতি দেওয়া Notification Access ব্যবহার করে। Accessibility/গোপন স্ক্রিন-নিয়ন্ত্রণ ব্যবহার করে না।

## Android Studio
1. Android Studio-তে এই ফোল্ডার Open করুন।
2. Gradle sync সম্পন্ন করুন।
3. ফোনে Run করুন।
4. অ্যাপের `AI সার্ভারের HTTPS URL`-এ `https://YOUR_SERVER/reply` দিন।
5. `Notification Access চালু করুন` চাপুন এবং Android Settings-এ অ্যাপটিকে অনুমতি দিন।
6. প্রথমে Auto Reply OFF রেখে Imo notification পরীক্ষা করুন। Reply action থাকলে পরে ON করুন।

## Backend চালানো
Python 3.10+ প্রয়োজন।
```bash
cd backend
pip install -r requirements.txt
export AI_API_KEY="YOUR_SECRET_API_KEY"
export AI_BASE_URL="https://api.openai.com/v1/chat/completions"
export AI_MODEL="gpt-4o-mini"
uvicorn server:app --host 0.0.0.0 --port 8000
```
Production-এ অবশ্যই HTTPS reverse proxy ব্যবহার করুন। API key শুধু backend environment-এ রাখুন, Android app-এ নয়।

## আরও কাজ বাকি
এটি একটি starter project; production release-এর আগে authentication, rate limit, persistent deduplication, opt-in contact allowlist, privacy policy, backend TLS/domain, error UI এবং বাস্তব Imo device testing যোগ করুন।


## শুধু Android ফোন দিয়ে APK বানানোর বিকল্প (GitHub Actions)
এই প্রজেক্টে `.github/workflows/android.yml` যোগ করা আছে। GitHub-এ প্রজেক্ট আপলোড করে Actions থেকে **Build Imo AI Auto Reply APK → Run workflow** চালালে GitHub-এর cloud runner APK build করবে।

1. মোবাইল ব্রাউজারে GitHub-এ sign in করে New repository তৈরি করুন।
2. ZIP extract করে প্রজেক্টের সব ফাইল repository-তে upload করুন (`.github` folder-সহ)।
3. Repository-র Actions tab খুলুন।
4. **Build Imo AI Auto Reply APK** workflow নির্বাচন করে **Run workflow** চাপুন।
5. কাজ শেষ হলে workflow run-এর Artifacts অংশ থেকে `ImoAIAutoReply-debug-apk` ডাউনলোড করুন; ZIP extract করলে `app-debug.apk` পাবেন।
6. APK ইনস্টল করার আগে Android-এর নির্দেশনা অনুযায়ী এই APK ইনস্টল করার অনুমতি দিন।

নোট: GitHub Actions account-এর usage limit ও repository settings-এর ওপর নির্ভরশীল। Build সফল হলেও Imo notification-এ inline Reply action আছে কি না ফোনে পরীক্ষা করতে হবে।
