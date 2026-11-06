package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.kurum.Sube;

public class Doktor extends Calisan {

    private final String unvan;
    private final String uzmanlikAlani;

    public Doktor(String unvan, String ad, String soyad, String uzmanlikAlani, Sube sube, String telefon, String eposta) {
        super(ad, soyad, sube, telefon, eposta);
        this.unvan = unvan == null || unvan.isBlank() ? "Dr." : unvan.trim();
        this.uzmanlikAlani = zorunlu(uzmanlikAlani, "uzmanlık alanı");
    }

    @Override
    public Rol getRol() {
        return Rol.DOKTOR;
    }

    /** Çok biçimlilik: doktorlar unvanlarıyla gösterilir. */
    @Override
    public String gorunenAd() {
        return unvan + " " + super.gorunenAd();
    }

    public String getUnvan() {
        return unvan;
    }

    public String getUzmanlikAlani() {
        return uzmanlikAlani;
    }
}
