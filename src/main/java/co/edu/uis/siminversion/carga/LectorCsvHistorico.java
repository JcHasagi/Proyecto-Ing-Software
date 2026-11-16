package co.edu.uis.siminversion.carga;

import co.edu.uis.siminversion.precio.PrecioDiario;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lee un CSV de histórico diario de un instrumento (REQ-018).
 *
 * <p>Formato: primera línea con encabezado; columnas fecha, apertura, maximo, minimo, cierre y,
 * opcional, volumen. También acepta los nombres en inglés de Stooq y yfinance (Date, Open, High,
 * Low, Close, Volume) e ignora columnas desconocidas como "Adj Close". Separador coma o punto y coma,
 * punto decimal sin separador de miles, fechas AAAA-MM-DD.
 */
public class LectorCsvHistorico {

    public static final int ESCALA_PRECIO = 8;
    static final int MAXIMO_ERRORES_REPORTADOS = 50;

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");

    enum Columna {
        FECHA("fecha", true),
        APERTURA("apertura", true),
        MAXIMO("máximo", true),
        MINIMO("mínimo", true),
        CIERRE("cierre", true),
        VOLUMEN("volumen", false);

        final String nombre;
        final boolean obligatoria;

        Columna(String nombre, boolean obligatoria) {
            this.nombre = nombre;
            this.obligatoria = obligatoria;
        }
    }

    private static final Map<String, Columna> ALIAS = Map.ofEntries(
            Map.entry("fecha", Columna.FECHA), Map.entry("date", Columna.FECHA),
            Map.entry("apertura", Columna.APERTURA), Map.entry("open", Columna.APERTURA),
            Map.entry("maximo", Columna.MAXIMO), Map.entry("high", Columna.MAXIMO),
            Map.entry("minimo", Columna.MINIMO), Map.entry("low", Columna.MINIMO),
            Map.entry("cierre", Columna.CIERRE), Map.entry("close", Columna.CIERRE),
            Map.entry("volumen", Columna.VOLUMEN), Map.entry("volume", Columna.VOLUMEN));

    public ResultadoLectura leer(Reader fuente) throws IOException {
        BufferedReader lector = new BufferedReader(fuente);
        List<String> errores = new ArrayList<>();
        List<PrecioDiario> filas = new ArrayList<>();
        Map<LocalDate, Integer> lineaPorFecha = new HashMap<>();
        Map<Columna, Integer> posiciones = null;
        String separador = ",";
        int erroresDeFilas = 0;
        int numeroLinea = 0;
        String linea;

        while ((linea = lector.readLine()) != null) {
            numeroLinea++;
            if (numeroLinea == 1 && linea.startsWith("﻿")) {
                linea = linea.substring(1);
            }
            if (linea.isBlank()) {
                continue;
            }
            if (posiciones == null) {
                separador = linea.contains(";") && !linea.contains(",") ? ";" : ",";
                posiciones = leerEncabezado(dividir(linea, separador), errores);
                if (posiciones == null) {
                    return new ResultadoLectura(List.of(), List.copyOf(errores));
                }
                continue;
            }
            try {
                PrecioDiario fila = convertirFila(dividir(linea, separador), posiciones);
                Integer lineaAnterior = lineaPorFecha.putIfAbsent(fila.fecha(), numeroLinea);
                if (lineaAnterior != null) {
                    throw new FilaInvalidaException("la fecha " + fila.fecha()
                            + " está repetida (ya aparece en la línea " + lineaAnterior + ")");
                }
                filas.add(fila);
            } catch (FilaInvalidaException filaInvalida) {
                erroresDeFilas++;
                if (errores.size() < MAXIMO_ERRORES_REPORTADOS) {
                    errores.add("Línea " + numeroLinea + ": " + filaInvalida.getMessage() + ".");
                }
            }
        }

        if (posiciones == null) {
            errores.add("El archivo está vacío.");
        } else if (erroresDeFilas > errores.size()) {
            errores.add("... y " + (erroresDeFilas - errores.size()) + " errores más.");
        } else if (filas.isEmpty() && errores.isEmpty()) {
            errores.add("El archivo no contiene filas de datos.");
        }
        filas.sort(Comparator.comparing(PrecioDiario::fecha));
        return new ResultadoLectura(List.copyOf(filas), List.copyOf(errores));
    }

