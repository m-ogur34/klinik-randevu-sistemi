package tr.sau.randevu.olay;

import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.randevu.Randevu;

import java.time.LocalDateTime;

/** Randevu üzerinde gerçekleşen bir olay; dinleyicilere (Observer) iletilir. */
public record RandevuOlayi(Tur tur, Randevu randevu, Kisi islemYapan, LocalDateTime zaman) {

    public enum Tur { OLUSTURULDU, ONAYLANDI, REDDEDILDI, IPTAL_EDILDI, TAMAMLANDI }
}
