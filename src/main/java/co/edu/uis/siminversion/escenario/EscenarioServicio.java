package co.edu.uis.siminversion.escenario;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista de escenarios de REQ-014. La creación del portafolio la hace PortafolioServicio.crearDesdeEscenario. */
@Service
public class EscenarioServicio {

    private final EscenarioRepositorio escenarioRepositorio;
    private final ProveedorDePrecios proveedorDePrecios;
    private final InstrumentoRepositorio instrumentoRepositorio;

    public EscenarioServicio(EscenarioRepositorio escenarioRepositorio, ProveedorDePrecios proveedorDePrecios,
                             InstrumentoRepositorio instrumentoRepositorio) {
        this.escenarioRepositorio = escenarioRepositorio;
        this.proveedorDePrecios = proveedorDePrecios;
        this.instrumentoRepositorio = instrumentoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<VistaEscenario> listar() {
        long enCatalogo = instrumentoRepositorio.countByActivoTrue();
        return escenarioRepositorio.findAllByOrderByOrdenVisualizacionAsc().stream()
                .map(escenario -> new VistaEscenario(escenario, proveedorDePrecios
                        .instrumentosActivosConPrecios(escenario.getFechaInicio(), escenario.getFechaFin()),
                        enCatalogo))
                .toList();
    }
}
