package tr.sau.randevu.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.olay.BildirimKutusu;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tüm sayfalara oturumdaki kullanıcıyı ve bildirimlerini ekler; yetki hatalarında girişe yönlendirir. */
@ControllerAdvice
public class OrtakModel {

    private static final Locale TURKCE = Locale.forLanguageTag("tr-TR");

    private final Oturum oturum;
    private final BildirimKutusu bildirimler;

    public OrtakModel(Oturum oturum, BildirimKutusu bildirimler) {
        this.oturum = oturum;
        this.bildirimler = bildirimler;
    }

    @ModelAttribute("kullanici")
    public Kisi kullanici(HttpSession session) {
        return oturum.kullanici(session);
    }

    @ModelAttribute("bildirimler")
    public List<BildirimKutusu.Bildirim> bildirimler(HttpSession session) {
        Kisi k = oturum.kullanici(session);
        return k == null ? List.of() : bildirimler.kisininBildirimleri(k.getId());
    }

    @ModelAttribute("gunAdlari")
    public Map<DayOfWeek, String> gunAdlari() {
        Map<DayOfWeek, String> adlar = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek g : DayOfWeek.values()) {
            adlar.put(g, g.getDisplayName(TextStyle.FULL, TURKCE));
        }
        return adlar;
    }

    @ExceptionHandler(YetkiHatasi.class)
    public String yetkiHatasi(YetkiHatasi e, RedirectAttributes ra) {
        ra.addFlashAttribute("hata", e.getMessage());
        return "redirect:/";
    }
}
