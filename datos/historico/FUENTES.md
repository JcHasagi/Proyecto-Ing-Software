# Fuente de los históricos

Un CSV por instrumento del catálogo inicial, en el formato de la carga masiva (REQ-018):
`fecha,apertura,maximo,minimo,cierre,volumen`. La app los carga al arrancar para cada instrumento que aún
no tiene precios (`siminversion.historico.directorio`, por defecto esta carpeta).

## Origen

- **Fuente:** Yahoo Finance (endpoint `v8/finance/chart`, intervalo diario).
- **Descarga:** 2026-11-15, con `scripts/descargar_historicos.py` (solo biblioteca estándar de Python).
- **Stooq**, la fuente prevista inicialmente, no sirvió: exige una verificación de navegador con JavaScript
  antes de entregar los CSV.

| Instrumentos | Símbolo en Yahoo | Desde | Hasta | Días |
|---|---|---|---|---|
| 12 acciones y 4 ETFs | el mismo (`AAPL`, `SPY`, ...) | 2005-01-03 | 2026-11-13 | 5502 cada uno |
| `EURUSD`, `GBPUSD`, `JPYUSD`, `COPUSD` | `EURUSD=X`, `GBPUSD=X`, `JPYUSD=X`, `COPUSD=X` | 2005-01-03 | 2026-11-13 | 5672 a 5684 |
| `BTCUSD`, `LTCUSD` | `BTC-USD`, `LTC-USD` | 2014-09-17 | 2026-11-14 | 4442 |
| `ETHUSD` | `ETH-USD` | 2017-11-09 | 2026-11-14 | 3293 |

## Tratamiento

- Precios por unidad en USD. Las divisas son los USD que vale 1 unidad de la divisa (Yahoo las cotiza así directamente).
- Acciones y ETFs **ajustados por splits, no por dividendos**: el simulador no modela ninguno de los dos, y así no hay
  saltos artificiales en la serie. Por eso AAPL en 2005 vale alrededor de 1 USD.
- Yahoo entrega flotantes con ruido de precisión simple; se redondean a 7 cifras significativas, con un máximo de
  8 decimales y redondeo `HALF_EVEN`.
- Las divisas se guardan sin volumen (Yahoo reporta 0; el mercado de divisas no tiene volumen centralizado).
- Se descartaron días con valores vacíos: entre 21 y 33 por divisa y 1 por criptomoneda.
- En las divisas, entre 104 y 263 días traían el máximo o el mínimo por fuera de la apertura o el cierre. Se ajustaron
  para que el rango del día incluya ambos, como exige la carga.
- Se excluyó el día de la descarga, que podía estar incompleto.

Cada serie de acciones trae dos huecos reales que la carga reporta como advertencia: el cierre del mercado por el
funeral del presidente Ford (enero de 2007) y el huracán Sandy (octubre de 2012).

## Uso

Los datos de Yahoo Finance están pensados para uso personal y educativo. Antes de publicar esta carpeta en un
repositorio público, revisen los términos de Yahoo o reemplacen la fuente.

## Actualizar

1. `python scripts/descargar_historicos.py` reescribe los CSV con el rango hasta el día anterior.
2. Con la base vacía, la app los carga sola al arrancar. Si ya tiene precios, súbanlos desde `/admin/carga`:
   las fechas existentes se omiten y solo se agregan las nuevas.
