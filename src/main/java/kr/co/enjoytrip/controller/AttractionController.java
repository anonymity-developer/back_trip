package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.config.AppLifecycle;
import kr.co.enjoytrip.dto.AttractionDto;
import kr.co.enjoytrip.service.AttractionService;

@WebServlet({"/home", "/attractions", "/attractions/*", "/admin/sync/tour"})
public class AttractionController extends HttpServlet {
    private final AttractionService attractions=AppLifecycle.attractions();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String path=req.getRequestURI().substring(req.getContextPath().length());
        try {
            if(path.equals("/home")) {view(req,res,"home");return;}
            if(path.equals("/attractions")) {
                int page=parse(req.getParameter("page"),1);
                if(page<1 || page>10000) {res.sendError(400);return;}
                Integer area=optional(req.getParameter("region"));
                Integer type=optional(req.getParameter("type"));
                String keyword=req.getParameter("keyword");
                if(keyword!=null && keyword.length()>100) {res.sendError(400);return;}
                List<AttractionDto> rows=attractions.search(area,type,keyword,page);
                req.setAttribute("attractions",rows.size()>20?rows.subList(0,20):rows); req.setAttribute("page",page); req.setAttribute("hasNext",rows.size()>20);
                req.setAttribute("region",area); req.setAttribute("type",type); req.setAttribute("keyword",keyword);
                view(req,res,"attractions");return;
            }
            if(path.startsWith("/attractions/")) {
                long id=Long.parseLong(path.substring("/attractions/".length()));
                AttractionDto attraction=attractions.find(id);
                if(attraction==null) {res.sendError(404);return;}
                req.setAttribute("attraction",attraction); req.setAttribute("images",attractions.images(attraction));
                view(req,res,"attraction-detail");return;
            }
            res.sendError(404);
        } catch(NumberFormatException e) {res.sendError(400);}
        catch(SQLException e) {throw new ServletException(e);}
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        String path=req.getRequestURI().substring(req.getContextPath().length());
        if(!path.equals("/admin/sync/tour")) {res.sendError(404);return;}
        if(!"ADMIN".equals(req.getSession().getAttribute("userRole"))) {res.sendError(403);return;}
        AppLifecycle.startTourSync();
        res.sendRedirect(req.getContextPath()+"/admin?sync=1");
    }

    private int parse(String value,int defaultValue){return value==null||value.isBlank()?defaultValue:Integer.parseInt(value);}
    private Integer optional(String value){return value==null||value.isBlank()?null:Integer.valueOf(value);}
    private void view(HttpServletRequest req,HttpServletResponse res,String name) throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(req,res);
    }
}