    private static Map<Columna, Integer> leerEncabezado(String[] nombres, List<String> errores) {
        Map<Columna, Integer> posiciones = new EnumMap<>(Columna.class);
        for (int indice = 0; indice < nombres.length; indice++) {
            Columna columna = ALIAS.get(normalizarNombre(nombres[indice]));
            if (columna != null) {
                posiciones.putIfAbsent(columna, indice);
            }
        }
        List<String> faltantes = Stream.of(Columna.values())
                .filter(columna -> columna.obligatoria && !posiciones.containsKey(columna))
                .map(columna -> columna.nombre)
                .toList();
        if (!faltantes.isEmpty()) {
            errores.add("Línea 1: al encabezado le faltan las columnas " + String.join(", ", faltantes)
                    + ". Se esperan fecha, apertura, maximo, minimo, cierre y, opcional, volumen"
                    + " (o en inglés: Date, Open, High, Low, Close, Volume).");
            return null;
        }
        return posiciones;
    }

    private static PrecioDiario convertirFila(String[] celdas, Map<Columna, Integer> posiciones) {
        int columnasNecesarias = posiciones.values().stream().max(Integer::compare).orElse(0) + 1;
        if (celdas.length < columnasNecesarias) {
            throw new FilaInvalidaException("tiene " + celdas.length + " columnas y se esperaban "
                    + columnasNecesarias);
        }
        LocalDate fecha = leerFecha(celdas[posiciones.get(Columna.FECHA)]);
        BigDecimal apertura = leerPrecio(celdas, posiciones, Columna.APERTURA);
        BigDecimal maximo = leerPrecio(celdas, posiciones, Columna.MAXIMO);
        BigDecimal minimo = leerPrecio(celdas, posiciones, Columna.MINIMO);
        BigDecimal cierre = leerPrecio(celdas, posiciones, Columna.CIERRE);
        Long volumen = posiciones.containsKey(Columna.VOLUMEN)
                ? leerVolumen(celdas[posiciones.get(Columna.VOLUMEN)])
                : null;

        if (minimo.compareTo(maximo) > 0) {
            throw new FilaInvalidaException("el mínimo (" + minimo.toPlainString() + ") es mayor que el máximo ("
                    + maximo.toPlainString() + ")");
        }
        if (fueraDeRango(apertura, minimo, maximo) || fueraDeRango(cierre, minimo, maximo)) {
            throw new FilaInvalidaException("la apertura y el cierre deben estar entre el mínimo y el máximo");
        }
        return new PrecioDiario(fecha, apertura, maximo, minimo, cierre, volumen);
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException fechaMalFormada) {
            throw new FilaInvalidaException("fecha inválida '" + texto + "' (formato esperado AAAA-MM-DD)");
        }
    }

    private static BigDecimal leerPrecio(String[] celdas, Map<Columna, Integer> posiciones, Columna columna) {
        String texto = celdas[posiciones.get(columna)];
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto).setScale(ESCALA_PRECIO, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException numeroMalFormado) {
            throw new FilaInvalidaException("valor de " + columna.nombre + " inválido '" + texto
                    + "' (usa punto decimal y sin separador de miles)");
        }
        if (valor.signum() <= 0) {
            throw new FilaInvalidaException("el valor de " + columna.nombre + " debe ser mayor que cero");
        }
        return valor;
    }

    private static Long leerVolumen(String texto) {
        if (texto.isEmpty()) {
            return null;
        }
        try {
            long volumen = new BigDecimal(texto).longValueExact();
            if (volumen < 0) {
                throw new FilaInvalidaException("el volumen no puede ser negativo");
            }
            return volumen;
        } catch (NumberFormatException | ArithmeticException volumenInvalido) {
            throw new FilaInvalidaException("volumen inválido '" + texto + "' (debe ser un entero)");
        }
    }

    private static boolean fueraDeRango(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        return valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0;
    }

    private static String[] dividir(String linea, String separador) {
        return Stream.of(linea.split(Pattern.quote(separador), -1))
                .map(celda -> celda.trim().replace("\"", ""))
                .toArray(String[]::new);
    }

    private static String normalizarNombre(String nombre) {
        String sinTildes = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(nombre, Normalizer.Form.NFD))
                .replaceAll("");
        return sinTildes.trim().toLowerCase(Locale.ROOT);
    }

    private static final class FilaInvalidaException extends RuntimeException {
        FilaInvalidaException(String mensaje) {
            super(mensaje);
        }
    }
}
