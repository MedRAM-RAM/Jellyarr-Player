# Jellyarr

تطبيق أندرويد (Kotlin + Jetpack Compose) يجمع ثلاثة أنواع من المصادر في مكان واحد، بواجهة داكنة مستوحاة من Nuvio وAIOStreams:

| المصدر | ما يقدّمه |
|---|---|
| **Jellyfin** | تصفّح المكتبات، متابعة المشاهدة، الحلقات التالية، المفضلة/تمت المشاهدة، تشغيل مباشر أو Transcode مع مزامنة التقدّم |
| **إضافات Stremio** | أي إضافة عبر رابط `manifest.json` (مثل **AIOStreams**، Torrentio، Cinemeta، OpenSubtitles): كتالوجات، بيانات، روابط، ترجمات |
| **مزوّدات Nuvio** | سكربتات JavaScript بصيغة Nuvio (`getStreams(tmdbId, type, season, episode)`) تعمل داخل WebView مخفي مع `fetch` و`axios` و`cheerio` مبنية على OkHttp |

## المزايا
- **صفحة المصادر بأسلوب AIOStreams**: كل المصادر تُستعلم بالتوازي، مع شارات الجودة (4K/HDR/DV)، الترميز، الصوت، الحجم، المشاركين، وحالة الكاش في Debrid، وفلاتر وترتيب.
- رئيسية بشريط عرض (Hero) وصفوف من Jellyfin وكتالوجات الإضافات.
- استكشاف وبحث موحّد عبر Jellyfin وكل الإضافات.
- مشغّل ExoPlayer: مسارات الصوت والترجمة، ترجمات من الإضافات، السرعة، وضع العرض، النقر المزدوج للتقديم، ومشغّل خارجي (VLC/MX).
- سجل مشاهدة محلي لمحتوى الإضافات.
- دعم روابط `stremio://` للتثبيت المباشر.

## الإعداد
1. **Jellyfin**: الإعدادات ← Jellyfin ← أدخل العنوان وبيانات الدخول.
2. **AIOStreams**: اضبط إعداداتك من صفحة التهيئة (مثل `https://aiostreams.elfhosted.com/stremio/configure`) ثم الصق رابط manifest في: الإعدادات ← إضافات Stremio.
3. **Nuvio**: الإعدادات ← مزوّدات Nuvio ← أضف رابط مستودع (أو المستودع الرسمي)، ثم أضف مفتاح TMDB من إعدادات التشغيل.

## البناء
```bash
./gradlew assembleDebug        # يتطلب Android SDK و debug.keystore في جذر المشروع
```
يبني GitHub Actions ملف APK تلقائياً عند كل push (artifact باسم `jellyarr-debug-apk`).

## ملاحظات
- روابط التورنت الخام (infoHash دون url) تحتاج خدمة Debrid عبر AIOStreams؛ يعرض التطبيق خيار فتح الرابط المغناطيسي بتطبيق خارجي.
- طبقة `cheerio` في مشغّل Nuvio تغطي الدوال الشائعة عبر `DOMParser`، و`crypto-js` غير مضمّنة، فقد لا تعمل بعض المزوّدات.
