package kr.co.enjoytrip.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.config.AppLifecycle;

@WebServlet({"/news","/admin/sync/news"})
public class NewsController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        if(!req.getServletPath().equals("/news")){res.sendError(404);return;}
        try{int page=req.getParameter("page")==null?1:Integer.parseInt(req.getParameter("page"));if(page<1||page>10000){res.sendError(400);return;}
            var rows=AppLifecycle.news().list(page);req.setAttribute("news",rows.size()>20?rows.subList(0,20):rows);req.setAttribute("page",page);req.setAttribute("hasNext",rows.size()>20);
            req.getRequestDispatcher("/WEB-INF/views/news.jsp").forward(req,res);
        }catch(NumberFormatException e){res.sendError(400);}catch(Exception e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException{
        if(!req.getServletPath().equals("/admin/sync/news")){res.sendError(404);return;}
        if(!"ADMIN".equals(req.getSession().getAttribute("userRole"))){res.sendError(403);return;}
        if(!AppLifecycle.syncEnabled()){res.sendError(503,"동기화가 비활성화되어 있습니다.");return;}
        AppLifecycle.startNewsSync();res.sendRedirect(req.getContextPath()+"/admin");
    }
}
