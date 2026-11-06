package tr.sau.randevu.model.kisi;

import tr.sau.randevu.model.Varlik;

import java.util.Objects;

/**
 * Sistemdeki tüm kişilerin soyut atası (kalıtım hiyerarşisinin kökü).
 * Her alt sınıf kendi rolünü ve görünen adını bildirmek zorundadır (soyutlama + çok biçimlilik).
 */
public abstract class Kisi extends Varlik {

    private final String ad;
    private final String soyad;
    private String telefon;
    private String eposta;

    protected Kisi(String ad, String soyad, String telefon, String eposta) {
        this.ad = zorunlu(ad, "ad");
        this.soyad = zorunlu(soyad, "soyad");
        setTelefon(telefon);
        setEposta(eposta);
    }

    public abstract Rol getRol();

    /** Ekranlarda gösterilecek ad; ör. doktorlar için "Dr. Ayşe Yılmaz". */
    public String gorunenAd() {
        return ad + " " + soyad;
    }

    public String getAd() {
        return ad;
    }

    public String getSoyad() {
        return soyad;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        if (telefon != null && !telefon.matches("[0-9 +()-]{7,20}")) {
            throw new IllegalArgumentException("Geçersiz telefon numarası: " + telefon);
        }
        this.telefon = telefon;
    }

    public String getEposta() {
        return eposta;
    }

    public void setEposta(String eposta) {
        if (eposta != null && !eposta.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Geçersiz e-posta: " + eposta);
        }
        this.eposta = eposta;
    }

    protected static String zorunlu(String deger, String alan) {
        if (deger == null || deger.isBlank()) {
            throw new IllegalArgumentException(alan + " boş olamaz");
        }
        return deger.trim();
    }

    @Override
    public String toString() {
        return getRol().getGorunenAd() + ": " + gorunenAd();
    }

    protected static <T> T zorunlu(T deger, String alan) {
        return Objects.requireNonNull(deger, alan + " boş olamaz");
    }
}
