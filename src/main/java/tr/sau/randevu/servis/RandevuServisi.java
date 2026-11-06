package tr.sau.randevu.servis;

import tr.sau.randevu.depo.Depo;
import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.kural.CakismaKurali;
import tr.sau.randevu.kural.CalisanUygunlukKurali;
import tr.sau.randevu.kural.CalismaSaatiKurali;
import tr.sau.randevu.kural.GecmisZamanKurali;
import tr.sau.randevu.kural.HizmetKurali;
import tr.sau.randevu.kural.RandevuKurali;
import tr.sau.randevu.kural.RandevuTalebi;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.kisi.RandevuVerebilir;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.model.randevu.ZamanAraligi;
import tr.sau.randevu.olay.RandevuDinleyici;
import tr.sau.randevu.olay.RandevuOlayi;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Randevu iş akışının merkezi: oluşturma (kural denetimiyle), onay/ret, iptal, tamamlama ve uygun
 * saatlerin listelenmesi. Bağımlılıklar (depo, kurallar, saat, dinleyiciler) dışarıdan verilir
 * (Dependency Injection); bu sayede testlerde sabit bir saat ve sahte bir depo kullanılabilir.
 */
public class RandevuServisi {

    private final Depo<Randevu> depo;
    private final List<RandevuKurali> kurallar;
    private final Clock saat;
    private final List<RandevuDinleyici> dinleyiciler = new ArrayList<>();

    public RandevuServisi(Depo<Randevu> depo, List<RandevuKurali> kurallar, Clock saat) {
        this.depo = Objects.requireNonNull(depo);
        this.kurallar = List.copyOf(kurallar);
        this.saat = Objects.requireNonNull(saat);
    }

    /** Projede tanımlı tüm kurallarla hazır servis. */
    public static RandevuServisi varsayilan(Depo<Randevu> depo, Clock saat) {
        return new RandevuServisi(depo, varsayilanKurallar(), saat);
    }

    public static List<RandevuKurali> varsayilanKurallar() {
        return List.of(
                new GecmisZamanKurali(),
                new HizmetKurali(),
                new CalismaSaatiKurali(),
                new CalisanUygunlukKurali(),
                new CakismaKurali(CakismaKurali.Kapsam.CALISAN),
                new CakismaKurali(CakismaKurali.Kapsam.HASTA));
    }

    public void dinleyiciEkle(RandevuDinleyici dinleyici) {
        dinleyiciler.add(Objects.requireNonNull(dinleyici));
    }

    // ---------------------------------------------------------------- oluşturma

    public synchronized Randevu olustur(Hasta hasta, RandevuVerebilir calisan, Hizmet hizmet, LocalDateTime baslangic) {
        LocalDateTime simdi = LocalDateTime.now(saat);
        RandevuTalebi talep = talep(hasta, calisan, hizmet, baslangic, simdi);
        kurallar.forEach(k -> k.dogrula(talep));
        Randevu randevu = depo.kaydet(new Randevu(hasta, calisan, hizmet, baslangic, simdi));
        yayinla(RandevuOlayi.Tur.OLUSTURULDU, randevu, hasta);
        return randevu;
    }

    /**
     * Bir çalışanın verilen gündeki boş başlangıç saatleri. Her uygunluk dilimi hizmet süresi adımlarla
     * taranır ve her aday saat, randevu oluştururken kullanılan kuralların aynısından geçirilir.
     */
    public List<LocalDateTime> uygunSaatler(Hasta hasta, RandevuVerebilir calisan, Hizmet hizmet, LocalDate gun) {
        LocalDateTime simdi = LocalDateTime.now(saat);
        List<LocalDateTime> sonuc = new ArrayList<>();
        for (UygunlukDilimi dilim : calisan.getUygunluklar()) {
            if (dilim.gun() != gun.getDayOfWeek()) {
                continue;
            }
            LocalDateTime aday = gun.atTime(dilim.saatAraligi().baslangic());
            LocalDateTime son = gun.atTime(dilim.saatAraligi().bitis());
            while (!aday.plus(hizmet.getSure()).isAfter(son)) {
                if (gecerliMi(talep(hasta, calisan, hizmet, aday, simdi))) {
                    sonuc.add(aday);
                }
                aday = aday.plus(hizmet.getSure());
            }
        }
        sonuc.sort(Comparator.naturalOrder());
        return sonuc;
    }

