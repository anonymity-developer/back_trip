<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<p><a href="${pageContext.request.contextPath}/hot-places">← Hot Place 목록</a></p>
<article class="panel">
  <span class="eyebrow">Shared memory</span>
  <h1><c:out value="${place.title}"/></h1>
  <p><c:out value="${place.placeName}"/> · <c:out value="${place.placeType}"/> · <c:out value="${place.visitedOn}"/></p>
  <p class="muted">작성자: <c:out value="${place.author}"/></p>
  <img class="detail-image" src="${pageContext.request.contextPath}/photos/${place.id}" alt="<c:out value='${place.placeName}'/>">
  <p class="post-body"><c:out value="${place.description}"/></p>
  <c:if test="${not empty place.latitude and not empty place.longitude}">
    <h2>지도에서 보기</h2>
    <div class="map-shell"><div id="hot-map" class="map" aria-label="Hot Place 위치 지도"></div><p class="map-status" role="status">지도를 불러오는 중입니다.</p></div>
  </c:if>
</article>
<c:if test="${currentUserId eq place.userId}"><div class="actions"><a class="button" href="${pageContext.request.contextPath}/hot-places/${place.id}/edit">수정</a><form method="post" action="${pageContext.request.contextPath}/hot-places/${place.id}/delete" onsubmit="return confirm('장소를 삭제할까요?')"><input type="hidden" name="csrf" value="${csrf}"><button class="danger">삭제</button></form></div></c:if>
<c:if test="${not empty place.latitude and not empty place.longitude}">
  <script src="${pageContext.request.contextPath}/assets/maps.js"></script>
  <script src="${pageContext.request.contextPath}/assets/leaflet.js"></script>
  <script>
  EnjoyTripMaps.ready('hot-map', (container, maps) => {
    const point = [${place.latitude}, ${place.longitude}];
    const map = EnjoyTripMaps.createMap(container, point);
    maps.marker(point).addTo(map);
    return map;
  });
  </script>
</c:if>
<%@ include file="_footer.jspf" %>
