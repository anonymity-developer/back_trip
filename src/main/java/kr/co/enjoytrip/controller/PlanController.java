package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.dto.PlanDto;
import kr.co.enjoytrip.service.PlanService;
import kr.co.enjoytrip.service.ValidationException;

@WebServlet({"/plans","/plans/*","/api/plans/*"})
public class PlanController extends HttpServlet {
    private final PlanService plans=new PlanService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String suffix=suffix(req);Long viewer=viewer(req);
        try{
            if(suffix.isEmpty()){req.setAttribute("plans",plans.list(viewer));view(req,res,"plan-list");return;}
            if(suffix.equals("new")){if(viewer==null){login(req,res);return;}view(req,res,"plan-form");return;}
            String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);
            if(parts.length==2&&parts[1].equals("edit")){req.setAttribute("plan",plans.owned(id,viewer));view(req,res,"plan-form");return;}
            PlanDto plan=plans.visible(id,viewer);if(plan==null){res.sendError(404);return;}
            req.setAttribute("plan",plan);req.setAttribute("stops",plans.stops(id));view(req,res,"plan-detail");
        }catch(ValidationException e){res.sendError(403);}catch(NumberFormatException e){res.sendError(400);}catch(SQLException e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String suffix=suffix(req);Long viewer=viewer(req);
        if(viewer==null){login(req,res);return;}
        try{
            if(suffix.equals("new")){
                long id=plans.create(viewer,req.getParameter("title"),req.getParameter("startDate"),req.getParameter("endDate"),req.getParameter("budget"),req.getParameter("details"),"on".equals(req.getParameter("isPublic")));
                redirect(req,res,id);return;
            }
            String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);
            if(parts.length==2){switch(parts[1]){
                case "edit" -> plans.update(id,viewer,req.getParameter("title"),req.getParameter("startDate"),req.getParameter("endDate"),req.getParameter("budget"),req.getParameter("details"),"on".equals(req.getParameter("isPublic")));
                case "delete" -> {plans.delete(id,viewer);res.sendRedirect(req.getContextPath()+"/plans");return;}
                case "stops" -> plans.addStop(id,viewer,Long.parseLong(req.getParameter("attractionId")),req.getParameter("visitDate"),req.getParameter("budget"),req.getParameter("memo"));
                default -> {res.sendError(404);return;}
            }}else if(parts.length==3&&parts[1].equals("stops")&&parts[2].equals("delete")){
                plans.deleteStop(id,viewer,Long.parseLong(req.getParameter("stopId")));
            }else if(parts.length==3&&parts[1].equals("stops")&&parts[2].equals("order")){
                plans.reorder(id,viewer,req.getParameterValues("stopId"));res.setStatus(204);return;
            }else{res.sendError(404);return;}
            redirect(req,res,id);
        }catch(ValidationException e){res.sendError(400,e.getMessage());}catch(NumberFormatException e){res.sendError(400);}catch(SQLException e){throw new ServletException(e);}
    }
    private String suffix(HttpServletRequest req){String path=req.getPathInfo();return path==null?"":path.replaceFirst("^/","");}
    private Long viewer(HttpServletRequest req){return(Long)req.getSession().getAttribute("userId");}
    private void login(HttpServletRequest req,HttpServletResponse res)throws IOException{res.sendRedirect(req.getContextPath()+"/login");}
    private void redirect(HttpServletRequest req,HttpServletResponse res,long id)throws IOException{res.sendRedirect(req.getContextPath()+"/plans/"+id);}
    private void view(HttpServletRequest req,HttpServletResponse res,String name)throws ServletException,IOException{req.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(req,res);}
}
