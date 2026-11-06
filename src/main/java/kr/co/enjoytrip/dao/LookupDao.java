package kr.co.enjoytrip.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.ContentTypeDto;
import kr.co.enjoytrip.dto.GugunDto;
import kr.co.enjoytrip.dto.SidoDto;

public class LookupDao {
    public List<SidoDto> sidos() throws SQLException {
        List<SidoDto> rows = new ArrayList<>();
        try (Connection c = Database.open();
             PreparedStatement s = c.prepareStatement("SELECT sido_code,sido_name FROM sidos ORDER BY sido_code");
             ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new SidoDto(r.getInt(1), r.getString(2)));
        }
        return rows;
    }

    public List<ContentTypeDto> contentTypes() throws SQLException {
        List<ContentTypeDto> rows = new ArrayList<>();
        try (Connection c = Database.open();
             PreparedStatement s = c.prepareStatement("SELECT content_type_id,content_type_name FROM contenttypes ORDER BY content_type_id");
             ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new ContentTypeDto(r.getInt(1), r.getString(2)));
        }
        return rows;
    }

    public List<GugunDto> guguns(int sidoCode) throws SQLException {
        List<GugunDto> rows = new ArrayList<>();
        try (Connection c = Database.open();
             PreparedStatement s = c.prepareStatement("SELECT sido_code,gugun_code,gugun_name FROM guguns WHERE sido_code=? ORDER BY gugun_code")) {
            s.setInt(1, sidoCode);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new GugunDto(r.getInt(1), r.getInt(2), r.getString(3)));
            }
        }
        return rows;
    }
}
