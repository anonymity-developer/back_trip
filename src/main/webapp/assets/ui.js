(() => {
  const header = document.querySelector('.site-header');
  const toggle = header?.querySelector('.nav-toggle');
  if (header && toggle) {
    const setOpen = open => {
      header.classList.toggle('menu-open', open);
      toggle.setAttribute('aria-expanded', String(open));
      toggle.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    };
    toggle.addEventListener('click', () => setOpen(!header.classList.contains('menu-open')));
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') setOpen(false);
    });
    const current = location.pathname.replace(/\/$/, '');
    header.querySelectorAll('nav a, .account > a').forEach(link => {
      const target = new URL(link.href).pathname.replace(/\/$/, '');
      if (current === target || current.startsWith(target + '/')) link.setAttribute('aria-current', 'page');
    });
  }

  document.querySelectorAll('.card-media img[data-fallback]').forEach(image => {
    const showFallback = () => { image.hidden = true; };
    const showImage = () => { image.classList.add('is-loaded'); };
    image.addEventListener('error', showFallback, { once: true });
    image.addEventListener('load', showImage, { once: true });
    if (image.complete) image.naturalWidth === 0 ? showFallback() : showImage();
  });
})();
