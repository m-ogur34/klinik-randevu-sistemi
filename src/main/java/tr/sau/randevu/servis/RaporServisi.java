package tr.sau.randevu.servis;

import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.RandevuDurumu;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Yönetici raporu: bir kuruluşun randevularını şube ve hizmet bazında özetler.
 * Ciro, gerçekleşmiş veya kesinleşmiş (onaylı + tamamlanmış) randevuların, randevu anında sabitlenen
 * ücretlerinin toplamıdır; onay bekleyenler "beklenen" olarak ayrıca gösterilir.
 */
public class RaporServisi {

    /** Bir gruba (şube veya hizmet) ait durum sayıları ve tutarlar. */
    public record Ozet(String ad, Map<RandevuDurumu, Long> durumlar, BigDecimal ciro, BigDecimal bekleyenTutar) {

        public long toplam() {
            return durumlar.values().stream().mapToLong(Long::longValue).sum();
        }

        public long sayi(RandevuDurumu durum) {
            return durumlar.getOrDefault(durum, 0L);
        }

        /** Reddedilen ve iptal edilenlerin toplama oranı (0–1). */
        public double kayipOrani() {
            long toplam = toplam();
            return toplam == 0 ? 0 : (double) (sayi(RandevuDurumu.REDDEDILDI) + sayi(RandevuDurumu.IPTAL_EDILDI)) / toplam;
        }
    }

    public record KurulusRaporu(Ozet genel, List<Ozet> subeler, List<Ozet> hizmetler) {
    }

    private final RandevuServisi randevuServisi;

    public RaporServisi(RandevuServisi randevuServisi) {
        this.randevuServisi = Objects.requireNonNull(randevuServisi);
    }

    public KurulusRaporu kurulusRaporu(SaglikKurulusu kurulus) {
        List<Randevu> randevular = randevuServisi.kurulusunRandevulari(kurulus);
        return new KurulusRaporu(
                ozetle(kurulus.getAd(), randevular),
                grupla(randevular, Randevu::getSube, r -> r.getSube().getAd()),
                // Aynı adlı hizmet farklı şubelerde ayrı nesnedir; etikete şube adı eklenir
                grupla(randevular, Randevu::getHizmet, r -> r.getHizmet().getAd() + " · " + r.getSube().getAd()));
    }

    /** Randevuları anahtara göre gruplar; grubun etiketi gruptaki ilk randevudan üretilir. */
    private static <K> List<Ozet> grupla(List<Randevu> randevular, Function<Randevu, K> anahtar, Function<Randevu, String> etiket) {
        return randevular.stream()
                .collect(Collectors.groupingBy(anahtar))
                .values().stream()
                .map(grup -> ozetle(etiket.apply(grup.get(0)), grup))
                .sorted(Comparator.comparing(Ozet::ciro).reversed().thenComparing(Ozet::ad))
                .toList();
    }

    private static Ozet ozetle(String ad, List<Randevu> randevular) {
        Map<RandevuDurumu, Long> durumlar = new EnumMap<>(RandevuDurumu.class);
        randevular.forEach(r -> durumlar.merge(r.getDurum(), 1L, Long::sum));
        BigDecimal ciro = toplamUcret(randevular, RandevuDurumu.ONAYLANDI, RandevuDurumu.TAMAMLANDI);
        BigDecimal bekleyen = toplamUcret(randevular, RandevuDurumu.ONAY_BEKLIYOR);
        return new Ozet(ad, durumlar, ciro, bekleyen);
    }

    private static BigDecimal toplamUcret(List<Randevu> randevular, RandevuDurumu... durumlar) {
        List<RandevuDurumu> istenen = List.of(durumlar);
        return randevular.stream()
                .filter(r -> istenen.contains(r.getDurum()))
                .map(Randevu::getUcret)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
