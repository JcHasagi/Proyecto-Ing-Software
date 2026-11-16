package co.edu.uis.siminversion.rendimiento;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class RendimientoControlador {

    private final RendimientoServicio rendimientoServicio;

    public RendimientoControlador(RendimientoServicio rendimientoServicio) {
        this.rendimientoServicio = rendimientoServicio;
    }

    @GetMapping("/portafolios/{id}/rendimiento")
    public String rendimiento(@PathVariable long id, Principal principal, Model modelo) {
        modelo.addAttribute("rendimiento", rendimientoServicio.rendimiento(principal.getName(), id));
        return "portafolio/rendimiento";
    }
}
