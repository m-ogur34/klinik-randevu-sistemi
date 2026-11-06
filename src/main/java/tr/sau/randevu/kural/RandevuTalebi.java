package tr.sau.randevu.kural;

import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.RandevuVerebilir;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.ZamanAraligi;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kuralların değerlendirdiği randevu isteği.
 *
 * @param aktifRandevular sistemdeki takvimde yer tutan (onay bekleyen veya onaylı) randevular
 */
public record RandevuTalebi(Hasta hasta, RandevuVerebilir calisan, Hizmet hizmet, ZamanAraligi zaman,
                            LocalDateTime simdi, List<Randevu> aktifRandevular) {
}
