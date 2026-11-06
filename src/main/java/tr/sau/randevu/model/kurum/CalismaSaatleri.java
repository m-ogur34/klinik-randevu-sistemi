package tr.sau.randevu.model.kurum;

import tr.sau.randevu.model.randevu.SaatAraligi;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Bir şubenin haftalık çalışma saatleri. Tanımlanmayan günler kapalı kabul edilir. */
public class CalismaSaatleri {

    private final Map<DayOfWeek, SaatAraligi> gunler = new EnumMap<>(DayOfWeek.class);

    public CalismaSaatleri ayarla(DayOfWeek gun, SaatAraligi aralik) {
        gunler.put(gun, aralik);
        return this;
    }

    public CalismaSaatleri kapat(DayOfWeek gun) {
        gunler.remove(gun);
        return this;
    }

    /** Pazartesi–Cuma aynı saatler için kısayol. */
    public static CalismaSaatleri haftaIci(SaatAraligi aralik) {
        CalismaSaatleri c = new CalismaSaatleri();
        for (DayOfWeek g : DayOfWeek.values()) {
            if (g.getValue() <= 5) {
                c.ayarla(g, aralik);
            }
        }
        return c;
    }

    public Optional<SaatAraligi> getir(DayOfWeek gun) {
        return Optional.ofNullable(gunler.get(gun));
    }

    public boolean kapsarMi(DayOfWeek gun, SaatAraligi aralik) {
        return getir(gun).map(acik -> acik.kapsarMi(aralik)).orElse(false);
    }

    public Map<DayOfWeek, SaatAraligi> tumu() {
        return Collections.unmodifiableMap(gunler);
    }
}
