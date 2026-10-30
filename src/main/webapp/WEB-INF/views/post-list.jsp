<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="page-heading">
  <div><span class="eyebrow">EnjoyTrip community</span><h1><c:out value="${heading}"/></h1><p>여행 경험과 정보를 함께 나눠보세요.</p></div>
  <c:if test="${not empty currentUserId and (base eq '/posts' or currentUserRole eq 'ADMIN')}"><a class="button primary" href="${pageContext.request.contextPath}${base}/new">글 쓰기 <span aria-hidden="true">→</span></a></c:if>
</div>
<section class="panel search-panel"><form method="get" action="${pageContext.request.contextPath}${base}" class="simple-search"><label>제목 검색<input name="keyword" value="<c:out value='${keyword}'/>" maxlength="100" placeholder="찾고 싶은 제목을 입력하세요"></label><button class="primary">검색</button></form></section>
<section class="panel list-panel"><c:choose>
  <c:when test="${empty posts}"><div class="empty-state">등록된 글이 없습니다.</div></c:when>
  <c:otherwise><div class="table-scroll"><table><thead><tr><th>제목</th><th>작성자</th><th>작성일</th></tr></thead><tbody><c:forEach var="post" items="${posts}"><tr><td><a class="table-title" href="${pageContext.request.contextPath}${base}/${post.id}"><c:out value="${post.title}"/></a></td><td><c:out value="${post.author}"/></td><td><c:out value="${post.createdAt}"/></td></tr></c:forEach></tbody></table></div></c:otherwise>
</c:choose></section>
<div class="actions pagination"><c:if test="${page > 1}"><c:url var="prevUrl" value="${base}"><c:param name="page" value="${page - 1}"/><c:param name="keyword" value="${keyword}"/></c:url><a class="button" href="${prevUrl}">← 이전</a></c:if><c:if test="${hasNext}"><c:url var="nextUrl" value="${base}"><c:param name="page" value="${page + 1}"/><c:param name="keyword" value="${keyword}"/></c:url><a class="button" href="${nextUrl}">다음 →</a></c:if></div>
<%@ include file="_footer.jspf" %>
