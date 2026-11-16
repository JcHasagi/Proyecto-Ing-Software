package co.edu.uis.siminversion.portafolio;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Portafolio ficticio con su propio reloj de simulación. No guarda efectivo ni posiciones:
 * se derivan del libro de transacciones (RN-06).
 */
@Entity
@Table(name = "portafolio")
public class Portafolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "saldo_inicial", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoInicial;

    @Column(name = "fecha_inicio_sim", nullable = false)
    private LocalDate fechaInicioSim;

    @Column(name = "fecha_actual_sim", nullable = false)
    private LocalDate fechaActualSim;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPortafolio estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    /** Escenario histórico del que salió (REQ-014); null en un portafolio normal. */
    @Column(name = "escenario_id")
    private Long escenarioId;

    /** Bloqueo optimista: dos operaciones simultáneas sobre el mismo portafolio no pueden pisarse. */
    @Version
    private Long version;

    protected Portafolio() {
        // Requerido por JPA.
    }

    public Portafolio(Long usuarioId, String nombre, BigDecimal saldoInicial, LocalDate fechaInicioSim,
                      Long escenarioId, LocalDateTime creadoEn) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.saldoInicial = saldoInicial;
        this.fechaInicioSim = fechaInicioSim;
        this.fechaActualSim = fechaInicioSim;
        this.escenarioId = escenarioId;
        this.estado = EstadoPortafolio.EN_CURSO;
        this.creadoEn = creadoEn;
    }

    public boolean admiteOperaciones() {
        return estado == EstadoPortafolio.EN_CURSO;
    }

    /**
     * Un portafolio finalizado o archivado solo se consulta. {@code consecuencia} completa el mensaje para el
     * usuario, por ejemplo "solo permite consultas.".
     */
    public void exigirEnCurso(String consecuencia) {
        if (!admiteOperaciones()) {
            throw new OperacionInvalidaException("El portafolio está "
                    + estado.getEtiqueta().toLowerCase(Locale.ROOT) + ": " + consecuencia);
        }
    }

    /** RN-09: el reloj solo avanza. */
    public void avanzarA(LocalDate fecha) {
        if (!fecha.isAfter(fechaActualSim)) {
            throw new IllegalStateException("El reloj no puede retroceder: " + fechaActualSim + " -> " + fecha);
        }
        this.fechaActualSim = fecha;
    }

    public void finalizar() {
        this.estado = EstadoPortafolio.FINALIZADO;
    }

    public void archivar() {
        this.estado = EstadoPortafolio.ARCHIVADO;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public LocalDate getFechaInicioSim() {
        return fechaInicioSim;
    }

    public LocalDate getFechaActualSim() {
        return fechaActualSim;
    }

    public EstadoPortafolio getEstado() {
        return estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public Long getEscenarioId() {
        return escenarioId;
    }
}
