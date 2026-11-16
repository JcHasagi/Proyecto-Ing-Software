package co.edu.uis.siminversion.orden;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import java.security.Principal;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * REQ-006/007 en dos pasos: la estimación muestra costo, comisión y efectivo resultante, y la confirmación
 * registra la orden. REQ-008: cancelación.
 */
@Controller
@RequestMapping("/portafolios/{portafolioId}/ordenes")
public class OrdenControlador {

    private final OrdenServicio ordenServicio;

    public OrdenControlador(OrdenServicio ordenServicio) {
        this.ordenServicio = ordenServicio;
    }

    @PostMapping("/estimacion")
    public String estimar(@PathVariable long portafolioId, @RequestParam String simbolo,
                          @RequestParam(required = false) TipoOrden tipo,
                          @RequestParam(required = false) String cantidad, Principal principal, Model modelo,
                          RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("estimacion",
                    ordenServicio.estimar(principal.getName(), portafolioId, simbolo, tipo, cantidadEntera(cantidad)));
            return "orden/confirmacion";
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
            redireccion.addFlashAttribute("cantidadIngresada", cantidad);
            return volverAlInstrumento(simbolo, redireccion);
        }
    }

    @PostMapping
    public String emitir(@PathVariable long portafolioId, @RequestParam String simbolo,
                         @RequestParam(required = false) TipoOrden tipo,
                         @RequestParam(required = false) String cantidad, Principal principal,
                         RedirectAttributes redireccion) {
        try {
            Orden orden = ordenServicio.emitir(principal.getName(), portafolioId, simbolo, tipo, cantidadEntera(cantidad));
            redireccion.addFlashAttribute("mensajeExito", "Orden de " + orden.getTipo().getEtiqueta().toLowerCase(Locale.ROOT)
                    + " de " + orden.getCantidad() + " " + orden.getInstrumento().getSimbolo()
                    + " registrada como pendiente. Se ejecutará al cierre del siguiente día bursátil, cuando avances el reloj.");
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return volverAlInstrumento(simbolo, redireccion);
    }

    @PostMapping("/{ordenId}/cancelar")
    public String cancelar(@PathVariable long portafolioId, @PathVariable long ordenId, Principal principal,
                           RedirectAttributes redireccion) {
        try {
            Orden orden = ordenServicio.cancelar(principal.getName(), portafolioId, ordenId);
            redireccion.addFlashAttribute("mensajeExito", "Orden de " + orden.getTipo().getEtiqueta().toLowerCase(Locale.ROOT)
                    + " de " + orden.getCantidad() + " " + orden.getInstrumento().getSimbolo() + " cancelada.");
        } catch (OperacionInvalidaException invalida) {
            redireccion.addFlashAttribute("mensajeError", invalida.getMessage());
        }
        return "redirect:/portafolios/{portafolioId}";
    }

    /** RN-07: solo enteros positivos. Cualquier otro texto llega como null y el servicio explica el error. */
    private static Long cantidadEntera(String texto) {
        if (texto == null || !texto.trim().matches("\\d{1,15}")) {
            return null;
        }
        return Long.parseLong(texto.trim());
    }

    private static String volverAlInstrumento(String simbolo, RedirectAttributes redireccion) {
        redireccion.addAttribute("simbolo", simbolo);
        return "redirect:/catalogo/{simbolo}";
    }
}
