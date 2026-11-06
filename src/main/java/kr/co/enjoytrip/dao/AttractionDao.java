package kr.co.enjoytrip.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.AttractionDto;

public class AttractionDao {
    private static final String COLUMNS = "id,content_id,title,content_type,area_code,sigungu_code,address,description,latitude,longitude,image_url,source_updated_at";
    private static final String UPSERT = "INSERT INTO attractions(content_id,title,content_type,area_code,sigungu_code,address,description,latitude,longitude,image_url,source_updated_at) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title),content_type=VALUES(content_type)," +
            "area_code=VALUES(area_code),sigungu_code=VALUES(sigungu_code),address=COALESCE(NULLIF(VALUES(address),''),address)," +
            "latitude=COALESCE(VALUES(latitude),latitude),longitude=COALESCE(VALUES(longitude),longitude)," +
            "image_url=COALESCE(NULLIF(VALUES(image_url),''),image_url),source_updated_at=COALESCE(NULLIF(VALUES(source_updated_at),''),source_updated_at)";

    public List<AttractionDto> search(Integer area, Integer type, String keyword, int page, int size) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM attractions WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (area != null) { sql.append(" AND area_code=?"); args.add(area); }
        if (type != null) { sql.append(" AND content_type=?"); args.add(type); }
        if (keyword != null && !keyword.isBlank()) { sql.append(" AND title LIKE ?"); args.add("%" + keyword.trim() + "%"); }
        sql.append(" ORDER BY title,id LIMIT ? OFFSET ?"); args.add(size); args.add((page-1)*20);
        List<AttractionDto> rows = new ArrayList<>();
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(sql.toString())) {
            for (int i=0;i<args.size();i++) s.setObject(i+1,args.get(i));
            try (ResultSet r=s.executeQuery()) { while (r.next()) rows.add(map(r)); }
        }
        return rows;
    }

    public AttractionDto find(long id) throws SQLException {
        try (Connection c=Database.open(); PreparedStatement s=c.prepareStatement("SELECT " + COLUMNS + " FROM attractions WHERE id=?")) {
            s.setLong(1,id);
            try (ResultSet r=s.executeQuery()) { return r.next()?map(r):null; }
        }
    }

    public void upsert(AttractionDto a) throws SQLException {
        try (Connection c=Database.open(); PreparedStatement s=c.prepareStatement(UPSERT)) {
            bind(s,a); s.executeUpdate();
        }
    }

    public void upsertAll(List<AttractionDto> attractions) throws SQLException {
        if (attractions.isEmpty()) return;
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(UPSERT)) {
            c.setAutoCommit(false);
            try {
                for (AttractionDto a : attractions) {
                    bind(s,a);
                    s.addBatch();
                }
                s.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<String> images(long attractionId) throws SQLException {
        List<String> images=new ArrayList<>();
        try (Connection c=Database.open(); PreparedStatement s=c.prepareStatement(
                "SELECT image_url FROM attraction_images WHERE attraction_id=? ORDER BY display_order,id")) {
            s.setLong(1,attractionId); try(ResultSet r=s.executeQuery()){while(r.next()) images.add(r.getString(1));}
        }
        return images;
    }

    public void addImage(long attractionId,String url,int order) throws SQLException {
        try (Connection c=Database.open(); PreparedStatement s=c.prepareStatement(
                "INSERT IGNORE INTO attraction_images(attraction_id,image_url,display_order) VALUES(?,?,?)")) {
            s.setLong(1,attractionId); s.setString(2,url); s.setInt(3,order); s.executeUpdate();
        }
    }

    public void updateDescription(long id,String description)throws SQLException{
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("UPDATE attractions SET description=? WHERE id=?")){
            s.setString(1,description);s.setLong(2,id);s.executeUpdate();
        }
    }

    private AttractionDto map(ResultSet r) throws SQLException {
        return new AttractionDto(r.getLong("id"),r.getLong("content_id"),r.getString("title"),r.getInt("content_type"),
                r.getInt("area_code"),r.getObject("sigungu_code",Integer.class),r.getString("address"),r.getString("description"),
                r.getObject("latitude",Double.class),r.getObject("longitude",Double.class),r.getString("image_url"),r.getString("source_updated_at"));
    }

    private void bind(PreparedStatement s,AttractionDto a) throws SQLException {
        s.setLong(1,a.contentId()); s.setString(2,a.title()); s.setInt(3,a.contentType()); s.setInt(4,a.areaCode());
        s.setObject(5,a.sigunguCode()); s.setString(6,a.address()); s.setString(7,a.description());
        s.setObject(8,a.latitude()); s.setObject(9,a.longitude()); s.setString(10,a.imageUrl()); s.setString(11,a.sourceUpdatedAt());
    }
}
