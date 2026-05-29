(function () {
    const toggle = document.querySelector('[data-nav-toggle]');
    const panel = document.getElementById('main-nav');
    if (!toggle || !panel) return;

    function setOpen(open) {
        document.body.classList.toggle('nav-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
    }

    toggle.addEventListener('click', function () {
        setOpen(!document.body.classList.contains('nav-open'));
    });

    panel.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () {
            setOpen(false);
        });
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') setOpen(false);
    });

    window.matchMedia('(min-width: 992px)').addEventListener('change', function (e) {
        if (e.matches) setOpen(false);
    });
})();
