package tr.sau.randevu.model.kurum;

import tr.sau.randevu.model.Varlik;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/** Kurumun sunduğu bir sağlık hizmeti: süre ve ücretiyle birlikte (ör. Genel Muayene, 20 dk, 750 TL). */
public class Hizmet extends Varlik {

    private final String ad;
    private final HizmetTuru tur;
    private final String bolum;
    private Duration sure;
    private BigDecimal ucret;

    public Hizmet(String ad, HizmetTuru tur, String bolum, Duration sure, BigDecimal ucret) {
        if (ad == null || ad.isBlank()) {
            throw new IllegalArgumentException("Hizmet adı boş olamaz");
        }
        this.ad = ad.trim();
        this.tur = Objects.requireNonNull(tur, "hizmet türü");
        this.bolum = Objects.requireNonNull(bolum, "bölüm");
        setSure(sure);
        setUcret(ucret);
    }

    public String getAd() {
        return ad;
    }

    public HizmetTuru getTur() {
        return tur;
    }

    public String getBolum() {
        return bolum;
    }

    public Duration getSure() {
        return sure;
    }

    public void setSure(Duration sure) {
        if (sure == null || sure.isNegative() || sure.isZero() || sure.toMinutes() > 8 * 60) {
            throw new IllegalArgumentException("Hizmet süresi 1 dakika ile 8 saat arasında olmalı");
        }
        this.sure = sure;
    }

    public BigDecimal getUcret() {
        return ucret;
    }

    public void setUcret(BigDecimal ucret) {
        if (ucret == null || ucret.signum() < 0) {
            throw new IllegalArgumentException("Ücret negatif olamaz");
        }
        this.ucret = ucret;
    }

    @Override
    public String toString() {
        return ad + " (" + sure.toMinutes() + " dk, " + ucret + " TL)";
    }
}
