package tr.sau.randevu.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.hata.YetkiHatasi;

/** Form işlemlerini çalıştırır: başarıda bilgi, iş kuralı hatasında hata mesajı gösterip aynı sayfaya döner. */
final class Islem {

    private Islem() {
    }

    static String calistir(RedirectAttributes ra, String donus, String basariMesaji, Runnable is) {
        try {
            is.run();
            ra.addFlashAttribute("bilgi", basariMesaji);
        } catch (YetkiHatasi e) {
            throw e; // OrtakModel ele alır
        } catch (RandevuHatasi | IllegalArgumentException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:" + donus;
    }
}
