package co.edu.uis.siminversion.orden;

/** PENDIENTE → EJECUTADA | CANCELADA | RECHAZADA. Los tres últimos son definitivos. */
public enum EstadoOrden {
    PENDIENTE("Pendiente"),
    EJECUTADA("Ejecutada"),
    CANCELADA("Cancelada"),
    RECHAZADA("Rechazada");

    private final String etiqueta;

    EstadoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
