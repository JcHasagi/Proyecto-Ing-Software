package co.edu.uis.siminversion.orden;

import static co.edu.uis.siminversion.comun.Textos.textoDinero;
import static co.edu.uis.siminversion.comun.Textos.textoFecha;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioNoEncontradoException;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Emisión (REQ-006/007) y cancelación (REQ-008) de órdenes. La ejecución la hace el reloj (REQ-010). */
@Service
public class OrdenServicio {

    private final PortafolioServicio portafolioServicio;
    private final OrdenRepositorio ordenRepositorio;
    private final InstrumentoRepositorio instrumentoRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;
    private final Clock relojSistema;

    public OrdenServicio(PortafolioServicio portafolioServicio, OrdenRepositorio ordenRepositorio,
                         InstrumentoRepositorio instrumentoRepositorio, ProveedorDePrecios proveedorDePrecios,
                         Clock relojSistema) {
        this.portafolioServicio = portafolioServicio;
        this.ordenRepositorio = ordenRepositorio;
        this.instrumentoRepositorio = instrumentoRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
        this.relojSistema = relojSistema;
    }

    @Transactional(readOnly = true)
    public EstimacionOrden estimar(String correo, long portafolioId, String simbolo, TipoOrden tipo, Long cantidad) {
        return estimar(portafolioServicio.obtener(correo, portafolioId), simbolo, tipo, cantidad);
    }

    /** Valida de nuevo todo al confirmar y registra la orden como PENDIENTE; el efectivo no cambia todavía. */
    @Transactional
    public Orden emitir(String correo, long portafolioId, String simbolo, TipoOrden tipo, Long cantidad) {
        Portafolio portafolio = portafolioServicio.obtenerParaModificar(correo, portafolioId);
        EstimacionOrden estimacion = estimar(portafolio, simbolo, tipo, cantidad);
        return ordenRepositorio.save(new Orden(portafolio.getId(), estimacion.instrumento(), tipo, cantidad,
                portafolio.getFechaActualSim(), LocalDateTime.now(relojSistema)));
    }

    @Transactional
    public Orden cancelar(String correo, long portafolioId, long ordenId) {
        Portafolio portafolio = portafolioServicio.obtenerParaModificar(correo, portafolioId);
        Orden orden = ordenRepositorio.findByIdAndPortafolioId(ordenId, portafolio.getId())
                .orElseThrow(PortafolioNoEncontradoException::new);
        if (!orden.estaPendiente()) {
            throw new OperacionInvalidaException("La orden ya está " + orden.getEstado().getEtiqueta().toLowerCase(Locale.ROOT)
                    + "; no se puede cancelar.");
        }
        orden.cancelar(portafolio.getFechaActualSim(), "Cancelada por el usuario");
        return orden;
    }

    private EstimacionOrden estimar(Portafolio portafolio, String simbolo, TipoOrden tipo, Long cantidad) {
        portafolio.exigirEnCurso("solo permite consultas.");
        if (tipo == null) {
            throw new OperacionInvalidaException("Elige si quieres comprar o vender.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser un número entero mayor que cero.");
        }
        String simboloNormalizado = simbolo == null ? "" : simbolo.trim().toUpperCase(Locale.ROOT);
        Instrumento instrumento = instrumentoRepositorio.findBySimbolo(simboloNormalizado)
                .orElseThrow(() -> new OperacionInvalidaException("El instrumento " + simboloNormalizado
                        + " no está disponible."));
        // REQ-017: un instrumento deshabilitado no admite compras, pero se conservan y se pueden vender las posiciones.
        if (!instrumento.isActivo() && tipo == TipoOrden.COMPRA) {
            throw new OperacionInvalidaException("El instrumento " + instrumento.getSimbolo()
                    + " está deshabilitado: solo se pueden vender las unidades que ya tienes.");
        }
        LocalDate fechaSimulada = portafolio.getFechaActualSim();
        PrecioDiario referencia = proveedorDePrecios.ultimoCierreHasta(instrumento.getId(), fechaSimulada)
                .orElseThrow(() -> new OperacionInvalidaException(instrumento.getSimbolo()
                        + " todavía no tiene precios al " + textoFecha(fechaSimulada) + "; no se puede operar."));

        BigDecimal monto = ReglasDeOperacion.montoOperado(cantidad, referencia.cierre());
        BigDecimal comision = ReglasDeOperacion.comision(monto);
        EstadoDelLibro libro = portafolioServicio.libro(portafolio.getId());
        List<Orden> pendientes = ordenRepositorio.findByPortafolioIdAndEstadoOrderByIdAsc(portafolio.getId(),
                EstadoOrden.PENDIENTE);
        // Sin descontar lo comprometido, dos órdenes válidas por separado podrían superar juntas el efectivo.
        BigDecimal efectivoDisponible = libro.efectivo()
                .subtract(portafolioServicio.costoDeComprasPendientes(pendientes, fechaSimulada));
        long ventasPendientes = pendientes.stream()
                .filter(orden -> orden.getTipo() == TipoOrden.VENTA && orden.getInstrumento().getId().equals(instrumento.getId()))
                .mapToLong(Orden::getCantidad)
                .sum();
        long unidadesDisponibles = libro.cantidadDe(instrumento.getSimbolo()) - ventasPendientes;

        BigDecimal total;
        if (tipo == TipoOrden.COMPRA) {
            total = monto.add(comision);
            if (total.compareTo(efectivoDisponible) > 0) {
                throw new OperacionInvalidaException("Efectivo insuficiente: la compra costaría unos " + textoDinero(total)
                        + " con la comisión y tienes " + textoDinero(efectivoDisponible)
                        + " disponibles (efectivo menos compras pendientes).");
            }
        } else {
            if (unidadesDisponibles <= 0) {
                throw new OperacionInvalidaException("No tienes unidades de " + instrumento.getSimbolo()
                        + " disponibles para vender.");
            }
            if (cantidad > unidadesDisponibles) {
                throw new OperacionInvalidaException("Solo puedes vender " + unidadesDisponibles + " unidades de "
                        + instrumento.getSimbolo() + " (tu posición menos las ventas pendientes).");
            }
            total = monto.subtract(comision);
            if (efectivoDisponible.add(total).signum() < 0) {
                throw new OperacionInvalidaException("El ingreso de la venta no alcanza a cubrir la comisión de "
                        + textoDinero(comision) + ".");
            }
        }
        BigDecimal efectivoResultante = tipo == TipoOrden.COMPRA
                ? efectivoDisponible.subtract(total)
                : efectivoDisponible.add(total);
        return new EstimacionOrden(portafolio.getId(), instrumento, tipo, cantidad, referencia.cierre(),
                referencia.fecha(), fechaSimulada, monto, comision, total, efectivoDisponible, efectivoResultante,
                unidadesDisponibles);
    }
}
