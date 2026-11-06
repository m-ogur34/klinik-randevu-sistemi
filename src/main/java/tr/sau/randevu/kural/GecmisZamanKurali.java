package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;

/** Geçmiş bir zamana randevu alınamaz. */
public class GecmisZamanKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep) {
        if (!talep.zaman().baslangic().isAfter(talep.simdi())) {
            throw new RandevuHatasi("Geçmiş bir zamana randevu alınamaz");
        }
    }
}
