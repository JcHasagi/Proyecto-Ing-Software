package co.edu.uis.siminversion.orden;

import co.edu.uis.siminversion.instrumento.Instrumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Intención del usuario de comprar o vender. El hecho ejecutado es la {@code Transaccion}. */
@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portafolio_id", nullable = false)
    private Long portafolioId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoOrden tipo;

    @Column(nullable = false)
    private long cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @Column(name = "fecha_emision_sim", nullable = false)
    private LocalDate fechaEmisionSim;

    /** Fecha simulada en que se ejecutó, rechazó o canceló. */
    @Column(name = "fecha_resolucion_sim")
    private LocalDate fechaResolucionSim;

    /** Motivo del rechazo o la cancelación. */
    @Column(length = 255)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Version
    private Long version;

    protected Orden() {
        // Requerido por JPA.
    }

    public Orden(Long portafolioId, Instrumento instrumento, TipoOrden tipo, long cantidad, LocalDate fechaEmisionSim,
                 LocalDateTime creadoEn) {
        this.portafolioId = portafolioId;
        this.instrumento = instrumento;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.estado = EstadoOrden.PENDIENTE;
        this.fechaEmisionSim = fechaEmisionSim;
        this.creadoEn = creadoEn;
    }

    public boolean estaPendiente() {
        return estado == EstadoOrden.PENDIENTE;
    }

    public void ejecutar(LocalDate fecha) {
        resolver(EstadoOrden.EJECUTADA, fecha, null);
    }

    public void rechazar(LocalDate fecha, String motivo) {
        resolver(EstadoOrden.RECHAZADA, fecha, motivo);
    }

    public void cancelar(LocalDate fecha, String motivo) {
        resolver(EstadoOrden.CANCELADA, fecha, motivo);
    }

    private void resolver(EstadoOrden nuevoEstado, LocalDate fecha, String motivo) {
        if (!estaPendiente()) {
            throw new IllegalStateException("La orden " + id + " ya está " + estado);
        }
        this.estado = nuevoEstado;
        this.fechaResolucionSim = fecha;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getPortafolioId() {
        return portafolioId;
    }

    public Instrumento getInstrumento() {
        return instrumento;
    }

    public TipoOrden getTipo() {
        return tipo;
    }

    public long getCantidad() {
        return cantidad;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public LocalDate getFechaEmisionSim() {
        return fechaEmisionSim;
    }

    public LocalDate getFechaResolucionSim() {
        return fechaResolucionSim;
    }

    public String getMotivo() {
        return motivo;
    }
}
