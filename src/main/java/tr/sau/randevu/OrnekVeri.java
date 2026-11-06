package tr.sau.randevu;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tr.sau.randevu.model.kisi.Calisan;
import tr.sau.randevu.model.kisi.Doktor;
import tr.sau.randevu.model.kisi.Hasta;
import tr.sau.randevu.model.kisi.SaglikPersoneli;
import tr.sau.randevu.model.kisi.Yonetici;
import tr.sau.randevu.model.kurum.CalismaSaatleri;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.HizmetTuru;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.model.randevu.SaatAraligi;
import tr.sau.randevu.model.randevu.UygunlukDilimi;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Uygulama açılırken demo verisini yükler: çok şubeli bir hastane ve tek şubeli bir klinik.
 * Tüm kişi ve kurum adları kurgusaldır. {@code ornek-veri.yukle=false} ile kapatılabilir.
 */
@Component
@ConditionalOnProperty(name = "ornek-veri.yukle", havingValue = "true", matchIfMissing = true)
public class OrnekVeri implements ApplicationRunner {

    private final KurumServisi kurum;
    private final RandevuServisi randevu;
    private final Clock saat;

    public OrnekVeri(KurumServisi kurum, RandevuServisi randevu, Clock saat) {
        this.kurum = kurum;
        this.randevu = randevu;
        this.saat = saat;
    }

