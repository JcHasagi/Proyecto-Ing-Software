package co.edu.uis.siminversion.historial;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import co.edu.uis.siminversion.transaccion.TipoTransaccion;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HistorialControlador {

    private final HistorialServicio historialServicio;

    public HistorialControlador(HistorialServicio historialServicio) {
        this.historialServicio = historialServicio;
    }

    @GetMapping("/portafolios/{id}/transacciones")
    public String historial(@PathVariable long id, @RequestParam(required = false) TipoTransaccion tipo,
                            @RequestParam(required = false) String simbolo,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            Principal principal, Model modelo) {
        FiltroHistorial filtro = new FiltroHistorial(tipo, simbolo, desde, hasta);
        try {
            modelo.addAttribute("historial", historialServicio.consultar(principal.getName(), id, filtro));
        } catch (OperacionInvalidaException rangoInvalido) {
            modelo.addAttribute("mensajeError", rangoInvalido.getMessage());
            modelo.addAttribute("historial",
                    historialServicio.consultar(principal.getName(), id, new FiltroHistorial(tipo, simbolo, null, null)));
        }
        modelo.addAttribute("tipos", TipoTransaccion.values());
        return "portafolio/transacciones";
    }
}
