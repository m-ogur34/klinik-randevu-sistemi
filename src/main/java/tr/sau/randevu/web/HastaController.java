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
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Hasta ekranı: adım adım randevu alma (şube → hizmet → çalışan → gün → saat) ve randevularım. */
@Controller
@RequestMapping("/hasta")
public class HastaController {

    private final KurumServisi kurum;
    private final RandevuServisi randevu;
    private final Oturum oturum;
    private final Clock saat;

    public HastaController(KurumServisi kurum, RandevuServisi randevu, Oturum oturum, Clock saat) {
        this.kurum = kurum;
        this.randevu = randevu;
        this.oturum = oturum;
        this.saat = saat;
    }

    @GetMapping
    public String panel(@RequestParam(required = false) Long subeId, @RequestParam(required = false) Long hizmetId,
                        @RequestParam(required = false) Long calisanId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tarih,
                        HttpSession session, Model model) {
        Hasta hasta = oturum.gerekli(session, Hasta.class);
        LocalDate gun = tarih != null ? tarih : LocalDate.now(saat).plusDays(1);
        model.addAttribute("subeler", kurum.subeler());
        model.addAttribute("tarih", gun);
        model.addAttribute("enErkenTarih", LocalDate.now(saat));

        if (subeId != null) {
            Sube sube = kurum.sube(subeId);
            model.addAttribute("sube", sube);
            if (hizmetId != null && sube.getHizmetler().stream().anyMatch(h -> h.getId() == hizmetId.longValue())) {
                Hizmet hizmet = kurum.hizmet(hizmetId);
                model.addAttribute("hizmet", hizmet);
                model.addAttribute("calisanlar", kurum.hizmetVerenler(sube, hizmet));
                if (calisanId != null && kurum.kisi(calisanId) instanceof Calisan c && c.hizmetVeriyorMu(hizmet)) {
                    var saatler = randevu.uygunSaatler(hasta, c, hizmet, gun);
                    // Gün seçilmemişse önümüzdeki iki hafta içindeki ilk uygun güne atla
                    for (int i = 0; tarih == null && saatler.isEmpty() && i < 14; i++) {
                        gun = gun.plusDays(1);
                        saatler = randevu.uygunSaatler(hasta, c, hizmet, gun);
                    }
                    model.addAttribute("tarih", gun);
                    model.addAttribute("calisan", c);
                    model.addAttribute("saatler", saatler);
                }
            }
        }
        model.addAttribute("randevular", randevu.hastaninRandevulari(hasta));
        return "hasta";
    }

    @PostMapping("/randevu")
    public String randevuAl(@RequestParam long calisanId, @RequestParam long hizmetId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baslangic,
                            HttpSession session, RedirectAttributes ra) {
        Hasta hasta = oturum.gerekli(session, Hasta.class);
        return Islem.calistir(ra, "/hasta", "Randevu talebiniz alındı; onaylandığında bildirim alacaksınız.", () -> {
            if (!(kurum.kisi(calisanId) instanceof Calisan c)) {
                throw new IllegalArgumentException("Çalışan bulunamadı");
            }
            randevu.olustur(hasta, c, kurum.hizmet(hizmetId), baslangic);
        });
    }

    @PostMapping("/randevu/{id}/iptal")
    public String iptal(@PathVariable long id, @RequestParam(required = false) String gerekce,
                        HttpSession session, RedirectAttributes ra) {
        Hasta hasta = oturum.gerekli(session, Hasta.class);
        return Islem.calistir(ra, "/hasta", "Randevu iptal edildi.", () -> randevu.iptalEt(id, hasta, gerekce));
    }
}
