package tr.sau.randevu.olay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.sau.randevu.model.randevu.Randevu;

/**
 * Her randevu olayını uygulama günlüğüne yazar (denetim kaydı). Observer kalıbının genişletilebilirliğine
 * örnek: randevu servisine dokunmadan yalnızca yeni bir dinleyici eklenerek yeni bir davranış kazanıldı.
 */
public class RandevuGunlugu implements RandevuDinleyici {

    private static final Logger LOG = LoggerFactory.getLogger(RandevuGunlugu.class);

    @Override
    public void olayGerceklesti(RandevuOlayi olay) {
        Randevu r = olay.randevu();
        LOG.info("[{}] randevu#{} {} -> {} | {} {} | islem: {}", olay.tur(), r.getId(), r.getHasta().gorunenAd(),
                r.getCalisan().gorunenAd(), r.getHizmet().getAd(), r.getZaman().baslangic(), olay.islemYapan().gorunenAd());
    }
}
