package co.edu.uis.siminversion.reloj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.EstadoPortafolio;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.reloj.ResumenAvance.OrdenProcesada;
import co.edu.uis.siminversion.transaccion.TransaccionRepositorio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * REQ-003 y REQ-006 a REQ-010 y REQ-015 de punta a punta, con precios inventados.
 * Marzo de 2024: el 1 es viernes; el 2 y el 3 (fin de semana) solo cotiza BTC; el 7 es el último dato.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CicloTransaccionalTest {

    private static final String ANA = "ana@example.com";
    private static final LocalDate VIERNES = LocalDate.of(2024, 3, 1);
    private static final LocalDate SABADO = LocalDate.of(2024, 3, 2);
    private static final LocalDate LUNES = LocalDate.of(2024, 3, 4);
    private static final LocalDate MARTES = LocalDate.of(2024, 3, 5);
    private static final LocalDate MIERCOLES = LocalDate.of(2024, 3, 6);
    private static final LocalDate ULTIMO_DATO = LocalDate.of(2024, 3, 7);

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private RegistroDeInstantaneas registroDeInstantaneas;

    @Autowired
    private TransaccionRepositorio transaccionRepositorio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    private long portafolioId;

    @BeforeEach
    void preparar() {
        registroUsuarioServicio.registrarInversionista(ANA, "Ana", "clave-segura-1");
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2024-02-29", "100");
        precios.insertar("AAPL", "2024-03-01", "110");
        precios.insertar("AAPL", "2024-03-04", "120");
        precios.insertar("AAPL", "2024-03-05", "130");
        precios.insertar("AAPL", "2024-03-06", "125");
        precios.insertar("BTCUSD", "2024-03-01", "1000");
        precios.insertar("BTCUSD", "2024-03-02", "1100");
        precios.insertar("BTCUSD", "2024-03-03", "1200");
        precios.insertar("BTCUSD", "2024-03-04", "1300");
        precios.insertar("BTCUSD", "2024-03-05", "1250");
        precios.insertar("BTCUSD", "2024-03-06", "1400");
        precios.insertar("BTCUSD", "2024-03-07", "1500");
        portafolioId = portafolioServicio.crear(ANA, "Prueba", new BigDecimal("10000"), VIERNES).getId();
    }

    @Test
    void comprarYAvanzarElRelojDejaLaOperacionEnLaPosicionYElEfectivo() {
        Orden orden = ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 10L);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("10000");

        ResumenAvance avance = relojServicio.avanzar(ANA, portafolioId, LUNES);

        // El fin de semana solo cotiza BTC: la orden de AAPL espera al lunes y se ejecuta a su cierre (120),
        // no al cierre del viernes (110), que era el precio visible al emitirla.
        assertThat(avance.diasBursatiles()).isEqualTo(3);
        assertThat(avance.ejecutadas()).singleElement().satisfies(ejecutada -> {
            assertThat(ejecutada.fecha()).isEqualTo(LUNES);
            assertThat(ejecutada.precio()).isEqualByComparingTo("120");
            assertThat(ejecutada.monto()).isEqualByComparingTo("1200.00");
            assertThat(ejecutada.comision()).isEqualByComparingTo("5.00");
        });
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.EJECUTADA);
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("8795.00");
        assertThat(portafolioServicio.posicionEn(portafolioId, "AAPL")).hasValueSatisfying(posicion -> {
            assertThat(posicion.cantidad()).isEqualTo(10);
            assertThat(posicion.costoBaseUnitario()).isEqualByComparingTo("120");
        });
        assertThat(portafolioServicio.obtener(ANA, portafolioId).getFechaActualSim()).isEqualTo(LUNES);
        assertThat(registroDeInstantaneas.dePortafolio(portafolioId))
                .extracting(InstantaneaDiaria::fecha)
                .containsExactly(SABADO, SABADO.plusDays(1), LUNES);
        assertThat(registroDeInstantaneas.dePortafolio(portafolioId).getLast().valorTotal())
                .isEqualByComparingTo("9995.00");
    }

    @Test
    void unaCriptomonedaSeEjecutaAlCierreDelSabado() {
        ordenServicio.emitir(ANA, portafolioId, "BTCUSD", TipoOrden.COMPRA, 2L);

        ResumenAvance avance = relojServicio.avanzar(ANA, portafolioId, SABADO);

        assertThat(avance.ejecutadas()).singleElement().satisfies(ejecutada -> {
            assertThat(ejecutada.fecha()).isEqualTo(SABADO);
            assertThat(ejecutada.precio()).isEqualByComparingTo("1100");
            assertThat(ejecutada.comision()).isEqualByComparingTo("6.60");
        });
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("7793.40");
    }

    @Test
    void laValidacionDeEfectivoDescuentaLasComprasPendientes() {
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 60L);   // 6.600 + 19,80

        // Sola cabría (4.413,20), pero junto a la pendiente supera los 10.000.
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 40L))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Efectivo insuficiente");
        assertThat(ordenServicio.estimar(ANA, portafolioId, "BTCUSD", TipoOrden.COMPRA, 1L).efectivoDisponible())
                .isEqualByComparingTo("3380.20");
    }

    @Test
    void unaCompraQueYaNoAlcanzaAlEjecutarQuedaRechazadaConElMotivo() {
        // Al cierre del viernes (110) cuesta 9.378,05; al ejecutarse el lunes (120) costaría 10.230,60.
        Orden orden = ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 85L);

        ResumenAvance avance = relojServicio.avanzar(ANA, portafolioId, LUNES);

        assertThat(avance.ejecutadas()).isEmpty();
        assertThat(avance.rechazadas()).singleElement().extracting(OrdenProcesada::motivo).asString()
                .contains("Efectivo insuficiente");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.RECHAZADA);
        assertThat(orden.getMotivo()).contains("Efectivo insuficiente");
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("10000");
    }

    @Test
    void noSePuedeVenderMasQueLaPosicionMenosLasVentasPendientes() {
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 10L);
        relojServicio.avanzar(ANA, portafolioId, LUNES);
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.VENTA, 6L);

        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.VENTA, 5L))
                .hasMessageContaining("Solo puedes vender 4");
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "BTCUSD", TipoOrden.VENTA, 1L))
                .hasMessageContaining("No tienes unidades");

        relojServicio.avanzar(ANA, portafolioId, MARTES);

        // Venta de 6 a 130: 780 − 5 de comisión. El costo base unitario no cambia (RN-05).
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("9570.00");
        assertThat(portafolioServicio.posicionEn(portafolioId, "AAPL")).hasValueSatisfying(posicion -> {
            assertThat(posicion.cantidad()).isEqualTo(4);
            assertThat(posicion.costoBaseUnitario()).isEqualByComparingTo("120");
        });
    }

    @Test
    void laCantidadDebeSerUnEnteroPositivo() {
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 0L))
                .hasMessageContaining("entero");
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, null))
                .hasMessageContaining("entero");
    }

    @Test
    void unaOrdenCanceladaNoSeEjecutaNiSePuedeCancelarDeNuevo() {
        Orden orden = ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 1L);

        ordenServicio.cancelar(ANA, portafolioId, orden.getId());

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
        assertThatThrownBy(() -> ordenServicio.cancelar(ANA, portafolioId, orden.getId()))
                .hasMessageContaining("cancelada");
        assertThat(relojServicio.avanzar(ANA, portafolioId, LUNES).ejecutadas()).isEmpty();
    }

    @Test
    void elRelojNoRetrocedeNiPasaDelUltimoDato() {
        assertThatThrownBy(() -> relojServicio.avanzar(ANA, portafolioId, VIERNES))
                .hasMessageContaining("no puede retroceder");
        assertThatThrownBy(() -> relojServicio.avanzar(ANA, portafolioId, ULTIMO_DATO.plusDays(1)))
                .hasMessageContaining("07/03/2024");
        assertThat(relojServicio.destinoDeSalto(ANA, portafolioId, RelojServicio.Salto.DIA)).isEqualTo(SABADO);
        assertThat(relojServicio.destinoDeSalto(ANA, portafolioId, RelojServicio.Salto.MES)).isEqualTo(ULTIMO_DATO);
    }

    @Test
    void alLlegarAlUltimoDatoSeCancelanLasPendientesYElPortafolioQuedaFinalizado() {
        relojServicio.avanzar(ANA, portafolioId, MIERCOLES);
        // AAPL no cotiza el 7: esta orden ya no puede ejecutarse.
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 1L);

        ResumenAvance avance = relojServicio.avanzar(ANA, portafolioId, ULTIMO_DATO);

        assertThat(avance.finalizado()).isTrue();
        assertThat(avance.canceladas()).hasSize(1);
        assertThat(portafolioServicio.obtener(ANA, portafolioId).getEstado()).isEqualTo(EstadoPortafolio.FINALIZADO);
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "BTCUSD", TipoOrden.COMPRA, 1L))
                .hasMessageContaining("finalizado");
        assertThatThrownBy(() -> relojServicio.avanzar(ANA, portafolioId, ULTIMO_DATO))
                .hasMessageContaining("finalizado");
    }

    @Test
    void reiniciarArchivaElPortafolioYCreaUnoNuevoConLasMismasCondiciones() {
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 10L);
        relojServicio.avanzar(ANA, portafolioId, LUNES);
        Orden pendiente = ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 1L);

        Portafolio nuevo = portafolioServicio.reiniciar(ANA, portafolioId);

        Portafolio anterior = portafolioServicio.obtener(ANA, portafolioId);
        assertThat(anterior.getEstado()).isEqualTo(EstadoPortafolio.ARCHIVADO);
        assertThat(anterior.getFechaActualSim()).isEqualTo(LUNES);
        assertThat(transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId)).hasSize(3);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoOrden.CANCELADA);

        assertThat(nuevo.getEstado()).isEqualTo(EstadoPortafolio.EN_CURSO);
        assertThat(nuevo.getSaldoInicial()).isEqualByComparingTo("10000");
        assertThat(nuevo.getFechaActualSim()).isEqualTo(VIERNES);
        assertThat(portafolioServicio.efectivo(nuevo.getId())).isEqualByComparingTo("10000");
        assertThat(portafolioServicio.activo(ANA)).map(Portafolio::getId).contains(nuevo.getId());
        assertThatThrownBy(() -> ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 1L))
                .hasMessageContaining("archivado");
    }

    @Test
    void crearValidaElSaldoInicialYLaFechaDelReloj() {
        assertThatThrownBy(() -> portafolioServicio.crear(ANA, "X", new BigDecimal("999.99"), VIERNES))
                .hasMessageContaining("US$ 1.000");
        assertThatThrownBy(() -> portafolioServicio.crear(ANA, "X", new BigDecimal("100000.01"), VIERNES))
                .hasMessageContaining("US$ 100.000");
        assertThatThrownBy(() -> portafolioServicio.crear(ANA, "X", new BigDecimal("5000"), LocalDate.of(2024, 2, 1)))
                .hasMessageContaining("29/02/2024");
        assertThatThrownBy(() -> portafolioServicio.crear(ANA, "X", new BigDecimal("5000"), ULTIMO_DATO))
                .hasMessageContaining("anterior al 07/03/2024");

        Portafolio creado = portafolioServicio.crear(ANA, "Otro", new BigDecimal("5000"), LocalDate.of(2024, 2, 29));
        assertThat(transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(creado.getId()))
                .singleElement()
                .satisfies(deposito -> assertThat(deposito.getMonto()).isEqualByComparingTo("5000"));
        assertThat(portafolioServicio.activo(ANA)).map(Portafolio::getId).contains(creado.getId());
    }
}
