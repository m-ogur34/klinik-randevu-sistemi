package tr.sau.randevu.model.randevu;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Takvimde somut bir zaman aralığı, ör. 12.11.2026 10:00–10:20. */
public record ZamanAraligi(LocalDateTime baslangic, LocalDateTime bitis) {

    public ZamanAraligi {
        Objects.requireNonNull(baslangic, "başlangıç");
        Objects.requireNonNull(bitis, "bitiş");
        if (!baslangic.isBefore(bitis)) {
            throw new IllegalArgumentException("Başlangıç bitişten önce olmalı");
        }
    }

    public static ZamanAraligi sureIle(LocalDateTime baslangic, Duration sure) {
        return new ZamanAraligi(baslangic, baslangic.plus(sure));
    }

    public boolean cakisirMi(ZamanAraligi diger) {
        return baslangic.isBefore(diger.bitis) && diger.baslangic.isBefore(bitis);
    }

    /** Aralık tek bir güne mi ait? (Gece yarısını aşan randevulara izin verilmez.) */
    public boolean tekGundeMi() {
        return baslangic.toLocalDate().equals(bitis.toLocalDate());
    }

    /** Aynı günün saat aralığı olarak (tekGundeMi() true olmalı). */
    public SaatAraligi saatAraligi() {
        return new SaatAraligi(baslangic.toLocalTime(), bitis.toLocalTime());
    }

    public Duration sure() {
        return Duration.between(baslangic, bitis);
    }
}
