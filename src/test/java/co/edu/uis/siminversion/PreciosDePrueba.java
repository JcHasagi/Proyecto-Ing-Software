package co.edu.uis.siminversion;

import co.edu.uis.siminversion.instrumento.InstrumentoRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserta cierres inventados para las pruebas (en el perfil de pruebas la BD empieza sin precios). */
public final class PreciosDePrueba {

    private final JdbcTemplate jdbcTemplate;
    private final InstrumentoRepositorio instrumentoRepositorio;

    public PreciosDePrueba(JdbcTemplate jdbcTemplate, InstrumentoRepositorio instrumentoRepositorio) {
        this.jdbcTemplate = jdbcTemplate;
        this.instrumentoRepositorio = instrumentoRepositorio;
    }

    public void insertar(String simbolo, String fecha, String cierre) {
        insertar(simbolo, LocalDate.parse(fecha), new BigDecimal(cierre));
    }

    public void insertar(String simbolo, LocalDate fecha, BigDecimal cierre) {
        long instrumentoId = instrumentoRepositorio.findBySimbolo(simbolo).orElseThrow().getId();
        jdbcTemplate.update("""
                INSERT INTO precio_historico (instrumento_id, fecha, apertura, maximo, minimo, cierre, volumen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, instrumentoId, fecha, cierre, cierre, cierre, cierre, 1000L);
    }
}
