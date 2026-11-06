package tr.sau.randevu.olay;

/**
 * Observer kalıbı: randevu servisi olayları yayınlar, dinleyiciler tepki verir. Servis, bildirimin
 * e-posta mı SMS mi ekran mesajı mı olduğunu bilmez; yeni bir bildirim kanalı yeni bir dinleyicidir.
 */
@FunctionalInterface
public interface RandevuDinleyici {

    void olayGerceklesti(RandevuOlayi olay);
}
