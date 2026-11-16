package co.edu.uis.siminversion.educacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** REQ-016: entre 6 y 8 artículos en la BD, con los temas pedidos en el plan. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContenidoEducativoTest {

    @Autowired
    private ArticuloRepositorio articuloRepositorio;

    @Autowired
    private WebApplicationContext contexto;

    @Test
    void hayEntreSeisYOchoArticulosConLosTemasDelPlan() {
        List<Articulo> articulos = articuloRepositorio.findAllByOrderByOrdenVisualizacionAsc();

        assertThat(articulos).hasSizeBetween(6, 8);
        assertThat(articulos).extracting(Articulo::getTitulo).contains("Qué es una acción", "Qué es un ETF",
                "Diversificación", "Riesgo", "Volatilidad", "Interés compuesto");
        assertThat(articulos).allSatisfy(articulo -> assertThat(articulo.parrafos()).hasSizeGreaterThanOrEqualTo(3));
        assertThat(articulos).extracting(Articulo::getOrdenVisualizacion).isSorted();
    }

    @Test
    void laListaYCadaArticuloSeMuestranYUnTemaInexistenteDa404() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        Articulo primero = articuloRepositorio.findAllByOrderByOrdenVisualizacionAsc().getFirst();

        mvc.perform(get("/aprende").with(user("ana@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Interés compuesto")));
        mvc.perform(get("/aprende/" + primero.getId()).with(user("ana@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(primero.parrafos().getFirst())))
                .andExpect(content().string(containsString("No es una recomendación de inversión")));
        mvc.perform(get("/aprende/999999").with(user("ana@example.com"))).andExpect(status().isNotFound());
    }
}
