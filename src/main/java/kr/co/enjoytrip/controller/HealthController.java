package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.Connection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.config.Database;

@WebServlet("/health")
public class HealthController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{
        res.setContentType("text/plain;charset=UTF-8");
        try(Connection connection=Database.open()) {
            if(connection.isValid(2))res.getWriter().write("OK");
            else {res.setStatus(503);res.getWriter().write("DB unavailable");}
        }catch(Exception e){res.setStatus(503);res.getWriter().write("DB unavailable");}
    }
}
