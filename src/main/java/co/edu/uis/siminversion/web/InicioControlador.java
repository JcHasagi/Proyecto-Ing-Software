package co.edu.uis.siminversion.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioControlador {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/portafolio";
    }
}
