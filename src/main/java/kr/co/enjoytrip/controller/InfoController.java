package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Map;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.service.AttractionService;
import kr.co.enjoytrip.config.AppLifecycle;
import kr.co.enjoytrip.dto.AttractionDto;
import kr.co.enjoytrip.service.InfoService;
import kr.co.enjoytrip.service.ValidationException;

@WebServlet("/api/attractions/*")
public class InfoController extends HttpServlet {
    private final AttractionService attractions=AppLifecycle.attractions();
    private final InfoService info=new InfoService();
    private final Gson gson=new Gson();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        try{
            String[] parts=req.getPathInfo().substring(1).split("/");
            if(parts.length!=2){res.sendError(404);return;}
            long id=Long.parseLong(parts[0]);AttractionDto attraction=attractions.findStored(id);
            if(attraction==null){res.sendError(404);return;}
            Object result;
            switch(parts[1]){
                case "weather" -> result=info.weather(attraction);
                case "sun" -> {
                    String date=req.getParameter("date");LocalDate day=date==null?LocalDate.now(ZoneId.of("Asia/Seoul")):LocalDate.parse(date);
                    if(day.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(30))||day.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")).plusYears(1))){res.sendError(400);return;}
                    result=info.sun(attraction,day);
                }
                case "chargers" -> result=info.chargers(attraction);
                case "photos" -> result=Map.of("images",AppLifecycle.attractions().images(attraction),"fetchedAt",Instant.now().toString());
                default -> {res.sendError(404);return;}
            }
            res.setContentType("application/json;charset=UTF-8");res.getWriter().write(gson.toJson(result));
        }catch(ValidationException|IllegalArgumentException e){res.sendError(400,e.getMessage());}
        catch(Exception e){getServletContext().log("External info unavailable",e);res.sendError(503,"외부 정보를 불러올 수 없습니다.");}
    }
}
