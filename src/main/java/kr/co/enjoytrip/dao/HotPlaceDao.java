package kr.co.enjoytrip.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.HotPlaceDto;

public class HotPlaceDao {
    public List<HotPlaceDto> list()throws SQLException{
        List<HotPlaceDto> rows=new ArrayList<>();
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT h.*,u.name author FROM hot_places h JOIN users u ON u.id=h.user_id ORDER BY h.created_at DESC LIMIT 100");ResultSet r=s.executeQuery()){
            while(r.next())rows.add(map(r));
        }
        return rows;
    }
    public HotPlaceDto find(long id)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT h.*,u.name author FROM hot_places h JOIN users u ON u.id=h.user_id WHERE h.id=?")){
            s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }
    public long insert(long userId,String title,String place,String type,LocalDate date,String description,Double lat,Double lng,String image)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("INSERT INTO hot_places(user_id,title,place_name,place_type,visited_on,description,latitude,longitude,image_path) VALUES(?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            bind(s,userId,title,place,type,date,description,lat,lng,image);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}
        }
    }
    public boolean update(long id,long userId,String title,String place,String type,LocalDate date,String description,Double lat,Double lng,String image)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("UPDATE hot_places SET title=?,place_name=?,place_type=?,visited_on=?,description=?,latitude=?,longitude=?,image_path=? WHERE id=? AND user_id=?")){
            s.setString(1,title);s.setString(2,place);s.setString(3,type);s.setDate(4,Date.valueOf(date));s.setString(5,description);s.setObject(6,lat);s.setObject(7,lng);s.setString(8,image);s.setLong(9,id);s.setLong(10,userId);return s.executeUpdate()==1;
        }
    }
    public boolean delete(long id,long userId)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("DELETE FROM hot_places WHERE id=? AND user_id=?")){s.setLong(1,id);s.setLong(2,userId);return s.executeUpdate()==1;}
    }
    public List<String> imagePathsForUser(long userId)throws SQLException{
        List<String> paths=new ArrayList<>();
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT image_path FROM hot_places WHERE user_id=? AND image_path IS NOT NULL")){
            s.setLong(1,userId);try(ResultSet r=s.executeQuery()){while(r.next())paths.add(r.getString(1));}
        }
        return paths;
    }
    private void bind(PreparedStatement s,long userId,String title,String place,String type,LocalDate date,String description,Double lat,Double lng,String image)throws SQLException{
        s.setLong(1,userId);s.setString(2,title);s.setString(3,place);s.setString(4,type);s.setDate(5,Date.valueOf(date));s.setString(6,description);s.setObject(7,lat);s.setObject(8,lng);s.setString(9,image);
    }
    private HotPlaceDto map(ResultSet r)throws SQLException{return new HotPlaceDto(r.getLong("id"),r.getLong("user_id"),r.getString("author"),r.getString("title"),r.getString("place_name"),r.getString("place_type"),r.getDate("visited_on").toLocalDate(),r.getString("description"),r.getObject("latitude",Double.class),r.getObject("longitude",Double.class),r.getString("image_path"));}
}
