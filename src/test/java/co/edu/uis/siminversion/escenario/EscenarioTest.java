package co.edu.uis.siminversion.escenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.EstadoPortafolio;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.reloj.RelojServicio;
import co.edu.uis.siminversion.reloj.ResumenAvance;
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
 * REQ-014 con el escenario «Crisis financiera de 2008» (2008-09-02 a 2009-03-09). Hay un precio después del fin
 * del escenario para comprobar que el límite lo pone el escenario y no el histórico.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EscenarioTest {

    private static final String ANA = "ana@example.com";
    private static final LocalDate INICIO = LocalDate.of(2008, 9, 2);
    private static final LocalDate VIERNES_ANTES_DEL_FIN = LocalDate.of(2009, 3, 6);
    private static final LocalDate FIN = LocalDate.of(2009, 3, 9);

    @Autowired
    private EscenarioServicio escenarioServicio;

    @Autowired
    private EscenarioRepositorio escenarioRepositorio;

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private RelojServicio relojServicio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    private Escenario crisis;

    @BeforeEach
    void preparar() {
        registroUsuarioServicio.registrarInversionista(ANA, "Ana", "clave-segura-1");
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2008-09-02", "5");
        precios.insertar("AAPL", "2008-09-03", "5.10");
        precios.insertar("AAPL", "2009-03-06", "3");
        precios.insertar("AAPL", "2009-03-09", "3.10");
        precios.insertar("AAPL", "2009-03-10", "3.30");
        // MSFT solo cotiza el viernes: una orden emitida ese día ya no alcanza a ejecutarse dentro del escenario.
        precios.insertar("MSFT", "2009-03-06", "15");
        crisis = escenarioRepositorio.findAllByOrderByOrdenVisualizacionAsc().getFirst();
    }

    @Test
    void hayCuatroEscenariosEnOrdenConSuCoberturaDePrecios() {
        assertThat(escenarioServicio.listar()).extracting(vista -> vista.escenario().getNombre())
                .containsExactly("Crisis financiera de 2008", "Mercado alcista de 2013", "Caída por COVID-19 (2020)",
                        "Mercado bajista de 2022");
        assertThat(escenarioServicio.listar().getFirst().instrumentosConPrecios()).isEqualTo(2);
        assertThat(crisis.getFechaInicio()).isEqualTo(INICIO);
        assertThat(crisis.getFechaFin()).isEqualTo(FIN);
    }

    @Test
    void crearDesdeUnEscenarioPoneElRelojAlInicioYLoDejaComoActivo() {
        Portafolio portafolio = portafolioServicio.crearDesdeEscenario(ANA, crisis.getId(), new BigDecimal("10000"));

        assertThat(portafolio.getNombre()).isEqualTo("Crisis financiera de 2008");
        assertThat(portafolio.getFechaActualSim()).isEqualTo(INICIO);
        assertThat(portafolio.getEscenarioId()).isEqualTo(crisis.getId());
        assertThat(portafolioServicio.fechaLimite(portafolio)).isEqualTo(FIN);
        assertThat(portafolioServicio.activo(ANA)).map(Portafolio::getId).contains(portafolio.getId());
        assertThatThrownBy(() -> portafolioServicio.crearDesdeEscenario(ANA, crisis.getId(), new BigDecimal("500")))
                .hasMessageContaining("US$ 1.000");
    }

    @Test
    void elRelojNoPasaDelFinDelEscenarioYAlLlegarElPortafolioQuedaFinalizado() {
        long id = portafolioServicio.crearDesdeEscenario(ANA, crisis.getId(), new BigDecimal("10000")).getId();

        assertThatThrownBy(() -> relojServicio.avanzar(ANA, id, FIN.plusDays(1)))
                .hasMessageContaining("El escenario termina el 09/03/2009");

        relojServicio.avanzar(ANA, id, VIERNES_ANTES_DEL_FIN);
        assertThat(relojServicio.destinoDeSalto(ANA, id, RelojServicio.Salto.MES)).isEqualTo(FIN);
        Orden msft = ordenServicio.emitir(ANA, id, "MSFT", TipoOrden.COMPRA, 10L);

        ResumenAvance avance = relojServicio.avanzar(ANA, id, FIN);

        assertThat(avance.finalizado()).isTrue();
        assertThat(msft.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
        assertThat(msft.getMotivo()).contains("escenario");
        assertThat(portafolioServicio.obtener(ANA, id).getEstado()).isEqualTo(EstadoPortafolio.FINALIZADO);
    }

    @Test
    void reiniciarUnPortafolioDeEscenarioConservaElEscenario() {
        long id = portafolioServicio.crearDesdeEscenario(ANA, crisis.getId(), new BigDecimal("7000")).getId();
        relojServicio.avanzar(ANA, id, VIERNES_ANTES_DEL_FIN);

        Portafolio nuevo = portafolioServicio.reiniciar(ANA, id);

        assertThat(nuevo.getEscenarioId()).isEqualTo(crisis.getId());
        assertThat(nuevo.getFechaActualSim()).isEqualTo(INICIO);
        assertThat(nuevo.getSaldoInicial()).isEqualByComparingTo("7000");
    }
}
