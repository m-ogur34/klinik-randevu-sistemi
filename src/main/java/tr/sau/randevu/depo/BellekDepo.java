package tr.sau.randevu.depo;

import tr.sau.randevu.model.Varlik;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/** Bellek içi, iş parçacığı güvenli depo. Kayıtlar eklenme sırasıyla (id) döner. */
public class BellekDepo<T extends Varlik> implements Depo<T> {

    private final Map<Long, T> kayitlar = new ConcurrentSkipListMap<>();
    private final AtomicLong sayac = new AtomicLong();

    @Override
    public <S extends T> S kaydet(S nesne) {
        if (nesne.getId() == null) {
            nesne.kimlikAta(sayac.incrementAndGet());
        }
        kayitlar.put(nesne.getId(), nesne);
        return nesne;
    }

    @Override
    public Optional<T> bul(long id) {
        return Optional.ofNullable(kayitlar.get(id));
    }

    @Override
    public List<T> tumu() {
        return new ArrayList<>(kayitlar.values());
    }

    @Override
    public List<T> filtrele(Predicate<? super T> kosul) {
        return kayitlar.values().stream().filter(kosul).map(t -> (T) t).toList();
    }
}
