package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;

/** Seçilen çalışan bu hizmeti vermeli ve hizmet çalışanın şubesinde sunulmalı. */
public class HizmetKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep) {
        if (!talep.calisan().getSube().hizmetSunuyorMu(talep.hizmet())) {
            throw new RandevuHatasi(talep.calisan().getSube().tamAd() + " '" + talep.hizmet().getAd() + "' hizmetini sunmuyor");
        }
        if (!talep.calisan().hizmetVeriyorMu(talep.hizmet())) {
            throw new RandevuHatasi(talep.calisan().gorunenAd() + " '" + talep.hizmet().getAd() + "' hizmetini vermiyor");
        }
    }
}
