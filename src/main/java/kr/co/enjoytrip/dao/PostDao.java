package kr.co.enjoytrip.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.PostDto;

public class PostDao {
    public List<PostDto> list(String kind,String keyword,int page) throws SQLException {
        List<PostDto> rows=new ArrayList<>();
        String sql="SELECT p.*,u.name author FROM posts p JOIN users u ON u.id=p.user_id WHERE p.kind=?"+
                (keyword==null||keyword.isBlank()?"":" AND p.title LIKE ?")+" ORDER BY p.created_at DESC,p.id DESC LIMIT 21 OFFSET ?";
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,kind);int n=2;
            if(keyword!=null&&!keyword.isBlank()) s.setString(n++,"%"+keyword.trim()+"%");
            s.setInt(n,(page-1)*20);
            try(ResultSet r=s.executeQuery()){while(r.next())rows.add(map(r));}
        }
        return rows;
    }
    public PostDto find(long id,String kind) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT p.*,u.name author FROM posts p JOIN users u ON u.id=p.user_id WHERE p.id=? AND p.kind=?")) {
            s.setLong(1,id);s.setString(2,kind);
            try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }
    public void insert(long userId,String kind,String title,String body) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("INSERT INTO posts(user_id,kind,title,body) VALUES(?,?,?,?)")) {
            s.setLong(1,userId);s.setString(2,kind);s.setString(3,title);s.setString(4,body);s.executeUpdate();
        }
    }
    public boolean update(long id,long userId,String kind,String title,String body) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("UPDATE posts SET title=?,body=? WHERE id=? AND (user_id=? OR kind='NOTICE') AND kind=?")) {
            s.setString(1,title);s.setString(2,body);s.setLong(3,id);s.setLong(4,userId);s.setString(5,kind);return s.executeUpdate()==1;
        }
    }
    public boolean delete(long id,long userId,String kind) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("DELETE FROM posts WHERE id=? AND (user_id=? OR kind='NOTICE') AND kind=?")) {
            s.setLong(1,id);s.setLong(2,userId);s.setString(3,kind);return s.executeUpdate()==1;
        }
    }
    private PostDto map(ResultSet r) throws SQLException {
        return new PostDto(r.getLong("id"),r.getLong("user_id"),r.getString("author"),r.getString("kind"),r.getString("title"),r.getString("body"),r.getTimestamp("created_at").toString(),r.getTimestamp("updated_at").toString());
    }
}
