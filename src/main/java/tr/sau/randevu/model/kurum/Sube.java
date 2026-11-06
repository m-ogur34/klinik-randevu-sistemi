package tr.sau.randevu.model.kurum;

import tr.sau.randevu.model.Varlik;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Bir sağlık kuruluşunun şubesi: kendi adresi, çalışma saatleri, bölümleri ve hizmetleri vardır. */
public class Sube extends Varlik {

    private final SaglikKurulusu kurulus;
    private final String ad;
    private String adres;
    private CalismaSaatleri calismaSaatleri;
    private final Set<String> bolumler = new LinkedHashSet<>();
    private final Set<Hizmet> hizmetler = new LinkedHashSet<>();

    /** Paket içi: şubeler yalnızca SaglikKurulusu.subeAc(...) ile oluşturulur. */
    Sube(SaglikKurulusu kurulus, String ad, String adres, CalismaSaatleri calismaSaatleri) {
        this.kurulus = Objects.requireNonNull(kurulus);
        if (ad == null || ad.isBlank()) {
            throw new IllegalArgumentException("Şube adı boş olamaz");
        }
        this.ad = ad.trim();
        this.adres = adres;
        this.calismaSaatleri = Objects.requireNonNull(calismaSaatleri, "çalışma saatleri");
    }

    public void bolumEkle(String bolum) {
        bolumler.add(Objects.requireNonNull(bolum).trim());
    }

    public void hizmetEkle(Hizmet hizmet) {
        if (!bolumler.contains(hizmet.getBolum())) {
            throw new IllegalArgumentException("Önce '" + hizmet.getBolum() + "' bölümü tanımlanmalı");
        }
        hizmetler.add(hizmet);
    }

    public boolean hizmetSunuyorMu(Hizmet hizmet) {
        return hizmetler.contains(hizmet);
    }

    public SaglikKurulusu getKurulus() {
        return kurulus;
    }

    public String getAd() {
        return ad;
    }

    /** Ör. "Sakarya Şifa Hastanesi – Adapazarı" */
    public String tamAd() {
        return kurulus.getAd() + " – " + ad;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public CalismaSaatleri getCalismaSaatleri() {
        return calismaSaatleri;
    }

    public void setCalismaSaatleri(CalismaSaatleri calismaSaatleri) {
        this.calismaSaatleri = Objects.requireNonNull(calismaSaatleri);
    }

    public Set<String> getBolumler() {
        return Collections.unmodifiableSet(bolumler);
    }

    public Set<Hizmet> getHizmetler() {
        return Collections.unmodifiableSet(hizmetler);
    }

    @Override
    public String toString() {
        return tamAd();
    }
}
