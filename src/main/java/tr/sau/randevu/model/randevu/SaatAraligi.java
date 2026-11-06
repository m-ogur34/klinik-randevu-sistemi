package tr.sau.randevu.model.randevu;

import java.time.LocalTime;
import java.util.Objects;

/** Gün içi saat aralığı, ör. 09:00–17:00. Değişmez (immutable) değer nesnesi. */
public record SaatAraligi(LocalTime baslangic, LocalTime bitis) {

    public SaatAraligi {
        Objects.requireNonNull(baslangic, "başlangıç");
        Objects.requireNonNull(bitis, "bitiş");
        if (!baslangic.isBefore(bitis)) {
            throw new IllegalArgumentException("Başlangıç saati bitişten önce olmalı: " + baslangic + "–" + bitis);
        }
    }

    public static SaatAraligi of(String baslangic, String bitis) {
        return new SaatAraligi(LocalTime.parse(baslangic), LocalTime.parse(bitis));
    }

    public boolean kapsarMi(SaatAraligi diger) {
        return !diger.baslangic.isBefore(baslangic) && !diger.bitis.isAfter(bitis);
    }

    /** Uç uca değen aralıklar (10:00–10:30 ve 10:30–11:00) çakışmaz. */
    public boolean cakisirMi(SaatAraligi diger) {
        return baslangic.isBefore(diger.bitis) && diger.baslangic.isBefore(bitis);
    }

    @Override
    public String toString() {
        return baslangic + "–" + bitis;
    }
}
