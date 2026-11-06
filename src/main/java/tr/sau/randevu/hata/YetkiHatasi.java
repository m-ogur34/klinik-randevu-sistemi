package tr.sau.randevu.hata;

/** Kullanıcının rolü veya kuruluşu bu işlemi yapmaya yetkili değil. */
public class YetkiHatasi extends RandevuHatasi {

    public YetkiHatasi(String mesaj) {
        super(mesaj);
    }
}
