<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<c:choose><c:when test="${empty place}"><c:set var="formAction" value="${pageContext.request.contextPath}/hot-places/new"/></c:when><c:otherwise><c:set var="formAction" value="${pageContext.request.contextPath}/hot-places/${place.id}/edit"/></c:otherwise></c:choose>
<section class="panel narrow">
  <span class="eyebrow">Share a place</span>
  <h1>Hot Place <c:out value="${empty place ? '등록' : '수정'}"/></h1>
  <form method="post" action="${formAction}" enctype="multipart/form-data">
    <input type="hidden" name="csrf" value="${csrf}">
    <label>제목<input name="title" maxlength="200" value="<c:out value='${place.title}'/>" required></label>
    <label>장소명<input name="placeName" maxlength="200" value="<c:out value='${place.placeName}'/>" required></label>
    <label>장소 유형<input name="placeType" maxlength="80" value="<c:out value='${place.placeType}'/>" placeholder="관광지, 카페 등" required></label>
    <label>방문일<input name="visitedOn" type="date" value="${place.visitedOn}" required></label>
    <label>설명<textarea name="description" maxlength="20000" required><c:out value="${place.description}"/></textarea></label>
    <label>사진<c:if test="${not empty place.imagePath}"><small>새 파일을 선택하지 않으면 기존 사진을 유지합니다.</small></c:if><input name="image" type="file" accept="image/jpeg,image/png,image/webp" ${empty place ? 'required' : ''}></label>
    <label>위도<input name="latitude" id="latitude" type="number" step="any" value="${place.latitude}"></label>
    <label>경도<input name="longitude" id="longitude" type="number" step="any" value="${place.longitude}"></label>
    <p class="muted">지도에서 장소를 클릭하면 좌표가 입력됩니다.</p><div class="map-shell"><div id="pick-map" class="map" aria-label="장소 좌표 선택 지도"></div><p class="map-status" role="status">지도를 불러오는 중입니다.</p></div>
    <button class="primary">저장</button>
  </form>
</section>
  <script src="${pageContext.request.contextPath}/assets/maps.js"></script>
  <script src="${pageContext.request.contextPath}/assets/leaflet.js"></script>
  <script>
  EnjoyTripMaps.ready('pick-map', (container, maps) => {
    const lat = document.getElementById('latitude');
    const lng = document.getElementById('longitude');
    const initial = [Number(lat.value) || 37.5665, Number(lng.value) || 126.9780];
    const map = EnjoyTripMaps.createMap(container, initial, 12);
    const marker = maps.marker(initial).addTo(map);
    map.on('click', event => {
      lat.value = event.latlng.lat;
      lng.value = event.latlng.lng;
      marker.setLatLng(event.latlng);
    });
    return map;
  });
  </script>
<%@ include file="_footer.jspf" %>
