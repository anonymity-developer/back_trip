package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.config.AppLifecycle;
import kr.co.enjoytrip.service.AttractionService;
import com.google.gson.Gson;

@WebServlet("/api/attractions/search")
public class AttractionSearchController extends HttpServlet {
    private final AttractionService attractions=AppLifecycle.attractions();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String query=req.getParameter("q");
        if(query==null||query.isBlank()||query.length()>100){res.sendError(400);return;}
        try{
            var rows=attractions.autocomplete(query);
            var result=rows.stream().map(a->new SearchItem(a.id(),a.title())).toList();
            res.setContentType("application/json;charset=UTF-8");res.getWriter().write(new Gson().toJson(result));
        }catch(SQLException e){throw new ServletException(e);}
    }
    private record SearchItem(long id,String title){}
}
