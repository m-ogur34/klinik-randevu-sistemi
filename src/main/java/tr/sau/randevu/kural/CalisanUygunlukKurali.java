package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;

/** Hastalar yalnızca çalışanın tanımlı uygunluk zaman dilimlerine randevu alabilir. */
public class CalisanUygunlukKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep) {
        if (!talep.calisan().uygunMu(talep.zaman())) {
            throw new RandevuHatasi(talep.calisan().gorunenAd() + " bu saatte randevu vermiyor");
        }
    }
}
