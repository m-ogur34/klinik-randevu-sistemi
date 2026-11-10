package tr.sau.randevu;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tr.sau.randevu.depo.BellekDepo;
import tr.sau.randevu.depo.Depo;
import tr.sau.randevu.model.kisi.Kisi;
import tr.sau.randevu.model.kurum.Hizmet;
import tr.sau.randevu.model.kurum.SaglikKurulusu;
import tr.sau.randevu.model.kurum.Sube;
import tr.sau.randevu.model.randevu.Randevu;
import tr.sau.randevu.olay.BildirimKutusu;
import tr.sau.randevu.olay.RandevuGunlugu;
import tr.sau.randevu.servis.HatirlatmaServisi;
import tr.sau.randevu.servis.KurumServisi;
import tr.sau.randevu.servis.RandevuServisi;
import tr.sau.randevu.servis.RaporServisi;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Nesnelerin birbirine bağlandığı tek yer (composition root). Alan sınıfları ve servisler Spring'e
 * bağımlı değildir; Spring yalnızca burada devreye girer.
 */
@Configuration
public class UygulamaYapilandirmasi {

    @Bean
    Clock saat() {
        return Clock.system(ZoneId.of("Europe/Istanbul"));
    }

    @Bean
    Depo<Randevu> randevuDepo() {
        return new BellekDepo<>();
    }

    @Bean
    BildirimKutusu bildirimKutusu() {
        return new BildirimKutusu();
    }

    @Bean
    KurumServisi kurumServisi() {
        Depo<SaglikKurulusu> kuruluslar = new BellekDepo<>();
        Depo<Sube> subeler = new BellekDepo<>();
        Depo<Hizmet> hizmetler = new BellekDepo<>();
        Depo<Kisi> kisiler = new BellekDepo<>();
        return new KurumServisi(kuruluslar, subeler, hizmetler, kisiler);
    }

    @Bean
    RandevuServisi randevuServisi(Depo<Randevu> randevuDepo, Clock saat, BildirimKutusu bildirimKutusu) {
        RandevuServisi servis = RandevuServisi.varsayilan(randevuDepo, saat);
        servis.dinleyiciEkle(bildirimKutusu);
        servis.dinleyiciEkle(new RandevuGunlugu());
        return servis;
    }

    /** Onaylı randevulara başlamadan 24 saat önce hatırlatma gönderilir. */
    @Bean
    HatirlatmaServisi hatirlatmaServisi(Depo<Randevu> randevuDepo, BildirimKutusu bildirimKutusu, Clock saat) {
        return new HatirlatmaServisi(randevuDepo, bildirimKutusu, saat, Duration.ofHours(24));
    }

    @Bean
    RaporServisi raporServisi(RandevuServisi randevuServisi) {
        return new RaporServisi(randevuServisi);
    }
}
