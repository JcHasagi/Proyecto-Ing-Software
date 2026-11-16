package co.edu.uis.siminversion.instrumento;

import java.time.LocalDate;
import java.time.Period;

/** Ventanas de tiempo del gráfico de REQ-005, contadas hacia atrás desde la fecha límite, de la más corta a la más amplia. */
public enum RangoHistorico {

    UN_MES("1 mes", "1M", Period.ofMonths(1)),
    SEIS_MESES("6 meses", "6M", Period.ofMonths(6)),
    UN_ANIO("1 año", "1A", Period.ofYears(1)),
    CINCO_ANIOS("5 años", "5A", Period.ofYears(5)),
    TODO("Todo", "Todo", null);

    private static final LocalDate INICIO_DEL_HISTORICO = LocalDate.of(1900, 1, 1);

    private final String etiqueta;
    private final String etiquetaCorta;
    private final Period duracion;

    RangoHistorico(String etiqueta, String etiquetaCorta, Period duracion) {
        this.etiqueta = etiqueta;
        this.etiquetaCorta = etiquetaCorta;
        this.duracion = duracion;
    }

    public LocalDate desde(LocalDate hasta) {
        return duracion == null ? INICIO_DEL_HISTORICO : hasta.minus(duracion);
    }

    /** Rango siguiente hacia "Todo" (botón de alejar); el más amplio se devuelve a sí mismo. */
    public RangoHistorico masAmplio() {
        RangoHistorico[] rangos = values();
        return rangos[Math.min(ordinal() + 1, rangos.length - 1)];
    }

    /** Rango siguiente hacia "1 mes" (botón de acercar); el más corto se devuelve a sí mismo. */
    public RangoHistorico masCorto() {
        return values()[Math.max(ordinal() - 1, 0)];
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getEtiquetaCorta() {
        return etiquetaCorta;
    }
}
