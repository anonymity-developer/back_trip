<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<div class="section-heading">
  <span class="eyebrow">Manage EnjoyTrip</span>
  <h1>관리자</h1>
  <p>여행지와 뉴스를 최신 상태로 관리하세요.</p>
</div>
<c:if test="${not empty temporaryPassword}"><div class="alert">임시 비밀번호: <strong><c:out value="${temporaryPassword}"/></strong><p>지금 복사해 본인 확인을 마친 사용자에게 별도로 전달하세요. 다시 표시되지 않습니다.</p></div></c:if>
<div class="grid admin-grid">
  <section class="panel">
    <span class="card-label">Destination data</span>
    <h2>관광지 데이터</h2>
    <p class="sync-status"><c:out value="${syncStatus}"/></p>
    <form method="post" action="${pageContext.request.contextPath}/admin/sync/tour"><input type="hidden" name="csrf" value="${csrf}"><button class="primary">관광지 동기화 시작 →</button></form>
  </section>
  <section class="panel">
    <span class="card-label">Travel news</span>
    <h2>관광 뉴스</h2>
    <p class="sync-status"><c:out value="${newsStatus}"/></p>
    <form method="post" action="${pageContext.request.contextPath}/admin/sync/news"><input type="hidden" name="csrf" value="${csrf}"><button class="primary">뉴스 수집 시작 →</button></form>
  </section>
</div>
<section class="panel">
  <h2>비밀번호 초기화 요청</h2>
  <table><thead><tr><th>아이디</th><th>이메일</th><th>요청 시각</th><th>처리</th></tr></thead><tbody>
    <c:forEach var="row" items="${resets}"><tr><td><c:out value="${row.loginId}"/></td><td><c:out value="${row.email}"/></td><td><c:out value="${row.requestedAt}"/></td><td><form method="post" action="${pageContext.request.contextPath}/admin/password-resets/${row.id}/complete" onsubmit="return confirm('별도 채널로 본인 확인을 완료했나요?')"><input type="hidden" name="csrf" value="${csrf}"><button>임시 비밀번호 발급</button></form></td></tr></c:forEach>
  </tbody></table>
  <c:if test="${empty resets}"><p class="muted">대기 중인 요청이 없습니다.</p></c:if>
</section>
<%@ include file="_footer.jspf" %>
