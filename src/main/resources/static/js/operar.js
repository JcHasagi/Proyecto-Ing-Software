// Botones −10, −1, +1 y +10 del panel de operar: ajustan la cantidad sin bajar de 1 (solo enteros, RN-07).
(function () {
    'use strict';

    var campo = document.getElementById('cantidad');
    if (!campo) {
        return;
    }
    document.querySelectorAll('.cantidad .paso').forEach(function (boton) {
        boton.addEventListener('click', function () {
            var actual = parseInt(campo.value, 10);
            var siguiente = (isNaN(actual) ? 1 : actual) + parseInt(boton.dataset.paso, 10);
            campo.value = Math.max(1, siguiente);
        });
    });
})();
