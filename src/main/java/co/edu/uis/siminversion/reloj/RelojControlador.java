package co.edu.uis.siminversion.reloj;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** REQ-009: avanzar el reloj hasta una fecha elegida o con un atajo (siguiente día, una semana, un mes). */
@Controller
public class RelojControlador {

    private final RelojServicio relojServicio;

    public RelojControlador(RelojServicio relojServicio) {
        this.relojServicio = relojServicio;
    }

    @PostMapping("/portafolios/{id}/reloj")
    public String avanzar(@PathVariable long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                          @RequestParam(required = false) RelojServicio.Salto salto, Principal principal,
                          RedirectAttributes redireccion) {
        try {
            LocalDate destino = salto == null ? fecha : relojServicio.destinoDeSalto(principal.getName(), id, salto);
            redireccion.addFlashAttribute("avance", relojServicio.avanzar(principal.getName(), id, destino));
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return "redirect:/portafolios/{id}";
    }
}
