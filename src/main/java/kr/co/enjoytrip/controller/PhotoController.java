package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.dto.HotPlaceDto;
import kr.co.enjoytrip.service.HotPlaceService;

@WebServlet("/photos/*")
public class PhotoController extends HttpServlet {
    private final HotPlaceService places=new HotPlaceService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        try{
            long id=Long.parseLong(req.getPathInfo().substring(1));HotPlaceDto place=places.find(id);
            if(place==null||place.imagePath()==null){res.sendError(404);return;}
            Path file=places.photoPath(place);if(!Files.isRegularFile(file)){res.sendError(404);return;}
            String name=place.imagePath();res.setContentType(name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg");
            res.setHeader("Cache-Control","public, max-age=3600");Files.copy(file,res.getOutputStream());
        }catch(NumberFormatException|NullPointerException e){res.sendError(400);}catch(SQLException e){throw new ServletException(e);}
    }
}
