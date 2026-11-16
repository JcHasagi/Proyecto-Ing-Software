package co.edu.uis.siminversion.instrumento;

public enum TipoInstrumento {

    // diasMaximosEntreCierres: separación normal entre dos cierres seguidos. Si es mayor, la carga
    // lo advierte como hueco. Acciones, ETFs y divisas no cotizan fines de semana ni festivos;
    // las criptomonedas cotizan todos los días.
    ACCION("Acción", 4),
    ETF("ETF", 4),
    DIVISA("Divisa", 4),
    CRIPTOMONEDA("Criptomoneda", 1);

    private final String etiqueta;
    private final int diasMaximosEntreCierres;

    TipoInstrumento(String etiqueta, int diasMaximosEntreCierres) {
        this.etiqueta = etiqueta;
        this.diasMaximosEntreCierres = diasMaximosEntreCierres;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getDiasMaximosEntreCierres() {
        return diasMaximosEntreCierres;
    }
}
