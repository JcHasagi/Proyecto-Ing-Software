package co.edu.uis.siminversion.escenario;

import static co.edu.uis.siminversion.comun.Textos.textoFecha;
import static co.edu.uis.siminversion.web.Entradas.comoMonto;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.portafolio.Portafolio;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** REQ-014: elegir un escenario histórico crea un portafolio nuevo con el reloj al inicio del período. */
@Controller
public class EscenarioControlador {

    private final EscenarioServicio escenarioServicio;
    private final PortafolioServicio portafolioServicio;

    public EscenarioControlador(EscenarioServicio escenarioServicio, PortafolioServicio portafolioServicio) {
        this.escenarioServicio = escenarioServicio;
        this.portafolioServicio = portafolioServicio;
    }

    @GetMapping("/escenarios")
    public String escenarios(Model modelo) {
        modelo.addAttribute("escenarios", escenarioServicio.listar());
        modelo.addAttribute("saldoPorDefecto", ReglasDeOperacion.SALDO_INICIAL_POR_DEFECTO);
        return "escenario/lista";
    }

    @PostMapping("/escenarios/{id}")
    public String empezar(@PathVariable long id, @RequestParam(required = false) String saldoInicial,
                          Principal principal, RedirectAttributes redireccion) {
        try {
            Portafolio portafolio = portafolioServicio.crearDesdeEscenario(principal.getName(), id, comoMonto(saldoInicial));
            redireccion.addFlashAttribute("mensajeExito", "Escenario iniciado: el reloj empieza el "
                    + textoFecha(portafolio.getFechaActualSim()) + ". Este es ahora tu portafolio activo.");
            redireccion.addAttribute("portafolio", portafolio.getId());
            return "redirect:/portafolios/{portafolio}";
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
            return "redirect:/escenarios";
        }
    }
}
