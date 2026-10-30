<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<c:choose><c:when test="${empty plan}"><c:set var="formAction" value="${pageContext.request.contextPath}/plans/new"/></c:when><c:otherwise><c:set var="formAction" value="${pageContext.request.contextPath}/plans/${plan.id}/edit"/></c:otherwise></c:choose>
<section class="panel narrow"><h1>여행 계획 <c:out value="${empty plan ? '작성' : '수정'}"/></h1><form method="post" action="${formAction}"><input type="hidden" name="csrf" value="${csrf}">
<label>제목<input name="title" maxlength="200" value="<c:out value='${plan.title}'/>" required></label>
<label>시작일<input name="startDate" type="date" value="${plan.startDate}" required></label><label>종료일<input name="endDate" type="date" value="${plan.endDate}" required></label>
<label>예상 경비 (원)<input name="budget" type="number" min="0" step="1" value="${empty plan ? '0' : plan.budget}" required></label>
<label>상세 계획<textarea name="details" maxlength="20000"><c:out value="${plan.details}"/></textarea></label>
<label class="check"><input name="isPublic" type="checkbox" ${plan.isPublic ? 'checked' : ''}>공유 URL 공개</label><button class="primary">저장</button></form></section>
<%@ include file="_footer.jspf" %>
