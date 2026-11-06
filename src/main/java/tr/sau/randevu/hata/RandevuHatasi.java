package tr.sau.randevu.hata;

/** İş kuralı ihlali (çakışma, uygunluk dışı saat, geçersiz durum geçişi vb.). Mesaj kullanıcıya gösterilir. */
public class RandevuHatasi extends RuntimeException {

    public RandevuHatasi(String mesaj) {
        super(mesaj);
    }
}
