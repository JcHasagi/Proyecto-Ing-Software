package co.edu.uis.siminversion.web;

import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.portafolio.ResumenBarra;
import java.security.Principal;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Datos y errores comunes a todas las páginas. */
@ControllerAdvice(annotations = Controller.class)
public class ModeloComun {

    private final PortafolioServicio portafolioServicio;

    public ModeloComun(PortafolioServicio portafolioServicio) {
        this.portafolioServicio = portafolioServicio;
    }

    /** Portafolio activo para la barra superior: nombre, fecha simulada y efectivo. */
    @ModelAttribute("portafolioActivo")
    public ResumenBarra portafolioActivo(Principal principal) {
        return principal == null ? null : portafolioServicio.resumenDelActivo(principal.getName()).orElse(null);
    }

    /** Dos operaciones simultáneas sobre el mismo portafolio (por ejemplo, un doble clic en "Avanzar"). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String operacionSimultanea(Model modelo) {
        modelo.addAttribute("status", HttpStatus.CONFLICT.value());
        return "error";
    }
}
