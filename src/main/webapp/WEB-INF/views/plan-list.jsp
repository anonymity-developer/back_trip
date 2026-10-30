<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="page-heading">
  <div><span class="eyebrow">Make your journey</span><h1>여행 계획</h1><p>가고 싶은 곳을 날짜와 동선에 맞춰 정리해 보세요.</p></div>
  <c:if test="${not empty currentUserId}"><a class="button primary" href="${pageContext.request.contextPath}/plans/new">새 계획 만들기 <span aria-hidden="true">→</span></a></c:if>
</div>
<c:choose>
  <c:when test="${empty plans}"><div class="empty-state">아직 공개된 여행 계획이 없습니다.</div></c:when>
  <c:otherwise><div class="grid plan-grid"><c:forEach var="plan" items="${plans}">
    <a class="panel plan-card" href="${pageContext.request.contextPath}/plans/${plan.id}">
      <div class="plan-card-top"><span class="card-label">Travel plan</span><span class="visibility-badge"><c:out value="${plan.isPublic ? '공개' : '비공개'}"/></span></div>
      <h2><c:out value="${plan.title}"/></h2>
      <p class="plan-dates"><c:out value="${plan.startDate}"/> <span aria-hidden="true">→</span> <c:out value="${plan.endDate}"/></p>
      <div class="plan-card-bottom"><span><c:out value="${plan.author}"/></span><span>계획 보기 →</span></div>
    </a>
  </c:forEach></div></c:otherwise>
</c:choose>
<%@ include file="_footer.jspf" %>
