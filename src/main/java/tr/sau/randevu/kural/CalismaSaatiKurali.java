package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.model.randevu.ZamanAraligi;

/** Randevu, şubenin o günkü çalışma saatleri içinde başlamalı ve bitmeli. */
public class CalismaSaatiKurali implements RandevuKurali {

    @Override
    public void dogrula(RandevuTalebi talep) {
        ZamanAraligi z = talep.zaman();
        if (!z.tekGundeMi()) {
            throw new RandevuHatasi("Randevu tek bir gün içinde bitmeli");
        }
        var calismaSaatleri = talep.calisan().getSube().getCalismaSaatleri();
        if (!calismaSaatleri.kapsarMi(z.baslangic().getDayOfWeek(), z.saatAraligi())) {
            String acik = calismaSaatleri.getir(z.baslangic().getDayOfWeek()).map(Object::toString).orElse("kapalı");
            throw new RandevuHatasi("Şube bu saatte hizmet vermiyor (o gün çalışma saatleri: " + acik + ")");
        }
    }
}
