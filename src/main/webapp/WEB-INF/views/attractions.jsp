<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="section-heading">
  <span class="eyebrow">Find your place</span>
  <h1>여행지 찾기</h1>
  <p>지역과 취향에 맞는 장소를 찾아보세요.</p>
</div>
<section class="panel">
  <form class="filters" method="get" action="${pageContext.request.contextPath}/attractions">
    <label>지역
      <select name="region">
        <option value="">전체 지역</option>
        <option value="1" ${region == 1 ? 'selected' : ''}>서울</option><option value="2" ${region == 2 ? 'selected' : ''}>인천</option>
        <option value="3" ${region == 3 ? 'selected' : ''}>대전</option><option value="4" ${region == 4 ? 'selected' : ''}>대구</option>
        <option value="5" ${region == 5 ? 'selected' : ''}>광주</option><option value="6" ${region == 6 ? 'selected' : ''}>부산</option>
        <option value="7" ${region == 7 ? 'selected' : ''}>울산</option><option value="8" ${region == 8 ? 'selected' : ''}>세종</option>
        <option value="31" ${region == 31 ? 'selected' : ''}>경기</option><option value="32" ${region == 32 ? 'selected' : ''}>강원</option>
        <option value="33" ${region == 33 ? 'selected' : ''}>충북</option><option value="34" ${region == 34 ? 'selected' : ''}>충남</option>
        <option value="35" ${region == 35 ? 'selected' : ''}>경북</option><option value="36" ${region == 36 ? 'selected' : ''}>경남</option>
        <option value="37" ${region == 37 ? 'selected' : ''}>전북</option><option value="38" ${region == 38 ? 'selected' : ''}>전남</option>
        <option value="39" ${region == 39 ? 'selected' : ''}>제주</option>
      </select>
    </label>
    <label>유형
      <select name="type">
        <option value="">전체 유형</option>
        <option value="12" ${type == 12 ? 'selected' : ''}>관광지</option><option value="14" ${type == 14 ? 'selected' : ''}>문화시설</option>
        <option value="15" ${type == 15 ? 'selected' : ''}>축제·공연</option><option value="25" ${type == 25 ? 'selected' : ''}>여행코스</option>
        <option value="28" ${type == 28 ? 'selected' : ''}>레포츠</option><option value="32" ${type == 32 ? 'selected' : ''}>숙박</option>
        <option value="38" ${type == 38 ? 'selected' : ''}>쇼핑</option><option value="39" ${type == 39 ? 'selected' : ''}>음식점</option>
      </select>
    </label>
    <label>검색어<input name="keyword" maxlength="100" placeholder="장소 이름을 입력하세요" value="<c:out value='${keyword}'/>"></label>
    <button class="primary">검색 <span aria-hidden="true">→</span></button>
  </form>
</section>
<c:choose>
  <c:when test="${empty attractions}"><div class="empty-state">조건에 맞는 여행지가 없습니다. 다른 지역이나 검색어를 선택해 보세요.</div></c:when>
  <c:otherwise>
    <div class="grid">
      <c:forEach var="item" items="${attractions}">
        <a class="card" href="${pageContext.request.contextPath}/attractions/${item.id}">
          <div class="card-media">
            <c:choose>
              <c:when test="${not empty item.imageUrl}"><img src="<c:out value='${item.imageUrl}'/>" alt="" loading="lazy" data-fallback><span class="card-placeholder" aria-hidden="true"><span>✦</span><small>여행지 사진 준비 중</small></span></c:when>
              <c:otherwise><span class="card-placeholder" aria-hidden="true"><span>✦</span><small>여행지 사진 준비 중</small></span></c:otherwise>
            </c:choose>
          </div>
          <div class="card-body"><span class="card-label">여행지</span><h2><c:out value="${item.title}"/></h2><p class="muted"><c:out value="${item.address}"/></p><span class="card-arrow">자세히 보기 →</span></div>
        </a>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
<div class="actions" style="margin-top:1.5rem">
  <c:if test="${page > 1}"><c:url var="prevUrl" value="/attractions"><c:param name="page" value="${page - 1}"/><c:param name="region" value="${region}"/><c:param name="type" value="${type}"/><c:param name="keyword" value="${keyword}"/></c:url><a class="button" href="${prevUrl}">← 이전</a></c:if>
  <c:if test="${hasNext}"><c:url var="nextUrl" value="/attractions"><c:param name="page" value="${page + 1}"/><c:param name="region" value="${region}"/><c:param name="type" value="${type}"/><c:param name="keyword" value="${keyword}"/></c:url><a class="button" href="${nextUrl}">다음 →</a></c:if>
</div>
<%@ include file="_footer.jspf" %>
