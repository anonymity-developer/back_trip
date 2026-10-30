<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<c:choose><c:when test="${empty post}"><c:set var="formAction" value="${pageContext.request.contextPath}${base}/new"/></c:when><c:otherwise><c:set var="formAction" value="${pageContext.request.contextPath}${base}/${post.id}/edit"/></c:otherwise></c:choose>
<section class="panel"><h1><c:out value="${heading}"/> <c:out value="${empty post ? '작성' : '수정'}"/></h1><form method="post" action="${formAction}"><input type="hidden" name="csrf" value="${csrf}">
<label>제목<input name="title" maxlength="200" value="<c:out value='${post.title}'/>" required></label><label>본문<textarea name="body" maxlength="20000" required><c:out value="${post.body}"/></textarea></label><button class="primary">저장</button></form></section>
<%@ include file="_footer.jspf" %>
