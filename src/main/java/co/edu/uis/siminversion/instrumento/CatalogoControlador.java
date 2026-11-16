package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/catalogo")
public class CatalogoControlador {

    private final CatalogoServicio catalogoServicio;
    private final PortafolioServicio portafolioServicio;
    private final Clock relojSistema;

    public CatalogoControlador(CatalogoServicio catalogoServicio, PortafolioServicio portafolioServicio,
                               Clock relojSistema) {
        this.catalogoServicio = catalogoServicio;
        this.portafolioServicio = portafolioServicio;
        this.relojSistema = relojSistema;
    }

    @GetMapping
    public String catalogo(@RequestParam(required = false) TipoInstrumento tipo,
                           @RequestParam(required = false) String texto, Principal principal, Model modelo) {
        modelo.addAttribute("instrumentos",
                catalogoServicio.consultarCatalogo(tipo, texto,
                        fechaLimiteDePrecios(portafolioServicio.activo(principal.getName()))));
        modelo.addAttribute("tipos", TipoInstrumento.values());
        modelo.addAttribute("tipoSeleccionado", tipo);
        modelo.addAttribute("texto", texto);
        return "catalogo/lista";
    }

    @GetMapping("/{simbolo}")
    public String detalle(@PathVariable String simbolo,
                          @RequestParam(defaultValue = "UN_ANIO") RangoHistorico rango, Principal principal,
                          Model modelo) {
        Optional<Portafolio> activo = portafolioServicio.activo(principal.getName());
        DetalleInstrumento detalle = catalogoServicio.consultarDetalle(simbolo, rango, fechaLimiteDePrecios(activo));
        modelo.addAttribute("detalle", detalle);
        modelo.addAttribute("rangos", RangoHistorico.values());
        // REQ-005: la posición del usuario en el instrumento, en el portafolio activo.
        modelo.addAttribute("posicion", activo
                .flatMap(portafolio -> portafolioServicio.posicionEn(portafolio.getId(), detalle.instrumento().simbolo()))
                .orElse(null));
        return "catalogo/detalle";
    }

    /**
     * Fecha máxima de los precios visibles (REQ-004/005): la fecha simulada del portafolio activo.
     * Sin portafolio todavía no hay simulación y se muestra el histórico hasta hoy.
     */
    private LocalDate fechaLimiteDePrecios(Optional<Portafolio> activo) {
        return activo.map(Portafolio::getFechaActualSim)
                .orElseGet(() -> LocalDate.now(relojSistema));
    }
}
