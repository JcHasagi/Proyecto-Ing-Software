package co.edu.uis.siminversion.educacion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** REQ-016: lista de temas y lectura de cada artículo. Solo consulta; no hay lógica de negocio. */
@Controller
public class ContenidoEducativoControlador {

    private final ArticuloRepositorio articuloRepositorio;

    public ContenidoEducativoControlador(ArticuloRepositorio articuloRepositorio) {
        this.articuloRepositorio = articuloRepositorio;
    }

    @GetMapping("/aprende")
    public String temas(Model modelo) {
        modelo.addAttribute("articulos", articuloRepositorio.findAllByOrderByOrdenVisualizacionAsc());
        return "aprende/lista";
    }

    @GetMapping("/aprende/{id}")
    public String articulo(@PathVariable long id, Model modelo) {
        Articulo articulo = articuloRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tema no encontrado"));
        modelo.addAttribute("articulo", articulo);
        modelo.addAttribute("articulos", articuloRepositorio.findAllByOrderByOrdenVisualizacionAsc());
        return "aprende/articulo";
    }
}
