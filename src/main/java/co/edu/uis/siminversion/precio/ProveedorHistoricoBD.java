package co.edu.uis.siminversion.precio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Precios desde la tabla precio_historico, precargada sin conexión a internet. */
@Service
@Transactional(readOnly = true)
public class ProveedorHistoricoBD implements ProveedorDePrecios {

    private final PrecioHistoricoRepositorio repositorio;

    public ProveedorHistoricoBD(PrecioHistoricoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<BigDecimal> precioCierre(long instrumentoId, LocalDate fecha) {
        return repositorio.findById(new PrecioHistoricoId(instrumentoId, fecha)).map(PrecioHistorico::getCierre);
    }

    @Override
    public Optional<LocalDate> siguienteDiaBursatil(long instrumentoId, LocalDate fecha) {
        return repositorio.primeraFechaPosterior(instrumentoId, fecha);
    }

    @Override
    public Optional<PrecioDiario> ultimoCierreHasta(long instrumentoId, LocalDate fecha) {
        return repositorio.ultimoHasta(instrumentoId, fecha).map(PrecioHistorico::comoPrecioDiario);
    }

    @Override
    public List<PrecioDiario> historico(long instrumentoId, LocalDate desde, LocalDate hasta) {
        return repositorio.buscarEntre(instrumentoId, desde, hasta).stream()
                .map(PrecioHistorico::comoPrecioDiario)
                .toList();
    }

    @Override
    public List<LocalDate> diasBursatilesDelMercado(LocalDate despuesDe, LocalDate hasta) {
        return repositorio.fechasDelMercadoEntre(despuesDe, hasta);
    }

    @Override
    public Optional<LocalDate> primerDiaBursatilDelMercadoDesde(LocalDate fecha) {
        return repositorio.primeraFechaDelMercadoDesde(fecha);
    }

    @Override
    public Optional<LocalDate> primeraFechaDelHistorico() {
        return repositorio.primeraFecha();
    }

    @Override
    public Optional<LocalDate> ultimaFechaDelHistorico() {
        return repositorio.ultimaFecha();
    }

    @Override
    public long instrumentosActivosConPrecios(LocalDate desde, LocalDate hasta) {
        return repositorio.contarInstrumentosActivosConPrecios(desde, hasta);
    }

    @Override
    public List<CoberturaHistorico> coberturaPorInstrumento() {
        return repositorio.coberturaPorInstrumento();
    }
}
