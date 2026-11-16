package co.edu.uis.siminversion.instrumento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Símbolo o id que no está en el catálogo: la página responde 404. */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Instrumento no encontrado")
public class InstrumentoNoEncontradoException extends RuntimeException {

    public InstrumentoNoEncontradoException(String simbolo) {
        super("No existe un instrumento con símbolo " + simbolo);
    }

    private InstrumentoNoEncontradoException(long id) {
        super("No existe un instrumento con id " + id);
    }

    public static InstrumentoNoEncontradoException conId(long id) {
        return new InstrumentoNoEncontradoException(id);
    }
}
