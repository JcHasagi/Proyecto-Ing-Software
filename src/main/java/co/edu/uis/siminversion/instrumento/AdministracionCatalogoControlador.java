package co.edu.uis.siminversion.instrumento;

import co.edu.uis.siminversion.comun.OperacionInvalidaException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** REQ-017. Bajo /admin/**: solo el administrador (ver SeguridadConfig). */
@Controller
@RequestMapping("/admin/instrumentos")
public class AdministracionCatalogoControlador {

    private final AdministracionCatalogoServicio servicio;

    public AdministracionCatalogoControlador(AdministracionCatalogoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String lista(Model modelo) {
        modelo.addAttribute("filas", servicio.listar());
        return "admin/instrumentos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) {
        modelo.addAttribute("formulario", new FormularioInstrumento());
        return formulario(modelo, null);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formulario") FormularioInstrumento formulario, BindingResult errores,
                        Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                Instrumento instrumento = servicio.crear(formulario);
                redireccion.addFlashAttribute("mensajeExito", "Instrumento " + instrumento.getSimbolo()
                        + " agregado. Carga su histórico para que tenga precios.");
                return "redirect:/admin/instrumentos";
            } catch (OperacionInvalidaException invalida) {
                errores.reject("invalido", invalida.getMessage());
            }
        }
        return formulario(modelo, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model modelo) {
        modelo.addAttribute("formulario", FormularioInstrumento.de(servicio.obtener(id)));
        return formulario(modelo, id);
    }

    @PostMapping("/{id}")
    public String guardar(@PathVariable long id, @Valid @ModelAttribute("formulario") FormularioInstrumento formulario,
                          BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                Instrumento instrumento = servicio.editar(id, formulario);
                redireccion.addFlashAttribute("mensajeExito", "Instrumento " + instrumento.getSimbolo() + " actualizado.");
                return "redirect:/admin/instrumentos";
            } catch (OperacionInvalidaException invalida) {
                errores.reject("invalido", invalida.getMessage());
            }
        }
        return formulario(modelo, id);
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable long id, @RequestParam boolean activo, RedirectAttributes redireccion) {
        Instrumento instrumento = servicio.cambiarActivo(id, activo);
        redireccion.addFlashAttribute("mensajeExito", instrumento.getSimbolo() + (activo
                ? " vuelve a aparecer en el catálogo."
                : " ya no aparece en el catálogo ni admite compras; las posiciones existentes se conservan."));
        return "redirect:/admin/instrumentos";
    }

    private String formulario(Model modelo, Long id) {
        modelo.addAttribute("instrumentoId", id);
        modelo.addAttribute("tipos", TipoInstrumento.values());
        return "admin/instrumento-formulario";
    }
}
