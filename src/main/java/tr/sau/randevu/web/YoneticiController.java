package tr.sau.randevu.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tr.sau.randevu.model.kisi.Calisan;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;
import tr.sau.randevu.servis.RaporServisi;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Yönetici ekranı: yalnızca kendi kuruluşunun şube, bölüm, hizmet, çalışan ve randevularını yönetir. */
@Controller
@RequestMapping("/yonetici")
public class YoneticiController {

    private final KurumServisi kurum;
    private final RandevuServisi randevu;
    private final RaporServisi rapor;
    private final Oturum oturum;

    public YoneticiController(KurumServisi kurum, RandevuServisi randevu, RaporServisi rapor, Oturum oturum) {
        this.kurum = kurum;
        this.randevu = randevu;
        this.rapor = rapor;
        this.oturum = oturum;
    }

    @GetMapping
    public String panel(HttpSession session, Model model) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        Map<Sube, List<Calisan>> calisanlar = new LinkedHashMap<>();
        y.getKurulus().getSubeler().forEach(s -> calisanlar.put(s, kurum.calisanlar(s)));
        model.addAttribute("kurulus", y.getKurulus());
        model.addAttribute("calisanlar", calisanlar);
        model.addAttribute("randevular", randevu.kurulusunRandevulari(y.getKurulus()));
        model.addAttribute("rapor", rapor.kurulusRaporu(y.getKurulus()));
        model.addAttribute("hizmetTurleri", HizmetTuru.values());
        model.addAttribute("gunler", DayOfWeek.values());
        return "yonetici";
    }

    @PostMapping("/sube")
    public String subeAc(@RequestParam String ad, @RequestParam(required = false) String adres,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime acilis,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime kapanis,
                         HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Şube açıldı: " + ad, () -> kurum.subeAc(y, y.getKurulus(), ad, adres,
                CalismaSaatleri.haftaIci(new SaatAraligi(acilis, kapanis))));
    }

    @PostMapping("/sube/{subeId}/calisma-saati")
    public String calismaSaati(@PathVariable long subeId, @RequestParam DayOfWeek gun,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime acilis,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime kapanis,
                               @RequestParam(defaultValue = "false") boolean kapali,
                               HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Çalışma saati güncellendi.", () -> kurum.calismaSaatiAyarla(y,
                kurum.sube(subeId), gun, kapali ? null : new SaatAraligi(acilis, kapanis)));
    }

    @PostMapping("/sube/{subeId}/bolum")
    public String bolumEkle(@PathVariable long subeId, @RequestParam String bolum, HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Bölüm eklendi: " + bolum, () -> kurum.bolumEkle(y, kurum.sube(subeId), bolum));
    }

    @PostMapping("/sube/{subeId}/hizmet")
    public String hizmetEkle(@PathVariable long subeId, @RequestParam String ad, @RequestParam HizmetTuru tur,
                             @RequestParam String bolum, @RequestParam int sureDakika, @RequestParam BigDecimal ucret,
                             HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Hizmet tanımlandı: " + ad, () -> kurum.hizmetTanimla(y, kurum.sube(subeId),
                ad, tur, bolum, Duration.ofMinutes(sureDakika), ucret));
    }

    @PostMapping("/sube/{subeId}/calisan")
    public String calisanEkle(@PathVariable long subeId, @RequestParam String tip, @RequestParam(required = false) String unvan,
                              @RequestParam String ad, @RequestParam String soyad, @RequestParam String alan,
                              HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        Sube sube = kurum.sube(subeId);
        return Islem.calistir(ra, "/yonetici", "Çalışan eklendi: " + ad + " " + soyad, () -> {
            if ("doktor".equals(tip)) {
                kurum.doktorEkle(y, sube, unvan, ad, soyad, alan, null, null);
            } else {
                kurum.personelEkle(y, sube, ad, soyad, alan, null, null);
            }
        });
    }

    @PostMapping("/calisan/{calisanId}/hizmet")
    public String hizmetAta(@PathVariable long calisanId, @RequestParam long hizmetId, HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Hizmet çalışana atandı.", () -> {
            if (!(kurum.kisi(calisanId) instanceof Calisan c)) {
                throw new IllegalArgumentException("Çalışan bulunamadı");
            }
            kurum.hizmetAta(y, c, kurum.hizmet(hizmetId));
        });
    }

    @PostMapping("/randevu/{id}/onayla")
    public String onayla(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Randevu onaylandı.", () -> randevu.onayla(id, y));
    }

    @PostMapping("/randevu/{id}/reddet")
    public String reddet(@PathVariable long id, @RequestParam(required = false) String gerekce, HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Randevu reddedildi.", () -> randevu.reddet(id, y, gerekce));
    }

    @PostMapping("/randevu/{id}/iptal")
    public String iptal(@PathVariable long id, @RequestParam(required = false) String gerekce, HttpSession session, RedirectAttributes ra) {
        Yonetici y = oturum.gerekli(session, Yonetici.class);
        return Islem.calistir(ra, "/yonetici", "Randevu iptal edildi.", () -> randevu.iptalEt(id, y, gerekce));
    }
}
