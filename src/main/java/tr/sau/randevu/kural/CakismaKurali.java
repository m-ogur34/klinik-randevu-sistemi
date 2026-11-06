package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.model.randevu.Randevu;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Çakışma kontrolü: aynı çalışanın ya da aynı hastanın aktif randevularıyla zaman çakışması olamaz.
 * İki kontrol aynı mantığı paylaştığı için tek sınıfta, iki örnek olarak kullanılır.
 */
public class CakismaKurali implements RandevuKurali {

    public enum Kapsam { CALISAN, HASTA }

    private static final DateTimeFormatter SAAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final Kapsam kapsam;

    public CakismaKurali(Kapsam kapsam) {
        this.kapsam = Objects.requireNonNull(kapsam);
    }

    @Override
    public void dogrula(RandevuTalebi talep) {
        for (Randevu r : talep.aktifRandevular()) {
            boolean ayniKisi = kapsam == Kapsam.CALISAN
                    ? Objects.equals(r.getCalisan().getId(), talep.calisan().getId())
                    : Objects.equals(r.getHasta().getId(), talep.hasta().getId());
            if (ayniKisi && r.aktifMi() && r.getZaman().cakisirMi(talep.zaman())) {
                String kim = kapsam == Kapsam.CALISAN ? talep.calisan().gorunenAd() + " adlı çalışanın" : "Sizin";
                throw new RandevuHatasi(kim + " bu saatle çakışan bir randevusu var ("
                        + r.getZaman().baslangic().format(SAAT) + ")");
            }
        }
    }
}
