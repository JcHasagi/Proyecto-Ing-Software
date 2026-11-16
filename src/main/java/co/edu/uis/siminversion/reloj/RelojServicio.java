package co.edu.uis.siminversion.reloj;

import static co.edu.uis.siminversion.comun.Textos.textoDinero;
import static co.edu.uis.siminversion.comun.Textos.textoFecha;

import co.edu.uis.siminversion.ahorro.CalculadoraDeIntereses;
import co.edu.uis.siminversion.ahorro.CuentaAhorro;
import co.edu.uis.siminversion.ahorro.CuentaAhorroRepositorio;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.OrdenRepositorio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import co.edu.uis.siminversion.transaccion.Posicion;
import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import co.edu.uis.siminversion.transaccion.Transaccion;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avance del reloj de simulación (REQ-009) y ejecución de órdenes pendientes (REQ-010).
 * Recorre los días bursátiles del mercado uno por uno: en cada uno (1) ejecuta las órdenes, (2) suma los
 * intereses de la cuenta de ahorro y (3) guarda el valor del portafolio. Todo el avance ocurre en una sola
 * transacción de base de datos.
 */
@Service
public class RelojServicio {

    /** Atajos de la interfaz para elegir la fecha de destino. */
    public enum Salto { DIA, SEMANA, MES }

    private final PortafolioServicio portafolioServicio;
    private final OrdenRepositorio ordenRepositorio;
    private final TransaccionRepositorio transaccionRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;
    private final RegistroDeInstantaneas registroDeInstantaneas;
    private final CuentaAhorroRepositorio cuentaAhorroRepositorio;
    private final Clock relojSistema;

    public RelojServicio(PortafolioServicio portafolioServicio, OrdenRepositorio ordenRepositorio,
                         TransaccionRepositorio transaccionRepositorio, ProveedorDePrecios proveedorDePrecios,
                         RegistroDeInstantaneas registroDeInstantaneas, CuentaAhorroRepositorio cuentaAhorroRepositorio,
                         Clock relojSistema) {
        this.portafolioServicio = portafolioServicio;
        this.ordenRepositorio = ordenRepositorio;
        this.transaccionRepositorio = transaccionRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
        this.registroDeInstantaneas = registroDeInstantaneas;
        this.cuentaAhorroRepositorio = cuentaAhorroRepositorio;
        this.relojSistema = relojSistema;
    }

    /** Fecha de destino de un atajo, sin pasar del último dato del histórico. */
    @Transactional(readOnly = true)
    public LocalDate destinoDeSalto(String correo, long portafolioId, Salto salto) {
        Portafolio portafolio = portafolioServicio.obtener(correo, portafolioId);
        LocalDate actual = portafolio.getFechaActualSim();
        LocalDate destino = switch (salto) {
            case DIA -> proveedorDePrecios.primerDiaBursatilDelMercadoDesde(actual.plusDays(1)).orElse(actual.plusDays(1));
            case SEMANA -> actual.plusWeeks(1);
            case MES -> actual.plusMonths(1);
        };
        LocalDate limite = portafolioServicio.fechaLimite(portafolio);
        return destino.isAfter(limite) ? limite : destino;
    }

