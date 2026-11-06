package tr.sau.randevu.model.randevu;

import java.time.DayOfWeek;
import java.util.Objects;

/** Bir çalışanın haftalık tekrar eden uygunluk dilimi, ör. Salı 09:00–12:00. */
public record UygunlukDilimi(DayOfWeek gun, SaatAraligi saatAraligi) {

    public UygunlukDilimi {
        Objects.requireNonNull(gun, "gün");
        Objects.requireNonNull(saatAraligi, "saat aralığı");
    }

    public boolean kapsarMi(ZamanAraligi aralik) {
        return aralik.tekGundeMi()
                && aralik.baslangic().getDayOfWeek() == gun
                && saatAraligi.kapsarMi(aralik.saatAraligi());
    }

    public boolean cakisirMi(UygunlukDilimi diger) {
        return gun == diger.gun && saatAraligi.cakisirMi(diger.saatAraligi);
    }

    @Override
    public String toString() {
        return gun + " " + saatAraligi;
    }
}
