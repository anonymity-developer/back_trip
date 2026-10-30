package kr.co.enjoytrip.service;

import java.sql.SQLException;
import java.util.List;
import kr.co.enjoytrip.dao.PostDao;
import kr.co.enjoytrip.dto.PostDto;

public class PostService {
    private final PostDao dao=new PostDao();
    public List<PostDto> list(String kind,String keyword,int page) throws SQLException{return dao.list(kind,keyword,page);}
    public PostDto find(long id,String kind) throws SQLException{return dao.find(id,kind);}
    public void create(Long userId,boolean admin,String kind,String title,String body) throws SQLException {
        authorize(userId,admin,kind);validate(title,body);dao.insert(userId,kind,title.trim(),body.trim());
    }
    public void update(long id,Long userId,boolean admin,String kind,String title,String body) throws SQLException {
        authorize(userId,admin,kind);validate(title,body);
        if(!dao.update(id,userId,kind,title.trim(),body.trim())) throw new ValidationException("글을 수정할 권한이 없습니다.");
    }
    public void delete(long id,Long userId,boolean admin,String kind) throws SQLException {
        authorize(userId,admin,kind);
        if(!dao.delete(id,userId,kind)) throw new ValidationException("글을 삭제할 권한이 없습니다.");
    }
    private void authorize(Long id,boolean admin,String kind) {
        if(id==null||("NOTICE".equals(kind)&&!admin)) throw new ValidationException("권한이 없습니다.");
    }
    private void validate(String title,String body) {
        if(title==null||title.isBlank()||title.length()>200||body==null||body.isBlank()||body.length()>20000)
            throw new ValidationException("제목(1~200자)과 본문(1~20000자)을 입력하세요.");
    }
}
