# Hastane / Klinik Randevu Sistemi

BSM515 Nesneye Dayalı Programlama Dilleri dersi proje ödevi (Seçenek A).
Sakarya Üniversitesi · Bilgisayar Mühendisliği Tezli Yüksek Lisans · Muhammed Oğur (Y195012002)

Tek şubeli bir klinik ya da çok şubeli bir hastane grubu için randevu sistemi. Java 21 ve Spring Boot ile
geliştirilmektedir; amaç nesneye dayalı programlama ilkelerini (sınıf yapısı, kalıtım, çok biçimlilik,
kapsülleme, soyutlama) ve SOLID prensiplerini somut bir üründe göstermektir.

## Gereksinimler ve karşılıkları

| Gereksinim (ödev belgesi) | Karşılayan sınıf(lar) | Durum |
|---|---|---|
| Hastane/klinik tanımlama, tek ve çok şubeli yapı | `SaglikKurulusu`, `Sube` | ✅ model |
| Çalışma saatleri ve bölümler | `CalismaSaatleri`, `Sube.bolumEkle` | ✅ model |
| Hizmetler: süre ve ücret | `Hizmet`, `HizmetTuru` | ✅ model |
| Doktor ve sağlık personeli kaydı, uzmanlık, verdiği hizmetler | `Doktor`, `SaglikPersoneli`, `Calisan` | ✅ model |
| Uygunluk zaman dilimleri; yalnızca bu aralıklara randevu | `UygunlukDilimi`, `CalisanUygunlukKurali` | ✅ |
| Randevu oluşturma, çakışma kontrolü | `RandevuServisi`, `CakismaKurali` | ✅ |
| Randevu detayları (işlem, süre, ücret, çalışan, tarih/saat) | `Randevu` (oluşturulduğu anda sabitlenir) | ✅ |
| Onay mekanizması | `RandevuDurumu`, `RandevuServisi.onayla/reddet` | ✅ |
| Roller: Hasta, Doktor/Sağlık Personeli, Yönetici | `Rol`, `Hasta`, `Calisan`, `Yonetici` | ✅ model |
| Yetki çerçevesinde tanımlama | `Yonetici.yetkiliMi`, `YetkiHatasi` | ⏳ yönetici ekranları |
| Web arayüzü | — | ⏳ |

## Sınıf yapısı

```
Varlik (soyut: id)
├── Kisi (soyut: ad, soyad, iletişim; getRol(), gorunenAd())
│   ├── Hasta
│   ├── Yonetici ──────────────── bir SaglikKurulusu'na bağlı, yalnızca onu yönetir
│   └── Calisan (soyut) ── implements RandevuVerebilir
│       ├── Doktor            gorunenAd() → "Uzm. Dr. Ayşe Kaya"
│       └── SaglikPersoneli   gorunenAd() → "Can Yıldız (Laborant)"
├── SaglikKurulusu ── 1..* Sube ── bölümler, Hizmet'ler, CalismaSaatleri
├── Hizmet (süre, ücret, tür, bölüm)
└── Randevu (hasta, çalışan, hizmet, zaman, ücret, RandevuDurumu)
```

## Kullanılan NDP ilkeleri ve tasarım kalıpları

| İlke / kalıp | Projedeki karşılığı |
|---|---|
| **Kalıtım** | `Kisi → Calisan → Doktor / SaglikPersoneli`, `Varlik` ortak atası |
| **Soyutlama** | `Kisi`, `Calisan` soyut sınıflar; `getRol()` soyut metot |
| **Çok biçimlilik** | `gorunenAd()` her sınıfta farklı; servis Doktor ve Personeli ayırt etmeden `RandevuVerebilir` ile çalışır |
| **Kapsülleme** | `Randevu`'nun durumu yalnızca `onayla()`, `reddet()`, `iptalEt()`, `tamamla()` ile ve kurallı geçişlerle değişir; koleksiyonlar dışarıya salt okunur verilir; T.C. kimlik no yalnızca maskeli gösterilir |
| **Strategy + Open/Closed** | Her iş kuralı ayrı bir `RandevuKurali` sınıfı; yeni kural eklemek servisi değiştirmez |
| **Single Responsibility** | Geçmiş zaman, hizmet, çalışma saati, uygunluk ve çakışma kontrolleri ayrı sınıflarda |
| **Observer** | `RandevuDinleyici`; `BildirimKutusu` onay/ret/iptal bildirimlerini ilgili kişiye iletir |
| **Repository + Generics** | `Depo<T extends Varlik>`, `BellekDepo<T>` |
| **Dependency Inversion / Injection** | Servis; depo, kural listesi ve saati (`Clock`) dışarıdan alır, bu sayede sabit saatle test edilir |
| **Değer nesneleri** | `SaatAraligi`, `ZamanAraligi`, `UygunlukDilimi` (Java `record`, değişmez) |

## Randevu durumları

```
ONAY_BEKLIYOR ──onayla──▶ ONAYLANDI ──tamamla──▶ TAMAMLANDI
     ├──reddet──▶ REDDEDILDI      └──iptal──▶ IPTAL_EDILDI
     └──iptal───▶ IPTAL_EDILDI
```
Takvimde yalnızca "onay bekliyor" ve "onaylandı" durumundaki randevular yer tutar; reddedilen veya iptal
edilen randevunun saati yeniden alınabilir.

## Çalıştırma

Gereksinimler: Java 21+, Maven.

```bash
mvn test              # birim testleri
mvn spring-boot:run   # uygulama (web arayüzü geliştirme aşamasında)
```

## Geliştirme planı

- [x] Alan modeli, randevu kuralları, onay mekanizması, bildirimler, birim testleri
- [ ] Örnek veri (2 kuruluş, çok şubeli hastane + tek şubeli klinik)
- [ ] Yönetici işlemleri: şube, bölüm, hizmet, çalışan tanımlama (yetki kontrolüyle)
- [ ] Web arayüzü: giriş (rol seçimi), hasta ekranları
- [ ] Doktor / personel ekranları: onay bekleyenler, takvim, uygunluk
- [ ] Yönetici ekranları
- [ ] UML sınıf diyagramı
- [ ] Sunum
