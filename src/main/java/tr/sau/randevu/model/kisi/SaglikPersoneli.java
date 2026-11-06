package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.kurum.Sube;

/** Doktor dışındaki randevu verebilen personel: radyoloji teknikeri, laborant, fizyoterapist vb. */
public class SaglikPersoneli extends Calisan {

    private final String gorev;

    public SaglikPersoneli(String ad, String soyad, String gorev, Sube sube, String telefon, String eposta) {
        super(ad, soyad, sube, telefon, eposta);
        this.gorev = zorunlu(gorev, "görev");
    }

    @Override
    public Rol getRol() {
        return Rol.SAGLIK_PERSONELI;
    }

    @Override
    public String gorunenAd() {
        return super.gorunenAd() + " (" + gorev + ")";
    }

    public String getGorev() {
        return gorev;
    }
}
