package co.edu.uis.siminversion.portafolio;

/**
 * EN_CURSO: admite órdenes y avance del reloj. FINALIZADO: el reloj llegó al último dato del histórico.
 * ARCHIVADO: quedó atrás tras un reinicio (REQ-015). Los dos últimos solo permiten consultas.
 */
public enum EstadoPortafolio {
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado"),
    ARCHIVADO("Archivado");

    private final String etiqueta;

    EstadoPortafolio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
