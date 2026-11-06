package tr.sau.randevu.model.randevu;

import tr.sau.randevu.hata.RandevuHatasi;
import tr.sau.randevu.model.Varlik;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.RandevuVerebilir;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.Sube;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Randevu. Detaylar (hizmet, süre, ücret, çalışan, tarih/saat) oluşturulduğu an sabitlenir: hizmetin
 * ücreti sonradan değişse bile alınmış randevunun ücreti değişmez. Durum yalnızca kurallı geçiş
 * metotlarıyla değiştirilebilir (kapsülleme).
 */
public class Randevu extends Varlik {

    private final Hasta hasta;
    private final RandevuVerebilir calisan;
    private final Hizmet hizmet;
    private final Sube sube;
    private final ZamanAraligi zaman;
    private final BigDecimal ucret;
    private final LocalDateTime olusturulmaZamani;
    private RandevuDurumu durum = RandevuDurumu.ONAY_BEKLIYOR;
    private String durumAciklamasi;

    public Randevu(Hasta hasta, RandevuVerebilir calisan, Hizmet hizmet, LocalDateTime baslangic, LocalDateTime simdi) {
        this.hasta = Objects.requireNonNull(hasta, "hasta");
        this.calisan = Objects.requireNonNull(calisan, "çalışan");
        this.hizmet = Objects.requireNonNull(hizmet, "hizmet");
        this.sube = calisan.getSube();
        this.zaman = ZamanAraligi.sureIle(Objects.requireNonNull(baslangic, "başlangıç"), hizmet.getSure());
        this.ucret = hizmet.getUcret();
        this.olusturulmaZamani = Objects.requireNonNull(simdi);
    }

    public void onayla() {
        gec(RandevuDurumu.ONAYLANDI, null);
    }

    public void reddet(String gerekce) {
        gec(RandevuDurumu.REDDEDILDI, gerekce);
    }

    public void iptalEt(String gerekce) {
        gec(RandevuDurumu.IPTAL_EDILDI, gerekce);
    }

    public void tamamla() {
        gec(RandevuDurumu.TAMAMLANDI, null);
    }

    private void gec(RandevuDurumu yeni, String aciklama) {
        if (!durum.gecilebilecekDurumlar().contains(yeni)) {
            throw new RandevuHatasi("Randevu '" + durum.getGorunenAd() + "' durumundayken '"
                    + yeni.getGorunenAd() + "' durumuna geçemez");
        }
        this.durum = yeni;
        this.durumAciklamasi = aciklama;
    }

    public boolean aktifMi() {
        return durum.aktifMi();
    }

    public Hasta getHasta() {
        return hasta;
    }

    public RandevuVerebilir getCalisan() {
        return calisan;
    }

    public Hizmet getHizmet() {
        return hizmet;
    }

    public Sube getSube() {
        return sube;
    }

    public ZamanAraligi getZaman() {
        return zaman;
    }

    public BigDecimal getUcret() {
        return ucret;
    }

    public LocalDateTime getOlusturulmaZamani() {
        return olusturulmaZamani;
    }

    public RandevuDurumu getDurum() {
        return durum;
    }

    public String getDurumAciklamasi() {
        return durumAciklamasi;
    }

    @Override
    public String toString() {
        return "Randevu#" + getId() + " " + hasta.gorunenAd() + " → " + calisan.gorunenAd() + ", "
                + hizmet.getAd() + ", " + zaman.baslangic() + " [" + durum.getGorunenAd() + "]";
    }
}
