package tr.sau.randevu.servis;

import tr.sau.randevu.depo.Depo;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;
import tr.sau.randevu.olay.BildirimKutusu;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Yaklaşan onaylı randevular için hastaya hatırlatma gönderir. Her randevu için yalnızca bir kez
 * hatırlatılır; uygulamada Spring zamanlayıcısı bu servisi düzenli aralıklarla çağırır.
 */
public class HatirlatmaServisi {

    private static final DateTimeFormatter TARIH = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Depo<Randevu> randevuDepo;
    private final BildirimKutusu bildirimler;
    private final Clock saat;
    private final Duration onceden;
    private final Set<Long> hatirlatilanlar = new HashSet<>();

    public HatirlatmaServisi(Depo<Randevu> randevuDepo, BildirimKutusu bildirimler, Clock saat, Duration onceden) {
        this.randevuDepo = Objects.requireNonNull(randevuDepo);
        this.bildirimler = Objects.requireNonNull(bildirimler);
        this.saat = Objects.requireNonNull(saat);
        this.onceden = Objects.requireNonNull(onceden);
    }

    /**
     * Şu andan itibaren {@code onceden} süresi içinde başlayacak, onaylanmış ve henüz hatırlatılmamış
     * randevular için hastaya bildirim gönderir.
     *
     * @return bu çağrıda hatırlatılan randevular
     */
    public synchronized List<Randevu> yaklasanlariHatirlat() {
        LocalDateTime simdi = LocalDateTime.now(saat);
        LocalDateTime sinir = simdi.plus(onceden);
        List<Randevu> hatirlatilacak = randevuDepo.filtrele(r ->
                r.getDurum() == RandevuDurumu.ONAYLANDI
                        && r.getZaman().baslangic().isAfter(simdi)
                        && !r.getZaman().baslangic().isAfter(sinir)
                        && !hatirlatilanlar.contains(r.getId()));
        for (Randevu r : hatirlatilacak) {
            long kalanDakika = Duration.between(simdi, r.getZaman().baslangic()).toMinutes();
            bildirimler.bildir(r.getHasta().getId(), "Hatırlatma: " + r.getHizmet().getAd() + " randevunuz "
                    + r.getZaman().baslangic().format(TARIH) + " (" + kalanSure(kalanDakika) + " sonra), "
                    + r.getCalisan().gorunenAd() + ", " + r.getSube().tamAd(), simdi);
            hatirlatilanlar.add(r.getId());
        }
        return hatirlatilacak;
    }

    private static String kalanSure(long dakika) {
        long saat = dakika / 60;
        long dk = dakika % 60;
        if (saat == 0) {
            return dk + " dk";
        }
        return dk == 0 ? saat + " saat" : saat + " saat " + dk + " dk";
    }
}
