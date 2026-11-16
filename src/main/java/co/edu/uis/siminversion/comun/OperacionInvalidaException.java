package co.edu.uis.siminversion.comun;

/** Una regla de negocio impide la operación. El mensaje está escrito para mostrárselo al usuario. */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
