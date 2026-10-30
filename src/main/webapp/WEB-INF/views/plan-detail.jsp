<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<p><a href="${pageContext.request.contextPath}/plans">← 계획 목록</a></p>
<section class="panel">
  <span class="eyebrow">Travel itinerary</span>
  <h1><c:out value="${plan.title}"/></h1>
  <p><c:out value="${plan.startDate}"/> ~ <c:out value="${plan.endDate}"/> · 예상 경비 <c:out value="${plan.budget}"/>원</p>
  <p class="muted"><c:out value="${plan.author}"/> · <c:out value="${plan.isPublic ? '공개' : '비공개'}"/></p>
  <p class="post-body"><c:out value="${plan.details}"/></p>
  <c:if test="${currentUserId eq plan.userId}"><div class="actions"><a class="button" href="${pageContext.request.contextPath}/plans/${plan.id}/edit">계획 수정</a><form method="post" action="${pageContext.request.contextPath}/plans/${plan.id}/delete" onsubmit="return confirm('계획을 삭제할까요?')"><input type="hidden" name="csrf" value="${csrf}"><button class="danger">삭제</button></form></div></c:if>
</section>
<section class="panel">
  <h2>방문지와 동선</h2>
  <c:if test="${empty stops}"><p class="muted">방문지를 추가하면 이곳에 동선이 표시됩니다.</p></c:if>
  <ul id="stop-list" class="stop-list">
    <c:forEach var="stop" items="${stops}">
      <li data-id="${stop.id}" draggable="${currentUserId eq plan.userId}">
        <div>
          <strong><c:out value="${stop.attractionTitle}"/></strong> · <c:out value="${stop.visitDate}"/>
          <c:if test="${not empty stop.memo}"> · <c:out value="${stop.memo}"/></c:if>
          <c:if test="${not empty stop.latitude and not empty stop.longitude}"><span class="stop-point" data-lat="${stop.latitude}" data-lng="${stop.longitude}" hidden></span></c:if>
        </div>
        <c:if test="${currentUserId eq plan.userId}"><form method="post" action="${pageContext.request.contextPath}/plans/${plan.id}/stops/delete"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="stopId" value="${stop.id}"><button class="danger">제거</button></form></c:if>
      </li>
    </c:forEach>
  </ul>
  <c:choose>
    <c:when test="${not empty stops}"><div class="map-shell"><div id="plan-map" class="map" aria-label="여행 동선 지도"></div><p class="map-status" role="status">지도를 불러오는 중입니다.</p></div></c:when>
  </c:choose>
  <c:if test="${currentUserId eq plan.userId}">
    <p class="muted">방문지를 끌어서 순서를 바꿀 수 있습니다.</p>
    <form method="post" action="${pageContext.request.contextPath}/plans/${plan.id}/stops" id="add-stop">
      <input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="attractionId" id="attraction-id">
      <label>여행지 검색<input id="attraction-query" autocomplete="off" placeholder="여행지 이름" required></label>
      <div id="attraction-results"></div>
      <label>방문일<input name="visitDate" type="date" min="${plan.startDate}" max="${plan.endDate}" required></label>
      <label>예상 경비<input name="budget" type="number" min="0" value="0"></label>
      <label>메모<input name="memo" maxlength="1000"></label>
      <button class="primary">방문지 추가</button>
    </form>
  </c:if>
</section>
<c:if test="${not empty stops}">
  <script src="${pageContext.request.contextPath}/assets/maps.js"></script>
  <script src="${pageContext.request.contextPath}/assets/leaflet.js"></script>
  <script>
  const points = Array.from(document.querySelectorAll('.stop-point'), node => ({
    lat: Number(node.dataset.lat), lng: Number(node.dataset.lng)
  })).filter(point => Number.isFinite(point.lat) && Number.isFinite(point.lng));
  if (points.length === 0) {
    const status = document.querySelector('#plan-map + .map-status');
    status.textContent = '좌표가 있는 방문지가 없어 동선을 표시할 수 없습니다.';
    status.classList.add('map-status-error');
  } else {
    EnjoyTripMaps.ready('plan-map', (container, maps) => {
      const path = points.map(point => [point.lat, point.lng]);
      const map = EnjoyTripMaps.createMap(container, path[0]);
      path.forEach((point, index) => maps.marker(point).addTo(map).bindTooltip(String(index + 1)));
      if (path.length > 1) {
        maps.polyline(path, { color: '#08736b', weight: 4 }).addTo(map);
        map.fitBounds(maps.latLngBounds(path), { padding: [30, 30] });
      }
      return map;
    });
  }
  </script>
</c:if>
<c:if test="${currentUserId eq plan.userId}"><script src="${pageContext.request.contextPath}/assets/plan.js"></script><script>initPlan('${pageContext.request.contextPath}', ${plan.id}, '${csrf}');</script></c:if>
<%@ include file="_footer.jspf" %>
