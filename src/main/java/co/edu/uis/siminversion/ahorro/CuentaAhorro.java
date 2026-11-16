package co.edu.uis.siminversion.ahorro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuenta de ahorro de un portafolio (0..1). No guarda saldo: el saldo se deriva de las transacciones de
 * ahorro (RN-06). Solo recuerda su tasa y hasta qué fecha ya se sumaron intereses.
 */
@Entity
@Table(name = "cuenta_ahorro")
public class CuentaAhorro {

    @Id
    @Column(name = "portafolio_id")
    private Long portafolioId;

    /** Tasa nominal anual (0,04 = 4 %), fija desde la apertura. */
    @Column(name = "tasa_anual", nullable = false, precision = 9, scale = 6)
    private BigDecimal tasaAnual;

    @Column(name = "fecha_apertura_sim", nullable = false)
    private LocalDate fechaAperturaSim;

    @Column(name = "fecha_ultima_capitalizacion", nullable = false)
    private LocalDate fechaUltimaCapitalizacion;

    protected CuentaAhorro() {
        // Requerido por JPA.
    }

    public CuentaAhorro(Long portafolioId, BigDecimal tasaAnual, LocalDate fechaApertura) {
        this.portafolioId = portafolioId;
        this.tasaAnual = tasaAnual;
        this.fechaAperturaSim = fechaApertura;
        this.fechaUltimaCapitalizacion = fechaApertura;
    }

    public void capitalizadaHasta(LocalDate fecha) {
        if (fecha.isBefore(fechaUltimaCapitalizacion)) {
            throw new IllegalStateException("La capitalización no puede retroceder");
        }
        this.fechaUltimaCapitalizacion = fecha;
    }

    public Long getPortafolioId() {
        return portafolioId;
    }

    public BigDecimal getTasaAnual() {
        return tasaAnual;
    }

    public LocalDate getFechaAperturaSim() {
        return fechaAperturaSim;
    }

    public LocalDate getFechaUltimaCapitalizacion() {
        return fechaUltimaCapitalizacion;
    }
}
