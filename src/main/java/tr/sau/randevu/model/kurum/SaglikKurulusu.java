package tr.sau.randevu.model.kurum;

import tr.sau.randevu.model.Varlik;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Hastane veya klinik. Tek şubeli bir klinik de çok şubeli bir hastane grubu da aynı modelle temsil
 * edilir: tek şubeli kuruluşun yalnızca bir şubesi vardır.
 */
public class SaglikKurulusu extends Varlik {

    public enum Tur { HASTANE, KLINIK }

    private final String ad;
    private final Tur tur;
    private final List<Sube> subeler = new ArrayList<>();

    public SaglikKurulusu(String ad, Tur tur) {
        if (ad == null || ad.isBlank()) {
            throw new IllegalArgumentException("Kuruluş adı boş olamaz");
        }
        this.ad = ad.trim();
        this.tur = Objects.requireNonNull(tur, "kuruluş türü");
    }

    /** Şube yalnızca kuruluş üzerinden oluşturulabilir; böylece şube–kuruluş ilişkisi her zaman tutarlıdır. */
    public Sube subeAc(String subeAdi, String adres, CalismaSaatleri calismaSaatleri) {
        boolean ayniAd = subeler.stream().anyMatch(s -> s.getAd().equalsIgnoreCase(subeAdi));
        if (ayniAd) {
            throw new IllegalArgumentException("Bu adla bir şube zaten var: " + subeAdi);
        }
        Sube sube = new Sube(this, subeAdi, adres, calismaSaatleri);
        subeler.add(sube);
        return sube;
    }

    public boolean cokSubeliMi() {
        return subeler.size() > 1;
    }

    public String getAd() {
        return ad;
    }

    public Tur getTur() {
        return tur;
    }

    public List<Sube> getSubeler() {
        return Collections.unmodifiableList(subeler);
    }

    @Override
    public String toString() {
        return ad;
    }
}
