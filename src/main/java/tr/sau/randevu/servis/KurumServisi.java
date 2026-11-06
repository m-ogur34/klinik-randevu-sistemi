package tr.sau.randevu.servis;

import tr.sau.randevu.depo.Depo;
import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.model.kisi.Calisan;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.kisi.SaglikPersoneli;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Kuruluş tanımlamaları: şube, bölüm, hizmet, çalışan ve uygunluk. Her işlem "yetki çerçevesinde" yapılır:
 * yönetici yalnızca kendi kuruluşunu değiştirebilir, çalışan yalnızca kendi uygunluk dilimlerini düzenleyebilir.
 */
public class KurumServisi {

    private final Depo<SaglikKurulusu> kurulusDepo;
    private final Depo<Sube> subeDepo;
    private final Depo<Hizmet> hizmetDepo;
    private final Depo<Kisi> kisiDepo;

    public KurumServisi(Depo<SaglikKurulusu> kurulusDepo, Depo<Sube> subeDepo, Depo<Hizmet> hizmetDepo, Depo<Kisi> kisiDepo) {
        this.kurulusDepo = Objects.requireNonNull(kurulusDepo);
        this.subeDepo = Objects.requireNonNull(subeDepo);
        this.hizmetDepo = Objects.requireNonNull(hizmetDepo);
        this.kisiDepo = Objects.requireNonNull(kisiDepo);
    }

    // ---------------------------------------------------------------- kuruluş ve yönetici (sistem kurulumu)

    public SaglikKurulusu kurulusOlustur(String ad, SaglikKurulusu.Tur tur) {
        return kurulusDepo.kaydet(new SaglikKurulusu(ad, tur));
    }

    public Yonetici yoneticiAta(SaglikKurulusu kurulus, String ad, String soyad, String telefon, String eposta) {
        return kisiDepo.kaydet(new Yonetici(ad, soyad, kurulus, telefon, eposta));
    }

    // ---------------------------------------------------------------- yönetici işlemleri

    public Sube subeAc(Yonetici yonetici, SaglikKurulusu kurulus, String ad, String adres, CalismaSaatleri saatler) {
        yetkiliMi(yonetici, kurulus);
        return subeDepo.kaydet(kurulus.subeAc(ad, adres, saatler));
    }

    public void calismaSaatiAyarla(Yonetici yonetici, Sube sube, DayOfWeek gun, SaatAraligi aralik) {
        yetkiliMi(yonetici, sube.getKurulus());
        if (aralik == null) {
            sube.getCalismaSaatleri().kapat(gun);
        } else {
            sube.getCalismaSaatleri().ayarla(gun, aralik);
        }
    }

    public void bolumEkle(Yonetici yonetici, Sube sube, String bolum) {
        yetkiliMi(yonetici, sube.getKurulus());
        if (bolum == null || bolum.isBlank()) {
            throw new RandevuHatasi("Bölüm adı boş olamaz");
        }
        sube.bolumEkle(bolum);
    }

    public Hizmet hizmetTanimla(Yonetici yonetici, Sube sube, String ad, HizmetTuru tur, String bolum,
                                Duration sure, BigDecimal ucret) {
        yetkiliMi(yonetici, sube.getKurulus());
        Hizmet hizmet = hizmetDepo.kaydet(new Hizmet(ad, tur, bolum, sure, ucret));
        sube.hizmetEkle(hizmet);
        return hizmet;
    }

    public void hizmetGuncelle(Yonetici yonetici, Sube sube, Hizmet hizmet, Duration sure, BigDecimal ucret) {
        yetkiliMi(yonetici, sube.getKurulus());
        if (!sube.hizmetSunuyorMu(hizmet)) {
            throw new RandevuHatasi("Bu hizmet şubede tanımlı değil");
        }
        hizmet.setSure(sure);
        hizmet.setUcret(ucret);
    }

    public Doktor doktorEkle(Yonetici yonetici, Sube sube, String unvan, String ad, String soyad, String uzmanlik,
                             String telefon, String eposta) {
        yetkiliMi(yonetici, sube.getKurulus());
        return kisiDepo.kaydet(new Doktor(unvan, ad, soyad, uzmanlik, sube, telefon, eposta));
    }

    public SaglikPersoneli personelEkle(Yonetici yonetici, Sube sube, String ad, String soyad, String gorev,
                                        String telefon, String eposta) {
        yetkiliMi(yonetici, sube.getKurulus());
        return kisiDepo.kaydet(new SaglikPersoneli(ad, soyad, gorev, sube, telefon, eposta));
    }

    public void hizmetAta(Yonetici yonetici, Calisan calisan, Hizmet hizmet) {
        yetkiliMi(yonetici, calisan.getSube().getKurulus());
        calisan.hizmetEkle(hizmet);
    }

    /** Uygunluk dilimini çalışanın kendisi veya kuruluşun yöneticisi ekleyebilir. */
    public void uygunlukEkle(Kisi islemYapan, Calisan calisan, UygunlukDilimi dilim) {
        boolean kendisi = Objects.equals(islemYapan.getId(), calisan.getId());
        boolean yonetici = islemYapan instanceof Yonetici y && y.yetkiliMi(calisan.getSube());
        if (!kendisi && !yonetici) {
            throw new YetkiHatasi("Bu çalışanın uygunluk dilimlerini düzenleme yetkiniz yok");
        }
        calisan.uygunlukEkle(dilim);
    }

    // ---------------------------------------------------------------- hasta kaydı

    public Hasta hastaKaydet(String ad, String soyad, String tcKimlikNo, LocalDate dogumTarihi, String telefon, String eposta) {
        boolean kayitli = !kisiDepo.filtrele(k -> k instanceof Hasta h && h.tcKimlikNoEslesirMi(tcKimlikNo)).isEmpty();
        if (kayitli) {
            throw new RandevuHatasi("Bu T.C. kimlik numarasıyla kayıtlı bir hasta var");
        }
        return kisiDepo.kaydet(new Hasta(ad, soyad, tcKimlikNo, dogumTarihi, telefon, eposta));
    }

    // ---------------------------------------------------------------- sorgular

    public List<SaglikKurulusu> kuruluslar() {
        return kurulusDepo.tumu();
    }

    public List<Sube> subeler() {
        return subeDepo.tumu();
    }

    public Sube sube(long id) {
        return subeDepo.getir(id);
    }

    public Hizmet hizmet(long id) {
        return hizmetDepo.getir(id);
    }

    public Kisi kisi(long id) {
        return kisiDepo.getir(id);
    }

    public List<Kisi> kisiler() {
        return kisiDepo.tumu();
    }

    public List<Calisan> calisanlar(Sube sube) {
        return kisiDepo.filtrele(k -> k instanceof Calisan c && c.getSube().equals(sube)).stream()
                .map(Calisan.class::cast).toList();
    }

    /** Bir şubede verilen hizmeti sunan çalışanlar. */
    public List<Calisan> hizmetVerenler(Sube sube, Hizmet hizmet) {
        return calisanlar(sube).stream().filter(c -> c.hizmetVeriyorMu(hizmet)).toList();
    }

    private static void yetkiliMi(Yonetici yonetici, SaglikKurulusu kurulus) {
        if (yonetici == null || !yonetici.yetkiliMi(kurulus)) {
            throw new YetkiHatasi("Bu kuruluş üzerinde işlem yapma yetkiniz yok: " + kurulus.getAd());
        }
    }
}
