package tr.sau.randevu.model.kisi;

import java.time.LocalDate;

public class Hasta extends Kisi {

    private final String tcKimlikNo;
    private final LocalDate dogumTarihi;

    public Hasta(String ad, String soyad, String tcKimlikNo, LocalDate dogumTarihi, String telefon, String eposta) {
        super(ad, soyad, telefon, eposta);
        if (tcKimlikNo == null || !tcKimlikNo.matches("\\d{11}")) {
            throw new IllegalArgumentException("T.C. kimlik numarası 11 haneli olmalı");
        }
        this.tcKimlikNo = tcKimlikNo;
        this.dogumTarihi = zorunlu(dogumTarihi, "doğum tarihi");
    }

    @Override
    public Rol getRol() {
        return Rol.HASTA;
    }

    /** Kişisel veri: ekranlarda yalnızca maskelenmiş hali gösterilir (ör. 123******01). */
    public String maskeliTcKimlikNo() {
        return tcKimlikNo.substring(0, 3) + "******" + tcKimlikNo.substring(9);
    }

    public boolean tcKimlikNoEslesirMi(String aday) {
        return tcKimlikNo.equals(aday);
    }

    public LocalDate getDogumTarihi() {
        return dogumTarihi;
    }
}
