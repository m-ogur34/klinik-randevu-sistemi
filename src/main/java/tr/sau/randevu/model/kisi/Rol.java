package tr.sau.randevu.model.kisi;

/** Sistemdeki kullanıcı rolleri. */
public enum Rol {
    HASTA("Hasta"),
    DOKTOR("Doktor"),
    SAGLIK_PERSONELI("Sağlık Personeli"),
    YONETICI("Hastane / Klinik Yöneticisi");

    private final String gorunenAd;

    Rol(String gorunenAd) {
        this.gorunenAd = gorunenAd;
    }

    public String getGorunenAd() {
        return gorunenAd;
    }
}
