package co.edu.uis.siminversion.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uis.siminversion.PreciosDePrueba;
import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import co.edu.uis.siminversion.usuario.Rol;
import co.edu.uis.siminversion.usuario.UsuarioRepositorio;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeguridadYNavegacionWebTest {

    private static final RequestPostProcessor INVERSIONISTA =
            user("inversionista@example.com").roles(Rol.INVERSIONISTA.name());
    private static final RequestPostProcessor ADMINISTRADOR =
            user("admin@example.com").roles(Rol.ADMINISTRADOR.name());

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private ProveedorDePrecios proveedorDePrecios;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void unVisitanteEsRedirigidoAlInicioDeSesion() throws Exception {
        mvc.perform(get("/catalogo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void elInicioDeSesionYElRegistroSonPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/registro")).andExpect(status().isOk());
    }

    @Test
    void registroPorFormularioYLuegoInicioDeSesion() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("correo", "Nuevo@Example.com")
                        .param("nombre", "Nuevo Usuario")
                        .param("contrasena", "clave-segura-1")
                        .param("confirmacionContrasena", "clave-segura-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        assertThat(usuarioRepositorio.findByCorreo("nuevo@example.com"))
                .hasValueSatisfying(usuario -> assertThat(usuario.getRol()).isEqualTo(Rol.INVERSIONISTA));

        mvc.perform(formLogin("/login").user("correo", "nuevo@example.com").password("contrasena", "clave-segura-1"))
                .andExpect(authenticated().withUsername("nuevo@example.com"));
    }

    /** RNF-02: al cerrar la sesión queda invalidada y no se puede reutilizar. */
    @Test
    void unaSesionCerradaNoSePuedeReutilizar() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("correo", "sesion@example.com")
                        .param("nombre", "Sesion")
                        .param("contrasena", "clave-segura-1")
                        .param("confirmacionContrasena", "clave-segura-1"))
                .andExpect(redirectedUrl("/login?registrado"));
        MockHttpSession sesion = (MockHttpSession) mvc
                .perform(formLogin("/login").user("correo", "sesion@example.com")
                        .password("contrasena", "clave-segura-1"))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/catalogo").session(sesion)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/login?salida"));

        assertThat(sesion.isInvalid()).isTrue();
        mvc.perform(get("/catalogo").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void elRegistroMuestraErroresSinCrearLaCuenta() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("correo", "otro@example.com")
                        .param("nombre", "Otro")
                        .param("contrasena", "clave-segura-1")
                        .param("confirmacionContrasena", "diferente-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las contraseñas no coinciden.")));

        assertThat(usuarioRepositorio.existsByCorreo("otro@example.com")).isFalse();
    }

    @Test
    void unInversionistaNoPuedeUsarLaCargaMasiva() throws Exception {
        mvc.perform(get("/admin/carga").with(INVERSIONISTA)).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/carga").file(archivoCsv()).param("simbolo", "AAPL")
                        .with(INVERSIONISTA).with(csrf()))
                .andExpect(status().isForbidden());

        long aapl = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();
        assertThat(proveedorDePrecios.precioCierre(aapl, LocalDate.of(2024, 3, 1))).isEmpty();
    }

    @Test
    void elEnlaceDeCargaSoloApareceParaAdministradores() throws Exception {
        mvc.perform(get("/catalogo").with(INVERSIONISTA))
                .andExpect(content().string(not(containsString("Carga de históricos"))));
        mvc.perform(get("/catalogo").with(ADMINISTRADOR))
                .andExpect(content().string(containsString("Carga de históricos")));
    }

    @Test
    void unAdministradorValidaUnArchivoYVeElResumenSinGuardar() throws Exception {
        // El flujo completo (confirmar o cancelar) está en CargaEnDosPasosTest.
        mvc.perform(multipart("/admin/carga").file(archivoCsv()).param("simbolo", "AAPL")
                        .with(ADMINISTRADOR).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resumen de la validación")));

        long aapl = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();
        assertThat(proveedorDePrecios.precioCierre(aapl, LocalDate.of(2024, 3, 1))).isEmpty();
    }

    @Test
    void elCatalogoListaYFiltraLosInstrumentos() throws Exception {
        mvc.perform(get("/catalogo").with(INVERSIONISTA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AAPL")))
                .andExpect(content().string(containsString("BTCUSD")));

        mvc.perform(get("/catalogo").param("tipo", "DIVISA").with(INVERSIONISTA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EURUSD")))
                .andExpect(content().string(not(containsString("AAPL"))));

        mvc.perform(get("/catalogo").param("texto", "bit").with(INVERSIONISTA))
                .andExpect(content().string(containsString("BTCUSD")))
                .andExpect(content().string(not(containsString("ETHUSD"))));
    }

    @Test
    void elDetalleMuestraElGraficoYUnSimboloInexistenteDa404() throws Exception {
        new PreciosDePrueba(jdbcTemplate, instrumentoRepositorio).insertar("AAPL", "2024-03-01", "101.75");

        mvc.perform(get("/catalogo/AAPL").param("rango", "TODO").with(INVERSIONISTA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("serieHistorico")))
                .andExpect(content().string(containsString("2024-03-01")));

        mvc.perform(get("/catalogo/NOEXISTE").with(INVERSIONISTA)).andExpect(status().isNotFound());
    }

    private static MockMultipartFile archivoCsv() {
        String contenido = """
                Date,Open,High,Low,Close,Volume
                2024-03-01,100.00,102.50,99.10,101.75,1250000
                """;
        return new MockMultipartFile("archivo", "aapl.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }
}
