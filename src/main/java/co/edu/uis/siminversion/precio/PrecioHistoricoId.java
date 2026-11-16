package co.edu.uis.siminversion.precio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class PrecioHistoricoId implements Serializable {

    @Column(name = "instrumento_id", nullable = false)
    private Long instrumentoId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    protected PrecioHistoricoId() {
        // Requerido por JPA.
    }

    public PrecioHistoricoId(Long instrumentoId, LocalDate fecha) {
        this.instrumentoId = instrumentoId;
        this.fecha = fecha;
    }

    public Long getInstrumentoId() {
        return instrumentoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof PrecioHistoricoId id
                && Objects.equals(instrumentoId, id.instrumentoId)
                && Objects.equals(fecha, id.fecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentoId, fecha);
    }
}
