package co.edu.uis.siminversion.transaccion;

import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.TipoOrden;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Movimiento del libro: inmutable. Efectivo y posiciones se calculan a partir de estas filas (RN-06). */
@Entity
@Immutable
@Table(name = "transaccion")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portafolio_id", nullable = false)
    private Long portafolioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoTransaccion tipo;

    @Column(name = "orden_id")
    private Long ordenId;

    /** Null en el depósito inicial. */
    @ManyToOne
    @JoinColumn(name = "instrumento_id")
    private Instrumento instrumento;

    /** Solo en compras y ventas. */
    @Column
    private Long cantidad;

    /** Solo en compras y ventas: cierre del día de ejecución. */
    @Column(name = "precio_ejecucion", precision = 20, scale = 8)
    private BigDecimal precioEjecucion;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal monto;

    @Column(name = "fecha_sim", nullable = false)
    private LocalDate fechaSim;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    protected Transaccion() {
        // Requerido por JPA.
    }

    private Transaccion(Long portafolioId, TipoTransaccion tipo, Long ordenId, Instrumento instrumento, Long cantidad,
                        BigDecimal precioEjecucion, BigDecimal monto, LocalDate fechaSim, LocalDateTime creadoEn) {
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto de una transacción no puede ser negativo: " + monto);
        }
        this.portafolioId = portafolioId;
        this.tipo = tipo;
        this.ordenId = ordenId;
        this.instrumento = instrumento;
        this.cantidad = cantidad;
        this.precioEjecucion = precioEjecucion;
        this.monto = monto;
        this.fechaSim = fechaSim;
        this.creadoEn = creadoEn;
    }

    /** REQ-003: el saldo inicial entra al portafolio como una transacción más. */
    public static Transaccion depositoInicial(Long portafolioId, BigDecimal monto, LocalDate fecha,
                                              LocalDateTime creadoEn) {
        return new Transaccion(portafolioId, TipoTransaccion.DEPOSITO_INICIAL, null, null, null, null, monto, fecha,
                creadoEn);
    }

    /** Compra o venta ejecutada de una orden. */
    public static Transaccion operacion(Orden orden, BigDecimal precioEjecucion, BigDecimal monto, LocalDate fecha,
                                        LocalDateTime creadoEn) {
        TipoTransaccion tipo = orden.getTipo() == TipoOrden.COMPRA ? TipoTransaccion.COMPRA : TipoTransaccion.VENTA;
        return new Transaccion(orden.getPortafolioId(), tipo, orden.getId(), orden.getInstrumento(), orden.getCantidad(),
                precioEjecucion, monto, fecha, creadoEn);
    }

    /** RN-04: la comisión de cada ejecución es su propia transacción. */
    public static Transaccion comision(Orden orden, BigDecimal monto, LocalDate fecha, LocalDateTime creadoEn) {
        return new Transaccion(orden.getPortafolioId(), TipoTransaccion.COMISION, orden.getId(), orden.getInstrumento(),
                null, null, monto, fecha, creadoEn);
    }

    /** REQ-013: depósito, retiro o interés de la cuenta de ahorro. */
    public static Transaccion movimientoDeAhorro(Long portafolioId, TipoTransaccion tipo, BigDecimal monto,
                                                 LocalDate fecha, LocalDateTime creadoEn) {
        if (!tipo.esDeAhorro()) {
            throw new IllegalArgumentException(tipo + " no es un movimiento de ahorro");
        }
        return new Transaccion(portafolioId, tipo, null, null, null, null, monto, fecha, creadoEn);
    }

    public Long getId() {
        return id;
    }

    public Long getPortafolioId() {
        return portafolioId;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public Instrumento getInstrumento() {
        return instrumento;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioEjecucion() {
        return precioEjecucion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDate getFechaSim() {
        return fechaSim;
    }
}
