// Formats input while the user types. The server still validates every value.
(function () {
    // "l6j1h6" -> "L6J 1H6"
    function formatPostal(value) {
        const chars = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
        return chars.length > 3 ? chars.slice(0, 3) + ' ' + chars.slice(3) : chars;
    }

    // "9055551234" -> "(905) 555-1234"
    function formatPhone(value) {
        const digits = value.replace(/\D/g, '').slice(0, 10);
        if (digits.length === 0) return '';
        if (digits.length <= 3) return '(' + digits;
        if (digits.length <= 6) return '(' + digits.slice(0, 3) + ') ' + digits.slice(3);
        return '(' + digits.slice(0, 3) + ') ' + digits.slice(3, 6) + '-' + digits.slice(6);
    }

    const formatters = { postal: formatPostal, phone: formatPhone };

    document.querySelectorAll('[data-format]').forEach(function (input) {
        const format = formatters[input.dataset.format];
        if (!format) return;
        const apply = function () { input.value = format(input.value); };
        input.addEventListener('input', apply);
        apply();
    });
})();
