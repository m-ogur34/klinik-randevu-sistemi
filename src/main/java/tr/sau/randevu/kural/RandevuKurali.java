package tr.sau.randevu.kural;

import tr.sau.randevu.hata.RandevuHatasi;

/**
 * Bir randevu talebinin geçerli olması için sağlanması gereken tek bir iş kuralı (Strategy kalıbı).
 *
 * <p>Her kural ayrı bir sınıftır (Single Responsibility). Yeni bir kural eklemek için randevu servisini
 * değiştirmek gerekmez; yalnızca yeni bir sınıf yazılıp kural listesine eklenir (Open/Closed).
 */
@FunctionalInterface
public interface RandevuKurali {

    /** Kural sağlanmıyorsa kullanıcıya gösterilecek mesajla {@link RandevuHatasi} fırlatır. */
    void dogrula(RandevuTalebi talep) throws RandevuHatasi;
}
