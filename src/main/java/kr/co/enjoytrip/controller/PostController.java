package kr.co.enjoytrip.controller;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.enjoytrip.dto.PostDto;
import kr.co.enjoytrip.service.PostService;
import kr.co.enjoytrip.service.ValidationException;

@WebServlet({"/notices","/notices/*","/posts","/posts/*"})
public class PostController extends HttpServlet {
    private final PostService posts=new PostService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String base=base(req),suffix=suffix(req),kind=kind(base);
        req.setAttribute("base",base);req.setAttribute("heading",kind.equals("NOTICE")?"공지사항":"공유게시판");
        try {
            if(suffix.isEmpty()) {
                int page=number(req.getParameter("page"),1);if(page<1||page>10000){res.sendError(400);return;}
                String keyword=req.getParameter("keyword");if(keyword!=null&&keyword.length()>100){res.sendError(400);return;}
                var rows=posts.list(kind,keyword,page);req.setAttribute("posts",rows.size()>20?rows.subList(0,20):rows);req.setAttribute("page",page);
                req.setAttribute("hasNext",rows.size()>20);req.setAttribute("keyword",keyword);view(req,res,"post-list");return;
            }
            if(suffix.equals("new")) {if(!canWrite(req,kind)){res.sendError(403);return;}view(req,res,"post-form");return;}
            String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);PostDto post=posts.find(id,kind);
            if(post==null){res.sendError(404);return;}req.setAttribute("post",post);
            if(parts.length==1) {view(req,res,"post-detail");return;}
            if(parts.length==2&&parts[1].equals("edit")&&owner(req,post)){view(req,res,"post-form");return;}
            res.sendError(403);
        } catch(NumberFormatException e){res.sendError(400);}catch(SQLException e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String base=base(req),suffix=suffix(req),kind=kind(base);
        req.setAttribute("base",base);req.setAttribute("heading",kind.equals("NOTICE")?"공지사항":"공유게시판");
        Long user=(Long)req.getSession().getAttribute("userId");boolean admin="ADMIN".equals(req.getSession().getAttribute("userRole"));
        try {
            if(suffix.equals("new")) posts.create(user,admin,kind,req.getParameter("title"),req.getParameter("body"));
            else {
                String[] parts=suffix.split("/");long id=Long.parseLong(parts[0]);
                if(parts.length!=2){res.sendError(404);return;}
                if(parts[1].equals("edit")) posts.update(id,user,admin,kind,req.getParameter("title"),req.getParameter("body"));
                else if(parts[1].equals("delete")) posts.delete(id,user,admin,kind);
                else {res.sendError(404);return;}
            }
            res.sendRedirect(req.getContextPath()+base);
        } catch(ValidationException e){res.sendError(403,e.getMessage());}
        catch(NumberFormatException e){res.sendError(400);}
        catch(SQLException e){throw new ServletException(e);}
    }
    private String base(HttpServletRequest req){return req.getServletPath().startsWith("/notices")?"/notices":"/posts";}
    private String suffix(HttpServletRequest req){String value=req.getPathInfo();return value==null?"":value.replaceFirst("^/","");}
    private String kind(String base){return base.equals("/notices")?"NOTICE":"BOARD";}
    private int number(String value,int fallback){return value==null||value.isBlank()?fallback:Integer.parseInt(value);}
    private boolean canWrite(HttpServletRequest req,String kind){return req.getSession().getAttribute("userId")!=null&&(!kind.equals("NOTICE")||"ADMIN".equals(req.getSession().getAttribute("userRole")));}
    private boolean owner(HttpServletRequest req,PostDto post){return canWrite(req,post.kind())&&(req.getSession().getAttribute("userId").equals(post.userId())||post.kind().equals("NOTICE"));}
    private void view(HttpServletRequest req,HttpServletResponse res,String name)throws ServletException,IOException{req.getRequestDispatcher("/WEB-INF/views/"+name+".jsp").forward(req,res);}
}
