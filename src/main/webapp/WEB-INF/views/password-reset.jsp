<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="panel narrow"><h1>비밀번호 찾기</h1><p>등록한 이메일로 초기화를 요청하세요. 관리자가 별도로 본인 확인 후 임시 비밀번호를 전달합니다.</p>
<c:if test="${param.requested eq '1'}"><p class="success">계정이 확인되면 관리자가 요청을 처리합니다.</p></c:if>
<form method="post" action="${pageContext.request.contextPath}/password-reset"><input type="hidden" name="csrf" value="${csrf}"><label>이메일<input name="email" type="email" required></label><button class="primary">초기화 요청</button></form></section>
<%@ include file="_footer.jspf" %>
