package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.enjoytrip.dto.UserDto;
import kr.co.enjoytrip.service.UserService;
import kr.co.enjoytrip.service.ValidationException;

@WebServlet({"/signup", "/login", "/logout", "/me", "/me/delete", "/password-reset", "/password-change", "/admin", "/admin/password-resets/*"})
public class AuthController extends HttpServlet {
    private final UserService users = new UserService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        try {
            switch (path) {
                case "/signup" -> view(req, res, "signup");
                case "/login" -> view(req, res, "login");
                case "/me" -> { req.setAttribute("profile", users.find(userId(req))); view(req, res, "profile"); }
                case "/password-reset" -> view(req, res, "password-reset");
                case "/password-change" -> view(req, res, "password-change");
                case "/admin" -> { req.setAttribute("resets", users.pendingResets()); req.setAttribute("syncStatus", kr.co.enjoytrip.config.AppLifecycle.attractions().syncStatus()); req.setAttribute("newsStatus", kr.co.enjoytrip.config.AppLifecycle.news().status()); view(req, res, "admin"); }
                default -> res.sendError(404);
            }
        } catch (SQLException e) { throw new ServletException(e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        try {
            switch (path) {
                case "/signup" -> { users.register(param(req,"loginId"), param(req,"email"), param(req,"name"), req.getParameter("password")); redirect(req,res,"/login?registered=1"); }
                case "/login" -> {
                    UserDto user = users.authenticate(param(req,"loginId"), req.getParameter("password"));
                    req.getSession().invalidate();
                    HttpSession session = req.getSession(true);
                    req.changeSessionId();
                    session.setAttribute("userId", user.id()); session.setAttribute("userName", user.name());
                    session.setAttribute("userRole", user.role()); session.setAttribute("mustChangePassword", user.mustChangePassword());
                    redirect(req,res,user.mustChangePassword() ? "/password-change" : "/");
                }
                case "/logout" -> { req.getSession().invalidate(); redirect(req,res,"/"); }
                case "/me" -> { users.update(userId(req), param(req,"email"), param(req,"name")); req.getSession().setAttribute("userName", param(req,"name")); redirect(req,res,"/me?updated=1"); }
                case "/me/delete" -> { users.delete(userId(req), req.getParameter("password")); req.getSession().invalidate(); redirect(req,res,"/"); }
                case "/password-reset" -> { users.requestReset(param(req,"email")); redirect(req,res,"/password-reset?requested=1"); }
                case "/password-change" -> { users.changePassword(userId(req), req.getParameter("oldPassword"), req.getParameter("newPassword")); req.getSession().setAttribute("mustChangePassword", false); redirect(req,res,"/me?passwordChanged=1"); }
                default -> {
                    if (path.startsWith("/admin/password-resets/") && path.endsWith("/complete")) {
                        long id = Long.parseLong(path.substring("/admin/password-resets/".length(), path.length()-"/complete".length()));
                        String temp = users.completeReset(id, userId(req));
                        req.setAttribute("temporaryPassword", temp);
                        req.setAttribute("resets", users.pendingResets());
                        view(req,res,"admin");
                    } else res.sendError(404);
                }
            }
        } catch (ValidationException e) {
            req.setAttribute("error", e.getMessage());
            if (path.equals("/me") || path.equals("/me/delete")) req.setAttribute("profile", usersSafeFind(req));
            view(req,res, path.startsWith("/admin") ? "admin" : path.equals("/me/delete") || path.equals("/me") ? "profile" : path.substring(1));
        } catch (NumberFormatException e) { res.sendError(400); }
        catch (SQLException e) { throw new ServletException(e); }
    }

    private UserDto usersSafeFind(HttpServletRequest req) throws ServletException {
        try { return users.find(userId(req)); } catch (SQLException e) { throw new ServletException(e); }
    }
    private long userId(HttpServletRequest req) { return (long) req.getSession().getAttribute("userId"); }
    private String path(HttpServletRequest req) { return req.getRequestURI().substring(req.getContextPath().length()); }
    private String param(HttpServletRequest req, String name) { return req.getParameter(name) == null ? "" : req.getParameter(name).trim(); }
    private void redirect(HttpServletRequest req, HttpServletResponse res, String path) throws IOException { res.sendRedirect(req.getContextPath()+path); }
    private void view(HttpServletRequest req, HttpServletResponse res, String name) throws ServletException, IOException { req.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(req,res); }
}
