package co.edu.uis.siminversion.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.orden.EstadoOrden;
import co.edu.uis.siminversion.orden.Orden;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.EstadoPortafolio;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Criterio de aceptación 4 (RNF-03): un usuario no puede leer ni modificar el portafolio de otro. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccesoCruzadoTest {

    private static final String ANA = "ana@example.com";
    private static final String BETO = "beto@example.com";
    private static final LocalDate INICIO = LocalDate.of(2024, 3, 1);
    private static final RequestPostProcessor COMO_ANA = user(ANA).roles("INVERSIONISTA");
    private static final RequestPostProcessor COMO_BETO = user(BETO).roles("INVERSIONISTA");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private RegistroUsuarioServicio registroUsuarioServicio;

    @Autowired
    private PortafolioServicio portafolioServicio;

    @Autowired
    private OrdenServicio ordenServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    private MockMvc mvc;
    private long portafolioDeAna;
    private Orden ordenDeAna;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        registroUsuarioServicio.registrarInversionista(ANA, "Ana", "clave-segura-1");
        registroUsuarioServicio.registrarInversionista(BETO, "Beto", "clave-segura-2");
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2024-03-01", "110");
        precios.insertar("AAPL", "2024-03-04", "120");
        portafolioDeAna = portafolioServicio.crear(ANA, "Portafolio de Ana", new BigDecimal("10000"), INICIO).getId();
        ordenDeAna = ordenServicio.emitir(ANA, portafolioDeAna, "AAPL", TipoOrden.COMPRA, 1L);
    }

    @Test
    void otroUsuarioNoPuedeVerNiModificarElPortafolio() throws Exception {
        String base = "/portafolios/" + portafolioDeAna;

        mvc.perform(get(base).with(COMO_BETO)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/reinicio").with(COMO_BETO)).andExpect(status().isNotFound());
        mvc.perform(post(base + "/activar").with(COMO_BETO).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(base + "/reloj").param("salto", "DIA").with(COMO_BETO).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post(base + "/reloj").param("fecha", "2024-03-04").with(COMO_BETO).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post(base + "/ordenes/estimacion").param("simbolo", "AAPL").param("tipo", "COMPRA")
                .param("cantidad", "1").with(COMO_BETO).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(base + "/ordenes").param("simbolo", "AAPL").param("tipo", "COMPRA")
                .param("cantidad", "1").with(COMO_BETO).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(base + "/ordenes/" + ordenDeAna.getId() + "/cancelar").with(COMO_BETO).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post(base + "/reinicio").with(COMO_BETO).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(base + "/rendimiento").with(COMO_BETO)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/transacciones").with(COMO_BETO)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/ahorro").with(COMO_BETO)).andExpect(status().isNotFound());
        mvc.perform(post(base + "/ahorro").with(COMO_BETO).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(base + "/ahorro/deposito").param("monto", "10").with(COMO_BETO).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post(base + "/ahorro/retiro").param("monto", "10").with(COMO_BETO).with(csrf()))
                .andExpect(status().isNotFound());

        Portafolio intacto = portafolioServicio.obtener(ANA, portafolioDeAna);
        assertThat(intacto.getFechaActualSim()).isEqualTo(INICIO);
        assertThat(intacto.getEstado()).isEqualTo(EstadoPortafolio.EN_CURSO);
        assertThat(ordenDeAna.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(portafolioServicio.listar(BETO)).isEmpty();
        assertThat(portafolioServicio.activo(BETO)).isEmpty();
        assertThat(portafolioServicio.resumen(ANA, portafolioDeAna).tieneCuentaAhorro()).isFalse();
    }

    @Test
    void elDuenoVeLasPaginasDeRendimientoTransaccionesYAhorro() throws Exception {
        String base = "/portafolios/" + portafolioDeAna;
        mvc.perform(get(base + "/rendimiento").with(COMO_ANA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("después de avanzar el reloj")));
        mvc.perform(get(base + "/transacciones").with(COMO_ANA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Depósito inicial")));
        mvc.perform(get(base + "/transacciones").param("desde", "2024-03-04").param("hasta", "2024-03-01")
                        .with(COMO_ANA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("corrige el rango")));
        mvc.perform(post(base + "/ahorro").with(COMO_ANA).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(base + "/ahorro/deposito").param("monto", "100").with(COMO_ANA).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(base + "/ahorro").with(COMO_ANA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Depósito a ahorro")));
        assertThat(portafolioServicio.saldoAhorro(portafolioDeAna)).isEqualByComparingTo("100");
    }

    @Test
    void elDuenoSiPuedeVerYOperarSuPortafolio() throws Exception {
        mvc.perform(get("/portafolios/" + portafolioDeAna).with(COMO_ANA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Portafolio de Ana")));
        mvc.perform(post("/portafolios/" + portafolioDeAna + "/ordenes/estimacion").param("simbolo", "AAPL")
                        .param("tipo", "COMPRA").param("cantidad", "2").with(COMO_ANA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar compra")));
    }
}