    @Transactional
    public ResumenAvance avanzar(String correo, long portafolioId, LocalDate destino) {
        Portafolio portafolio = portafolioServicio.obtenerParaModificar(correo, portafolioId);
        portafolio.exigirEnCurso("su reloj ya no avanza.");
        if (destino == null) {
            throw new OperacionInvalidaException("Indica la fecha hasta la que quieres avanzar.");
        }
        LocalDate desde = portafolio.getFechaActualSim();
        if (!destino.isAfter(desde)) {
            throw new OperacionInvalidaException("El reloj no puede retroceder: elige una fecha posterior al "
                    + textoFecha(desde) + ".");
        }
        // Límite del reloj: el fin del escenario (REQ-014) o el último dato del histórico.
        boolean esEscenario = portafolio.getEscenarioId() != null;
        LocalDate limite = portafolioServicio.fechaLimite(portafolio);
        if (destino.isAfter(limite)) {
            throw new OperacionInvalidaException((esEscenario
                    ? "El escenario termina el "
                    : "La última fecha disponible del histórico es el ") + textoFecha(limite) + ".");
        }
        List<LocalDate> dias = proveedorDePrecios.diasBursatilesDelMercado(desde, destino);
        if (dias.isEmpty()) {
            throw new OperacionInvalidaException("No hay días bursátiles entre el " + textoFecha(desde) + " y el "
                    + textoFecha(destino) + "; elige una fecha posterior.");
        }

        EstadoDelLibro libro = portafolioServicio.libro(portafolio.getId());
        List<Orden> pendientes = new ArrayList<>(
                ordenRepositorio.findByPortafolioIdAndEstadoOrderByIdAsc(portafolio.getId(), EstadoOrden.PENDIENTE));
        SeriesDePrecios series = new SeriesDePrecios(proveedorDePrecios, desde, destino);
        ResumenAvance.Constructor resumen = new ResumenAvance.Constructor();
        List<InstantaneaDiaria> instantaneas = new ArrayList<>(dias.size());
        LocalDateTime ahora = LocalDateTime.now(relojSistema);

        CuentaAhorro cuenta = cuentaAhorroRepositorio.findById(portafolio.getId()).orElse(null);

        for (LocalDate dia : dias) {
            ejecutarPendientes(dia, pendientes, libro, series, resumen, ahora);
            capitalizarIntereses(portafolio, dia, cuenta, libro, resumen, ahora);
            instantaneas.add(valorar(portafolio.getId(), dia, libro, series));
            portafolio.avanzarA(dia);
        }
        registroDeInstantaneas.insertar(instantaneas);

        // Sin más días bursátiles hasta el límite, el portafolio queda finalizado y las pendientes se cancelan.
        LocalDate fechaFinal = portafolio.getFechaActualSim();
        // Basta saber si queda algún día bursátil antes del límite; no hace falta traer la lista completa.
        boolean finalizado = proveedorDePrecios.primerDiaBursatilDelMercadoDesde(fechaFinal.plusDays(1))
                .filter(siguiente -> !siguiente.isAfter(limite))
                .isEmpty();
        if (finalizado) {
            String motivo = esEscenario
                    ? "El escenario terminó antes de que pudiera ejecutarse"
                    : "El histórico terminó antes de que pudiera ejecutarse";
            for (Orden orden : pendientes) {
                orden.cancelar(fechaFinal, motivo);
                resumen.cancelada(orden, fechaFinal);
            }
            portafolio.finalizar();
        }
        return resumen.construir(desde, portafolio.getFechaActualSim(), dias.size(), instantaneas.getLast(), finalizado);
    }

    /**
     * REQ-010 para un día: cada orden emitida antes de ese día cuyo instrumento cotizó se ejecuta al cierre del día
     * (RN-03), en orden de emisión, o se rechaza si ya no es válida. Si el instrumento no cotizó, sigue pendiente.
     */
    private void ejecutarPendientes(LocalDate dia, List<Orden> pendientes, EstadoDelLibro libro,
                                    SeriesDePrecios series, ResumenAvance.Constructor resumen, LocalDateTime ahora) {
        Iterator<Orden> iterador = pendientes.iterator();
        while (iterador.hasNext()) {
            Orden orden = iterador.next();
            if (!orden.getFechaEmisionSim().isBefore(dia)) {
                continue;
            }
            Optional<BigDecimal> cierre = series.cierreEn(orden.getInstrumento().getId(), dia);
            if (cierre.isEmpty()) {
                continue;
            }
            BigDecimal monto = ReglasDeOperacion.montoOperado(orden.getCantidad(), cierre.get());
            BigDecimal comision = ReglasDeOperacion.comision(monto);
            Optional<String> motivoDeRechazo = motivoDeRechazo(orden, monto, comision, libro);
            if (motivoDeRechazo.isPresent()) {
                orden.rechazar(dia, motivoDeRechazo.get());
                resumen.rechazada(orden, dia);
            } else {
                Transaccion operacion = Transaccion.operacion(orden, cierre.get(), monto, dia, ahora);
                Transaccion cargo = Transaccion.comision(orden, comision, dia, ahora);
                // Operación y comisión en el mismo paso: si algo falla, se revierte todo el avance (RNF-04).
                transaccionRepositorio.saveAll(List.of(operacion, cargo));
                libro.aplicar(operacion);
                libro.aplicar(cargo);
                orden.ejecutar(dia);
                resumen.ejecutada(orden, dia, cierre.get(), monto, comision);
            }
            iterador.remove();
        }
    }

