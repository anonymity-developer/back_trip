<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="section-heading">
  <span class="eyebrow">Shared memories</span>
  <h1>나만의 Hot Place</h1>
  <p>직접 다녀온 장소와 여행의 순간을 공유해 보세요.</p>
</div>
<c:if test="${not empty currentUserId}"><p><a class="button primary" href="${pageContext.request.contextPath}/hot-places/new">장소 등록 →</a></p></c:if>
<c:choose>
  <c:when test="${empty places}"><div class="empty-state">아직 등록된 장소가 없습니다.</div></c:when>
  <c:otherwise>
    <div class="grid">
      <c:forEach var="place" items="${places}">
        <a class="card" href="${pageContext.request.contextPath}/hot-places/${place.id}">
          <div class="card-media">
            <c:choose><c:when test="${not empty place.imagePath}"><img src="${pageContext.request.contextPath}/photos/${place.id}" alt="" loading="lazy" data-fallback><span class="card-placeholder" aria-hidden="true"><span>✦</span><small>장소 사진 준비 중</small></span></c:when><c:otherwise><span class="card-placeholder" aria-hidden="true"><span>✦</span><small>장소 사진 준비 중</small></span></c:otherwise></c:choose>
          </div>
          <div class="card-body"><span class="card-label">Hot Place</span><h2><c:out value="${place.title}"/></h2><p><c:out value="${place.placeName}"/></p><p class="muted"><c:out value="${place.author}"/> · <c:out value="${place.visitedOn}"/></p><span class="card-arrow">자세히 보기 →</span></div>
        </a>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="_footer.jspf" %>
