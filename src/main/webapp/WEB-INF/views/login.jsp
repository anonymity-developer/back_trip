<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="panel narrow"><h1>로그인</h1><c:if test="${param.registered eq '1'}"><p class="success">가입이 완료되었습니다.</p></c:if>
<form method="post" action="${pageContext.request.contextPath}/login"><input type="hidden" name="csrf" value="${csrf}">
<label>아이디<input name="loginId" autocomplete="username" required></label>
<label>비밀번호<input name="password" type="password" autocomplete="current-password" required></label>
<button class="primary">로그인</button></form><p><a href="${pageContext.request.contextPath}/password-reset">비밀번호 찾기</a></p></section>
<%@ include file="_footer.jspf" %>
