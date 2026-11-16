package co.edu.uis.siminversion.estadistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.escenario.EscenarioRepositorio;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** REQ-019: totales del rango (fechas reales), sin datos personales, solo para el administrador. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstadisticasTest {

    @Autowired
    private EstadisticasServicio estadisticasServicio;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private EscenarioRepositorio escenarioRepositorio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private Clock relojSistema;

    @Autowired
    private WebApplicationContext contexto;

    @Test
    void cuentaUsuariosPortafoliosYOrdenesDelRango() {
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2008-09-02", "5");
        precios.insertar("AAPL", "2024-03-01", "100");
        precios.insertar("AAPL", "2024-03-04", "110");
        registroUsuarioServicio.registrarInversionista("ana@example.com", "Ana", "clave-segura-1");
        registroUsuarioServicio.registrarInversionista("beto@example.com", "Beto", "clave-segura-2");
        long portafolio = portafolioServicio.crear("ana@example.com", "Uno", new BigDecimal("5000"),
                LocalDate.of(2024, 3, 1)).getId();
        portafolioServicio.crearDesdeEscenario("beto@example.com",
                escenarioRepositorio.findAllByOrderByOrdenVisualizacionAsc().getFirst().getId(), new BigDecimal("5000"));
        ordenServicio.emitir("ana@example.com", portafolio, "AAPL", TipoOrden.COMPRA, 2L);
        long cancelada = ordenServicio.emitir("ana@example.com", portafolio, "AAPL", TipoOrden.COMPRA, 1L).getId();
        ordenServicio.cancelar("ana@example.com", portafolio, cancelada);

        LocalDate hoy = LocalDate.now(relojSistema);
        EstadisticasDeUso hoyMismo = estadisticasServicio.calcular(hoy.minusDays(1), hoy.plusDays(1));

        assertThat(hoyMismo.usuariosRegistrados()).isEqualTo(2);
        assertThat(hoyMismo.portafoliosCreados()).isEqualTo(2);
        assertThat(hoyMismo.portafoliosDeEscenario()).isEqualTo(1);
        assertThat(hoyMismo.ordenesEmitidas()).isEqualTo(2);
        assertThat(hoyMismo.ordenesPorEstado()).containsEntry(EstadoOrden.PENDIENTE, 1L)
                .containsEntry(EstadoOrden.CANCELADA, 1L).containsEntry(EstadoOrden.EJECUTADA, 0L);
        assertThat(hoyMismo.instrumentosMasOperados()).singleElement()
                .satisfies(fila -> assertThat(fila).isEqualTo(new EstadisticasDeUso.InstrumentoOperado("AAPL", 2)));

        EstadisticasDeUso elAnioPasado = estadisticasServicio.calcular(hoy.minusYears(1), hoy.minusYears(1).plusDays(5));
        assertThat(elAnioPasado.usuariosRegistrados()).isZero();
        assertThat(elAnioPasado.ordenesEmitidas()).isZero();
        assertThat(elAnioPasado.totalUsuarios()).isEqualTo(2);

        assertThatThrownBy(() -> estadisticasServicio.calcular(hoy, hoy.minusDays(1)))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void soloElAdministradorVeLasEstadisticas() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        mvc.perform(get("/admin/estadisticas").with(user("ana@example.com").roles("INVERSIONISTA")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/estadisticas").with(user("admin@example.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuarios registrados")));
        mvc.perform(get("/admin/estadisticas").param("desde", "2026-02-01").param("hasta", "2026-01-01")
                        .with(user("admin@example.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("corrige el rango")));
    }
}
