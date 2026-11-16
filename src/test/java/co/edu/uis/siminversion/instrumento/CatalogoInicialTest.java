package co.edu.uis.siminversion.instrumento;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Las migraciones de Flyway arrancan en limpio y siembran el catálogo exigido. */
@SpringBootTest
@ActiveProfiles("test")
class CatalogoInicialTest {

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Test
    void elCatalogoInicialTieneAlMenos20InstrumentosDeLosCuatroTipos() {
        List<Instrumento> instrumentos = instrumentoRepositorio.findAll();

        assertThat(instrumentos).hasSizeGreaterThanOrEqualTo(20);
        assertThat(instrumentos).extracting(Instrumento::getTipo).contains(TipoInstrumento.values());
    }

    @Test
    void elPatronDeBusquedaTrataLosComodinesComoTextoLiteral() {
        assertThat(CatalogoServicio.patronDeBusqueda("  ")).isNull();
        assertThat(CatalogoServicio.patronDeBusqueda("Bit")).isEqualTo("%bit%");
        assertThat(CatalogoServicio.patronDeBusqueda("50%_!")).isEqualTo("%50!%!_!!%");
    }
}
