package co.edu.uis.siminversion.rendimiento;

import co.edu.uis.siminversion.comun.ReglasDeOperacion;
import co.edu.uis.siminversion.instrumento.Instrumento;
import co.edu.uis.siminversion.portafolio.PortafolioServicio;
import co.edu.uis.siminversion.portafolio.PosicionValorada;
import co.edu.uis.siminversion.portafolio.ResumenConLibro;
import co.edu.uis.siminversion.portafolio.ResumenPortafolio;
import co.edu.uis.siminversion.reloj.RegistroDeInstantaneas;
import co.edu.uis.siminversion.transaccion.EstadoDelLibro;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-011: efectivo y posiciones desde el libro, P&L realizado y no realizado, y evolución diaria del valor. */
@Service
public class RendimientoServicio {

    private final PortafolioServicio portafolioServicio;
    private final RegistroDeInstantaneas registroDeInstantaneas;

    public RendimientoServicio(PortafolioServicio portafolioServicio, RegistroDeInstantaneas registroDeInstantaneas) {
        this.portafolioServicio = portafolioServicio;
        this.registroDeInstantaneas = registroDeInstantaneas;
    }

    @Transactional(readOnly = true)
    public Rendimiento rendimiento(String correo, long portafolioId) {
        // Un solo recorrido del libro sirve para el resumen y para los resultados.
        ResumenConLibro resumenConLibro = portafolioServicio.resumenConLibro(correo, portafolioId);
        ResumenPortafolio resumen = resumenConLibro.resumen();
        EstadoDelLibro libro = resumenConLibro.libro();

        Map<String, PosicionValorada> abiertas = resumen.posiciones().stream()
                .collect(Collectors.toMap(fila -> fila.posicion().instrumento().getSimbolo(), Function.identity()));
        Map<String, BigDecimal> realizadas = libro.gananciaRealizadaPorSimbolo();
        BigDecimal cero = ReglasDeOperacion.redondearDinero(BigDecimal.ZERO);

        List<ResultadoPorInstrumento> porInstrumento = new ArrayList<>();
        for (Map.Entry<String, Instrumento> operado : libro.instrumentosOperados().entrySet()) {
            PosicionValorada abierta = abiertas.get(operado.getKey());
            porInstrumento.add(new ResultadoPorInstrumento(operado.getValue(), libro.cantidadDe(operado.getKey()),
                    realizadas.getOrDefault(operado.getKey(), cero),
                    abierta == null ? cero : abierta.gananciaNoRealizada()));
        }
        BigDecimal noRealizada = resumen.posiciones().stream()
                .map(PosicionValorada::gananciaNoRealizada)
                .reduce(cero, BigDecimal::add);

        return new Rendimiento(resumen, libro.gananciaRealizada(), noRealizada,
                ReglasDeOperacion.redondearDinero(libro.comisionesPagadas()),
                ReglasDeOperacion.redondearDinero(libro.interesesGanados()), porInstrumento,
                registroDeInstantaneas.dePortafolio(portafolioId));
    }
}
