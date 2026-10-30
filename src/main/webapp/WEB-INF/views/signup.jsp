<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="panel narrow"><h1>회원가입</h1><form method="post" action="${pageContext.request.contextPath}/signup"><input type="hidden" name="csrf" value="${csrf}">
<label>아이디<input name="loginId" autocomplete="username" minlength="4" maxlength="50" required></label>
<label>이메일<input name="email" type="email" autocomplete="email" required></label>
<label>이름<input name="name" maxlength="80" required></label>
<label>비밀번호<input name="password" type="password" autocomplete="new-password" minlength="10" required></label>
<button class="primary">가입하기</button></form></section>
<%@ include file="_footer.jspf" %>
