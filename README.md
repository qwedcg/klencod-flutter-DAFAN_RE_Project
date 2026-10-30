# DAFAN RE

تطبيق Android أولي للتحليل الثابت للملفات الثنائية، بواجهة محلية داكنة.

## الميزات الحالية
- فتح الملفات باستخدام Android Storage Access Framework.
- التعرف على ELF/SO وAPK/ZIP وDEX وPE.
- قراءة معلومات ELF الأساسية (32/64-bit، endian، architecture، entry point).
- قراءة حقول أساسية من DEX وPE.
- استخراج ASCII strings.
- عرض Hex dump.
- استعراض أسماء الملفات داخل APK/ZIP.

## حدود الإصدار الأول
هذا ليس بديلاً مكتملاً عن IDA Pro أو Ghidra بعد. لا يحتوي هذا الإصدار على محرك disassembly/decompiler فعلي ولا يفك حماية الملفات. الخطوة التالية هي دمج محرك مثل Rizin/Capstone بعد إعداد مكتبات Android ARM64.

## البناء عبر GitHub من الهاتف
1. أنشئ مستودعاً جديداً باسم `DAFAN-RE`.
2. ارفع جميع ملفات هذا المشروع إلى جذر المستودع.
3. افتح تبويب Actions.
4. شغّل `Build DAFAN RE APK` أو انتظر البناء بعد push.
5. افتح نتيجة التشغيل ثم Artifacts وحمّل `DAFAN-RE-debug`.
6. فك ZIP وثبّت `app-debug.apk`.

## الترخيص
هذا المشروع المبدئي من إعداد DAFAN. عند إضافة محركات خارجية، التزم بتراخيصها وإشعاراتها.
