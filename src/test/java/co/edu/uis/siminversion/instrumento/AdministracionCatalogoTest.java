package co.edu.uis.siminversion.instrumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.orden.OrdenServicio;
import co.edu.uis.siminversion.orden.TipoOrden;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.reloj.RelojServicio;
import co.edu.uis.siminversion.usuario.RegistroUsuarioServicio;
import java.math.BigDecimal;
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

/** REQ-017: agregar, editar y deshabilitar instrumentos, solo como administrador. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdministracionCatalogoTest {

    @Autowired
    private AdministracionCatalogoServicio servicio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private CatalogoServicio catalogoServicio;

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
    private WebApplicationContext contexto;

    private static FormularioInstrumento formulario(String simbolo, String nombre, TipoInstrumento tipo) {
        FormularioInstrumento formulario = new FormularioInstrumento();
        formulario.setSimbolo(simbolo);
        formulario.setNombre(nombre);
        formulario.setTipo(tipo);
        return formulario;
    }

    @Test
    void agregaYEditaInstrumentosSinRepetirSimbolos() {
        Instrumento nuevo = servicio.crear(formulario("  nvda ", "NVIDIA Corporation", TipoInstrumento.ACCION));

        assertThat(nuevo.getSimbolo()).isEqualTo("NVDA");
        assertThat(nuevo.isActivo()).isTrue();
        assertThatThrownBy(() -> servicio.crear(formulario("aapl", "Otra", TipoInstrumento.ACCION)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("AAPL");
        assertThatThrownBy(() -> servicio.crear(formulario("con espacio", "X", TipoInstrumento.ACCION)))
                .isInstanceOf(OperacionInvalidaException.class);

        FormularioInstrumento cambios = formulario("NVDA", "NVIDIA Corp.", TipoInstrumento.ACCION);
        cambios.setSector("Tecnología");
        servicio.editar(nuevo.getId(), cambios);
        assertThat(instrumentoRepositorio.findBySimbolo("NVDA")).hasValueSatisfying(instrumento -> {
            assertThat(instrumento.getNombre()).isEqualTo("NVIDIA Corp.");
            assertThat(instrumento.getSector()).isEqualTo("Tecnología");
        });
        assertThatThrownBy(() -> servicio.editar(nuevo.getId(), formulario("MSFT", "X", TipoInstrumento.ACCION)))
                .hasMessageContaining("MSFT");
    }

    @Test
    void unInstrumentoDeshabilitadoSaleDelCatalogoNoAdmiteComprasYSeConservaLaPosicion() {
        registroUsuarioServicio.registrarInversionista("ana@example.com", "Ana", "clave-segura-1");
        PreciosDePrueba precios = new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio);
        precios.insertar("AAPL", "2024-03-01", "100");
        precios.insertar("AAPL", "2024-03-04", "110");
        precios.insertar("AAPL", "2024-03-05", "120");
        long portafolio = portafolioServicio.crear("ana@example.com", "Prueba", new BigDecimal("5000"),
                LocalDate.of(2024, 3, 1)).getId();
        ordenServicio.emitir("ana@example.com", portafolio, "AAPL", TipoOrden.COMPRA, 10L);
        relojServicio.avanzar("ana@example.com", portafolio, LocalDate.of(2024, 3, 4));
        long aapl = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();

        servicio.cambiarActivo(aapl, false);

        assertThat(catalogoServicio.consultarCatalogo(null, "aapl", LocalDate.of(2024, 3, 4))).isEmpty();
        assertThat(catalogoServicio.consultarDetalle("AAPL", RangoHistorico.TODO, LocalDate.of(2024, 3, 4))
                .instrumento().activo()).isFalse();
        assertThatThrownBy(() -> ordenServicio.emitir("ana@example.com", portafolio, "AAPL", TipoOrden.COMPRA, 1L))
                .hasMessageContaining("deshabilitado");
        ordenServicio.emitir("ana@example.com", portafolio, "AAPL", TipoOrden.VENTA, 10L);
        assertThat(portafolioServicio.posicionEn(portafolio, "AAPL")).isPresent();

        servicio.cambiarActivo(aapl, true);
        assertThat(catalogoServicio.consultarCatalogo(null, "aapl", LocalDate.of(2024, 3, 4))).hasSize(1);
    }

    @Test
    void soloElAdministradorEntraALaAdministracionDelCatalogo() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        mvc.perform(get("/admin/instrumentos").with(user("ana@example.com").roles("INVERSIONISTA")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/instrumentos").param("simbolo", "TSLA").param("nombre", "Tesla")
                        .param("tipo", "ACCION").with(user("ana@example.com").roles("INVERSIONISTA")).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/admin/instrumentos").with(user("admin@example.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BTCUSD")));
        mvc.perform(post("/admin/instrumentos").param("simbolo", "tsla").param("nombre", "Tesla Inc.")
                        .param("tipo", "ACCION").with(user("admin@example.com").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/instrumentos").param("simbolo", "").param("nombre", "")
                        .with(user("admin@example.com").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Indica el símbolo.")));

        assertThat(instrumentoRepositorio.findBySimbolo("TSLA")).isPresent();
        assertThat(instrumentoRepositorio.findBySimbolo("TSLA").get().getTipo()).isEqualTo(TipoInstrumento.ACCION);
    }
}
