package tr.sau.randevu.model;

/**
 * Depoda saklanan tüm nesnelerin ortak atası. Kimlik (id) yalnızca depo tarafından bir kez atanabilir;
 * dışarıdan değiştirilemez (kapsülleme).
 */
public abstract class Varlik {

    private Long id;

    public Long getId() {
        return id;
    }

    /** Depo tarafından kayıt sırasında çağrılır. */
    public void kimlikAta(long yeniId) {
        if (this.id != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " zaten kayıtlı (id=" + id + ")");
        }
        this.id = yeniId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || id == null) {
            return false;
        }
        return id.equals(((Varlik) o).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
