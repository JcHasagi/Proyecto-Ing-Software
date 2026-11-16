"""Descarga el histórico diario de los instrumentos del catálogo inicial desde Yahoo Finance y lo guarda
como CSV en el formato que acepta la carga masiva (REQ-018): fecha,apertura,maximo,minimo,cierre,volumen.

Uso (desde la raíz del proyecto):
    python scripts/descargar_historicos.py [carpeta_destino]

Solo usa la biblioteca estándar. Precios por unidad en USD, ajustados por splits y no por dividendos
(el simulador no modela dividendos). Las divisas se guardan sin volumen. Se excluye el día de la descarga,
que puede estar incompleto.
"""
import csv
import json
import sys
import time
import urllib.error
import urllib.request
from datetime import date, datetime, timezone
from decimal import ROUND_HALF_EVEN, Decimal
from pathlib import Path

INICIO = date(2005, 1, 1)

# Símbolo del catálogo -> (símbolo en Yahoo Finance, tipo de instrumento).
INSTRUMENTOS = {
    "AAPL": ("AAPL", "ACCION"),
    "MSFT": ("MSFT", "ACCION"),
    "AMZN": ("AMZN", "ACCION"),
    "GOOGL": ("GOOGL", "ACCION"),
    "JPM": ("JPM", "ACCION"),
    "BAC": ("BAC", "ACCION"),
    "KO": ("KO", "ACCION"),
    "PG": ("PG", "ACCION"),
    "JNJ": ("JNJ", "ACCION"),
    "XOM": ("XOM", "ACCION"),
    "WMT": ("WMT", "ACCION"),
    "DIS": ("DIS", "ACCION"),
    "SPY": ("SPY", "ETF"),
    "QQQ": ("QQQ", "ETF"),
    "GLD": ("GLD", "ETF"),
    "TLT": ("TLT", "ETF"),
    "EURUSD": ("EURUSD=X", "DIVISA"),
    "GBPUSD": ("GBPUSD=X", "DIVISA"),
    "JPYUSD": ("JPYUSD=X", "DIVISA"),
    "COPUSD": ("COPUSD=X", "DIVISA"),
    "BTCUSD": ("BTC-USD", "CRIPTOMONEDA"),
    "ETHUSD": ("ETH-USD", "CRIPTOMONEDA"),
    "LTCUSD": ("LTC-USD", "CRIPTOMONEDA"),
}

URL = ("https://query1.finance.yahoo.com/v8/finance/chart/{simbolo}"
       "?period1={desde}&period2={hasta}&interval=1d&events=history")
# Encabezado genérico: el servicio rechaza el agente por defecto de urllib.
ENCABEZADOS = {"User-Agent": "Mozilla/5.0"}

# Yahoo entrega los precios como flotantes con ruido de precisión simple (p. ej. 140.95199584960938).
# Se redondean a 7 cifras significativas, con un máximo de 8 decimales (la escala de precio_historico).
CIFRAS_SIGNIFICATIVAS = 7
ESCALA_MAXIMA = 8
# Sumar 6 horas al instante de apertura del día deja la fecha local correcta para Nueva York
# (apertura 13:30-14:30 UTC), Londres (00:00 local = 23:00 o 00:00 UTC) y UTC (cripto),
# sin depender de la base de zonas horarias, que Python en Windows no siempre trae.
DESPLAZAMIENTO_SEGUNDOS = 6 * 3600


def redondear(valor):
    numero = Decimal(repr(valor))
    if numero == 0:
        return numero
    decimales = min(ESCALA_MAXIMA, max(0, CIFRAS_SIGNIFICATIVAS - 1 - numero.adjusted()))
    return numero.quantize(Decimal(1).scaleb(-decimales), rounding=ROUND_HALF_EVEN)


def descargar_json(simbolo_yahoo, desde, hasta):
    url = URL.format(simbolo=urllib.request.quote(simbolo_yahoo), desde=desde, hasta=hasta)
    for intento in range(1, 4):
        try:
            peticion = urllib.request.Request(url, headers=ENCABEZADOS)
            with urllib.request.urlopen(peticion, timeout=60) as respuesta:
                return json.load(respuesta)
        except (urllib.error.URLError, TimeoutError) as error:
            if intento == 3:
                raise
            print(f"  reintento {intento} para {simbolo_yahoo}: {error}", file=sys.stderr)
            time.sleep(5 * intento)
    raise RuntimeError("inalcanzable")


def convertir(datos, tipo, hoy):
    """Filas válidas por fecha, más el conteo de filas descartadas, corregidas y repetidas."""
    resultado = datos["chart"]["result"][0]
    marcas = resultado.get("timestamp") or []
    cotizacion = resultado["indicators"]["quote"][0]
    filas = {}
    descartadas = corregidas = repetidas = 0
    for indice, marca in enumerate(marcas):
        fecha = datetime.fromtimestamp(marca + DESPLAZAMIENTO_SEGUNDOS, tz=timezone.utc).date()
        if fecha >= hoy:
            continue
        valores = [cotizacion[campo][indice] for campo in ("open", "high", "low", "close")]
        if any(valor is None or valor <= 0 for valor in valores):
            descartadas += 1
            continue
        apertura, maximo, minimo, cierre = (redondear(valor) for valor in valores)
        # La carga exige minimo <= apertura, cierre <= maximo; algunos días de Yahoo no lo cumplen.
        maximo_coherente = max(maximo, apertura, cierre, minimo)
        minimo_coherente = min(minimo, apertura, cierre, maximo)
        if (maximo_coherente, minimo_coherente) != (maximo, minimo):
            corregidas += 1
        volumen = cotizacion["volume"][indice]
        texto_volumen = "" if tipo == "DIVISA" or volumen is None else str(int(volumen))
        if fecha in filas:
            repetidas += 1
        filas[fecha] = [fecha.isoformat(), format(apertura, "f"), format(maximo_coherente, "f"),
                        format(minimo_coherente, "f"), format(cierre, "f"), texto_volumen]
    return filas, descartadas, corregidas, repetidas


def main():
    destino = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent / "datos" / "historico"
    destino.mkdir(parents=True, exist_ok=True)
    hoy = datetime.now(timezone.utc).date()
    desde = int(datetime(INICIO.year, INICIO.month, INICIO.day, tzinfo=timezone.utc).timestamp())
    hasta = int(datetime(hoy.year, hoy.month, hoy.day, tzinfo=timezone.utc).timestamp())

    print(f"{'símbolo':8} {'filas':>6}  {'desde':10}  {'hasta':10}  descart. corrig. repet. fin_semana")
    for simbolo, (simbolo_yahoo, tipo) in INSTRUMENTOS.items():
        datos = descargar_json(simbolo_yahoo, desde, hasta)
        if datos["chart"].get("error"):
            raise RuntimeError(f"{simbolo_yahoo}: {datos['chart']['error']}")
        filas, descartadas, corregidas, repetidas = convertir(datos, tipo, hoy)
        fechas = sorted(filas)
        fines_de_semana = sum(1 for fecha in fechas if fecha.weekday() >= 5)
        with open(destino / f"{simbolo}.csv", "w", newline="", encoding="utf-8") as archivo:
            escritor = csv.writer(archivo)
            escritor.writerow(["fecha", "apertura", "maximo", "minimo", "cierre", "volumen"])
            escritor.writerows(filas[fecha] for fecha in fechas)
        print(f"{simbolo:8} {len(fechas):6}  {fechas[0]}  {fechas[-1]}  {descartadas:8} {corregidas:7} "
              f"{repetidas:6} {fines_de_semana:10}")
        time.sleep(1)


if __name__ == "__main__":
    main()
