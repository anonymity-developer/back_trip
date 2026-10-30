package kr.co.enjoytrip.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.PlanDto;
import kr.co.enjoytrip.dto.PlanStopDto;

public class PlanDao {
    public List<PlanDto> list(Long viewer) throws SQLException {
        List<PlanDto> rows=new ArrayList<>();
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement(
                "SELECT p.*,u.name author FROM plans p JOIN users u ON u.id=p.user_id WHERE p.is_public=TRUE OR p.user_id=? ORDER BY p.updated_at DESC LIMIT 100")) {
            s.setLong(1,viewer==null?-1:viewer);
            try(ResultSet r=s.executeQuery()){while(r.next())rows.add(map(r));}
        }
        return rows;
    }
    public PlanDto find(long id) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("SELECT p.*,u.name author FROM plans p JOIN users u ON u.id=p.user_id WHERE p.id=?")) {
            s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?map(r):null;}
        }
    }
    public List<PlanStopDto> stops(long planId) throws SQLException {
        List<PlanStopDto> rows=new ArrayList<>();
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement(
                "SELECT s.*,a.title attraction_title,a.latitude,a.longitude FROM plan_stops s JOIN attractions a ON a.id=s.attraction_id WHERE s.plan_id=? ORDER BY s.visit_date,s.stop_order")) {
            s.setLong(1,planId);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new PlanStopDto(r.getLong("id"),r.getLong("plan_id"),r.getLong("attraction_id"),r.getString("attraction_title"),r.getDate("visit_date").toLocalDate(),r.getInt("stop_order"),r.getBigDecimal("budget"),r.getString("memo"),r.getObject("latitude",Double.class),r.getObject("longitude",Double.class)));}
        }
        return rows;
    }
    public long insert(long userId,String title,LocalDate start,LocalDate end,BigDecimal budget,String details,boolean isPublic) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement(
                "INSERT INTO plans(user_id,title,start_date,end_date,budget,details,is_public) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            bind(s,userId,title,start,end,budget,details,isPublic);s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}
        }
    }
    public boolean update(long id,long userId,String title,LocalDate start,LocalDate end,BigDecimal budget,String details,boolean isPublic) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement(
                "UPDATE plans SET title=?,start_date=?,end_date=?,budget=?,details=?,is_public=? WHERE id=? AND user_id=?")) {
            s.setString(1,title);s.setDate(2,Date.valueOf(start));s.setDate(3,Date.valueOf(end));s.setBigDecimal(4,budget);s.setString(5,details);s.setBoolean(6,isPublic);s.setLong(7,id);s.setLong(8,userId);return s.executeUpdate()==1;
        }
    }
    public boolean delete(long id,long userId) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("DELETE FROM plans WHERE id=? AND user_id=?")) {
            s.setLong(1,id);s.setLong(2,userId);return s.executeUpdate()==1;
        }
    }
    public void addStop(long planId,long attractionId,LocalDate date,BigDecimal budget,String memo) throws SQLException {
        try(Connection c=Database.open()) {
            c.setAutoCommit(false);
            try(PreparedStatement lock=c.prepareStatement("SELECT id FROM plans WHERE id=? FOR UPDATE")) {
                lock.setLong(1,planId);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new SQLException("Plan not found");}
            }
            try(PreparedStatement s=c.prepareStatement("INSERT INTO plan_stops(plan_id,attraction_id,visit_date,stop_order,budget,memo) VALUES(?,?,?,(SELECT COALESCE(MAX(x.stop_order),0)+1 FROM (SELECT stop_order FROM plan_stops WHERE plan_id=? AND visit_date=?) x),?,?)")) {
                s.setLong(1,planId);s.setLong(2,attractionId);s.setDate(3,Date.valueOf(date));s.setLong(4,planId);s.setDate(5,Date.valueOf(date));s.setBigDecimal(6,budget);s.setString(7,memo);s.executeUpdate();
            }
            c.commit();
        }
    }
    public boolean deleteStop(long planId,long stopId) throws SQLException {
        try(Connection c=Database.open();PreparedStatement s=c.prepareStatement("DELETE FROM plan_stops WHERE id=? AND plan_id=?")) {
            s.setLong(1,stopId);s.setLong(2,planId);return s.executeUpdate()==1;
        }
    }
    public void reorder(long planId,long[] ids) throws SQLException {
        try(Connection c=Database.open()) {
            c.setAutoCommit(false);
            try {
                List<Long> current=new ArrayList<>();
                try(PreparedStatement s=c.prepareStatement("SELECT id FROM plan_stops WHERE plan_id=? ORDER BY id FOR UPDATE")) {
                    s.setLong(1,planId);try(ResultSet r=s.executeQuery()){while(r.next())current.add(r.getLong(1));}
                }
                if(current.size()!=ids.length) throw new SQLException("Stop list changed");
                for(long id:ids) if(!current.remove(id)) throw new SQLException("Invalid stop id");
                // Clear the unique order constraint before assigning the requested order.
                try(PreparedStatement s=c.prepareStatement("UPDATE plan_stops SET stop_order=stop_order+100000 WHERE plan_id=?")) {s.setLong(1,planId);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("UPDATE plan_stops SET stop_order=? WHERE id=? AND plan_id=?")) {
                    int order=1;for(long id:ids){s.setInt(1,order++);s.setLong(2,id);s.setLong(3,planId);s.addBatch();}s.executeBatch();
                }
                c.commit();
            } catch(SQLException e){c.rollback();throw e;}
        }
    }
    private void bind(PreparedStatement s,long userId,String title,LocalDate start,LocalDate end,BigDecimal budget,String details,boolean isPublic)throws SQLException{
        s.setLong(1,userId);s.setString(2,title);s.setDate(3,Date.valueOf(start));s.setDate(4,Date.valueOf(end));s.setBigDecimal(5,budget);s.setString(6,details);s.setBoolean(7,isPublic);
    }
    private PlanDto map(ResultSet r)throws SQLException{return new PlanDto(r.getLong("id"),r.getLong("user_id"),r.getString("author"),r.getString("title"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getBigDecimal("budget"),r.getString("details"),r.getBoolean("is_public"));}
}
