package kr.co.enjoytrip.filter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter("/*")
public class SecurityFilter implements Filter {
    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        HttpSession session = req.getSession();
        if (session.getAttribute("csrf") == null) {
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            session.setAttribute("csrf", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        req.setAttribute("csrf", session.getAttribute("csrf"));
        req.setAttribute("currentUserId", session.getAttribute("userId"));
        req.setAttribute("currentUserName", session.getAttribute("userName"));
        req.setAttribute("currentUserRole", session.getAttribute("userRole"));
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if ("POST".equals(req.getMethod())) {
            String token = req.getParameter("csrf");
            if (token == null || !token.equals(session.getAttribute("csrf"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "잘못된 요청입니다."); return;
            }
        }
        if (path.startsWith("/admin") && !"ADMIN".equals(session.getAttribute("userRole"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }
        boolean memberPath = path.startsWith("/me") || path.startsWith("/plans/new") || path.startsWith("/hot-places/new") || path.equals("/password-change");
        if (memberPath && session.getAttribute("userId") == null) {
            res.sendRedirect(req.getContextPath() + "/login"); return;
        }
        if (Boolean.TRUE.equals(session.getAttribute("mustChangePassword"))
                && !path.equals("/password-change") && !path.equals("/logout") && !path.startsWith("/assets/")) {
            res.sendRedirect(req.getContextPath() + "/password-change"); return;
        }
        chain.doFilter(request, response);
    }
}
