package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.dto.HotPlaceDto;
import kr.co.enjoytrip.service.HotPlaceService;
import kr.co.enjoytrip.service.ValidationException;

@WebServlet({"/hot-places","/hot-places/*"})
@MultipartConfig(maxFileSize=5_000_000,maxRequestSize=6_000_000)
public class HotPlaceController extends HttpServlet {
    private final HotPlaceService places=new HotPlaceService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String suffix=suffix(req);Long user=viewer(req);
        try{
            if(suffix.isEmpty()){req.setAttribute("places",places.list());view(req,res,"hot-list");return;}
            if(suffix.equals("new")){if(user==null){login(req,res);return;}view(req,res,"hot-form");return;}
            String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);
            if(parts.length==2&&parts[1].equals("edit")){req.setAttribute("place",places.owned(id,user));view(req,res,"hot-form");return;}
            HotPlaceDto place=places.find(id);if(place==null){res.sendError(404);return;}
            req.setAttribute("place",place);view(req,res,"hot-detail");
        }catch(ValidationException e){res.sendError(403);}catch(NumberFormatException e){res.sendError(400);}catch(SQLException e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String suffix=suffix(req);Long user=viewer(req);if(user==null){login(req,res);return;}
        try{
            if(suffix.equals("new")){
                long id=places.create(user,req.getParameter("title"),req.getParameter("placeName"),req.getParameter("placeType"),req.getParameter("visitedOn"),req.getParameter("description"),req.getParameter("latitude"),req.getParameter("longitude"),req.getPart("image"));
                redirect(req,res,id);return;
            }
            String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);
            if(parts.length!=2){res.sendError(404);return;}
            if(parts[1].equals("edit")){places.update(id,user,req.getParameter("title"),req.getParameter("placeName"),req.getParameter("placeType"),req.getParameter("visitedOn"),req.getParameter("description"),req.getParameter("latitude"),req.getParameter("longitude"),req.getPart("image"));redirect(req,res,id);return;}
            if(parts[1].equals("delete")){places.delete(id,user);res.sendRedirect(req.getContextPath()+"/hot-places");return;}
            res.sendError(404);
        }catch(ValidationException e){res.sendError(400,e.getMessage());}catch(NumberFormatException e){res.sendError(400);}catch(Exception e){throw new ServletException(e);}
    }
    private String suffix(HttpServletRequest req){String path=req.getPathInfo();return path==null?"":path.replaceFirst("^/","");}
    private Long viewer(HttpServletRequest req){return(Long)req.getSession().getAttribute("userId");}
    private void login(HttpServletRequest req,HttpServletResponse res)throws IOException{res.sendRedirect(req.getContextPath()+"/login");}
    private void redirect(HttpServletRequest req,HttpServletResponse res,long id)throws IOException{res.sendRedirect(req.getContextPath()+"/hot-places/"+id);}
    private void view(HttpServletRequest req,HttpServletResponse res,String name)throws ServletException,IOException{req.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(req,res);}
}
