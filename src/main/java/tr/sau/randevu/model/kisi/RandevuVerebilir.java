package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.model.randevu.ZamanAraligi;

import java.util.List;
import java.util.Set;

/**
 * Kendisinden randevu alınabilen her şeyin sözleşmesi. Randevu servisi Doktor ya da Sağlık Personeli
 * ayrımı yapmadan yalnızca bu arayüzle çalışır (çok biçimlilik, Dependency Inversion).
 * İleride ör. bir MR cihazı da bu arayüzü uygulayarak randevu verebilir hâle gelebilir.
 */
public interface RandevuVerebilir {

    Long getId();

    String gorunenAd();

    Sube getSube();

    Set<Hizmet> getHizmetler();

    List<UygunlukDilimi> getUygunluklar();

    /** Verilen zaman aralığının tamamı bir uygunluk dilimi içinde mi? */
    boolean uygunMu(ZamanAraligi aralik);

    default boolean hizmetVeriyorMu(Hizmet hizmet) {
        return getHizmetler().contains(hizmet);
    }
}
