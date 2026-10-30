<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="panel narrow"><h1>내 정보</h1><c:if test="${param.updated eq '1' or param.passwordChanged eq '1'}"><p class="success">변경되었습니다.</p></c:if>
<p>아이디: <c:out value="${profile.loginId}"/></p><form method="post" action="${pageContext.request.contextPath}/me"><input type="hidden" name="csrf" value="${csrf}">
<label>이름<input name="name" value="<c:out value='${profile.name}'/>" maxlength="80" required></label>
<label>이메일<input name="email" type="email" value="<c:out value='${profile.email}'/>" required></label><button class="primary">정보 수정</button></form>
<p><a href="${pageContext.request.contextPath}/password-change">비밀번호 변경</a></p></section>
<section class="panel narrow"><h2>회원 탈퇴</h2><form method="post" action="${pageContext.request.contextPath}/me/delete" onsubmit="return confirm('회원 정보와 작성한 콘텐츠가 삭제됩니다. 탈퇴할까요?')"><input type="hidden" name="csrf" value="${csrf}"><label>현재 비밀번호<input name="password" type="password" required></label><button class="danger">탈퇴</button></form></section>
<%@ include file="_footer.jspf" %>
