# SMOKEFREE Android Kurulum Paketi (APK Oluşturma Rehberi)

Bu arşivde **SMOKEFREE** uygulamasının tam Android Studio projesi, Widget tanımlamaları (4x2, 4x1, 2x2, 2x1) ve otomatik APK derleme yapılandırması yer almaktadır.

## Yöntem 1: Android Studio ile Tek Tıkla APK Alma
1. ZIP dosyasını klasöre çıkarın ve **Android Studio** ile açın.
2. Gradle senkronizasyonu tamamlandıktan sonra üst menüden **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçeneğine tıklayın.
3. Oluşan `app-debug.apk` dosyasını telefonunuza gönderip kurun.

## Yöntem 2: Bilgisayara Program Kurmadan (GitHub Actions ile Otomatik APK)
1. Ücretsiz bir GitHub deposu (repository) oluşturup bu ZIP içindeki dosyaları yükleyin.
2. **Actions** sekmesine tıklayın; `.github/workflows/build-apk.yml` iş akışı otomatik çalışarak `SmokeFree-Debug-APK` kurulum dosyasını (`.apk`) indirmeniz için hazır hale getirecektir.
