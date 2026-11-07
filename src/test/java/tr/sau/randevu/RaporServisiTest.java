package tr.sau.randevu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.sau.randevu.depo.BellekDepo;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.servis.RandevuServisi;
import tr.sau.randevu.servis.RaporServisi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RaporServisiTest {

    private static final LocalDate PAZARTESI = LocalDate.of(2026, 11, 9);

    private RandevuServisi randevu;
    private RaporServisi rapor;
    private SaglikKurulusu kurulus;
    private Doktor adapazariDr;
    private Doktor serdivanDr;
    private Hizmet muayeneA;
    private Hizmet muayeneS;
    private Hasta h1;
    private Hasta h2;
    private Hasta h3;

    @BeforeEach
    void kur() {
        ZoneId tz = ZoneId.of("Europe/Istanbul");
        Clock saat = Clock.fixed(LocalDateTime.of(2026, 11, 6, 12, 0).atZone(tz).toInstant(), tz);
        randevu = RandevuServisi.varsayilan(new BellekDepo<>(), saat);
        rapor = new RaporServisi(randevu);

        kurulus = new SaglikKurulusu("Test Hastanesi", SaglikKurulusu.Tur.HASTANE);
        kurulus.kimlikAta(1);
        Sube a = kurulus.subeAc("Adapazarı", null, CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
        Sube s = kurulus.subeAc("Serdivan", null, CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
        a.kimlikAta(1);
        s.kimlikAta(2);
        muayeneA = hizmet(a, 1, "750");
        muayeneS = hizmet(s, 2, "500");
        adapazariDr = doktor(a, muayeneA, 10);
        serdivanDr = doktor(s, muayeneS, 11);
        h1 = hasta(100, "00000000001");
        h2 = hasta(101, "00000000002");
        h3 = hasta(102, "00000000003");
    }

    private Hizmet hizmet(Sube sube, long id, String ucret) {
        sube.bolumEkle("Dahiliye");
        Hizmet h = new Hizmet("Genel Muayene", HizmetTuru.MUAYENE, "Dahiliye", Duration.ofMinutes(20), new BigDecimal(ucret));
        h.kimlikAta(id);
        sube.hizmetEkle(h);
        return h;
    }

    private Doktor doktor(Sube sube, Hizmet hizmet, long id) {
        Doktor d = new Doktor(null, "Dr", "Test" + id, "Dahiliye", sube, null, null);
        d.kimlikAta(id);
        d.hizmetEkle(hizmet);
        d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));
        return d;
    }

    private Hasta hasta(long id, String tc) {
        Hasta h = new Hasta("Hasta", "No" + id, tc, LocalDate.of(1990, 1, 1), null, null);
        h.kimlikAta(id);
        return h;
    }

    @Test
    void subeVeHizmetBazindaSayilarVeCiroHesaplanir() {
        Randevu onayli = randevu.olustur(h1, adapazariDr, muayeneA, PAZARTESI.atTime(9, 0));
        randevu.onayla(onayli.getId(), adapazariDr);
        Randevu reddedilen = randevu.olustur(h2, adapazariDr, muayeneA, PAZARTESI.atTime(9, 20));
        randevu.reddet(reddedilen.getId(), adapazariDr, "dolu");
        randevu.olustur(h3, adapazariDr, muayeneA, PAZARTESI.atTime(9, 40)); // onay bekliyor
        Randevu serdivan = randevu.olustur(h1, serdivanDr, muayeneS, PAZARTESI.atTime(10, 0));
        randevu.onayla(serdivan.getId(), serdivanDr);

        RaporServisi.KurulusRaporu r = rapor.kurulusRaporu(kurulus);

        assertThat(r.genel().toplam()).isEqualTo(4);
        assertThat(r.genel().ciro()).isEqualByComparingTo("1250");          // 750 + 500 (onaylılar)
        assertThat(r.genel().bekleyenTutar()).isEqualByComparingTo("750");  // onay bekleyen
        assertThat(r.genel().kayipOrani()).isEqualTo(0.25);                 // 1 ret / 4

        // En yüksek cirolu şube önce gelir
        assertThat(r.subeler()).extracting(RaporServisi.Ozet::ad).containsExactly("Adapazarı", "Serdivan");
        RaporServisi.Ozet adapazari = r.subeler().get(0);
        assertThat(adapazari.sayi(RandevuDurumu.ONAYLANDI)).isEqualTo(1);
        assertThat(adapazari.sayi(RandevuDurumu.REDDEDILDI)).isEqualTo(1);
        assertThat(adapazari.sayi(RandevuDurumu.ONAY_BEKLIYOR)).isEqualTo(1);

        // Aynı adlı hizmetler şube adıyla ayrışır
        assertThat(r.hizmetler()).extracting(RaporServisi.Ozet::ad)
                .containsExactly("Genel Muayene · Adapazarı", "Genel Muayene · Serdivan");
    }

    @Test
    void randevusuOlmayanKurulusIcinBosRapor() {
        RaporServisi.KurulusRaporu r = rapor.kurulusRaporu(kurulus);
        assertThat(r.genel().toplam()).isZero();
        assertThat(r.genel().ciro()).isEqualByComparingTo("0");
        assertThat(r.genel().kayipOrani()).isZero();
        assertThat(r.subeler()).isEmpty();
    }
}
