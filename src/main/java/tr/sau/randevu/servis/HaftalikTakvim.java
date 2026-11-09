package tr.sau.randevu.servis;

import tr.sau.randevu.model.kisi.RandevuVerebilir;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bir çalışanın Pazartesi–Pazar haftalık görünümü: her gün için uygunluk dilimleri ve o günün randevuları.
 * Reddedilen ve iptal edilen randevular takvimde yer tutmadığı için gösterilmez.
 */
public record HaftalikTakvim(LocalDate haftaBasi, List<Gun> gunler) {

    public record Gun(LocalDate tarih, boolean bugun, List<SaatAraligi> uygunluklar, List<Randevu> randevular) {

        /** Uygunluk dilimlerinin toplam dakikası. */
        public long uygunDakika() {
            return uygunluklar.stream().mapToLong(a -> Duration.between(a.baslangic(), a.bitis()).toMinutes()).sum();
        }

        /** Randevuların kapladığı toplam dakika. */
        public long doluDakika() {
            return randevular.stream().mapToLong(r -> r.getZaman().sure().toMinutes()).sum();
        }

        /** Doluluk oranı (0–100); uygunluk yoksa 0. */
        public int dolulukYuzdesi() {
            long uygun = uygunDakika();
            return uygun == 0 ? 0 : (int) Math.min(100, Math.round(100.0 * doluDakika() / uygun));
        }
    }

    /** Verilen tarihin içinde bulunduğu haftanın Pazartesi günü. */
    public static LocalDate haftaBasi(LocalDate tarih) {
        return tarih.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static HaftalikTakvim olustur(RandevuVerebilir calisan, List<Randevu> calisaninRandevulari,
                                          LocalDate herhangiBirGun, LocalDate bugun) {
        LocalDate pazartesi = haftaBasi(herhangiBirGun);
        List<Gun> gunler = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate tarih = pazartesi.plusDays(i);
            List<SaatAraligi> uygunluklar = calisan.getUygunluklar().stream()
                    .filter(d -> d.gun() == tarih.getDayOfWeek())
                    .map(UygunlukDilimi::saatAraligi)
                    .sorted(Comparator.comparing(SaatAraligi::baslangic))
                    .toList();
            List<Randevu> randevular = calisaninRandevulari.stream()
                    .filter(r -> r.getZaman().baslangic().toLocalDate().equals(tarih))
                    .filter(r -> r.getDurum() != RandevuDurumu.REDDEDILDI && r.getDurum() != RandevuDurumu.IPTAL_EDILDI)
                    .sorted(Comparator.comparing(r -> r.getZaman().baslangic()))
                    .toList();
            gunler.add(new Gun(tarih, tarih.equals(bugun), uygunluklar, randevular));
        }
        return new HaftalikTakvim(pazartesi, gunler);
    }

    public LocalDate oncekiHafta() {
        return haftaBasi.minusWeeks(1);
    }

    public LocalDate sonrakiHafta() {
        return haftaBasi.plusWeeks(1);
    }

    public LocalDate haftaSonu() {
        return haftaBasi.plusDays(6);
    }

    public int toplamRandevu() {
        return gunler.stream().mapToInt(g -> g.randevular().size()).sum();
    }
}
