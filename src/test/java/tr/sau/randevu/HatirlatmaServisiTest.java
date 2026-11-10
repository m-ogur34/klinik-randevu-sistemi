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
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.olay.BildirimKutusu;
import tr.sau.randevu.servis.HatirlatmaServisi;
import tr.sau.randevu.servis.RandevuServisi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class HatirlatmaServisiTest {

    private static final ZoneId TZ = ZoneId.of("Europe/Istanbul");

    /** Testte zamanı ileri sarabilmek için ayarlanabilir saat. */
    static class TestSaati extends Clock {
        private Instant an;

        TestSaati(LocalDateTime baslangic) {
            this.an = baslangic.atZone(TZ).toInstant();
        }

        void ilerle(Duration sure) {
            an = an.plus(sure);
        }

        @Override
        public ZoneId getZone() {
            return TZ;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return an;
        }
    }

    private TestSaati saat;
    private RandevuServisi randevu;
    private BildirimKutusu bildirimler;
    private HatirlatmaServisi hatirlatma;
    private Doktor doktor;
    private Hizmet muayene;
    private Hasta hasta;

    @BeforeEach
    void kur() {
        saat = new TestSaati(LocalDateTime.of(2026, 11, 6, 12, 0)); // Cuma 12:00
        BellekDepo<Randevu> depo = new BellekDepo<>();
        randevu = RandevuServisi.varsayilan(depo, saat);
        bildirimler = new BildirimKutusu();
        hatirlatma = new HatirlatmaServisi(depo, bildirimler, saat, Duration.ofHours(24));

        SaglikKurulusu k = new SaglikKurulusu("Test", SaglikKurulusu.Tur.KLINIK);
        Sube sube = k.subeAc("Merkez", null, CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
        sube.bolumEkle("Dahiliye");
        muayene = new Hizmet("Muayene", HizmetTuru.MUAYENE, "Dahiliye", Duration.ofMinutes(20), BigDecimal.TEN);
        muayene.kimlikAta(1);
        sube.hizmetEkle(muayene);
        doktor = new Doktor(null, "Ali", "Demir", "Dahiliye", sube, null, null);
        doktor.kimlikAta(10);
        doktor.hizmetEkle(muayene);
        doktor.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));
        hasta = new Hasta("Ayşe", "Öztürk", "00000000001", LocalDate.of(1990, 1, 1), null, null);
        hasta.kimlikAta(100);
    }

    private long hatirlatmaSayisi() {
        return bildirimler.kisininBildirimleri(hasta.getId()).stream().filter(b -> b.mesaj().startsWith("Hatırlatma")).count();
    }

    @Test
    void yalnizca24SaattenAzKalanOnayliRandevuBirKezHatirlatilir() {
        Randevu r = randevu.olustur(hasta, doktor, muayene, LocalDateTime.of(2026, 11, 9, 10, 0)); // Pazartesi 10:00
        randevu.onayla(r.getId(), doktor);

        // Cuma 12:00: randevuya 70 saat var → hatırlatma yok
        assertThat(hatirlatma.yaklasanlariHatirlat()).isEmpty();

        // Pazar 11:00: 23 saat kaldı → hatırlatılır
        saat.ilerle(Duration.ofHours(47));
        assertThat(hatirlatma.yaklasanlariHatirlat()).containsExactly(r);
        assertThat(bildirimler.kisininBildirimleri(hasta.getId()).get(0).mesaj())
                .startsWith("Hatırlatma: Muayene randevunuz 09.11.2026 10:00 (23 saat sonra)");

        // Tekrar çalışınca aynı randevu ikinci kez hatırlatılmaz
        saat.ilerle(Duration.ofHours(1));
        assertThat(hatirlatma.yaklasanlariHatirlat()).isEmpty();
        assertThat(hatirlatmaSayisi()).isEqualTo(1);
    }

    @Test
    void onayBekleyenVeIptalEdilenRandevularHatirlatilmaz() {
        randevu.olustur(hasta, doktor, muayene, LocalDateTime.of(2026, 11, 9, 10, 0)); // onay bekliyor
        Randevu iptal = randevu.olustur(hasta, doktor, muayene, LocalDateTime.of(2026, 11, 9, 11, 0));
        randevu.onayla(iptal.getId(), doktor);
        randevu.iptalEt(iptal.getId(), hasta, "işim çıktı");

        saat.ilerle(Duration.ofHours(47)); // Pazar 11:00
        assertThat(hatirlatma.yaklasanlariHatirlat()).isEmpty();
        assertThat(hatirlatmaSayisi()).isZero();
    }

    @Test
    void baslamisRandevuHatirlatilmaz() {
        Randevu r = randevu.olustur(hasta, doktor, muayene, LocalDateTime.of(2026, 11, 9, 10, 0));
        randevu.onayla(r.getId(), doktor);
        saat.ilerle(Duration.ofHours(70).plusMinutes(5)); // Pazartesi 10:05, randevu başladı
        assertThat(hatirlatma.yaklasanlariHatirlat()).isEmpty();
    }
}
