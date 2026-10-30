package kr.co.enjoytrip.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.NewsDto;

public class NewsDao {
    public List<NewsDto> list(int page)throws SQLException{
        List<NewsDto> rows=new ArrayList<>();
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT * FROM news ORDER BY published_at DESC,id DESC LIMIT 21 OFFSET ?")){
            s.setInt(1,(page-1)*20);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new NewsDto(r.getLong("id"),r.getString("title"),r.getString("source"),r.getString("source_url"),r.getTimestamp("published_at")==null?"":r.getTimestamp("published_at").toString()));}
        }
        return rows;
    }
    public boolean insert(String title,String source,String url,LocalDateTime published)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("INSERT IGNORE INTO news(title,source,source_url,published_at) VALUES(?,?,?,?)")){
            s.setString(1,title);s.setString(2,source);s.setString(3,url);s.setObject(4,published);return s.executeUpdate()==1;
        }
    }
}
