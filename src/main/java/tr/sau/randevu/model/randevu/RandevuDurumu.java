package tr.sau.randevu.model.randevu;

import java.util.EnumSet;
import java.util.Set;

/**
 * Randevunun yaşam döngüsü. Hangi durumdan hangisine geçilebileceği burada tanımlıdır:
 *
 * <pre>
 *   ONAY_BEKLIYOR ──onayla──▶ ONAYLANDI ──tamamla──▶ TAMAMLANDI
 *        │                        │
 *        ├──reddet──▶ REDDEDILDI   └──iptal──▶ IPTAL_EDILDI
 *        └──iptal───▶ IPTAL_EDILDI
 * </pre>
 */
public enum RandevuDurumu {
    ONAY_BEKLIYOR("Onay bekliyor"),
    ONAYLANDI("Onaylandı"),
    REDDEDILDI("Reddedildi"),
    IPTAL_EDILDI("İptal edildi"),
    TAMAMLANDI("Tamamlandı");

    private final String gorunenAd;

    RandevuDurumu(String gorunenAd) {
        this.gorunenAd = gorunenAd;
    }

    public String getGorunenAd() {
        return gorunenAd;
    }

    public Set<RandevuDurumu> gecilebilecekDurumlar() {
        return switch (this) {
            case ONAY_BEKLIYOR -> EnumSet.of(ONAYLANDI, REDDEDILDI, IPTAL_EDILDI);
            case ONAYLANDI -> EnumSet.of(TAMAMLANDI, IPTAL_EDILDI);
            case REDDEDILDI, IPTAL_EDILDI, TAMAMLANDI -> EnumSet.noneOf(RandevuDurumu.class);
        };
    }

    /** Takvimde yer tutan (çakışma kontrolüne giren) durumlar. */
    public boolean aktifMi() {
        return this == ONAY_BEKLIYOR || this == ONAYLANDI;
    }
}
