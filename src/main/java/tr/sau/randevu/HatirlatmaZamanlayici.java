package tr.sau.randevu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tr.sau.randevu.servis.HatirlatmaServisi;

/** Hatırlatma servisini 15 dakikada bir çalıştırır (ilk çalışma açılıştan 10 sn sonra). */
@Component
@EnableScheduling
public class HatirlatmaZamanlayici {

    private static final Logger LOG = LoggerFactory.getLogger(HatirlatmaZamanlayici.class);
    private final HatirlatmaServisi hatirlatma;

    public HatirlatmaZamanlayici(HatirlatmaServisi hatirlatma) {
        this.hatirlatma = hatirlatma;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 15 * 60 * 1000)
    public void calistir() {
        int adet = hatirlatma.yaklasanlariHatirlat().size();
        if (adet > 0) {
            LOG.info("{} randevu için hatırlatma gönderildi", adet);
        }
    }
}
