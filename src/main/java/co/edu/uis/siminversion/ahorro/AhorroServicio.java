package co.edu.uis.siminversion.ahorro;

import static co.edu.uis.siminversion.comun.Textos.textoDinero;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import co.edu.uis.siminversion.transaccion.Transaccion;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-013: apertura de la cuenta de ahorro y movimientos entre el efectivo y la cuenta. */
@Service
public class AhorroServicio {

    private final PortafolioServicio portafolioServicio;
    private final CuentaAhorroRepositorio cuentaAhorroRepositorio;
    private final TransaccionRepositorio transaccionRepositorio;
    private final PropiedadesAhorro propiedades;
    private final Clock relojSistema;

    public AhorroServicio(PortafolioServicio portafolioServicio, CuentaAhorroRepositorio cuentaAhorroRepositorio,
                          TransaccionRepositorio transaccionRepositorio, PropiedadesAhorro propiedades,
                          Clock relojSistema) {
        this.portafolioServicio = portafolioServicio;
        this.cuentaAhorroRepositorio = cuentaAhorroRepositorio;
        this.transaccionRepositorio = transaccionRepositorio;
        this.propiedades = propiedades;
        this.relojSistema = relojSistema;
    }

    @Transactional(readOnly = true)
    public VistaAhorro vista(String correo, long portafolioId) {
        Portafolio portafolio = portafolioServicio.obtener(correo, portafolioId);
        List<Transaccion> libro = transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId);
        EstadoDelLibro estado = EstadoDelLibro.desde(libro);
        List<Transaccion> movimientos = libro.stream()
                .filter(transaccion -> transaccion.getTipo().esDeAhorro())
                .sorted(Comparator.comparing(Transaccion::getFechaSim).thenComparing(Transaccion::getId).reversed())
                .toList();
        // El efectivo ya salió del recorrido del libro: solo falta descontar las compras pendientes.
        BigDecimal efectivoDisponible = estado.efectivo()
                .subtract(portafolioServicio.costoDeComprasPendientes(portafolioId, portafolio.getFechaActualSim()));
        return new VistaAhorro(portafolio, cuentaAhorroRepositorio.findById(portafolioId).orElse(null),
                estado.saldoAhorro(), efectivoDisponible, estado.interesesGanados(), movimientos);
    }

    @Transactional
    public CuentaAhorro abrir(String correo, long portafolioId) {
        Portafolio portafolio = portafolioQueAdmiteMovimientos(correo, portafolioId);
        if (cuentaAhorroRepositorio.existsById(portafolioId)) {
            throw new OperacionInvalidaException("Este portafolio ya tiene una cuenta de ahorro; puedes depositar o retirar.");
        }
        return cuentaAhorroRepositorio.save(
                new CuentaAhorro(portafolioId, propiedades.tasaVigente(), portafolio.getFechaActualSim()));
    }

    /** Del efectivo a la cuenta. El efectivo comprometido en compras pendientes no se puede depositar. */
    @Transactional
    public Transaccion depositar(String correo, long portafolioId, BigDecimal monto) {
        Portafolio portafolio = portafolioQueAdmiteMovimientos(correo, portafolioId);
        exigirCuenta(portafolioId);
        BigDecimal montoRedondeado = montoValido(monto);
        BigDecimal disponible = portafolioServicio.efectivoDisponible(portafolioId, portafolio.getFechaActualSim());
        if (montoRedondeado.compareTo(disponible) > 0) {
            throw new OperacionInvalidaException("Efectivo insuficiente: tienes " + textoDinero(disponible)
                    + " disponibles (efectivo menos compras pendientes).");
        }
        return registrar(portafolio, TipoTransaccion.DEPOSITO_AHORRO, montoRedondeado);
    }

    /** De la cuenta al efectivo. */
    @Transactional
    public Transaccion retirar(String correo, long portafolioId, BigDecimal monto) {
        Portafolio portafolio = portafolioQueAdmiteMovimientos(correo, portafolioId);
        exigirCuenta(portafolioId);
        BigDecimal montoRedondeado = montoValido(monto);
        BigDecimal saldo = portafolioServicio.saldoAhorro(portafolioId);
        if (montoRedondeado.compareTo(saldo) > 0) {
            throw new OperacionInvalidaException("Saldo insuficiente en la cuenta de ahorro: tienes "
                    + textoDinero(saldo) + ".");
        }
        return registrar(portafolio, TipoTransaccion.RETIRO_AHORRO, montoRedondeado);
    }

    private Transaccion registrar(Portafolio portafolio, TipoTransaccion tipo, BigDecimal monto) {
        return transaccionRepositorio.save(Transaccion.movimientoDeAhorro(portafolio.getId(), tipo, monto,
                portafolio.getFechaActualSim(), LocalDateTime.now(relojSistema)));
    }

    private Portafolio portafolioQueAdmiteMovimientos(String correo, long portafolioId) {
        Portafolio portafolio = portafolioServicio.obtenerParaModificar(correo, portafolioId);
        portafolio.exigirEnCurso("solo permite consultas.");
        return portafolio;
    }

    private void exigirCuenta(long portafolioId) {
        if (!cuentaAhorroRepositorio.existsById(portafolioId)) {
            throw new OperacionInvalidaException("Primero abre la cuenta de ahorro.");
        }
    }

    private static BigDecimal montoValido(BigDecimal monto) {
        BigDecimal redondeado = monto == null ? BigDecimal.ZERO : ReglasDeOperacion.redondearDinero(monto);
        if (redondeado.signum() <= 0) {
            throw new OperacionInvalidaException("El monto debe ser mayor que cero.");
        }
        return redondeado;
    }
}
