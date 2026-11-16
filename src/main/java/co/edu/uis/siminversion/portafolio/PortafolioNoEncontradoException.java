package co.edu.uis.siminversion.portafolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Portafolio u orden inexistente o de otro usuario: en ambos casos se responde 404, sin revelar cuál. */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Portafolio no encontrado")
public class PortafolioNoEncontradoException extends RuntimeException {

    public PortafolioNoEncontradoException() {
        super("Portafolio no encontrado");
    }
}
