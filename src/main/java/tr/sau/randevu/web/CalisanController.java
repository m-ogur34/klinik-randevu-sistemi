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
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.servis.HaftalikTakvim;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Doktor ve sağlık personeli ekranı: onay bekleyenler, takvim, uygunluk dilimleri. */
@Controller
@RequestMapping("/calisan")
public class CalisanController {

    private final KurumServisi kurum;
    private final RandevuServisi randevu;
    private final Oturum oturum;
    private final Clock saat;

    public CalisanController(KurumServisi kurum, RandevuServisi randevu, Oturum oturum, Clock saat) {
        this.kurum = kurum;
        this.randevu = randevu;
        this.oturum = oturum;
        this.saat = saat;
    }

    @GetMapping
    public String panel(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hafta,
                        HttpSession session, Model model) {
        Calisan calisan = oturum.gerekli(session, Calisan.class);
        LocalDateTime simdi = LocalDateTime.now(saat);
        List<Randevu> tumu = randevu.calisaninRandevulari(calisan);
        model.addAttribute("takvim", HaftalikTakvim.olustur(calisan, tumu, hafta != null ? hafta : simdi.toLocalDate(), simdi.toLocalDate()));
        model.addAttribute("bekleyenler", tumu.stream().filter(r -> r.getDurum() == RandevuDurumu.ONAY_BEKLIYOR).toList());
        model.addAttribute("onaylilar", tumu.stream().filter(r -> r.getDurum() == RandevuDurumu.ONAYLANDI).toList());
        model.addAttribute("gecmis", tumu.stream().filter(r -> !r.aktifMi()).toList().reversed());
        model.addAttribute("uygunluklar", calisan.getUygunluklar().stream()
                .sorted(Comparator.comparing(UygunlukDilimi::gun).thenComparing(d -> d.saatAraligi().baslangic())).toList());
        model.addAttribute("gunler", DayOfWeek.values());
        model.addAttribute("simdi", simdi);
        return "calisan";
    }

    @PostMapping("/randevu/{id}/onayla")
    public String onayla(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Calisan c = oturum.gerekli(session, Calisan.class);
        return Islem.calistir(ra, "/calisan", "Randevu onaylandı.", () -> randevu.onayla(id, c));
    }

    @PostMapping("/randevu/{id}/reddet")
    public String reddet(@PathVariable long id, @RequestParam(required = false) String gerekce, HttpSession session, RedirectAttributes ra) {
        Calisan c = oturum.gerekli(session, Calisan.class);
        return Islem.calistir(ra, "/calisan", "Randevu reddedildi.", () -> randevu.reddet(id, c, gerekce));
    }

    @PostMapping("/randevu/{id}/iptal")
    public String iptal(@PathVariable long id, @RequestParam(required = false) String gerekce, HttpSession session, RedirectAttributes ra) {
        Calisan c = oturum.gerekli(session, Calisan.class);
        return Islem.calistir(ra, "/calisan", "Randevu iptal edildi.", () -> randevu.iptalEt(id, c, gerekce));
    }

    @PostMapping("/randevu/{id}/tamamla")
    public String tamamla(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        Calisan c = oturum.gerekli(session, Calisan.class);
        return Islem.calistir(ra, "/calisan", "Randevu tamamlandı olarak işaretlendi.", () -> randevu.tamamla(id, c));
    }

    @PostMapping("/uygunluk")
    public String uygunlukEkle(@RequestParam DayOfWeek gun,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime baslangic,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime bitis,
                               HttpSession session, RedirectAttributes ra) {
        Calisan c = oturum.gerekli(session, Calisan.class);
        return Islem.calistir(ra, "/calisan", "Uygunluk dilimi eklendi.",
                () -> kurum.uygunlukEkle(c, c, new UygunlukDilimi(gun, new SaatAraligi(baslangic, bitis))));
    }
}
