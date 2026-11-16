package co.edu.uis.siminversion.precio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/** Solo lectura desde JPA: las filas se insertan en lote con JDBC durante la carga masiva (REQ-018). */
@Entity
@Immutable
@Table(name = "precio_historico")
public class PrecioHistorico {

    @EmbeddedId
    private PrecioHistoricoId id;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal apertura;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal maximo;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal minimo;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal cierre;

    @Column
    private Long volumen;

    protected PrecioHistorico() {
        // Requerido por JPA.
    }

    public PrecioHistoricoId getId() {
        return id;
    }

    public BigDecimal getCierre() {
        return cierre;
    }

    public PrecioDiario comoPrecioDiario() {
        return new PrecioDiario(id.getFecha(), apertura, maximo, minimo, cierre, volumen);
    }
}
