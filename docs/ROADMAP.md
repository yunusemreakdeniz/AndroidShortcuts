# Kısayollar — Yol haritası

Hedef: Apple Kestirmeler (Shortcuts) ile yapılabilen her şeyi Android'de yapabilmek.
Temel: [OpenTasker](https://github.com/SysAdminDoc/OpenTasker) (MIT). Tasarım: Kısayollarım, Düzenleyici, Eylem ekle, Otomasyon, Galeri.

İşaretler: `[x]` bitti · `[~]` motor var, Kestirmeler tarzı arayüz/kolaylık eksik · `[ ]` yok.
Her döngü turu bu listeden sıradaki işleri alır; biten satır işaretlenir.

## A. Temel ve kimlik
- [x] OpenTasker'ı geçmişiyle taşı, CI (derleme + 3100+ test + lint), release
- [x] Diyalog adımları: Metin sor, Menüden seç, Uyarı göster, Sonucu göster
- [x] Kendi paket adı (`io.github.yunusemreakdeniz.shortcuts`) ve uygulama adı "Kısayollar"
- [ ] Kalıcı release imzası (repo sırları; kullanıcı izni gerekiyor)
- [ ] Türkçe arayüz (`values-tr`, ~2.800 metin; %80 eşiği)
- [x] Uygulama simgesi, tasarım renkleri (tema), APK adı

## B. Arayüz (tasarıma göre)
- [x] Kısayollarım: renkli kart ızgarası, tek dokunuşla çalıştır, açılış sekmesi
- [ ] Kısayollarım: arama ve sıralama
- [ ] Kısayol simgesi ve rengi seçici
- [ ] Düzenleyici: blok görünümü, değişkenler renkli etiket, sürükle-bırak sıralama
- [ ] Eylem ekle paneli: kategoriler, arama, önerilenler
- [ ] Otomasyon sekmesi: "Ne zaman → ne yap" kartları, aç/kapat
- [ ] Galeri: hazır kısayollar ve koleksiyonlar, tek dokunuşla ekle
- [x] Ana ekrana ekle: düzenleyici ⋮ ve uzun basma menüsünde; simge kısayolun kendi renk ve simgesi
- [x] Ana ekran widget'ı: kısayol ızgarası (renkli kartlar, dokununca çalışır)
- [~] Hızlı Ayarlar kutucukları (4 yuva var; kısayol seçimi kolaylaştırılacak)

## C. Girdi ve çıktı
- [x] Metin sor (metin, sayı, URL, çok satır)
- [x] Menüden seç
- [x] Listeden seç (çoklu seçim)
- [x] Tarih/saat sor
- [x] Dikte et (konuşmayı metne çevir)
- [x] Paylaş menüsünden girdi: Paylaş → "Kısayolla çalıştır" → kısayol seç; metin/bağlantı {{ ShortcutInput }} olur
- [~] Fotoğraf / dosya / kişi seç: Fotoğraf seç eklendi (Photos/ altına kopyalar); dosya ve kişi seçici sırada
- [x] Sonucu göster, Uyarı göster
- [~] Bildirim göster, Sesli oku (var)
- [ ] Hızlı bakış (dosya/görsel önizleme)

## D. Değişkenler ve veri
- [~] Sihirli değişkenler (çıktı değişkenleri var; otomatik adlandırma ve etiket arayüzü)
- [~] Metin: birleştir, böl, değiştir, eşleştir (var)
- [~] Liste/dizi, JSON/sözlük okuma (var)
- [ ] Sözlük oluştur / değer ata
- [x] Hesapla (matematik ifadesi)
- [ ] Sayı biçimlendir
- [~] Tarih biçimlendir / ekle (var)
- [x] Rastgele sayı
- [ ] Listeden rastgele öğe
- [ ] Base64/URL kodlama, hash (var.convert kısmen)

## E. Akış
- [~] Eğer / değilse, Her biri için tekrarla, Bekle, Durdur, Kısayol çalıştır (motor var)
- [ ] N kez tekrarla
- [ ] Menü dalları (seçeneğe göre farklı adımlar, tek blokta)
- [ ] Kısayola girdi verip çıktısını alma (task.run girdileri var; çıktı dönüşü)

## F. Web
- [x] URL aç, HTTP isteği (GET/POST/…), webhook
- [~] Web sayfası içeriği / HTML'den metin (data.read HTML var)
- [ ] URL bileşenlerini al, URL kodla
- [x] Yol tarifi al (harita)

## G. Medya ve belgeler
- [ ] Fotoğraf çek / video kaydet
- [ ] Görseli yeniden boyutlandır, dönüştür, kırp
- [ ] QR kod oluştur ve oku
- [ ] Ses kaydı
- [~] Müzik kontrolü (oynat/duraklat/sonraki var)
- [ ] PDF oluştur (metin/görselden)
- [ ] Metni çevir (çevrimdışı veya servis)

## H. Cihaz
- [x] Wi-Fi, Bluetooth, Rahatsız etme, parlaklık, ses, fener, duvar kağıdı (var)
- [x] Pil seviyesini al, şarj durumu
- [ ] Ağ bilgisi (SSID, IP), cihaz bilgisi
- [ ] Karanlık modu aç/kapat (izin gerektirir)
- [ ] Ekranı kaydet / ekran görüntüsü (kısmen var)

## I. Uygulamalar
- [x] Uygulama aç, Intent gönder, SMS
- [x] E-posta oluştur, telefonla ara
- [ ] WhatsApp / Telegram mesajı
- [ ] Takvim etkinliği ekle, hatırlatıcı/not
- [x] Alarm ve zamanlayıcı kur
- [x] Metni paylaş (paylaşım sayfası)
- [ ] Dosyayı paylaş

## J. Otomasyon tetikleyicileri
- [x] Saat, gün, konum, Wi-Fi, Bluetooth, şarj, uygulama, bildirim, NFC, USB, push (var)
- [ ] Pil seviyesi eşiği tetikleyicisi (kolay şablon)
- [ ] Otomasyonu çalıştırmadan önce sor (onay bildirimi)

## K. Paylaşım ve sesli komut
- [~] Kısayolu dosya olarak dışa/içe aktar (paketler var)
- [ ] Kısayol bağlantısı / QR ile paylaş
- [ ] "Hey Google, ... çalıştır" (App Actions / statik kısayollar)
- [~] MacroDroid ve Tasker içe aktarma (var)

## L. Yenilik turları (liste bitince)
- Yeni Android sürüm özellikleri, kullanıcı geri bildirimi, performans, UI/UX iyileştirmeleri.
