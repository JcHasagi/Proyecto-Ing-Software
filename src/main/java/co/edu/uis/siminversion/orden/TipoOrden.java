package co.edu.uis.siminversion.orden;

public enum TipoOrden {
    COMPRA("Compra"),
    VENTA("Venta");

    private final String etiqueta;

    TipoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
