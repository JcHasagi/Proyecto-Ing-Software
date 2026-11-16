package co.edu.uis.siminversion.portafolio;

import static co.edu.uis.siminversion.comun.Textos.textoFecha;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.precio.ProveedorDePrecios;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** REQ-003 (crear), REQ-015 (reiniciar), portafolio activo y vista del portafolio. */
@Controller
public class PortafolioControlador {

    private final PortafolioServicio portafolioServicio;
    private final ProveedorDePrecios proveedorDePrecios;

    public PortafolioControlador(PortafolioServicio portafolioServicio, ProveedorDePrecios proveedorDePrecios) {
        this.portafolioServicio = portafolioServicio;
        this.proveedorDePrecios = proveedorDePrecios;
    }

    /** Vista principal tras iniciar sesión: el portafolio activo, o la invitación a crear uno (REQ-002). */
    @GetMapping("/portafolio")
    public String portafolioActivo(Principal principal) {
        String correo = principal.getName();
        return portafolioServicio.activo(correo)
                .map(portafolio -> "redirect:/portafolios/" + portafolio.getId())
                .orElseGet(() -> portafolioServicio.tienePortafolios(correo)
                        ? "redirect:/portafolios"
                        : "redirect:/portafolios/nuevo");
    }

    @GetMapping("/portafolios")
    public String lista(Principal principal, Model modelo) {
        modelo.addAttribute("filas", portafolioServicio.listar(principal.getName()));
        return "portafolio/lista";
    }

    @GetMapping("/portafolios/nuevo")
    public String formularioNuevo(Model modelo) {
        modelo.addAttribute("formulario", new FormularioPortafolio());
        agregarLimitesDeFecha(modelo);
        return "portafolio/nuevo";
    }

    @PostMapping("/portafolios")
    public String crear(@Valid @ModelAttribute("formulario") FormularioPortafolio formulario, BindingResult errores,
                        Principal principal, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                Portafolio portafolio = portafolioServicio.crear(principal.getName(), formulario.getNombre(),
                        formulario.getSaldoInicial(), formulario.getFechaInicial());
                redireccion.addFlashAttribute("mensajeExito", "Portafolio creado. Su reloj empieza el "
                        + textoFecha(portafolio.getFechaActualSim()) + ".");
                redireccion.addAttribute("id", portafolio.getId());
                return "redirect:/portafolios/{id}";
            } catch (OperacionInvalidaException invalida) {
                errores.reject("invalida", invalida.getMessage());
            }
        }
        agregarLimitesDeFecha(modelo);
        return "portafolio/nuevo";
    }

    @PostMapping("/portafolios/{id}/activar")
    public String activar(@PathVariable long id, Principal principal) {
        portafolioServicio.activar(principal.getName(), id);
        return "redirect:/portafolios/{id}";
    }

    @GetMapping("/portafolios/{id}")
    public String detalle(@PathVariable long id, Principal principal, Model modelo) {
        modelo.addAttribute("resumen", portafolioServicio.resumen(principal.getName(), id));
        return "portafolio/detalle";
    }

    @GetMapping("/portafolios/{id}/reinicio")
    public String confirmarReinicio(@PathVariable long id, Principal principal, Model modelo) {
        modelo.addAttribute("resumen", portafolioServicio.resumen(principal.getName(), id));
        return "portafolio/reinicio";
    }

    @PostMapping("/portafolios/{id}/reinicio")
    public String reiniciar(@PathVariable long id, Principal principal, RedirectAttributes redireccion) {
        try {
            Portafolio nuevo = portafolioServicio.reiniciar(principal.getName(), id);
            redireccion.addFlashAttribute("mensajeExito",
                    "El portafolio anterior quedó archivado con todo su historial. Este es el nuevo portafolio activo.");
            redireccion.addAttribute("nuevo", nuevo.getId());
            return "redirect:/portafolios/{nuevo}";
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
            return "redirect:/portafolios/{id}";
        }
    }

    private void agregarLimitesDeFecha(Model modelo) {
        modelo.addAttribute("primeraFecha", proveedorDePrecios.primeraFechaDelHistorico().orElse(null));
        modelo.addAttribute("ultimaFecha", proveedorDePrecios.ultimaFechaDelHistorico().orElse(null));
    }
}
