package tr.sau.randevu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.sau.randevu.depo.BellekDepo;
import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.SaglikPersoneli;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.olay.BildirimKutusu;
import tr.sau.randevu.servis.RandevuServisi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RandevuServisiTest {

    // "Şimdi": 6 Kasım 2026 Cuma 12:00. Sonraki Pazartesi 9 Kasım.
    private static final LocalDateTime SIMDI = LocalDateTime.of(2026, 11, 6, 12, 0);
    private static final LocalDate PAZARTESI = LocalDate.of(2026, 11, 9);

    private RandevuServisi servis;
    private BildirimKutusu bildirimler;
    private Sube sube;
    private Hizmet muayene;
    private Hizmet kanTahlili;
    private Doktor doktor;
    private Doktor digerDoktor;
    private SaglikPersoneli laborant;
    private Hasta ayse;
    private Hasta mehmet;
    private Yonetici yonetici;
    private Yonetici baskaKurumYoneticisi;

    @BeforeEach
    void kur() {
        BellekDepo<Randevu> depo = new BellekDepo<>();
        Clock saat = Clock.fixed(SIMDI.atZone(ZoneId.of("Europe/Istanbul")).toInstant(), ZoneId.of("Europe/Istanbul"));
        servis = RandevuServisi.varsayilan(depo, saat);
        bildirimler = new BildirimKutusu();
        servis.dinleyiciEkle(bildirimler);

        SaglikKurulusu kurulus = new SaglikKurulusu("Sakarya Şifa Hastanesi", SaglikKurulusu.Tur.HASTANE);
        kurulus.kimlikAta(1);
        sube = kurulus.subeAc("Adapazarı", "Merkez", CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
        sube.bolumEkle("Dahiliye");
        sube.bolumEkle("Laboratuvar");
        muayene = new Hizmet("Genel Muayene", HizmetTuru.MUAYENE, "Dahiliye", Duration.ofMinutes(20), new BigDecimal("750"));
        kanTahlili = new Hizmet("Hemogram", HizmetTuru.LABORATUVAR, "Laboratuvar", Duration.ofMinutes(10), new BigDecimal("300"));
        muayene.kimlikAta(1);
        kanTahlili.kimlikAta(2);
        sube.hizmetEkle(muayene);
        sube.hizmetEkle(kanTahlili);

        doktor = new Doktor("Uzm. Dr.", "Ayşe", "Kaya", "İç Hastalıkları", sube, null, "ayse.kaya@sifa.test");
        doktor.kimlikAta(10);
        doktor.hizmetEkle(muayene);
        doktor.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));

        digerDoktor = new Doktor(null, "Ali", "Demir", "İç Hastalıkları", sube, null, null);
        digerDoktor.kimlikAta(11);
        digerDoktor.hizmetEkle(muayene);
        digerDoktor.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));

        laborant = new SaglikPersoneli("Can", "Yıldız", "Laborant", sube, null, null);
        laborant.kimlikAta(12);
        laborant.hizmetEkle(kanTahlili);
        laborant.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("08:00", "10:00")));

        ayse = new Hasta("Ayşe", "Öztürk", "12345678901", LocalDate.of(1990, 5, 1), "0555 111 22 33", null);
        ayse.kimlikAta(100);
        mehmet = new Hasta("Mehmet", "Çelik", "10987654321", LocalDate.of(1985, 3, 3), null, null);
        mehmet.kimlikAta(101);

        yonetici = new Yonetici("Zeynep", "Arslan", kurulus, null, null);
        yonetici.kimlikAta(200);
        SaglikKurulusu baskaKurum = new SaglikKurulusu("Başka Klinik", SaglikKurulusu.Tur.KLINIK);
        baskaKurum.kimlikAta(2);
        baskaKurumYoneticisi = new Yonetici("Emre", "Koç", baskaKurum, null, null);
        baskaKurumYoneticisi.kimlikAta(201);
    }

    private LocalDateTime pzt(int saat, int dakika) {
        return PAZARTESI.atTime(saat, dakika);
    }

    @Test
    void randevuOlusturulurVeDetaylarKaydedilir() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));

        assertThat(r.getId()).isNotNull();
        assertThat(r.getDurum()).isEqualTo(RandevuDurumu.ONAY_BEKLIYOR);
        assertThat(r.getZaman().bitis()).isEqualTo(pzt(9, 20)); // süre hizmetten gelir
        assertThat(r.getUcret()).isEqualByComparingTo("750");
        assertThat(r.getSube()).isSameAs(sube);
        assertThat(bildirimler.kisininBildirimleri(doktor.getId())).singleElement()
                .extracting(BildirimKutusu.Bildirim::mesaj).asString().contains("Yeni randevu talebi");
    }

    @Test
    void ucretRandevuAlindigiAndaSabitlenir() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        muayene.setUcret(new BigDecimal("900"));
        assertThat(r.getUcret()).isEqualByComparingTo("750");
    }

    @Test
    void ayniCalisanaCakisanRandevuAlinamaz() {
        servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        assertThatThrownBy(() -> servis.olustur(mehmet, doktor, muayene, pzt(9, 10)))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("çakışan");
        // uç uca randevu çakışma sayılmaz
        assertThat(servis.olustur(mehmet, doktor, muayene, pzt(9, 20)).getId()).isNotNull();
    }

    @Test
    void hastaAyniSaateIkiRandevuAlamaz() {
        servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        assertThatThrownBy(() -> servis.olustur(ayse, digerDoktor, muayene, pzt(9, 10)))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("Sizin");
    }

    @Test
    void calisaninUygunlukDilimiDisinaRandevuAlinamaz() {
        // 13:00 şubenin çalışma saatinde ama doktorun uygunluk diliminde değil
        assertThatThrownBy(() -> servis.olustur(ayse, doktor, muayene, pzt(13, 0)))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("randevu vermiyor");
        // dilimin sonunu aşan randevu da alınamaz (11:50 + 20 dk = 12:10)
        assertThatThrownBy(() -> servis.olustur(ayse, doktor, muayene, pzt(11, 50)))
                .isInstanceOf(RandevuHatasi.class);
    }

    @Test
    void subeninKapaliOlduguGuneRandevuAlinamaz() {
        LocalDateTime cumartesi = LocalDate.of(2026, 11, 14).atTime(10, 0);
        assertThatThrownBy(() -> servis.olustur(ayse, doktor, muayene, cumartesi))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("kapalı");
    }

    @Test
    void gecmisZamanaRandevuAlinamaz() {
        assertThatThrownBy(() -> servis.olustur(ayse, doktor, muayene, LocalDate.of(2026, 11, 2).atTime(9, 0)))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("Geçmiş");
    }

    @Test
    void calisanVermedigiHizmetIcinRandevuVeremez() {
        assertThatThrownBy(() -> servis.olustur(ayse, doktor, kanTahlili, pzt(9, 0)))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("vermiyor");
    }

    @Test
    void doktorOnaylayincaHastayaBildirimGider() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        servis.onayla(r.getId(), doktor);

        assertThat(r.getDurum()).isEqualTo(RandevuDurumu.ONAYLANDI);
        assertThat(bildirimler.kisininBildirimleri(ayse.getId())).first()
                .extracting(BildirimKutusu.Bildirim::mesaj).asString().startsWith("Randevunuz onaylandı");
    }

    @Test
    void yalnizcaIlgiliCalisanVeyaKendiKurumununYoneticisiOnaylayabilir() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));

        assertThatThrownBy(() -> servis.onayla(r.getId(), digerDoktor)).isInstanceOf(YetkiHatasi.class);
        assertThatThrownBy(() -> servis.onayla(r.getId(), ayse)).isInstanceOf(YetkiHatasi.class);
        assertThatThrownBy(() -> servis.onayla(r.getId(), baskaKurumYoneticisi)).isInstanceOf(YetkiHatasi.class);

        servis.onayla(r.getId(), yonetici);
        assertThat(r.getDurum()).isEqualTo(RandevuDurumu.ONAYLANDI);
    }

    @Test
    void gecersizDurumGecisleriEngellenir() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        servis.onayla(r.getId(), doktor);
        assertThatThrownBy(() -> servis.reddet(r.getId(), doktor, "geç kaldı"))
                .isInstanceOf(RandevuHatasi.class).hasMessageContaining("geçemez");
        servis.iptalEt(r.getId(), ayse, "işim çıktı");
        assertThatThrownBy(() -> servis.onayla(r.getId(), doktor)).isInstanceOf(RandevuHatasi.class);
    }

    @Test
    void reddedilenVeIptalEdilenRandevuTakvimiBosaltir() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        servis.reddet(r.getId(), doktor, "izinliyim");
        assertThat(servis.olustur(mehmet, doktor, muayene, pzt(9, 0)).getDurum()).isEqualTo(RandevuDurumu.ONAY_BEKLIYOR);
        assertThat(bildirimler.kisininBildirimleri(ayse.getId())).first()
                .extracting(BildirimKutusu.Bildirim::mesaj).asString().contains("Gerekçe: izinliyim");
    }

    @Test
    void hastaBaskasininRandevusunuIptalEdemez() {
        Randevu r = servis.olustur(ayse, doktor, muayene, pzt(9, 0));
        assertThatThrownBy(() -> servis.iptalEt(r.getId(), mehmet, "")).isInstanceOf(YetkiHatasi.class);
    }

    @Test
    void uygunSaatlerDoluSaatleriIcermez() {
        // Pazartesi 09:00–12:00, 20 dk'lık muayene → 9 aday saat
        assertThat(servis.uygunSaatler(ayse, doktor, muayene, PAZARTESI)).hasSize(9).first().isEqualTo(pzt(9, 0));

        servis.olustur(mehmet, doktor, muayene, pzt(9, 20));
        assertThat(servis.uygunSaatler(ayse, doktor, muayene, PAZARTESI)).hasSize(8).doesNotContain(pzt(9, 20));
        // doktorun uygun olmadığı gün boş liste
        assertThat(servis.uygunSaatler(ayse, doktor, muayene, PAZARTESI.plusDays(1))).isEmpty();
    }

    @Test
    void doktorVeSaglikPersoneliAyniArayuzleRandevuVerir() {
        // Çok biçimlilik: servis ikisini de RandevuVerebilir olarak görür
        Randevu muayeneRandevusu = servis.olustur(ayse, doktor, muayene, pzt(10, 0));
        Randevu tahlilRandevusu = servis.olustur(ayse, laborant, kanTahlili, pzt(8, 30));

        assertThat(muayeneRandevusu.getCalisan().gorunenAd()).isEqualTo("Uzm. Dr. Ayşe Kaya");
        assertThat(tahlilRandevusu.getCalisan().gorunenAd()).isEqualTo("Can Yıldız (Laborant)");
        assertThat(digerDoktor.gorunenAd()).isEqualTo("Dr. Ali Demir");
    }
}
