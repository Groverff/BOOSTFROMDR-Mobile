# BOOSTFROMDR Mobile V1.0

نسخة أولية Android مستقلة لتحليل مستشعر الجيروسكوب وتجربة فلترة One Euro محلياً.

## ما الذي يعمل في V1.0؟
- قراءة Gyroscope الحقيقي عبر Android SensorManager أثناء ظهور التطبيق.
- تمرير المحاور X/Y/Z إلى محرك C++ عبر JNI.
- تطبيق One Euro Filter مستقل على كل محور.
- عرض القيم الخام والمفلترة ومعدل أخذ العينات المقاس.
- مؤشر أولي للفرق بين الإشارة الخام والمفلترة.
- زر إعادة ضبط الفلاتر.
- واجهة Dashboard / Training Preview / Profiles Preview.

## حدود مهمة
- التطبيق لا يقرأ لمس Delta Force ولا يلتقط شاشة اللعبة.
- لا يحقن لمساً أو مدخلات، ولا يعدل إعدادات اللعبة.
- مؤشر Jitter الحالي هو متوسط الفرق بين الإشارة الخام والمفلترة، وليس مقياساً علمياً نهائياً لثبات التصويب.
- لا يوجد نموذج AI في V1.0؛ الفلترة الحالية خوارزمية One Euro حتمية وخفيفة.
- بيانات الجيروسكوب لا تمثل مباشرة حركة التصويب داخل اللعبة، لأن اللعبة قد تستخدم إعدادات ومعالجة داخلية مختلفة.

## متطلبات البناء
- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Android NDK 27.2.12479018
- CMake 3.22.1
- Gradle 8.9
- Android Gradle Plugin 8.7.3

## البناء من الهاتف فقط عبر GitHub Actions
1. أنشئ مستودعاً جديداً على GitHub من متصفح الهاتف.
2. ارفع محتويات هذا المشروع إلى جذر المستودع.
3. افتح تبويب Actions واسمح بتشغيل workflows إن طُلب ذلك.
4. شغّل workflow باسم `Build Android APK`.
5. بعد انتهاء البناء، افتح نتيجة التشغيل ونزّل artifact باسم `BOOSTFROMDR-debug-apk`.
6. فك ضغط الملف وثبّت APK على هاتفك. قد تحتاج السماح بالتثبيت من مصدر المتصفح/مدير الملفات.

## البناء محلياً في بيئة Android مناسبة
من مجلد المشروع:
```bash
gradle assembleDebug
```
الناتج:
`app/build/outputs/apk/debug/app-debug.apk`

## هيكل المشروع
```text
BOOSTFROMDR_Mobile_V1/
├── .github/workflows/android.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/boostfromdr/mobile/
│       │   ├── MainActivity.kt
│       │   └── NativeFilter.kt
│       └── cpp/
│           ├── CMakeLists.txt
│           └── native-lib.cpp
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
