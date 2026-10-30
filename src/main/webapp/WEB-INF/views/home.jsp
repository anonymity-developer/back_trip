<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>
<section class="hero">
  <div class="hero-copy">
    <span class="eyebrow">Your next journey starts here</span>
    <h1>가고 싶은 곳을 찾고<br><em>여행을 완성하세요.</em></h1>
    <p>전국의 여행지와 맛집을 둘러보고, 마음에 드는 곳을 모아 나만의 여행 동선을 만들어 보세요.</p>
    <div class="hero-actions">
      <a class="button primary" href="${pageContext.request.contextPath}/attractions">여행지 둘러보기 <span aria-hidden="true">→</span></a>
      <a class="button" href="${pageContext.request.contextPath}/plans">여행 계획 보기</a>
    </div>
  </div>
  <div class="hero-visual" aria-hidden="true">
    <div class="route-card">
      <span class="card-label">A little trip, a big memory</span>
      <div class="route-stop">새로운 여행지 발견</div>
      <div class="route-stop">좋아하는 장소 저장</div>
      <div class="route-stop">나만의 동선 완성</div>
      <div class="route-card-footer">오늘의 선택이 다음 여행이 됩니다.</div>
    </div>
  </div>
</section>
<div class="section-heading">
  <span class="eyebrow">Explore EnjoyTrip</span>
  <h2>어디부터 떠나볼까요?</h2>
  <p>원하는 방식으로 여행을 시작해 보세요.</p>
</div>
<div class="grid feature-grid">
  <a class="panel feature-card" href="${pageContext.request.contextPath}/attractions?type=12"><span class="card-label">Discover</span><h2>관광지 찾기</h2><p>전국의 명소와 문화 공간을 탐색해 보세요.</p></a>
  <a class="panel feature-card" href="${pageContext.request.contextPath}/attractions?type=39"><span class="card-label">Taste</span><h2>맛집 둘러보기</h2><p>여행의 즐거움을 더하는 한 끼를 찾아보세요.</p></a>
  <a class="panel feature-card" href="${pageContext.request.contextPath}/plans"><span class="card-label">Plan</span><h2>여행 계획 만들기</h2><p>방문 순서와 일정을 한곳에 정리하세요.</p></a>
</div>
<%@ include file="_footer.jspf" %>
