package tr.sau.randevu.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tr.sau.randevu.model.kisi.Calisan;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.kisi.Rol;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.servis.KurumServisi;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GirisController {

    private final KurumServisi kurum;
    private final Oturum oturum;

    public GirisController(KurumServisi kurum, Oturum oturum) {
        this.kurum = kurum;
        this.oturum = oturum;
    }

    @GetMapping("/")
    public String anaSayfa(HttpSession session, Model model) {
        Kisi k = oturum.kullanici(session);
        if (k != null) {
            return "redirect:" + panel(k);
        }
        Map<Rol, List<Kisi>> roller = new LinkedHashMap<>();
        for (Rol r : Rol.values()) {
            roller.put(r, kurum.kisiler().stream().filter(x -> x.getRol() == r)
                    .sorted(Comparator.comparing(Kisi::gorunenAd)).toList());
        }
        model.addAttribute("roller", roller);
        return "giris";
    }

    @PostMapping("/giris")
    public String giris(@RequestParam long kisiId, HttpSession session) {
        oturum.girisYap(session, kisiId);
        return "redirect:" + panel(kurum.kisi(kisiId));
    }

    @PostMapping("/cikis")
    public String cikis(HttpSession session) {
        oturum.cikisYap(session);
        return "redirect:/";
    }

    @PostMapping("/kayit")
    public String kayit(@RequestParam String ad, @RequestParam String soyad, @RequestParam String tcKimlikNo,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dogumTarihi,
                        @RequestParam(required = false) String telefon, @RequestParam(required = false) String eposta,
                        HttpSession session, RedirectAttributes ra) {
        try {
            Hasta h = kurum.hastaKaydet(ad, soyad, tcKimlikNo, dogumTarihi, bos(telefon), bos(eposta));
            oturum.girisYap(session, h.getId());
            ra.addFlashAttribute("bilgi", "Kaydınız oluşturuldu, hoş geldiniz " + h.getAd());
            return "redirect:/hasta";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("hata", e.getMessage());
            return "redirect:/";
        }
    }

    private static String bos(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    static String panel(Kisi k) {
        if (k instanceof Hasta) {
            return "/hasta";
        }
        if (k instanceof Calisan) {
            return "/calisan";
        }
        if (k instanceof Yonetici) {
            return "/yonetici";
        }
        return "/";
    }
}
