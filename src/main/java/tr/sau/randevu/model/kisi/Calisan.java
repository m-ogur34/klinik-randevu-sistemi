package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.model.randevu.ZamanAraligi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Randevu verebilen sağlık çalışanlarının ortak atası (Doktor, Sağlık Personeli).
 * Hangi hizmetleri verdiği ve hangi zaman dilimlerinde uygun olduğu burada tutulur.
 */
public abstract class Calisan extends Kisi implements RandevuVerebilir {

    private final Sube sube;
    private final Set<Hizmet> hizmetler = new LinkedHashSet<>();
    private final List<UygunlukDilimi> uygunluklar = new ArrayList<>();

    protected Calisan(String ad, String soyad, Sube sube, String telefon, String eposta) {
        super(ad, soyad, telefon, eposta);
        this.sube = zorunlu(sube, "şube");
    }

    @Override
    public Sube getSube() {
        return sube;
    }

    @Override
    public Set<Hizmet> getHizmetler() {
        return Collections.unmodifiableSet(hizmetler);
    }

    public void hizmetEkle(Hizmet hizmet) {
        if (!sube.hizmetSunuyorMu(hizmet)) {
            throw new IllegalArgumentException(sube.getAd() + " şubesi '" + hizmet.getAd() + "' hizmetini sunmuyor");
        }
        hizmetler.add(hizmet);
    }

    @Override
    public List<UygunlukDilimi> getUygunluklar() {
        return Collections.unmodifiableList(uygunluklar);
    }

    public void uygunlukEkle(UygunlukDilimi dilim) {
        if (!sube.getCalismaSaatleri().kapsarMi(dilim.gun(), dilim.saatAraligi())) {
            throw new IllegalArgumentException("Uygunluk dilimi şubenin çalışma saatleri dışında: " + dilim);
        }
        for (UygunlukDilimi mevcut : uygunluklar) {
            if (mevcut.cakisirMi(dilim)) {
                throw new IllegalArgumentException("Uygunluk dilimi mevcut bir dilimle çakışıyor: " + mevcut);
            }
        }
        uygunluklar.add(dilim);
    }

    @Override
    public boolean uygunMu(ZamanAraligi aralik) {
        return uygunluklar.stream().anyMatch(d -> d.kapsarMi(aralik));
    }
}
