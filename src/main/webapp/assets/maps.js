window.EnjoyTripMaps = (() => {
  function showStatus(container, message) {
    const status = container.closest('.map-shell')?.querySelector('.map-status');
    if (!status) return;
    status.textContent = message;
    status.hidden = false;
    status.classList.add('map-status-error');
  }

  function createMap(container, center, zoom = 13) {
    const map = L.map(container).setView(center, zoom);
    let tileErrors = 0;
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).on('tileerror', () => {
      if (++tileErrors >= 3) showStatus(container, '지도 배경을 불러오지 못했습니다. 인터넷 연결을 확인해 주세요.');
    }).addTo(map);
    return map;
  }

  function ready(id, render) {
    const container = document.getElementById(id);
    if (!container) return;
    if (!window.L) {
      showStatus(container, '지도 라이브러리를 불러오지 못했습니다. 인터넷 연결을 확인하고 새로고침해 주세요.');
      return;
    }
    try {
      const map = render(container, window.L);
      const status = container.closest('.map-shell')?.querySelector('.map-status');
      if (status) status.hidden = true;
      window.setTimeout(() => map?.invalidateSize(), 0);
    } catch (error) {
      console.error('EnjoyTrip map initialization failed', error);
      showStatus(container, '지도 위치를 표시하지 못했습니다.');
    }
  }

  return { ready, createMap };
})();
