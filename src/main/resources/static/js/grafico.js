// Gráfico de área del histórico (REQ-005), sin librerías externas: la demo debe funcionar sin internet.
// Lee window.serieHistorico = { fechas: ['AAAA-MM-DD', ...], cierres: ['123.45', ...], volumenes: [123, null, ...] }.
(function () {
    'use strict';

    var serie = window.serieHistorico;
    var lienzo = document.getElementById('grafico-historico');
    var lectura = document.getElementById('grafico-detalle');
    if (!serie || !lienzo || serie.fechas.length === 0) {
        return;
    }

    var total = serie.cierres.length;
    var cierres = serie.cierres.map(Number);
    var volumenes = (serie.volumenes || []).map(function (valor) { return valor === null ? 0 : Number(valor); });
    var volumenMaximo = Math.max.apply(null, volumenes.concat([0]));
    var hayVolumen = volumenMaximo > 0;

    // Opcionales: etiqueta del valor en la lectura y línea de referencia (p. ej. el saldo inicial).
    var etiqueta = serie.etiqueta || 'cierre';
    var referencia = serie.referencia === null || serie.referencia === undefined ? null : Number(serie.referencia);
    var extremos = referencia === null ? cierres : cierres.concat([referencia]);
    var minimo = Math.min.apply(null, extremos);
    var maximo = Math.max.apply(null, extremos);
    // Holgura del 8 % para que la línea no toque los bordes.
    var holgura = (maximo - minimo) * 0.08 || Math.abs(maximo) * 0.02 || 1;
    var techo = maximo + holgura;
    var piso = Math.max(minimo - holgura, 0);
    // Igual que la tabla: 2 decimales desde 1; hasta 8 para precios menores (p. ej. 1 JPY en USD).
    var decimalesMaximos = maximo >= 1 ? 2 : 8;

    var margen = { izquierda: 12, derecha: 88, arriba: 60, abajo: 30 };
    var indiceResaltado = null;

    function color(nombre) {
        return getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
    }

    function formatearPrecio(valor) {
        return valor.toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: decimalesMaximos });
    }

    function formatearFecha(texto) {
        var partes = texto.split('-');
        return partes[2] + '/' + partes[1] + '/' + partes[0];
    }

    function medidas() {
        var ancho = lienzo.clientWidth;
        var alto = lienzo.clientHeight;
        var anchoUtil = ancho - margen.izquierda - margen.derecha;
        var altoUtil = alto - margen.arriba - margen.abajo;
        // Con volumen, el precio usa el 78 % superior y las barras el 16 % inferior.
        return {
            ancho: ancho,
            alto: alto,
            anchoUtil: anchoUtil,
            altoPrecio: altoUtil * (hayVolumen ? 0.78 : 1),
            baseVolumen: margen.arriba + altoUtil,
            altoVolumen: altoUtil * 0.16
        };
    }

    function posicionX(indice, m) {
        return margen.izquierda + (total === 1 ? m.anchoUtil / 2 : indice * m.anchoUtil / (total - 1));
    }

    function posicionY(valor, m) {
        return margen.arriba + (techo - valor) * m.altoPrecio / (techo - piso);
    }

    function rectanguloRedondeado(contexto, x, y, ancho, alto, radio) {
        contexto.beginPath();
        contexto.moveTo(x + radio, y);
        contexto.arcTo(x + ancho, y, x + ancho, y + alto, radio);
        contexto.arcTo(x + ancho, y + alto, x, y + alto, radio);
        contexto.arcTo(x, y + alto, x, y, radio);
        contexto.arcTo(x, y, x + ancho, y, radio);
        contexto.closePath();
    }

    // Etiqueta sobre el eje de precios (derecha), como el último cierre o el punto bajo el cursor.
    function etiquetaDePrecio(contexto, texto, y, fondo, colorTexto, m) {
        contexto.font = '600 11px system-ui, sans-serif';
        var ancho = Math.min(contexto.measureText(texto).width + 12, margen.derecha - 4);
        var x = m.ancho - margen.derecha + 2;
        rectanguloRedondeado(contexto, x, y - 10, ancho, 20, 4);
        contexto.fillStyle = fondo;
        contexto.fill();
        contexto.fillStyle = colorTexto;
        contexto.textAlign = 'left';
        contexto.textBaseline = 'middle';
        contexto.fillText(texto, x + 6, y, ancho - 10);
    }

    function lineaPunteada(contexto, x1, y1, x2, y2, trazo) {
        contexto.save();
        contexto.setLineDash([4, 4]);
        contexto.strokeStyle = trazo;
        contexto.lineWidth = 1;
        contexto.beginPath();
        contexto.moveTo(x1, y1);
        contexto.lineTo(x2, y2);
        contexto.stroke();
        contexto.restore();
    }

    function dibujar() {
        var m = medidas();
        if (m.ancho <= 0 || m.anchoUtil <= 0) {
            return;
        }
        var escala = window.devicePixelRatio || 1;
        lienzo.width = Math.round(m.ancho * escala);
        lienzo.height = Math.round(m.alto * escala);
        var contexto = lienzo.getContext('2d');
        contexto.setTransform(escala, 0, 0, escala, 0, 0);
        contexto.clearRect(0, 0, m.ancho, m.alto);

        var bordeDerecho = m.ancho - margen.derecha;
        var baseArea = margen.arriba + m.altoPrecio;
        var rejilla = color('--grafico-rejilla');
        var textoSuave = color('--texto-tenue');

        // Rejilla horizontal y precios a la derecha.
        contexto.font = '11px system-ui, sans-serif';
        contexto.textAlign = 'left';
        contexto.textBaseline = 'middle';
        for (var guia = 0; guia <= 5; guia++) {
            var valorGuia = piso + (techo - piso) * guia / 5;
            var yGuia = Math.round(posicionY(valorGuia, m)) + 0.5;
            contexto.strokeStyle = rejilla;
            contexto.lineWidth = 1;
            contexto.beginPath();
            contexto.moveTo(margen.izquierda, yGuia);
            contexto.lineTo(bordeDerecho, yGuia);
            contexto.stroke();
            contexto.fillStyle = textoSuave;
            contexto.fillText(formatearPrecio(valorGuia), bordeDerecho + 8, yGuia, margen.derecha - 10);
        }

        // Rejilla vertical y fechas abajo.
        // Una etiqueta de fecha cada ~90 px como mínimo, para que no se encimen en pantallas angostas.
        var divisiones = total > 1 ? Math.max(1, Math.min(6, total - 1, Math.floor(m.anchoUtil / 90))) : 0;
        contexto.textBaseline = 'top';
        for (var division = 0; division <= divisiones; division++) {
            var indice = divisiones === 0 ? 0 : Math.round(division * (total - 1) / divisiones);
            var xGuia = Math.round(posicionX(indice, m)) + 0.5;
            contexto.strokeStyle = rejilla;
            contexto.beginPath();
            contexto.moveTo(xGuia, margen.arriba);
            contexto.lineTo(xGuia, m.baseVolumen);
            contexto.stroke();
            contexto.fillStyle = textoSuave;
            contexto.textAlign = division === 0 ? 'left' : (division === divisiones ? 'right' : 'center');
            contexto.fillText(formatearFecha(serie.fechas[indice]), xGuia, m.baseVolumen + 9);
        }

        // Barras de volumen: verde si el cierre subió frente al día anterior, rojo si bajó.
        if (hayVolumen) {
            var anchoBarra = Math.max(1, Math.min(8, m.anchoUtil / total * 0.7));
            var verde = color('--verde');
            var rojo = color('--rojo');
            contexto.globalAlpha = 0.55;
            for (var i = 0; i < total; i++) {
                if (volumenes[i] <= 0) {
                    continue;
                }
                var altoBarra = Math.max(1, volumenes[i] / volumenMaximo * m.altoVolumen);
                contexto.fillStyle = i > 0 && cierres[i] < cierres[i - 1] ? rojo : verde;
                contexto.fillRect(posicionX(i, m) - anchoBarra / 2, m.baseVolumen - altoBarra, anchoBarra, altoBarra);
            }
            contexto.globalAlpha = 1;
        }

        // Área degradada y línea de cierres.
        contexto.beginPath();
        for (var punto = 0; punto < total; punto++) {
            var x = posicionX(punto, m);
            var y = posicionY(cierres[punto], m);
            if (punto === 0) {
                contexto.moveTo(x, y);
            } else {
                contexto.lineTo(x, y);
            }
        }
        contexto.save();
        contexto.lineTo(posicionX(total - 1, m), baseArea);
        contexto.lineTo(posicionX(0, m), baseArea);
        contexto.closePath();
        var degradado = contexto.createLinearGradient(0, margen.arriba, 0, baseArea);
        degradado.addColorStop(0, color('--grafico-relleno-alto'));
        degradado.addColorStop(1, color('--grafico-relleno-bajo'));
        contexto.fillStyle = degradado;
        contexto.fill();
        contexto.restore();

        contexto.beginPath();
        for (var trazo = 0; trazo < total; trazo++) {
            if (trazo === 0) {
                contexto.moveTo(posicionX(trazo, m), posicionY(cierres[trazo], m));
            } else {
                contexto.lineTo(posicionX(trazo, m), posicionY(cierres[trazo], m));
            }
        }
        contexto.strokeStyle = color('--grafico-linea');
        contexto.lineWidth = 1.6;
        contexto.lineJoin = 'round';
        contexto.stroke();

        if (referencia !== null) {
            var yReferencia = posicionY(referencia, m);
            lineaPunteada(contexto, margen.izquierda, yReferencia, bordeDerecho, yReferencia, 'rgba(149, 156, 166, 0.6)');
            etiquetaDePrecio(contexto, formatearPrecio(referencia), yReferencia, color('--superficie-3'),
                color('--texto-suave'), m);
        }

        // Último cierre: línea punteada y etiqueta verde sobre el eje.
        var yUltimo = posicionY(cierres[total - 1], m);
        lineaPunteada(contexto, margen.izquierda, yUltimo, bordeDerecho, yUltimo, 'rgba(228, 230, 233, 0.35)');
        etiquetaDePrecio(contexto, formatearPrecio(cierres[total - 1]), yUltimo, color('--verde'), '#08130c', m);

        if (indiceResaltado !== null) {
            dibujarCursor(contexto, m, bordeDerecho);
        }
    }

    function dibujarCursor(contexto, m, bordeDerecho) {
        var xPunto = posicionX(indiceResaltado, m);
        var yPunto = posicionY(cierres[indiceResaltado], m);
        var trazo = 'rgba(228, 230, 233, 0.45)';
        lineaPunteada(contexto, xPunto, margen.arriba, xPunto, m.baseVolumen, trazo);
        lineaPunteada(contexto, margen.izquierda, yPunto, bordeDerecho, yPunto, trazo);

        contexto.beginPath();
        contexto.arc(xPunto, yPunto, 4.5, 0, Math.PI * 2);
        contexto.fillStyle = color('--grafico-linea');
        contexto.fill();
        contexto.lineWidth = 2;
        contexto.strokeStyle = color('--fondo');
        contexto.stroke();

        etiquetaDePrecio(contexto, formatearPrecio(cierres[indiceResaltado]), yPunto, color('--superficie-3'),
            color('--texto'), m);

        // Fecha bajo el cursor, sobre el eje de fechas.
        var textoFecha = formatearFecha(serie.fechas[indiceResaltado]);
        contexto.font = '600 11px system-ui, sans-serif';
        var anchoFecha = contexto.measureText(textoFecha).width + 14;
        var xFecha = Math.min(Math.max(xPunto - anchoFecha / 2, margen.izquierda), bordeDerecho - anchoFecha);
        rectanguloRedondeado(contexto, xFecha, m.baseVolumen + 5, anchoFecha, 20, 4);
        contexto.fillStyle = color('--superficie-3');
        contexto.fill();
        contexto.fillStyle = color('--texto');
        contexto.textAlign = 'center';
        contexto.textBaseline = 'middle';
        contexto.fillText(textoFecha, xFecha + anchoFecha / 2, m.baseVolumen + 15);
    }

    function resaltar(evento) {
        var m = medidas();
        var rectangulo = lienzo.getBoundingClientRect();
        var proporcion = (evento.clientX - rectangulo.left - margen.izquierda) / m.anchoUtil;
        indiceResaltado = Math.round(Math.min(Math.max(proporcion, 0), 1) * (total - 1));
        if (lectura) {
            var texto = formatearFecha(serie.fechas[indiceResaltado]) + ': ' + etiqueta + ' '
                + formatearPrecio(cierres[indiceResaltado]) + ' USD';
            if (volumenes[indiceResaltado] > 0) {
                texto += ' · volumen ' + volumenes[indiceResaltado].toLocaleString('es-CO');
            }
            lectura.textContent = texto;
        }
        dibujar();
    }

    lienzo.addEventListener('pointermove', resaltar);
    lienzo.addEventListener('pointerleave', function () {
        indiceResaltado = null;
        dibujar();
    });
    if (window.ResizeObserver) {
        new ResizeObserver(dibujar).observe(lienzo);
    } else {
        window.addEventListener('resize', dibujar);
    }
    dibujar();
})();
