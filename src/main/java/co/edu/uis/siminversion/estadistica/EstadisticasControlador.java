package co.edu.uis.siminversion.estadistica;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** REQ-019. Bajo /admin/**: solo el administrador. Por defecto muestra los últimos 30 días. */
@Controller
public class EstadisticasControlador {

    private static final int DIAS_POR_DEFECTO = 30;

    private final EstadisticasServicio estadisticasServicio;
    private final Clock relojSistema;

    public EstadisticasControlador(EstadisticasServicio estadisticasServicio, Clock relojSistema) {
        this.estadisticasServicio = estadisticasServicio;
        this.relojSistema = relojSistema;
    }

    @GetMapping("/admin/estadisticas")
    public String estadisticas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               Model modelo) {
        LocalDate hoy = LocalDate.now(relojSistema);
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : desde;
        modelo.addAttribute("desde", inicio);
        modelo.addAttribute("hasta", fin);
        try {
            modelo.addAttribute("estadisticas", estadisticasServicio.calcular(inicio, fin));
        } catch (OperacionInvalidaException rangoInvalido) {
            modelo.addAttribute("mensajeError", rangoInvalido.getMessage());
        }
        return "admin/estadisticas";
    }
}