    private boolean gecerliMi(RandevuTalebi talep) {
        try {
            kurallar.forEach(k -> k.dogrula(talep));
            return true;
        } catch (RandevuHatasi e) {
            return false;
        }
    }

    private RandevuTalebi talep(Hasta hasta, RandevuVerebilir calisan, Hizmet hizmet, LocalDateTime baslangic, LocalDateTime simdi) {
        return new RandevuTalebi(Objects.requireNonNull(hasta, "hasta"), Objects.requireNonNull(calisan, "çalışan"),
                Objects.requireNonNull(hizmet, "hizmet"), ZamanAraligi.sureIle(baslangic, hizmet.getSure()), simdi,
                depo.filtrele(Randevu::aktifMi));
    }

    // ---------------------------------------------------------------- onay mekanizması ve durum değişiklikleri

    public synchronized Randevu onayla(long randevuId, Kisi islemYapan) {
        Randevu r = depo.getir(randevuId);
        calisanVeyaYoneticiMi(r, islemYapan, "onaylama");
        r.onayla();
        yayinla(RandevuOlayi.Tur.ONAYLANDI, r, islemYapan);
        return r;
    }

    public synchronized Randevu reddet(long randevuId, Kisi islemYapan, String gerekce) {
        Randevu r = depo.getir(randevuId);
        calisanVeyaYoneticiMi(r, islemYapan, "reddetme");
        r.reddet(gerekce);
        yayinla(RandevuOlayi.Tur.REDDEDILDI, r, islemYapan);
        return r;
    }

    public synchronized Randevu iptalEt(long randevuId, Kisi islemYapan, String gerekce) {
        Randevu r = depo.getir(randevuId);
        boolean kendiRandevusu = Objects.equals(r.getHasta().getId(), islemYapan.getId());
        if (!kendiRandevusu) {
            calisanVeyaYoneticiMi(r, islemYapan, "iptal");
        }
        if (!r.getZaman().baslangic().isAfter(LocalDateTime.now(saat))) {
            throw new RandevuHatasi("Başlamış ya da geçmiş bir randevu iptal edilemez");
        }
        r.iptalEt(gerekce);
        yayinla(RandevuOlayi.Tur.IPTAL_EDILDI, r, islemYapan);
        return r;
    }

    public synchronized Randevu tamamla(long randevuId, Kisi islemYapan) {
        Randevu r = depo.getir(randevuId);
        calisanVeyaYoneticiMi(r, islemYapan, "tamamlama");
        r.tamamla();
        yayinla(RandevuOlayi.Tur.TAMAMLANDI, r, islemYapan);
        return r;
    }

    private static void calisanVeyaYoneticiMi(Randevu r, Kisi kisi, String islem) {
        boolean randevununCalisani = Objects.equals(r.getCalisan().getId(), kisi.getId()) && kisi instanceof RandevuVerebilir;
        boolean yetkiliYonetici = kisi instanceof Yonetici y && y.yetkiliMi(r.getSube());
        if (!randevununCalisani && !yetkiliYonetici) {
            throw new YetkiHatasi(kisi.gorunenAd() + " bu randevu için " + islem + " yetkisine sahip değil");
        }
    }

    // ---------------------------------------------------------------- sorgular

    public List<Randevu> hastaninRandevulari(Hasta hasta) {
        return sirali(depo.filtrele(r -> Objects.equals(r.getHasta().getId(), hasta.getId())));
    }

    public List<Randevu> calisaninRandevulari(RandevuVerebilir calisan) {
        return sirali(depo.filtrele(r -> Objects.equals(r.getCalisan().getId(), calisan.getId())));
    }

    public List<Randevu> onayBekleyenler(RandevuVerebilir calisan) {
        return calisaninRandevulari(calisan).stream().filter(r -> r.getDurum() == RandevuDurumu.ONAY_BEKLIYOR).toList();
    }

    public List<Randevu> kurulusunRandevulari(SaglikKurulusu kurulus) {
        return sirali(depo.filtrele(r -> r.getSube().getKurulus().equals(kurulus)));
    }

    private static List<Randevu> sirali(List<Randevu> liste) {
        return liste.stream().sorted(Comparator.comparing(r -> r.getZaman().baslangic())).toList();
    }

    private void yayinla(RandevuOlayi.Tur tur, Randevu randevu, Kisi islemYapan) {
        RandevuOlayi olay = new RandevuOlayi(tur, randevu, islemYapan, LocalDateTime.now(saat));
        dinleyiciler.forEach(d -> d.olayGerceklesti(olay));
    }
}