    /**
     * Paso 2 de REQ-009: interés de la cuenta de ahorro por los días transcurridos desde la última capitalización
     * (incluye los días sin cotización que el reloj saltó). Se registra como transacción de ahorro.
     */
    private void capitalizarIntereses(Portafolio portafolio, LocalDate dia, CuentaAhorro cuenta, EstadoDelLibro libro,
                                      ResumenAvance.Constructor resumen, LocalDateTime ahora) {
        if (cuenta == null) {
            return;
        }
        long dias = ChronoUnit.DAYS.between(cuenta.getFechaUltimaCapitalizacion(), dia);
        BigDecimal interes = CalculadoraDeIntereses.interes(libro.saldoAhorro(), cuenta.getTasaAnual(), dias);
        if (interes.signum() > 0) {
            Transaccion abono = Transaccion.movimientoDeAhorro(portafolio.getId(), TipoTransaccion.INTERES_AHORRO,
                    interes, dia, ahora);
            transaccionRepositorio.save(abono);
            libro.aplicar(abono);
            resumen.interes(interes);
        }
        cuenta.capitalizadaHasta(dia);
    }

    /** Segunda validación, con el efectivo y la posición del momento de ejecutar. */
    private static Optional<String> motivoDeRechazo(Orden orden, BigDecimal monto, BigDecimal comision,
                                                    EstadoDelLibro libro) {
        if (orden.getTipo() == TipoOrden.COMPRA) {
            BigDecimal costo = monto.add(comision);
            if (libro.efectivo().compareTo(costo) < 0) {
                return Optional.of("Efectivo insuficiente al ejecutar: la compra costaba " + textoDinero(costo)
                        + " y había " + textoDinero(libro.efectivo()) + ".");
            }
            return Optional.empty();
        }
        long enPosicion = libro.cantidadDe(orden.getInstrumento().getSimbolo());
        if (enPosicion < orden.getCantidad()) {
            return Optional.of("La posición cambió desde que se emitió la orden: había " + enPosicion
                    + " unidades y la orden pedía " + orden.getCantidad() + ".");
        }
        if (libro.efectivo().add(monto).subtract(comision).signum() < 0) {
            return Optional.of("El ingreso de la venta no alcanzaba a cubrir la comisión.");
        }
        return Optional.empty();
    }

    private static InstantaneaDiaria valorar(long portafolioId, LocalDate dia, EstadoDelLibro libro,
                                             SeriesDePrecios series) {
        BigDecimal valorPosiciones = BigDecimal.ZERO;
        for (Posicion posicion : libro.posiciones()) {
            BigDecimal cierre = series.ultimoCierreHasta(posicion.instrumento().getId(), dia).orElse(BigDecimal.ZERO);
            valorPosiciones = valorPosiciones.add(ReglasDeOperacion.montoOperado(posicion.cantidad(), cierre));
        }
        BigDecimal efectivo = ReglasDeOperacion.redondearDinero(libro.efectivo());
        BigDecimal ahorro = ReglasDeOperacion.redondearDinero(libro.saldoAhorro());
        return new InstantaneaDiaria(portafolioId, dia, efectivo, valorPosiciones, ahorro,
                efectivo.add(valorPosiciones).add(ahorro));
    }
}
