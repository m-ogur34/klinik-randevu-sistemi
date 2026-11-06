package tr.sau.randevu.depo;

import tr.sau.randevu.model.Varlik;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic depo arayüzü (Repository kalıbı). Servisler verinin nerede tutulduğunu bilmez;
 * bellek içi depo ileride bir veritabanı deposuyla değiştirilebilir (Dependency Inversion).
 */
public interface Depo<T extends Varlik> {

    T kaydet(T nesne);

    Optional<T> bul(long id);

    List<T> tumu();

    List<T> filtrele(Predicate<? super T> kosul);

    default T getir(long id) {
        return bul(id).orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı: id=" + id));
    }
}
