package co.edu.uis.siminversion.portafolio;

import static co.edu.uis.siminversion.comun.Textos.textoFecha;

import co.edu.uis.siminversion.ahorro.CuentaAhorroRepositorio;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.escenario.Escenario;
import co.edu.uis.siminversion.escenario.EscenarioRepositorio;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.OrdenRepositorio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import co.edu.uis.siminversion.transaccion.Posicion;
import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import co.edu.uis.siminversion.transaccion.Transaccion;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import co.edu.uis.siminversion.usuario.Usuario;
import co.edu.uis.siminversion.usuario.UsuarioRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Portafolios del usuario: creación (REQ-003), portafolio activo (REQ-002), consulta y reinicio (REQ-015).
 * Toda búsqueda incluye al dueño: un usuario nunca llega al portafolio de otro.
 */
@Service
public class PortafolioServicio {

    public static final LocalDate FECHA_INICIAL_POR_DEFECTO = LocalDate.of(2020, 1, 2);
    private static final int LARGO_MAXIMO_NOMBRE = 80;

    private final PortafolioRepositorio portafolioRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final TransaccionRepositorio transaccionRepositorio;
    private final OrdenRepositorio ordenRepositorio;
    private final CuentaAhorroRepositorio cuentaAhorroRepositorio;
    private final EscenarioRepositorio escenarioRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;
    private final Clock relojSistema;

    public PortafolioServicio(PortafolioRepositorio portafolioRepositorio, UsuarioRepositorio usuarioRepositorio,
                              TransaccionRepositorio transaccionRepositorio, OrdenRepositorio ordenRepositorio,
                              CuentaAhorroRepositorio cuentaAhorroRepositorio, EscenarioRepositorio escenarioRepositorio,
                              ProveedorDePrecios proveedorDePrecios, Clock relojSistema) {
        this.portafolioRepositorio = portafolioRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.transaccionRepositorio = transaccionRepositorio;
        this.ordenRepositorio = ordenRepositorio;
        this.cuentaAhorroRepositorio = cuentaAhorroRepositorio;
        this.escenarioRepositorio = escenarioRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
        this.relojSistema = relojSistema;
    }

    /** REQ-003: crea el portafolio, registra el depósito inicial y lo deja como activo. */
    @Transactional
    public Portafolio crear(String correo, String nombre, BigDecimal saldoInicial, LocalDate fechaInicial) {
        return crear(correo, nombre, saldoInicial, fechaInicial, null);
    }

    /**
     * REQ-014: portafolio nuevo con el reloj en el primer día bursátil del escenario. No modifica otros
     * portafolios (RN-09); el nuevo queda como activo.
     */
    @Transactional
    public Portafolio crearDesdeEscenario(String correo, long escenarioId, BigDecimal saldoInicial) {
        Escenario escenario = escenarioRepositorio.findById(escenarioId)
                .orElseThrow(() -> new OperacionInvalidaException("Ese escenario no existe."));
        return crear(correo, escenario.getNombre(), saldoInicial, escenario.getFechaInicio(), escenario);
    }

    private Portafolio crear(String correo, String nombre, BigDecimal saldoInicial, LocalDate fechaInicial,
                             Escenario escenario) {
        Usuario usuario = usuario(correo);
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new OperacionInvalidaException("El nombre es obligatorio y puede tener hasta 80 caracteres.");
        }
        if (!ReglasDeOperacion.saldoInicialValido(saldoInicial)) {
            throw new OperacionInvalidaException("El saldo inicial debe estar entre US$ 1.000 y US$ 100.000.");
        }
        LocalDate inicio = primerDiaBursatil(fechaInicial, fechaLimite(escenario));
        BigDecimal saldo = ReglasDeOperacion.redondearDinero(saldoInicial);
        LocalDateTime ahora = LocalDateTime.now(relojSistema);

