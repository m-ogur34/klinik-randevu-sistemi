package tr.sau.randevu;

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
import tr.sau.randevu.servis.HaftalikTakvim;
import tr.sau.randevu.servis.RandevuServisi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class HaftalikTakvimTest {

    @Test
    void haftaBasiPazartesidir() {
        assertThat(HaftalikTakvim.haftaBasi(LocalDate.of(2026, 11, 12))).isEqualTo(LocalDate.of(2026, 11, 9)); // Perşembe → Pazartesi
        assertThat(HaftalikTakvim.haftaBasi(LocalDate.of(2026, 11, 9))).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(HaftalikTakvim.haftaBasi(LocalDate.of(2026, 11, 15))).isEqualTo(LocalDate.of(2026, 11, 9)); // Pazar
    }

    @Test
    void gunlereUygunlukVeAktifRandevularDagitilir() {
        ZoneId tz = ZoneId.of("Europe/Istanbul");
        Clock saat = Clock.fixed(LocalDateTime.of(2026, 11, 6, 12, 0).atZone(tz).toInstant(), tz);
        RandevuServisi servis = RandevuServisi.varsayilan(new BellekDepo<>(), saat);

        SaglikKurulusu k = new SaglikKurulusu("Test", SaglikKurulusu.Tur.KLINIK);
        Sube sube = k.subeAc("Merkez", null, CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
        sube.bolumEkle("Dahiliye");
        Hizmet muayene = new Hizmet("Muayene", HizmetTuru.MUAYENE, "Dahiliye", Duration.ofMinutes(30), BigDecimal.TEN);
        muayene.kimlikAta(1);
        sube.hizmetEkle(muayene);
        Doktor d = new Doktor(null, "Ali", "Demir", "Dahiliye", sube, null, null);
        d.kimlikAta(10);
        d.hizmetEkle(muayene);
        d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("13:00", "15:00")));
        d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "11:00")));
        d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.WEDNESDAY, SaatAraligi.of("09:00", "10:00")));
        Hasta h1 = new Hasta("A", "B", "00000000001", LocalDate.of(1990, 1, 1), null, null);
        Hasta h2 = new Hasta("C", "D", "00000000002", LocalDate.of(1990, 1, 1), null, null);
        h1.kimlikAta(100);
        h2.kimlikAta(101);

        LocalDate pzt = LocalDate.of(2026, 11, 9);
        Randevu r1 = servis.olustur(h1, d, muayene, pzt.atTime(13, 30));
        servis.olustur(h2, d, muayene, pzt.atTime(9, 0));
        Randevu reddedilen = servis.olustur(h1, d, muayene, LocalDate.of(2026, 11, 11).atTime(9, 0));
        servis.reddet(reddedilen.getId(), d, "uygun değil");
        servis.onayla(r1.getId(), d);

        HaftalikTakvim t = HaftalikTakvim.olustur(d, servis.calisaninRandevulari(d), LocalDate.of(2026, 11, 12), pzt.plusDays(2));

        assertThat(t.haftaBasi()).isEqualTo(pzt);
        assertThat(t.gunler()).hasSize(7);
        HaftalikTakvim.Gun pazartesi = t.gunler().get(0);
        // Uygunluklar ve randevular saate göre sıralı
        assertThat(pazartesi.uygunluklar()).extracting(SaatAraligi::toString).containsExactly("09:00–11:00", "13:00–15:00");
        assertThat(pazartesi.randevular()).extracting(r -> r.getZaman().baslangic().toLocalTime().toString())
                .containsExactly("09:00", "13:30");
        assertThat(pazartesi.dolulukYuzdesi()).isEqualTo(25); // 60 dk dolu / 240 dk uygun
        // Reddedilen randevu takvimde yer tutmaz, Çarşamba bugün olarak işaretli
        HaftalikTakvim.Gun carsamba = t.gunler().get(2);
        assertThat(carsamba.randevular()).isEmpty();
        assertThat(carsamba.bugun()).isTrue();
        assertThat(t.gunler().get(6).uygunDakika()).isZero(); // Pazar
        assertThat(t.toplamRandevu()).isEqualTo(2);
        assertThat(t.oncekiHafta()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(t.sonrakiHafta()).isEqualTo(LocalDate.of(2026, 11, 16));
    }
}
