package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.dao.AttractionDao;

@WebServlet("/api/attractions/search")
public class AttractionSearchController extends HttpServlet {
    private final AttractionDao dao=new AttractionDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String query=req.getParameter("q");
        if(query==null||query.isBlank()||query.length()>100){res.sendError(400);return;}
        try{
            StringBuilder json=new StringBuilder("[");
            var rows=dao.search(null,null,query,1,10);
            for(int i=0;i<rows.size();i++){
                if(i>0)json.append(',');
                json.append("{\"id\":").append(rows.get(i).id()).append(",\"title\":\"").append(escape(rows.get(i).title())).append("\"}");
            }
            json.append(']');res.setContentType("application/json;charset=UTF-8");res.getWriter().write(json.toString());
        }catch(SQLException e){throw new ServletException(e);}
    }
    private String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
