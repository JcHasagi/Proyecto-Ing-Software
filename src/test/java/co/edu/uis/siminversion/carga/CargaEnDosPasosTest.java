package co.edu.uis.siminversion.carga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * REQ-018 en dos pasos: validar el archivo muestra un resumen y no guarda nada; los precios se guardan solo
 * cuando el administrador confirma, y puede cancelar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CargaEnDosPasosTest {

    private static final RequestPostProcessor ADMINISTRADOR = user("admin@example.com").roles("ADMINISTRADOR");
    private static final RequestPostProcessor INVERSIONISTA = user("ana@example.com").roles("INVERSIONISTA");

    /** Tres fechas; entre la segunda y la tercera hay un hueco de casi un mes. */
    private static final String VALIDO_CON_HUECO = """
            Date,Open,High,Low,Close,Volume
            2024-03-01,100.00,102.50,99.10,101.75,1250000
            2024-03-04,101.00,103.00,100.00,102.00,1100000
            2024-04-01,105.00,106.00,104.00,105.50,900000
            """;
    private static final String CON_FECHA_REPETIDA = """
            Date,Open,High,Low,Close,Volume
            2024-03-01,100.00,102.50,99.10,101.75,1250000
            2024-03-01,101.00,103.00,100.00,102.00,1100000
            """;

    @Autowired
    private CargaHistoricoServicio cargaHistoricoServicio;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private ProveedorDePrecios proveedorDePrecios;

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;
    private MockHttpSession sesion;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        sesion = new MockHttpSession();
    }

    @Test
    void validarNoGuardaNadaYConfirmarGuardaLasFilasNuevas() throws IOException {
        ValidacionCarga validacion = cargaHistoricoServicio.validar("aapl", new StringReader(VALIDO_CON_HUECO));

        assertThat(validacion.aceptada()).isTrue();
        assertThat(validacion.simbolo()).isEqualTo("AAPL");
        assertThat(validacion.filasLeidas()).isEqualTo(3);
        assertThat(validacion.cantidadDeFilasNuevas()).isEqualTo(3);
        assertThat(validacion.advertencias()).singleElement().asString().contains("Hueco");
        assertThat(preciosDeAapl()).isEmpty();

        ResultadoCarga resultado = cargaHistoricoServicio.confirmar(validacion);

        assertThat(resultado.aceptada()).isTrue();
        assertThat(resultado.filasInsertadas()).isEqualTo(3);
        assertThat(preciosDeAapl()).hasSize(3);
    }

    @Test
    void confirmarOmiteLasFechasQueSeCargaronDespuesDeValidarSinSobrescribirlas() throws IOException {
        ValidacionCarga validacion = cargaHistoricoServicio.validar("AAPL", new StringReader(VALIDO_CON_HUECO));
        // Mientras el resumen esperaba la confirmación, otra carga guardó el 1 de marzo con otro precio.
        cargaHistoricoServicio.cargar("AAPL", new StringReader("""
                Date,Open,High,Low,Close
                2024-03-01,50.00,51.00,49.00,50.50
                """));

        ResultadoCarga resultado = cargaHistoricoServicio.confirmar(validacion);

        assertThat(resultado.filasInsertadas()).isEqualTo(2);
        assertThat(resultado.fechasOmitidas()).containsExactly(LocalDate.of(2024, 3, 1));
        assertThat(preciosDeAapl()).hasSize(3);
        assertThat(preciosDeAapl().getFirst().cierre()).isEqualByComparingTo(new BigDecimal("50.50"));
    }

    @Test
    void elAdministradorVeElResumenYSoloAlConfirmarSeGuardanLosPrecios() throws Exception {
        subir(VALIDO_CON_HUECO)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resumen de la validación")))
                .andExpect(content().string(containsString("Todavía no se ha guardado nada")))
                .andExpect(content().string(containsString("Huecos en la serie")))
                .andExpect(content().string(containsString("Confirmar y guardar")))
                .andExpect(content().string(containsString("Cancelar")));
        assertThat(preciosDeAapl()).as("validar no guarda").isEmpty();

        MvcResult confirmacion = mvc.perform(post("/admin/carga/confirmacion").session(sesion)
                        .param("codigo", pendiente().codigo()).with(ADMINISTRADOR).with(csrf()))
                .andExpect(redirectedUrl("/admin/carga"))
                .andExpect(flash().attributeExists("resultado"))
                .andReturn();

        assertThat(preciosDeAapl()).hasSize(3);
        assertThat(sesion.getAttribute(CargaHistoricoControlador.CARGA_PENDIENTE)).isNull();
        mvc.perform(get("/admin/carga").session(sesion).flashAttrs(confirmacion.getFlashMap()).with(ADMINISTRADOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carga completada")))
                .andExpect(content().string(not(containsString("Confirmar y guardar"))));
    }

    @Test
    void cancelarDescartaElResumenYElCodigoViejoYaNoSirve() throws Exception {
        subir(VALIDO_CON_HUECO).andExpect(status().isOk());
        String codigo = pendiente().codigo();

        mvc.perform(post("/admin/carga/cancelacion").session(sesion).with(ADMINISTRADOR).with(csrf()))
                .andExpect(redirectedUrl("/admin/carga"))
                .andExpect(flash().attribute("mensajeExito", containsString("no se guardó ningún precio")));
        assertThat(sesion.getAttribute(CargaHistoricoControlador.CARGA_PENDIENTE)).isNull();

        mvc.perform(post("/admin/carga/confirmacion").session(sesion).param("codigo", codigo)
                        .with(ADMINISTRADOR).with(csrf()))
                .andExpect(redirectedUrl("/admin/carga"))
                .andExpect(flash().attribute("mensajeError", containsString("ya no está disponible")));
        assertThat(preciosDeAapl()).isEmpty();
    }

    @Test
    void unCodigoQueNoEsElDelResumenNoGuardaNada() throws Exception {
        subir(VALIDO_CON_HUECO).andExpect(status().isOk());

        mvc.perform(post("/admin/carga/confirmacion").session(sesion).param("codigo", "otro-codigo")
                        .with(ADMINISTRADOR).with(csrf()))
                .andExpect(flash().attributeExists("mensajeError"));

        assertThat(preciosDeAapl()).isEmpty();
        assertThat(pendiente()).as("el resumen sigue esperando").isNotNull();
    }

    @Test
    void unArchivoInvalidoSeRechazaCompletoSinPedirConfirmacion() throws Exception {
        subir(CON_FECHA_REPETIDA)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no pasó la validación")))
                .andExpect(content().string(containsString("2024-03-01")))
                .andExpect(content().string(not(containsString("Confirmar y guardar"))));

        assertThat(sesion.getAttribute(CargaHistoricoControlador.CARGA_PENDIENTE)).isNull();
        assertThat(preciosDeAapl()).isEmpty();
    }

    @Test
    void unArchivoSinFechasNuevasNoPideConfirmacion() throws Exception {
        cargaHistoricoServicio.cargar("AAPL", new StringReader(VALIDO_CON_HUECO));

        subir(VALIDO_CON_HUECO)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no hay nada que guardar")))
                .andExpect(content().string(not(containsString("Confirmar y guardar"))));

        assertThat(sesion.getAttribute(CargaHistoricoControlador.CARGA_PENDIENTE)).isNull();
    }

    @Test
    void elResumenAvisaLasFechasQueYaEstabanCargadasYQueNoSeSobrescriben() throws Exception {
        cargaHistoricoServicio.cargar("AAPL", new StringReader("""
                Date,Open,High,Low,Close
                2024-03-01,50.00,51.00,49.00,50.50
                """));

        subir(VALIDO_CON_HUECO)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se omitirá 1 fecha que ya estaba cargada")))
                .andExpect(content().string(containsString("Confirmar y guardar")));

        assertThat(pendiente().validacion().cantidadDeFilasNuevas()).isEqualTo(2);
    }

    @Test
    void unInversionistaNoPuedeConfirmarNiCancelarUnaCarga() throws Exception {
        mvc.perform(post("/admin/carga/confirmacion").param("codigo", "x").with(INVERSIONISTA).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/carga/cancelacion").with(INVERSIONISTA).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private ResultActions subir(String contenido) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "aapl.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(multipart("/admin/carga").file(archivo).param("simbolo", "AAPL").session(sesion)
                .with(ADMINISTRADOR).with(csrf()));
    }

    private CargaPendiente pendiente() {
        return (CargaPendiente) sesion.getAttribute(CargaHistoricoControlador.CARGA_PENDIENTE);
    }

    private List<PrecioDiario> preciosDeAapl() {
        long aapl = instrumentoRepositorio.findBySimbolo("AAPL").orElseThrow().getId();
        return proveedorDePrecios.historico(aapl, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
    }
}
