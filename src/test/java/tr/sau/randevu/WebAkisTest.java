package tr.sau.randevu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tr.sau.randevu.model.kisi.Calisan;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Örnek veriyle uygulamayı ayağa kaldırıp hasta → doktor → yönetici akışını HTTP üzerinden dener. */
@SpringBootTest
@AutoConfigureMockMvc
class WebAkisTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    KurumServisi kurum;
    @Autowired
    RandevuServisi randevu;

    private <T extends Kisi> T bul(Class<T> tur, String ad) {
        return kurum.kisiler().stream().filter(tur::isInstance).map(tur::cast)
                .filter(k -> k.getAd().equals(ad)).findFirst().orElseThrow();
    }

    private MockHttpSession girisYap(Kisi k) throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(post("/giris").session(s).param("kisiId", k.getId().toString())).andExpect(status().is3xxRedirection());
        return s;
    }

    @Test
    void girisSayfasiTumRolleriListeler() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Uzm. Dr. Ayşe Kaya")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hastane / Klinik Yöneticisi")));
    }

    @Test
    void hastaRandevuAlirDoktorOnaylarYoneticiGorur() throws Exception {
        Hasta fatma = bul(Hasta.class, "Fatma");
        Calisan elif = bul(Calisan.class, "Elif");
        Hizmet muayene = elif.getHizmetler().iterator().next();

        // Hasta: adım adım seçim ekranı ve ilk uygun saate randevu
        MockHttpSession hs = girisYap(fatma);
        LocalDate gun = LocalDate.now().plusDays(1);
        List<LocalDateTime> saatler = List.of();
        for (int i = 0; i < 7 && saatler.isEmpty(); i++, gun = gun.plusDays(1)) {
            saatler = randevu.uygunSaatler(fatma, elif, muayene, gun);
        }
        mvc.perform(get("/hasta").session(hs).param("subeId", elif.getSube().getId().toString())
                        .param("hizmetId", muayene.getId().toString()).param("calisanId", elif.getId().toString())
                        .param("tarih", gun.minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Uygun saatler")));
        mvc.perform(post("/hasta/randevu").session(hs).param("calisanId", elif.getId().toString())
                        .param("hizmetId", muayene.getId().toString()).param("baslangic", saatler.get(0).toString()))
                .andExpect(redirectedUrl("/hasta"));
        Randevu r = randevu.hastaninRandevulari(fatma).get(0);
        assertThat(r.getDurum()).isEqualTo(RandevuDurumu.ONAY_BEKLIYOR);

        // Aynı saate ikinci deneme çakışma hatası verir
        mvc.perform(post("/hasta/randevu").session(hs).param("calisanId", elif.getId().toString())
                        .param("hizmetId", muayene.getId().toString()).param("baslangic", saatler.get(0).toString()))
                .andExpect(redirectedUrl("/hasta"));
        assertThat(randevu.hastaninRandevulari(fatma)).hasSize(1);

        // Doktor onaylar
        MockHttpSession ds = girisYap(elif);
        mvc.perform(get("/calisan").session(ds)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fatma Kılıç")));
        mvc.perform(post("/calisan/randevu/" + r.getId() + "/onayla").session(ds)).andExpect(redirectedUrl("/calisan"));
        assertThat(r.getDurum()).isEqualTo(RandevuDurumu.ONAYLANDI);

        // Yönetici kendi kuruluşunun randevusunu görür
        MockHttpSession ys = girisYap(bul(Yonetici.class, "Zeynep"));
        mvc.perform(get("/yonetici").session(ys)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fatma Kılıç")));
    }

    @Test
    void hastaYoneticiSayfasinaGiremez() throws Exception {
        MockHttpSession hs = girisYap(bul(Hasta.class, "Aysel"));
        mvc.perform(get("/yonetici").session(hs)).andExpect(redirectedUrl("/"));
    }
}
