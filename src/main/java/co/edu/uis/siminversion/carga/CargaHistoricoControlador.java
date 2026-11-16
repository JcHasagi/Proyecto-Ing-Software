package co.edu.uis.siminversion.carga;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * REQ-018 en dos pasos: validar el archivo y mostrar el resumen; guardar solo cuando el administrador confirma.
 * La ruta /admin/** solo la puede usar el administrador (ver SeguridadConfig).
 */
@Controller
@RequestMapping("/admin/carga")
public class CargaHistoricoControlador {

    /** Atributo de sesión con la validación que espera confirmación. Hay una sola por sesión. */
    static final String CARGA_PENDIENTE = "cargaPendiente";

    private final CargaHistoricoServicio cargaHistoricoServicio;

    public CargaHistoricoControlador(CargaHistoricoServicio cargaHistoricoServicio) {
        this.cargaHistoricoServicio = cargaHistoricoServicio;
    }

    @GetMapping
    public String formulario(Model modelo, HttpSession sesion) {
        modelo.addAttribute("instrumentos", cargaHistoricoServicio.instrumentosDisponibles());
        modelo.addAttribute("pendiente", sesion.getAttribute(CARGA_PENDIENTE));
        return "admin/carga";
    }

    /** Paso 1: valida el archivo y muestra el resumen. Todavía no guarda nada. */
    @PostMapping
    public String validar(@RequestParam String simbolo, @RequestParam MultipartFile archivo, Model modelo,
                          HttpSession sesion) {
        // Una validación nueva reemplaza a la que estuviera esperando.
        sesion.removeAttribute(CARGA_PENDIENTE);
        ValidacionCarga validacion = validarArchivo(simbolo, archivo);
        if (validacion.aceptada() && validacion.tieneFilasNuevas()) {
            sesion.setAttribute(CARGA_PENDIENTE, CargaPendiente.de(validacion));
        }
        modelo.addAttribute("validacion", validacion);
        modelo.addAttribute("simboloSeleccionado", simbolo);
        return formulario(modelo, sesion);
    }

    /** Paso 2: el administrador confirma el resumen y se guardan las filas nuevas. */
    @PostMapping("/confirmacion")
    public String confirmar(@RequestParam String codigo, HttpSession sesion, RedirectAttributes redireccion) {
        if (!(sesion.getAttribute(CARGA_PENDIENTE) instanceof CargaPendiente pendiente)
                || !pendiente.codigo().equals(codigo)) {
            redireccion.addFlashAttribute("mensajeError",
                    "Esa validación ya no está disponible. Selecciona el archivo de nuevo.");
            return "redirect:/admin/carga";
        }
        sesion.removeAttribute(CARGA_PENDIENTE);
        redireccion.addFlashAttribute("resultado", cargaHistoricoServicio.confirmar(pendiente.validacion()));
        return "redirect:/admin/carga";
    }

    /** El administrador descarta el resumen: no se guarda nada. */
    @PostMapping("/cancelacion")
    public String cancelar(HttpSession sesion, RedirectAttributes redireccion) {
        sesion.removeAttribute(CARGA_PENDIENTE);
        redireccion.addFlashAttribute("mensajeExito", "Carga cancelada: no se guardó ningún precio.");
        return "redirect:/admin/carga";
    }

    private ValidacionCarga validarArchivo(String simbolo, MultipartFile archivo) {
        if (archivo.isEmpty()) {
            return ValidacionCarga.rechazada(simbolo, List.of("Selecciona un archivo CSV con datos."));
        }
        try (Reader contenido = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8)) {
            return cargaHistoricoServicio.validar(simbolo, contenido);
        } catch (IOException errorDeLectura) {
            return ValidacionCarga.rechazada(simbolo,
                    List.of("No se pudo leer el archivo: " + errorDeLectura.getMessage()));
        }
    }
}
