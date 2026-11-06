package tr.sau.randevu;

import org.junit.jupiter.api.Test;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelTest {

    private Sube sube() {
        SaglikKurulusu k = new SaglikKurulusu("Test Kliniği", SaglikKurulusu.Tur.KLINIK);
        return k.subeAc("Merkez", null, CalismaSaatleri.haftaIci(SaatAraligi.of("09:00", "17:00")));
    }

    @Test
    void hastaKimlikNumarasiMaskelenirVeDogrulanir() {
        Hasta h = new Hasta("Ayşe", "Öztürk", "12345678901", LocalDate.of(1990, 1, 1), null, null);
        assertThat(h.maskeliTcKimlikNo()).isEqualTo("123******01");
        assertThatThrownBy(() -> new Hasta("A", "B", "123", LocalDate.now(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gecersizIletisimBilgisiKabulEdilmez() {
        assertThatThrownBy(() -> new Hasta("A", "B", "12345678901", LocalDate.of(2000, 1, 1), "abc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Hasta("A", "B", "12345678901", LocalDate.of(2000, 1, 1), null, "eposta-degil"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tekVeCokSubeliKuruluslarAyniModelle() {
        SaglikKurulusu klinik = new SaglikKurulusu("Tek Şubeli Klinik", SaglikKurulusu.Tur.KLINIK);
        klinik.subeAc("Merkez", null, new CalismaSaatleri());
        assertThat(klinik.cokSubeliMi()).isFalse();
        klinik.subeAc("Serdivan", null, new CalismaSaatleri());
        assertThat(klinik.cokSubeliMi()).isTrue();
        assertThatThrownBy(() -> klinik.subeAc("merkez", null, new CalismaSaatleri()))
                .hasMessageContaining("zaten var");
    }

    @Test
    void hizmetIcinOnceBolumTanimlanmali() {
        Sube s = sube();
        Hizmet h = new Hizmet("Röntgen", HizmetTuru.RADYOLOJI, "Radyoloji", Duration.ofMinutes(15), BigDecimal.TEN);
        assertThatThrownBy(() -> s.hizmetEkle(h)).hasMessageContaining("bölümü");
        s.bolumEkle("Radyoloji");
        s.hizmetEkle(h);
        assertThat(s.hizmetSunuyorMu(h)).isTrue();
    }

    @Test
    void uygunlukDilimiCalismaSaatleriDisindaOlamazVeCakisamaz() {
        Sube s = sube();
        Doktor d = new Doktor(null, "Ali", "Demir", "Dahiliye", s, null, null);
        assertThatThrownBy(() -> d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("07:00", "10:00"))))
                .hasMessageContaining("çalışma saatleri dışında");
        assertThatThrownBy(() -> d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.SATURDAY, SaatAraligi.of("10:00", "12:00"))))
                .hasMessageContaining("çalışma saatleri dışında");
        d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("09:00", "12:00")));
        assertThatThrownBy(() -> d.uygunlukEkle(new UygunlukDilimi(DayOfWeek.MONDAY, SaatAraligi.of("11:00", "13:00"))))
                .hasMessageContaining("çakışıyor");
    }

    @Test
    void calisanSubedeOlmayanHizmetiVeremez() {
        Sube s = sube();
        Doktor d = new Doktor(null, "Ali", "Demir", "Dahiliye", s, null, null);
        Hizmet h = new Hizmet("MR", HizmetTuru.RADYOLOJI, "Radyoloji", Duration.ofMinutes(30), BigDecimal.ONE);
        assertThatThrownBy(() -> d.hizmetEkle(h)).hasMessageContaining("sunmuyor");
    }

    @Test
    void saatAraliklariUcUcaDegerseCakismaz() {
        assertThat(SaatAraligi.of("10:00", "10:30").cakisirMi(SaatAraligi.of("10:30", "11:00"))).isFalse();
        assertThat(SaatAraligi.of("10:00", "10:31").cakisirMi(SaatAraligi.of("10:30", "11:00"))).isTrue();
    }
}
