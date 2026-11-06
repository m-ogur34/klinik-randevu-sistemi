package tr.sau.randevu.model.kurum;

public enum HizmetTuru {
    MUAYENE("Muayene"),
    RADYOLOJI("Radyoloji"),
    LABORATUVAR("Laboratuvar"),
    FIZYOTERAPI("Fizyoterapi"),
    DIGER("Diğer");

    private final String gorunenAd;

    HizmetTuru(String gorunenAd) {
        this.gorunenAd = gorunenAd;
    }

    public String getGorunenAd() {
        return gorunenAd;
    }
}
