package co.edu.uis.siminversion.carga;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.PrecioDiario;
import co.edu.uis.siminversion.precio.PrecioHistoricoRepositorio;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CargaInicialHistoricosTest {

    @TempDir
    Path carpeta;

    @Autowired
    private InstrumentoRepositorio instrumentoRepositorio;

    @Autowired
    private PrecioHistoricoRepositorio precioHistoricoRepositorio;

    @Autowired
    private CargaHistoricoServicio cargaHistoricoServicio;

    @Autowired
    private ProveedorDePrecios proveedorDePrecios;

    private CargaInicialHistoricos cargaInicial(String directorio) {
        return new CargaInicialHistoricos(new PropiedadesHistorico(directorio), instrumentoRepositorio,
                precioHistoricoRepositorio, cargaHistoricoServicio);
    }

    private List<PrecioDiario> historicoDe(String simbolo) {
        long id = instrumentoRepositorio.findBySimbolo(simbolo).orElseThrow().getId();
        return proveedorDePrecios.historico(id, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
    }

    @Test
    void cargaLosArchivosDeInstrumentosSinPreciosYDespuesNoLosVuelveALeer() throws IOException {
        Files.writeString(carpeta.resolve("MSFT.csv"), """
                fecha,apertura,maximo,minimo,cierre,volumen
                2024-03-01,10.00,11.00,9.50,10.50,1000
                2024-03-04,10.50,12.00,10.00,11.75,2000
                """, StandardCharsets.UTF_8);

        cargaInicial(carpeta.toString()).run(null);
        assertThat(historicoDe("MSFT")).hasSize(2);

        // Con precios ya cargados, un archivo nuevo no se aplica: las actualizaciones van por /admin/carga.
        Files.writeString(carpeta.resolve("MSFT.csv"), """
                fecha,apertura,maximo,minimo,cierre,volumen
                2024-03-05,11.75,12.50,11.50,12.00,3000
                """, StandardCharsets.UTF_8);
        cargaInicial(carpeta.toString()).run(null);
        assertThat(historicoDe("MSFT")).hasSize(2);
    }

    @Test
    void noHaceNadaSinCarpetaConfiguradaOSiLaCarpetaNoExiste() throws IOException {
        cargaInicial("").run(null);
        cargaInicial(carpeta.resolve("no-existe").toString()).run(null);

        assertThat(historicoDe("MSFT")).isEmpty();
    }
}
