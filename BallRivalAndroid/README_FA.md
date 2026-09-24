# Ball Rival Android

نسخه کم‌مصرف گوی‌های شناور برای گوشی و تبلت اندروید.

## رفتار برنامه
- همه گوی‌ها در یک Canvas رسم می‌شوند؛ برای هر گوی View/Window جدا ساخته نمی‌شود.
- هیچ انیمیشن 60FPS وجود ندارد. منطق فقط 5 بار در ثانیه وضعیت را بررسی می‌کند و فقط هنگام تغییر واقعی redraw می‌شود.
- گوی‌ها ثابت می‌مانند و پس از زمان تصادفی مستقیماً به جای دیگری می‌پرند.
- Overlay کاملاً Touch-through است؛ PDF، مرورگر و برنامه زیر آن قابل لمس هستند.
- تعداد، اندازه، زمان ماندن و شفافیت قابل تنظیم است.
- گوشی و تبلت پشتیبانی می‌شوند.

## ساده‌ترین روش ساخت APK
1. Android Studio را نصب کن.
2. پوشه BallRivalAndroid را Open کن.
3. صبر کن Gradle Sync کامل شود. اگر SDK 35 خواست Install را بزن.
4. Build > Build App Bundle(s) / APK(s) > Build APK(s)
5. APK در app/build/outputs/apk/debug/app-debug.apk است.

## اجرای APK
1. نصب APK.
2. برنامه را باز کن و Start / Apply Overlay را بزن.
3. بار اول مجوز Display over other apps را روشن کن.
4. برگرد و دوباره Start / Apply Overlay را بزن.