        Portafolio portafolio = portafolioRepositorio.save(new Portafolio(usuario.getId(), nombreLimpio, saldo, inicio,
                escenario == null ? null : escenario.getId(), ahora));
        transaccionRepositorio.save(Transaccion.depositoInicial(portafolio.getId(), saldo, inicio, ahora));
        usuario.activarPortafolio(portafolio.getId());
        return portafolio;
    }

    /** RN-08: el reloj empieza en un día bursátil; si la fecha elegida no lo es, se toma el siguiente. */
    private LocalDate primerDiaBursatil(LocalDate fechaElegida, LocalDate limite) {
        if (fechaElegida == null) {
            throw new OperacionInvalidaException("Elige la fecha en la que empieza el reloj.");
        }
        LocalDate primera = proveedorDePrecios.primeraFechaDelHistorico()
                .orElseThrow(() -> new OperacionInvalidaException("Todavía no hay precios históricos cargados."));
        if (fechaElegida.isBefore(primera)) {
            throw new OperacionInvalidaException("El histórico empieza el " + textoFecha(primera)
                    + "; elige esa fecha o una posterior.");
        }
        LocalDate inicio = proveedorDePrecios.primerDiaBursatilDelMercadoDesde(fechaElegida).orElse(limite);
        if (!inicio.isBefore(limite)) {
            throw new OperacionInvalidaException("Elige una fecha anterior al " + textoFecha(limite)
                    + " para que queden días por simular.");
        }
        return inicio;
    }

    /**
     * Fecha máxima del reloj: el fin del escenario, si el portafolio salió de uno, o el último dato del histórico.
     * Al llegar a ella el portafolio queda finalizado.
     */
    @Transactional(readOnly = true)
    public LocalDate fechaLimite(Portafolio portafolio) {
        return fechaLimite(escenarioDe(portafolio).orElse(null));
    }

    @Transactional(readOnly = true)
    public Optional<Escenario> escenarioDe(Portafolio portafolio) {
        return portafolio.getEscenarioId() == null
                ? Optional.empty()
                : escenarioRepositorio.findById(portafolio.getEscenarioId());
    }

    private LocalDate fechaLimite(Escenario escenario) {
        LocalDate ultima = proveedorDePrecios.ultimaFechaDelHistorico()
                .orElseThrow(() -> new OperacionInvalidaException("Todavía no hay precios históricos cargados."));
        return escenario == null || escenario.getFechaFin().isAfter(ultima) ? ultima : escenario.getFechaFin();
    }

    @Transactional(readOnly = true)
    public List<FilaPortafolio> listar(String correo) {
        Usuario usuario = usuario(correo);
        return portafolioRepositorio.findByUsuarioIdOrderByCreadoEnDescIdDesc(usuario.getId()).stream()
                .map(portafolio -> new FilaPortafolio(portafolio, efectivo(portafolio.getId()),
                        portafolio.getId().equals(usuario.getPortafolioActivoId())))
                .toList();
    }

    /** Para decidir la vista inicial sin calcular el efectivo de cada portafolio. */
    @Transactional(readOnly = true)
    public boolean tienePortafolios(String correo) {
        return portafolioRepositorio.existsByUsuarioId(usuario(correo).getId());
    }

    @Transactional
    public void activar(String correo, long portafolioId) {
        Usuario usuario = usuario(correo);
        Portafolio portafolio = portafolioRepositorio.findByIdAndUsuarioId(portafolioId, usuario.getId())
                .orElseThrow(PortafolioNoEncontradoException::new);
        usuario.activarPortafolio(portafolio.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Portafolio> activo(String correo) {
        return usuarioRepositorio.findByCorreo(correo)
                .filter(usuario -> usuario.getPortafolioActivoId() != null)
                .flatMap(usuario -> portafolioRepositorio.findByIdAndUsuarioId(usuario.getPortafolioActivoId(),
                        usuario.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<ResumenBarra> resumenDelActivo(String correo) {
        return activo(correo).map(portafolio -> new ResumenBarra(portafolio.getId(), portafolio.getNombre(),
                portafolio.getFechaActualSim(), efectivo(portafolio.getId()), portafolio.getEstado()));
    }

    @Transactional(readOnly = true)
    public Portafolio obtener(String correo, long portafolioId) {
        return portafolioRepositorio.findByIdAndUsuarioId(portafolioId, usuario(correo).getId())
                .orElseThrow(PortafolioNoEncontradoException::new);
    }

    /** Para operaciones que lo modifican: con bloqueo optimista, dentro de la transacción de quien llama. */
    @Transactional
    public Portafolio obtenerParaModificar(String correo, long portafolioId) {
        return portafolioRepositorio.buscarParaModificar(portafolioId, usuario(correo).getId())
                .orElseThrow(PortafolioNoEncontradoException::new);
    }

    /** RN-06: el efectivo se calcula sumando el libro, nunca se guarda. */
    @Transactional(readOnly = true)
    public BigDecimal efectivo(long portafolioId) {
        return transaccionRepositorio.sumarMontos(portafolioId, TipoTransaccion.entradasDeEfectivo())
                .subtract(transaccionRepositorio.sumarMontos(portafolioId, TipoTransaccion.salidasDeEfectivo()));
    }

    /** RN-06: el saldo de la cuenta de ahorro también se deriva del libro. */
    @Transactional(readOnly = true)
    public BigDecimal saldoAhorro(long portafolioId) {
        return transaccionRepositorio.sumarMontos(portafolioId, TipoTransaccion.entradasDeAhorro())
                .subtract(transaccionRepositorio.sumarMontos(portafolioId, TipoTransaccion.salidasDeAhorro()));
    }

    /**
     * Efectivo que se puede comprometer: el del libro menos el costo estimado (con comisión) de las compras
     * pendientes, al último cierre visible. Sin esto, dos operaciones válidas por separado podrían superar
     * juntas el efectivo.
     */
    @Transactional(readOnly = true)
    public BigDecimal efectivoDisponible(long portafolioId, LocalDate fechaSimulada) {
        return efectivo(portafolioId).subtract(costoDeComprasPendientes(portafolioId, fechaSimulada));
    }

    @Transactional(readOnly = true)
    public BigDecimal costoDeComprasPendientes(long portafolioId, LocalDate fechaSimulada) {
        return costoDeComprasPendientes(
                ordenRepositorio.findByPortafolioIdAndEstadoOrderByIdAsc(portafolioId, EstadoOrden.PENDIENTE),
                fechaSimulada);
    }

    /** Igual que el anterior, con las órdenes pendientes que ya consultó quien llama. */
    @Transactional(readOnly = true)
    public BigDecimal costoDeComprasPendientes(List<Orden> pendientes, LocalDate fechaSimulada) {
        Map<Long, BigDecimal> cierres = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Orden orden : pendientes) {
            if (orden.getTipo() != TipoOrden.COMPRA) {
                continue;
            }
            // Un solo precio por instrumento aunque tenga varias compras pendientes.
            BigDecimal precio = cierres.computeIfAbsent(orden.getInstrumento().getId(),
                    instrumentoId -> proveedorDePrecios.ultimoCierreHasta(instrumentoId, fechaSimulada)
                            .map(PrecioDiario::cierre)
                            .orElse(BigDecimal.ZERO));
            BigDecimal monto = ReglasDeOperacion.montoOperado(orden.getCantidad(), precio);
            total = total.add(monto).add(ReglasDeOperacion.comision(monto));
        }
        return total;
    }

    /** Efectivo, posiciones y saldos recalculados desde cero con todas las transacciones del portafolio. */
    @Transactional(readOnly = true)
    public EstadoDelLibro libro(long portafolioId) {
        return EstadoDelLibro.desde(transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId));
    }

    @Transactional(readOnly = true)
    public ResumenPortafolio resumen(String correo, long portafolioId) {
        return resumenConLibro(correo, portafolioId).resumen();
    }

    /**
     * El resumen junto con el libro del que salió, para quien necesita los dos (REQ-011). El libro se lee y se
     * recorre una sola vez; el efectivo y el saldo de ahorro salen de ese mismo recorrido (RN-06).
     */
    @Transactional(readOnly = true)
    public ResumenConLibro resumenConLibro(String correo, long portafolioId) {
        Portafolio portafolio = obtener(correo, portafolioId);
        EstadoDelLibro libro = libro(portafolioId);
        List<PosicionValorada> posiciones = valorarPosiciones(libro, portafolio.getFechaActualSim());
        BigDecimal valorPosiciones = posiciones.stream()
                .map(PosicionValorada::valorMercado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Escenario escenario = escenarioDe(portafolio).orElse(null);
        ResumenPortafolio resumen = new ResumenPortafolio(portafolio, libro.efectivo(), posiciones, valorPosiciones,
                cuentaAhorroRepositorio.existsById(portafolioId), libro.saldoAhorro(),
                ordenRepositorio.findByPortafolioIdAndEstadoOrderByIdAsc(portafolioId, EstadoOrden.PENDIENTE),
                ordenRepositorio.findTop20ByPortafolioIdAndEstadoNotOrderByIdDesc(portafolioId, EstadoOrden.PENDIENTE),
                escenario, fechaLimite(escenario));
        return new ResumenConLibro(resumen, libro);
    }

    /** Posiciones abiertas valoradas al último cierre disponible en la fecha dada. */
    public List<PosicionValorada> valorarPosiciones(EstadoDelLibro libro, LocalDate fecha) {
        return libro.posiciones().stream().map(posicion -> valorar(posicion, fecha)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Posicion> posicionEn(long portafolioId, String simbolo) {
        return libro(portafolioId).posicion(simbolo);
    }

    private PosicionValorada valorar(Posicion posicion, LocalDate fecha) {
        Optional<PrecioDiario> precio = proveedorDePrecios.ultimoCierreHasta(posicion.instrumento().getId(), fecha);
        BigDecimal cierre = precio.map(PrecioDiario::cierre).orElse(BigDecimal.ZERO);
        BigDecimal valor = ReglasDeOperacion.montoOperado(posicion.cantidad(), cierre);
        return new PosicionValorada(posicion, cierre, precio.map(PrecioDiario::fecha).orElse(null), valor,
                valor.subtract(posicion.costoTotal()));
    }

    /**
     * REQ-015: cancela las órdenes pendientes, archiva el portafolio sin borrar nada y crea uno nuevo con el
     * mismo saldo y la misma fecha inicial, que pasa a ser el activo.
     */
    @Transactional
    public Portafolio reiniciar(String correo, long portafolioId) {
        Portafolio anterior = obtenerParaModificar(correo, portafolioId);
        if (anterior.getEstado() == EstadoPortafolio.ARCHIVADO) {
            throw new OperacionInvalidaException("Este portafolio ya está archivado.");
        }
        for (Orden orden : ordenRepositorio.findByPortafolioIdAndEstadoOrderByIdAsc(portafolioId, EstadoOrden.PENDIENTE)) {
            orden.cancelar(anterior.getFechaActualSim(), "Cancelada por el reinicio del portafolio");
        }
        anterior.archivar();
        return crear(correo, anterior.getNombre(), anterior.getSaldoInicial(), anterior.getFechaInicioSim(),
                escenarioDe(anterior).orElse(null));
    }

    private Usuario usuario(String correo) {
        return usuarioRepositorio.findByCorreo(correo).orElseThrow(PortafolioNoEncontradoException::new);
    }
}