    @Override
    public void run(ApplicationArguments args) {
        // --- Çok şubeli hastane
        SaglikKurulusu hastane = kurum.kurulusOlustur("Sakarya Şifa Hastanesi", SaglikKurulusu.Tur.HASTANE);
        Yonetici zeynep = kurum.yoneticiAta(hastane, "Zeynep", "Arslan", "0264 000 00 01", "zeynep.arslan@sifa.example");

        CalismaSaatleri adapazariSaat = CalismaSaatleri.haftaIci(SaatAraligi.of("08:00", "17:00"))
                .ayarla(DayOfWeek.SATURDAY, SaatAraligi.of("09:00", "13:00"));
        Sube adapazari = kurum.subeAc(zeynep, hastane, "Adapazarı", "Merkez Mah. Sağlık Cad. No:1", adapazariSaat);
        Sube serdivan = kurum.subeAc(zeynep, hastane, "Serdivan", "Kampüs Yolu No:12",
                CalismaSaatleri.haftaIci(SaatAraligi.of("09:00", "18:00")));

        for (Sube s : List.of(adapazari, serdivan)) {
            kurum.bolumEkle(zeynep, s, "Dahiliye");
            kurum.bolumEkle(zeynep, s, "Laboratuvar");
        }
        kurum.bolumEkle(zeynep, adapazari, "Kardiyoloji");
        kurum.bolumEkle(zeynep, serdivan, "Radyoloji");

        Hizmet muayeneA = kurum.hizmetTanimla(zeynep, adapazari, "Genel Muayene", HizmetTuru.MUAYENE, "Dahiliye", dk(20), tl(750));
        Hizmet kardiyoloji = kurum.hizmetTanimla(zeynep, adapazari, "Kardiyoloji Muayenesi", HizmetTuru.MUAYENE, "Kardiyoloji", dk(30), tl(1100));
        Hizmet ekg = kurum.hizmetTanimla(zeynep, adapazari, "EKG", HizmetTuru.DIGER, "Kardiyoloji", dk(15), tl(400));
        Hizmet hemogramA = kurum.hizmetTanimla(zeynep, adapazari, "Hemogram (Tam Kan Sayımı)", HizmetTuru.LABORATUVAR, "Laboratuvar", dk(10), tl(300));
        Hizmet muayeneS = kurum.hizmetTanimla(zeynep, serdivan, "Genel Muayene", HizmetTuru.MUAYENE, "Dahiliye", dk(20), tl(750));
        Hizmet rontgen = kurum.hizmetTanimla(zeynep, serdivan, "Akciğer Röntgeni", HizmetTuru.RADYOLOJI, "Radyoloji", dk(15), tl(600));
        Hizmet hemogramS = kurum.hizmetTanimla(zeynep, serdivan, "Hemogram (Tam Kan Sayımı)", HizmetTuru.LABORATUVAR, "Laboratuvar", dk(10), tl(300));

        Doktor ayse = kurum.doktorEkle(zeynep, adapazari, "Uzm. Dr.", "Ayşe", "Kaya", "İç Hastalıkları", null, "ayse.kaya@sifa.example");
        Doktor mehmet = kurum.doktorEkle(zeynep, adapazari, "Doç. Dr.", "Mehmet", "Yıldırım", "Kardiyoloji", null, null);
        Doktor elif = kurum.doktorEkle(zeynep, serdivan, "Dr.", "Elif", "Şahin", "İç Hastalıkları", null, null);
        SaglikPersoneli can = kurum.personelEkle(zeynep, adapazari, "Can", "Yıldız", "Laborant", null, null);
        SaglikPersoneli selin = kurum.personelEkle(zeynep, serdivan, "Selin", "Aydın", "Radyoloji Teknikeri", null, null);

        kurum.hizmetAta(zeynep, ayse, muayeneA);
        kurum.hizmetAta(zeynep, mehmet, kardiyoloji);
        kurum.hizmetAta(zeynep, mehmet, ekg);
        kurum.hizmetAta(zeynep, elif, muayeneS);
        kurum.hizmetAta(zeynep, can, hemogramA);
        kurum.hizmetAta(zeynep, selin, rontgen);
        kurum.hizmetAta(zeynep, selin, hemogramS);

        haftaIciUygunluk(ayse, "09:00", "12:00", DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        haftaIciUygunluk(ayse, "13:00", "16:00", DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        haftaIciUygunluk(mehmet, "10:00", "15:00", DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        haftaIciUygunluk(elif, "09:00", "17:00", DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        haftaIciUygunluk(can, "08:00", "11:00", DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        haftaIciUygunluk(can, "09:00", "12:00", DayOfWeek.SATURDAY);
        haftaIciUygunluk(selin, "09:00", "13:00", DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        // --- Tek şubeli klinik
        SaglikKurulusu klinik = kurum.kurulusOlustur("Serdivan Göz Kliniği", SaglikKurulusu.Tur.KLINIK);
        Yonetici emre = kurum.yoneticiAta(klinik, "Emre", "Koç", null, "emre.koc@gozklinigi.example");
        Sube gozMerkez = kurum.subeAc(emre, klinik, "Merkez", "Çark Cad. No:45",
                CalismaSaatleri.haftaIci(SaatAraligi.of("10:00", "19:00")));
        kurum.bolumEkle(emre, gozMerkez, "Göz Hastalıkları");
        Hizmet gozMuayene = kurum.hizmetTanimla(emre, gozMerkez, "Göz Muayenesi", HizmetTuru.MUAYENE, "Göz Hastalıkları", dk(20), tl(900));
        Doktor burak = kurum.doktorEkle(emre, gozMerkez, "Op. Dr.", "Burak", "Aksoy", "Göz Hastalıkları", null, null);
        kurum.hizmetAta(emre, burak, gozMuayene);
        haftaIciUygunluk(burak, "10:00", "18:00", DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        // --- Hastalar
        Hasta aysel = kurum.hastaKaydet("Aysel", "Öztürk", "00000000001", LocalDate.of(1990, 5, 1), "0555 000 00 01", null);
        Hasta murat = kurum.hastaKaydet("Murat", "Çelik", "00000000002", LocalDate.of(1985, 3, 3), null, null);
        kurum.hastaKaydet("Fatma", "Kılıç", "00000000003", LocalDate.of(1972, 9, 15), null, null);

        // --- Birkaç örnek randevu: biri onay bekliyor, biri onaylı
        ilkUygunSaatte(aysel, ayse, muayeneA);
        Randevu onayli = ilkUygunSaatte(murat, mehmet, kardiyoloji);
        if (onayli != null) {
            randevu.onayla(onayli.getId(), mehmet);
        }
        ilkUygunSaatte(murat, burak, gozMuayene);
    }

    private void haftaIciUygunluk(Calisan c, String bas, String bit, DayOfWeek... gunler) {
        for (DayOfWeek g : gunler) {
            kurum.uygunlukEkle(c, c, new UygunlukDilimi(g, SaatAraligi.of(bas, bit)));
        }
    }

    /** Bugünden itibaren 14 gün içindeki ilk boş saate randevu alır. */
    private Randevu ilkUygunSaatte(Hasta hasta, Calisan calisan, Hizmet hizmet) {
        LocalDate gun = LocalDate.now(saat).plusDays(1);
        for (int i = 0; i < 14; i++, gun = gun.plusDays(1)) {
            List<LocalDateTime> saatler = randevu.uygunSaatler(hasta, calisan, hizmet, gun);
            if (!saatler.isEmpty()) {
                return randevu.olustur(hasta, calisan, hizmet, saatler.get(Math.min(2, saatler.size() - 1)));
            }
        }
        return null;
    }

    private static Duration dk(int dakika) {
        return Duration.ofMinutes(dakika);
    }

    private static BigDecimal tl(int tutar) {
        return BigDecimal.valueOf(tutar);
    }
}
