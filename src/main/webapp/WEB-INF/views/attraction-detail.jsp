<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<p><a href="${pageContext.request.contextPath}/attractions">← 여행지 목록</a></p>
<section class="panel">
  <span class="eyebrow">Explore this place</span>
  <h1><c:out value="${attraction.title}"/></h1>
  <p class="muted"><c:out value="${attraction.address}"/></p>
  <c:if test="${not empty attraction.imageUrl}"><img class="detail-image" src="<c:out value='${attraction.imageUrl}'/>" alt="<c:out value='${attraction.title}'/>"></c:if>
  <c:if test="${not empty attraction.description}"><p class="post-body"><c:out value="${attraction.description}"/></p></c:if>
  <p class="muted">출처: 한국관광공사 · 갱신: <c:out value="${attraction.sourceUpdatedAt}"/></p>
  <h2>지도에서 보기</h2>
  <c:choose>
    <c:when test="${not empty attraction.latitude and not empty attraction.longitude}">
      <div class="map-shell"><div id="map" class="map" aria-label="관광지 위치 지도"></div><p class="map-status" role="status">지도를 불러오는 중입니다.</p></div>
    </c:when>
    <c:otherwise><div class="empty-state">이 여행지의 지도 위치 정보가 없습니다.</div></c:otherwise>
  </c:choose>
</section>
<c:if test="${not empty images}"><section class="panel"><h2>사진</h2><div class="grid"><c:forEach var="image" items="${images}"><img class="gallery-image" src="<c:out value='${image}'/>" alt="관광지 사진" loading="lazy"></c:forEach></div></section></c:if>
<section class="panel">
  <span class="eyebrow">Useful information</span>
  <h2>여행 정보</h2>
  <p id="weather">날씨 정보를 불러오는 중입니다.</p>
  <label>일출·일몰 날짜<input id="sun-date" type="date"></label>
  <p id="sun">일출·일몰 정보를 불러오는 중입니다.</p>
  <div id="chargers"></div><ul id="charger-list"></ul>
</section>
<script>
const infoBase = '${pageContext.request.contextPath}/api/attractions/${attraction.id}';
let chargerData = [];
let chargerMarkers = [];
function renderChargerMarkers(items) {
  chargerData = items;
  if (!window.attractionMap || !window.L) return;
  chargerMarkers.forEach(marker => marker.remove());
  chargerMarkers = items.filter(item => item.latitude != null && item.longitude != null
      && Number.isFinite(Number(item.latitude)) && Number.isFinite(Number(item.longitude)))
    .map(item => L.marker([Number(item.latitude), Number(item.longitude)])
      .addTo(window.attractionMap).bindPopup(document.createTextNode(item.name + ' · ' + item.status)));
}
async function loadInfo(kind) {
  const el = document.getElementById(kind);
  try {
    const query = kind === 'sun' && document.getElementById('sun-date').value
      ? '?date=' + encodeURIComponent(document.getElementById('sun-date').value) : '';
    const response = await fetch(infoBase + '/' + kind + query);
    if (!response.ok) throw Error();
    const data = await response.json();
    el.textContent = data.summary || '정보가 없습니다.';
    if (kind === 'chargers') {
      const list = document.getElementById('charger-list');
      list.replaceChildren();
      for (const item of data.chargers || []) {
        const li = document.createElement('li');
        li.textContent = item.name + ' · ' + item.status + ' · ' + item.address + ' (상태 갱신 ' + item.updatedAt + ')';
        list.append(li);
      }
      renderChargerMarkers(data.chargers || []);
    }
  } catch (error) {
    el.textContent = '현재 정보를 불러올 수 없습니다.';
  }
}
for (const kind of ['weather', 'sun', 'chargers']) loadInfo(kind);
document.getElementById('sun-date').addEventListener('change', () => loadInfo('sun'));
</script>
<c:if test="${not empty attraction.latitude and not empty attraction.longitude}">
  <script src="${pageContext.request.contextPath}/assets/maps.js"></script>
  <script src="${pageContext.request.contextPath}/assets/leaflet.js"></script>
  <script>
  EnjoyTripMaps.ready('map', (container, maps) => {
    const point = [${attraction.latitude}, ${attraction.longitude}];
    const map = EnjoyTripMaps.createMap(container, point);
    maps.marker(point).addTo(map).bindTooltip('관광지');
    window.attractionMap = map;
    renderChargerMarkers(chargerData);
    return map;
  });
  </script>
</c:if>
<%@ include file="_footer.jspf" %>
