package co.edu.uis.siminversion.rendimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.ahorro.AhorroServicio;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.historial.FiltroHistorial;
import co.edu.uis.siminversion.historial.HistorialServicio;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.portafolio.ResumenPortafolio;
import co.edu.uis.siminversion.reloj.InstantaneaDiaria;
import co.edu.uis.siminversion.reloj.RelojServicio;
import co.edu.uis.siminversion.reloj.ResumenAvance;
import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import co.edu.uis.siminversion.transaccion.Transaccion;
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
 * REQ-011, REQ-012 y REQ-013. Solo cotiza AAPL, así que el reloj salta el fin de semana: el lunes se pagan
 * tres días de intereses. Con US$ 3.650 al 4 % anual, cada día rinde US$ 0,40.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AhorroYRendimientoTest {

    private static final String ANA = "ana@example.com";
    private static final LocalDate VIERNES = LocalDate.of(2024, 3, 1);
    private static final LocalDate LUNES = LocalDate.of(2024, 3, 4);
    private static final LocalDate MARTES = LocalDate.of(2024, 3, 5);
    private static final LocalDate MIERCOLES = LocalDate.of(2024, 3, 6);

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private AhorroServicio ahorroServicio;

    @Autowired
    private RendimientoServicio rendimientoServicio;

    @Autowired
    private HistorialServicio historialServicio;

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
        precios.insertar("AAPL", "2024-03-01", "100");
        precios.insertar("AAPL", "2024-03-04", "120");
        precios.insertar("AAPL", "2024-03-05", "130");
        precios.insertar("AAPL", "2024-03-06", "125");
        portafolioId = portafolioServicio.crear(ANA, "Prueba", new BigDecimal("10000"), VIERNES).getId();
    }

    @Test
    void losInteresesSeSumanEnCadaDiaPorLosDiasTranscurridos() {
        ahorroServicio.abrir(ANA, portafolioId);
        ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("3650"));

        ResumenAvance avance = relojServicio.avanzar(ANA, portafolioId, MARTES);

        // Lunes: 3 días (vie → lun) = 1,20. Martes: 1 día sobre 3.651,20 = 0,40.
        assertThat(avance.interesesGanados()).isEqualByComparingTo("1.60");
        assertThat(transaccionRepositorio.findByPortafolioIdOrderByFechaSimAscIdAsc(portafolioId).stream()
                .filter(transaccion -> transaccion.getTipo() == TipoTransaccion.INTERES_AHORRO))
                .extracting(Transaccion::getFechaSim, transaccion -> transaccion.getMonto().stripTrailingZeros().toPlainString())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(LUNES, "1.2"),
                        org.assertj.core.groups.Tuple.tuple(MARTES, "0.4"));
        assertThat(portafolioServicio.saldoAhorro(portafolioId)).isEqualByComparingTo("3651.60");
        assertThat(portafolioServicio.efectivo(portafolioId)).isEqualByComparingTo("6350");
        InstantaneaDiaria ultima = rendimientoServicio.rendimiento(ANA, portafolioId).instantaneas().getLast();
        assertThat(ultima.saldoAhorro()).isEqualByComparingTo("3651.60");
        assertThat(ultima.valorTotal()).isEqualByComparingTo("10001.60");
    }

    @Test
    void losMovimientosDeAhorroRespetanElEfectivoDisponibleYElSaldo() {
        assertThatThrownBy(() -> ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("10")))
                .hasMessageContaining("Primero abre");
        ahorroServicio.abrir(ANA, portafolioId);
        assertThatThrownBy(() -> ahorroServicio.abrir(ANA, portafolioId)).hasMessageContaining("ya tiene");

        // Una compra pendiente compromete 5.000 + 15 de comisión: quedan 4.985 disponibles.
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 50L);
        assertThatThrownBy(() -> ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("5000")))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Efectivo insuficiente");
        ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("4985"));

        assertThatThrownBy(() -> ahorroServicio.retirar(ANA, portafolioId, new BigDecimal("5000")))
                .hasMessageContaining("Saldo insuficiente");
        assertThatThrownBy(() -> ahorroServicio.retirar(ANA, portafolioId, BigDecimal.ZERO))
                .hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> ahorroServicio.retirar(ANA, portafolioId, null))
                .hasMessageContaining("mayor que cero");
        ahorroServicio.retirar(ANA, portafolioId, new BigDecimal("985.004"));

        assertThat(portafolioServicio.saldoAhorro(portafolioId)).isEqualByComparingTo("4000.00");
    }

    @Test
    void elRendimientoSeparaRealizadoNoRealizadoComisionesEIntereses() {
        escenarioCompleto();

        Rendimiento rendimiento = rendimientoServicio.rendimiento(ANA, portafolioId);
        ResumenPortafolio resumen = rendimiento.resumen();

        // Compra de 10 a 120 y venta de 4 a 130: realizado (130 − 120) × 4. Quedan 6 a 125: no realizado (125 − 120) × 6.
        assertThat(rendimiento.gananciaRealizada()).isEqualByComparingTo("40.00");
        assertThat(rendimiento.gananciaNoRealizada()).isEqualByComparingTo("30.00");
        assertThat(rendimiento.comisionesPagadas()).isEqualByComparingTo("10.00");
        assertThat(rendimiento.interesesGanados()).isEqualByComparingTo("2.00");
        assertThat(resumen.efectivo()).isEqualByComparingTo("5660.00");
        assertThat(resumen.valorPosiciones()).isEqualByComparingTo("750.00");
        assertThat(resumen.saldoAhorro()).isEqualByComparingTo("3652.00");
        assertThat(resumen.valorTotal()).isEqualByComparingTo("10062.00");
        assertThat(resumen.rendimiento()).isEqualByComparingTo(rendimiento.gananciaRealizada()
                .add(rendimiento.gananciaNoRealizada())
                .subtract(rendimiento.comisionesPagadas())
                .add(rendimiento.interesesGanados()));
        assertThat(resumen.variacionPorcentual()).isEqualByComparingTo("0.62");

        assertThat(rendimiento.porInstrumento()).singleElement().satisfies(fila -> {
            assertThat(fila.cantidadActual()).isEqualTo(6);
            assertThat(fila.gananciaRealizada()).isEqualByComparingTo("40.00");
            assertThat(fila.gananciaNoRealizada()).isEqualByComparingTo("30.00");
        });
        assertThat(rendimiento.instantaneas()).extracting(InstantaneaDiaria::fecha)
                .containsExactly(LUNES, MARTES, MIERCOLES);
        assertThat(rendimiento.instantaneas().getLast().valorTotal()).isEqualByComparingTo("10062.00");
    }

    @Test
    void elHistorialEstaEnOrdenCronologicoYSeFiltraPorTipoInstrumentoYFechas() {
        escenarioCompleto();

        assertThat(historialServicio.consultar(ANA, portafolioId, FiltroHistorial.SIN_FILTRO).transacciones())
                .extracting(Transaccion::getTipo)
                .containsExactly(TipoTransaccion.DEPOSITO_INICIAL, TipoTransaccion.DEPOSITO_AHORRO,
                        TipoTransaccion.COMPRA, TipoTransaccion.COMISION, TipoTransaccion.INTERES_AHORRO,
                        TipoTransaccion.VENTA, TipoTransaccion.COMISION, TipoTransaccion.INTERES_AHORRO,
                        TipoTransaccion.INTERES_AHORRO);
        assertThat(filtrar(new FiltroHistorial(TipoTransaccion.COMISION, null, null, null))).hasSize(2);
        assertThat(filtrar(new FiltroHistorial(null, "aapl", null, null))).hasSize(4);
        assertThat(filtrar(new FiltroHistorial(null, null, MARTES, MARTES))).hasSize(3);
        assertThat(filtrar(new FiltroHistorial(TipoTransaccion.RETIRO_AHORRO, null, null, null))).isEmpty();
        assertThat(historialServicio.consultar(ANA, portafolioId, FiltroHistorial.SIN_FILTRO).simbolosOperados())
                .containsExactly("AAPL");
        assertThatThrownBy(() -> filtrar(new FiltroHistorial(null, null, MIERCOLES, LUNES)))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unPortafolioFinalizadoNoAdmiteMovimientosDeAhorro() {
        ahorroServicio.abrir(ANA, portafolioId);
        relojServicio.avanzar(ANA, portafolioId, MIERCOLES);

        assertThatThrownBy(() -> ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("10")))
                .hasMessageContaining("finalizado");
    }

    private java.util.List<Transaccion> filtrar(FiltroHistorial filtro) {
        return historialServicio.consultar(ANA, portafolioId, filtro).transacciones();
    }

    /** Viernes: ahorro de 3.650 y compra de 10 AAPL. Lunes: venta de 4. Avanza hasta el miércoles. */
    private void escenarioCompleto() {
        ahorroServicio.abrir(ANA, portafolioId);
        ahorroServicio.depositar(ANA, portafolioId, new BigDecimal("3650"));
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.COMPRA, 10L);
        relojServicio.avanzar(ANA, portafolioId, LUNES);
        ordenServicio.emitir(ANA, portafolioId, "AAPL", TipoOrden.VENTA, 4L);
        relojServicio.avanzar(ANA, portafolioId, MIERCOLES);
    }
}
