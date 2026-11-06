package tr.sau.randevu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.sau.randevu.depo.BellekDepo;
import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.servis.KurumServisi;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KurumServisiTest {

    private KurumServisi servis;
    private SaglikKurulusu hastane;
    private Yonetici yonetici;
    private Yonetici digerYonetici;
    private Sube sube;

    @BeforeEach
    void kur() {
        servis = new KurumServisi(new BellekDepo<>(), new BellekDepo<>(), new BellekDepo<>(), new BellekDepo<>());
        hastane = servis.kurulusOlustur("Sakarya Şifa Hastanesi", SaglikKurulusu.Tur.HASTANE);
        SaglikKurulusu klinik = servis.kurulusOlustur("Göz Kliniği", SaglikKurulusu.Tur.KLINIK);
        yonetici = servis.yoneticiAta(hastane, "Zeynep", "Arslan", null, null);
        digerYonetici = servis.yoneticiAta(klinik, "Emre", "Koç", null, null);
        sube = servis.subeAc(yonetici, hastane, "Adapazarı", null, CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00")));
    }

    @Test
    void yoneticiKendiKurulusunuTanimlayabilir() {
        servis.bolumEkle(yonetici, sube, "Dahiliye");
        Hizmet h = servis.hizmetTanimla(yonetici, sube, "Genel Muayene", HizmetTuru.MUAYENE, "Dahiliye",
                Duration.ofMinutes(20), new BigDecimal("750"));
        Doktor d = servis.doktorEkle(yonetici, sube, null, "Ayşe", "Kaya", "Dahiliye", null, null);
        servis.hizmetAta(yonetici, d, h);
        servis.uygunlukEkle(d, d, new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));

        assertThat(h.getId()).isNotNull();
        assertThat(servis.hizmetVerenler(sube, h)).containsExactly(d);
        assertThat(d.getUygunluklar()).hasSize(1);
    }

    @Test
    void yoneticiBaskaKurulusaDokunamaz() {
        assertThatThrownBy(() -> servis.bolumEkle(digerYonetici, sube, "Kardiyoloji")).isInstanceOf(YetkiHatasi.class);
        assertThatThrownBy(() -> servis.subeAc(digerYonetici, hastane, "Serdivan", null, new CalismaSaatleri()))
                .isInstanceOf(YetkiHatasi.class);
        assertThatThrownBy(() -> servis.doktorEkle(digerYonetici, sube, null, "A", "B", "C", null, null))
                .isInstanceOf(YetkiHatasi.class);
    }

    @Test
    void uygunluguYalnizcaCalisanVeyaYoneticisiEkleyebilir() {
        Doktor d = servis.doktorEkle(yonetici, sube, null, "Ayşe", "Kaya", "Dahiliye", null, null);
        Doktor baska = servis.doktorEkle(yonetici, sube, null, "Ali", "Demir", "Dahiliye", null, null);
        UygunlukDilimi dilim = new UygunlukDilimi(DayOfWeek.TUESDAY, SaatAraligi.of("09:00", "10:00"));

        assertThatThrownBy(() -> servis.uygunlukEkle(baska, d, dilim)).isInstanceOf(YetkiHatasi.class);
        assertThatThrownBy(() -> servis.uygunlukEkle(digerYonetici, d, dilim)).isInstanceOf(YetkiHatasi.class);
        servis.uygunlukEkle(yonetici, d, dilim);
        assertThat(d.getUygunluklar()).containsExactly(dilim);
    }

    @Test
    void calismaSaatiDegistirilebilirVeGunKapatilabilir() {
        servis.calismaSaatiAyarla(yonetici, sube, DayOfWeek.SATURDAY, SaatAraligi.of("09:00", "13:00"));
        servis.calismaSaatiAyarla(yonetici, sube, DayOfWeek.FRIDAY, null);
        assertThat(sube.getCalismaSaatleri().getir(DayOfWeek.SATURDAY)).isPresent();
        assertThat(sube.getCalismaSaatleri().getir(DayOfWeek.FRIDAY)).isEmpty();
    }

    @Test
    void ayniKimlikNumarasiylaIkinciHastaKaydedilemez() {
        Hasta h = servis.hastaKaydet("Ayşe", "Öztürk", "12345678901", LocalDate.of(1990, 1, 1), null, null);
        assertThat(h.getId()).isNotNull();
        assertThatThrownBy(() -> servis.hastaKaydet("Başka", "Kişi", "12345678901", LocalDate.of(1991, 1, 1), null, null))
                .isInstanceOf(RandevuHatasi.class);
    }
}
