package co.edu.uis.siminversion.ahorro;

import static co.edu.uis.siminversion.comun.Textos.textoDinero;
import static co.edu.uis.siminversion.web.Entradas.comoMonto;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.transaccion.Transaccion;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** REQ-013. */
@Controller
@RequestMapping("/portafolios/{id}/ahorro")
public class AhorroControlador {

    private final AhorroServicio ahorroServicio;

    public AhorroControlador(AhorroServicio ahorroServicio) {
        this.ahorroServicio = ahorroServicio;
    }

    @GetMapping
    public String vista(@PathVariable long id, Principal principal, Model modelo) {
        modelo.addAttribute("ahorro", ahorroServicio.vista(principal.getName(), id));
        return "portafolio/ahorro";
    }

    @PostMapping
    public String abrir(@PathVariable long id, Principal principal, RedirectAttributes redireccion) {
        try {
            ahorroServicio.abrir(principal.getName(), id);
            redireccion.addFlashAttribute("mensajeExito",
                    "Cuenta de ahorro abierta. Los intereses se suman cada vez que avanzas el reloj.");
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return "redirect:/portafolios/{id}/ahorro";
    }

    @PostMapping("/deposito")
    public String depositar(@PathVariable long id, @RequestParam(required = false) String monto, Principal principal,
                            RedirectAttributes redireccion) {
        try {
            Transaccion deposito = ahorroServicio.depositar(principal.getName(), id, comoMonto(monto));
            redireccion.addFlashAttribute("mensajeExito", "Depositaste " + textoDinero(deposito.getMonto()) + " en la cuenta de ahorro.");
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return "redirect:/portafolios/{id}/ahorro";
    }

    @PostMapping("/retiro")
    public String retirar(@PathVariable long id, @RequestParam(required = false) String monto, Principal principal,
                          RedirectAttributes redireccion) {
        try {
            Transaccion retiro = ahorroServicio.retirar(principal.getName(), id, comoMonto(monto));
            redireccion.addFlashAttribute("mensajeExito", "Retiraste " + textoDinero(retiro.getMonto()) + " al efectivo.");
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return "redirect:/portafolios/{id}/ahorro";
    }
}
