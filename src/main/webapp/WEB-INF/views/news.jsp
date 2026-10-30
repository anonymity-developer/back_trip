<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="page-heading"><div><span class="eyebrow">Travel stories</span><h1>여행 뉴스</h1><p>여행과 지역의 새로운 소식을 살펴보세요.</p></div></div>
<section class="panel list-panel">
  <c:choose>
    <c:when test="${empty news}"><div class="empty-state">수집된 관광 뉴스가 없습니다.</div></c:when>
    <c:otherwise><ul class="news-list"><c:forEach var="item" items="${news}">
      <li><div><span class="card-label"><c:out value="${item.source}"/></span><a href="<c:out value='${item.url}'/>" target="_blank" rel="noopener noreferrer"><c:out value="${item.title}"/></a><span class="muted"><c:out value="${item.publishedAt}"/></span></div><span class="list-arrow" aria-hidden="true">↗</span></li>
    </c:forEach></ul></c:otherwise>
  </c:choose>
</section>
<div class="actions pagination"><c:if test="${page > 1}"><a class="button" href="${pageContext.request.contextPath}/news?page=${page - 1}">← 이전</a></c:if><c:if test="${hasNext}"><a class="button" href="${pageContext.request.contextPath}/news?page=${page + 1}">다음 →</a></c:if></div>
<%@ include file="_footer.jspf" %>
