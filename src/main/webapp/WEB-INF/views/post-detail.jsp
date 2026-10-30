<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<p><a href="${pageContext.request.contextPath}${base}">← 목록</a></p><article class="panel"><h1><c:out value="${post.title}"/></h1><p class="muted"><c:out value="${post.author}"/> · <c:out value="${post.createdAt}"/></p><div class="post-body"><c:out value="${post.body}"/></div></article>
<c:if test="${currentUserId eq post.userId or (post.kind eq 'NOTICE' and currentUserRole eq 'ADMIN')}"><div class="actions"><a class="button" href="${pageContext.request.contextPath}${base}/${post.id}/edit">수정</a><form method="post" action="${pageContext.request.contextPath}${base}/${post.id}/delete" onsubmit="return confirm('글을 삭제할까요?')"><input type="hidden" name="csrf" value="${csrf}"><button class="danger">삭제</button></form></div></c:if>
<%@ include file="_footer.jspf" %>
