package tr.sau.randevu.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import tr.sau.randevu.hata.YetkiHatasi;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.servis.KurumServisi;

/**
 * Oturumdaki kullanıcı. Demo amaçlı giriş, kullanıcı listesinden seçilerek yapılır (şifre yoktur);
 * gerçek bir sistemde bu sınıfın yerini Spring Security alır.
 */
@Component
public class Oturum {

    private static final String ANAHTAR = "kullaniciId";
    private final KurumServisi kurum;

    public Oturum(KurumServisi kurum) {
        this.kurum = kurum;
    }

    public void girisYap(HttpSession session, long kisiId) {
        kurum.kisi(kisiId); // var mı kontrolü
        session.setAttribute(ANAHTAR, kisiId);
    }

    public void cikisYap(HttpSession session) {
        session.invalidate();
    }

    public Kisi kullanici(HttpSession session) {
        Object id = session.getAttribute(ANAHTAR);
        return id == null ? null : kurum.kisi((Long) id);
    }

    /** Oturumdaki kullanıcı beklenen türdeyse onu döndürür, değilse YetkiHatasi fırlatır. */
    public <T extends Kisi> T gerekli(HttpSession session, Class<T> tur) {
        Kisi k = kullanici(session);
        if (!tur.isInstance(k)) {
            throw new YetkiHatasi("Bu sayfa için uygun rolle giriş yapmalısınız");
        }
        return tur.cast(k);
    }
}
