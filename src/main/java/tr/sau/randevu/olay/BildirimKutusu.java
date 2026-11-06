package tr.sau.randevu.olay;

import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.randevu.Randevu;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Olayları ilgili kişilere uygulama içi bildirim olarak iletir. */
public class BildirimKutusu implements RandevuDinleyici {

    public record Bildirim(Long aliciId, String mesaj, LocalDateTime zaman) {
    }

    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final List<Bildirim> bildirimler = new ArrayList<>();

    @Override
    public synchronized void olayGerceklesti(RandevuOlayi olay) {
        Randevu r = olay.randevu();
        String ozet = r.getHizmet().getAd() + ", " + r.getZaman().baslangic().format(TARIH) + ", " + r.getCalisan().gorunenAd();
        switch (olay.tur()) {
            case OLUSTURULDU -> gonder(r.getCalisan().getId(), "Yeni randevu talebi: " + r.getHasta().gorunenAd() + " – " + ozet, olay);
            case ONAYLANDI -> gonder(r.getHasta().getId(), "Randevunuz onaylandı: " + ozet, olay);
            case REDDEDILDI -> gonder(r.getHasta().getId(), "Randevunuz reddedildi: " + ozet + gerekce(r), olay);
            case IPTAL_EDILDI -> {
                Kisi yapan = olay.islemYapan();
                Long alici = Objects.equals(yapan.getId(), r.getHasta().getId()) ? r.getCalisan().getId() : r.getHasta().getId();
                gonder(alici, "Randevu iptal edildi: " + ozet + gerekce(r), olay);
            }
            case TAMAMLANDI -> gonder(r.getHasta().getId(), "Randevunuz tamamlandı: " + ozet, olay);
        }
    }

    private static String gerekce(Randevu r) {
        return r.getDurumAciklamasi() == null || r.getDurumAciklamasi().isBlank() ? "" : " (Gerekçe: " + r.getDurumAciklamasi() + ")";
    }

    private void gonder(Long aliciId, String mesaj, RandevuOlayi olay) {
        bildirimler.add(new Bildirim(aliciId, mesaj, olay.zaman()));
    }

    public synchronized List<Bildirim> kisininBildirimleri(Long kisiId) {
        return bildirimler.stream().filter(b -> Objects.equals(b.aliciId(), kisiId)).toList().reversed();
    }
}
