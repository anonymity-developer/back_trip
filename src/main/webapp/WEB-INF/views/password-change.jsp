<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="panel narrow"><h1>비밀번호 변경</h1><form method="post" action="${pageContext.request.contextPath}/password-change"><input type="hidden" name="csrf" value="${csrf}">
<label>현재 또는 임시 비밀번호<input name="oldPassword" type="password" required></label>
<label>새 비밀번호<input name="newPassword" type="password" minlength="10" required></label><button class="primary">변경</button></form></section>
<%@ include file="_footer.jspf" %>
