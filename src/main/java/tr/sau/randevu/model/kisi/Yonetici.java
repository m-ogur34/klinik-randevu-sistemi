package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;

/** Bir sağlık kuruluşunun yöneticisi; yalnızca kendi kuruluşu üzerinde tanımlama yapabilir. */
public class Yonetici extends Kisi {

    private final SaglikKurulusu kurulus;

    public Yonetici(String ad, String soyad, SaglikKurulusu kurulus, String telefon, String eposta) {
        super(ad, soyad, telefon, eposta);
        this.kurulus = zorunlu(kurulus, "kuruluş");
    }

    @Override
    public Rol getRol() {
        return Rol.YONETICI;
    }

    public SaglikKurulusu getKurulus() {
        return kurulus;
    }

    public boolean yetkiliMi(SaglikKurulusu hedef) {
        return kurulus.equals(hedef);
    }

    public boolean yetkiliMi(Sube sube) {
        return yetkiliMi(sube.getKurulus());
    }
}
