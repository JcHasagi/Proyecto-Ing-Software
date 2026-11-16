package co.edu.uis.siminversion.educacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Artículo educativo (REQ-016). El contenido es texto plano; los párrafos se separan con una línea en blanco. */
@Entity
@Table(name = "articulo")
public class Articulo {

    private static final Pattern SEPARADOR_DE_PARRAFOS = Pattern.compile("\\R\\s*\\R");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String resumen;

    @Column(nullable = false, length = 8000)
    private String contenido;

    @Column(name = "orden_visualizacion", nullable = false)
    private int ordenVisualizacion;

    protected Articulo() {
        // Requerido por JPA.
    }

    public boolean tieneContenido() {
        return contenido != null && !contenido.isBlank();
    }

    /** Se muestran como texto (escapado), nunca como HTML. */
    public List<String> parrafos() {
        if (!tieneContenido()) {
            return List.of();
        }
        return Stream.of(SEPARADOR_DE_PARRAFOS.split(contenido.strip()))
                .map(parrafo -> parrafo.strip().replaceAll("\\s*\\R\\s*", " "))
                .filter(parrafo -> !parrafo.isEmpty())
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getResumen() {
        return resumen;
    }

    public String getContenido() {
        return contenido;
    }

    public int getOrdenVisualizacion() {
        return ordenVisualizacion;
    }
}
