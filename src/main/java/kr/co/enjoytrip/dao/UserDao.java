package kr.co.enjoytrip.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import kr.co.enjoytrip.config.Database;
import kr.co.enjoytrip.dto.UserDto;

public class UserDao {
    public UserDto findByLogin(String loginId) throws SQLException {
        return find("SELECT * FROM users WHERE login_id=?", loginId);
    }

    public UserDto findByEmail(String email) throws SQLException {
        return find("SELECT * FROM users WHERE email=?", email);
    }

    public UserDto findById(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    private UserDto find(String sql, String value) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, value);
            try (ResultSet r = s.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public void insert(String loginId, String email, String name, String hash, String role) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO users(login_id,email,name,password_hash,role) VALUES(?,?,?,?,?)")) {
            s.setString(1, loginId); s.setString(2, email); s.setString(3, name);
            s.setString(4, hash); s.setString(5, role); s.executeUpdate();
        }
    }

    public void update(long id, String email, String name) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE users SET email=?,name=? WHERE id=?")) {
            s.setString(1, email); s.setString(2, name); s.setLong(3, id); s.executeUpdate();
        }
    }

    public void updatePassword(long id, String hash, boolean forceChange) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE users SET password_hash=?,must_change_password=? WHERE id=?")) {
            s.setString(1, hash); s.setBoolean(2, forceChange); s.setLong(3, id); s.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement("DELETE FROM users WHERE id=? AND role='USER'")) {
            s.setLong(1, id); s.executeUpdate();
        }
    }

    public void requestReset(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO password_reset_requests(user_id) SELECT ? WHERE NOT EXISTS " +
                "(SELECT 1 FROM password_reset_requests WHERE user_id=? AND status='PENDING')")) {
            s.setLong(1, id); s.setLong(2, id); s.executeUpdate();
        }
    }

    public List<ResetRequest> pendingResets() throws SQLException {
        List<ResetRequest> rows = new ArrayList<>();
        try (Connection c = Database.open(); PreparedStatement s = c.prepareStatement(
                "SELECT r.id,u.login_id,u.email,r.requested_at FROM password_reset_requests r JOIN users u ON u.id=r.user_id WHERE r.status='PENDING' ORDER BY r.requested_at" );
             ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new ResetRequest(r.getLong(1), r.getString(2), r.getString(3), r.getTimestamp(4).toString()));
        }
        return rows;
    }

    public boolean completeReset(long requestId, long adminId, String hash) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement s = c.prepareStatement("SELECT user_id FROM password_reset_requests WHERE id=? AND status='PENDING' FOR UPDATE")) {
                    s.setLong(1, requestId);
                    try (ResultSet r = s.executeQuery()) { if (!r.next()) { c.rollback(); return false; } userId = r.getLong(1); }
                }
                try (PreparedStatement s = c.prepareStatement("UPDATE users SET password_hash=?,must_change_password=TRUE WHERE id=?")) {
                    s.setString(1, hash); s.setLong(2, userId); s.executeUpdate();
                }
                try (PreparedStatement s = c.prepareStatement("UPDATE password_reset_requests SET status='COMPLETED',processed_at=NOW(),processed_by=? WHERE id=?")) {
                    s.setLong(1, adminId); s.setLong(2, requestId); s.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private UserDto map(ResultSet r) throws SQLException {
        return new UserDto(r.getLong("id"), r.getString("login_id"), r.getString("email"), r.getString("name"),
                r.getString("password_hash"), r.getString("role"), r.getBoolean("must_change_password"));
    }

    public record ResetRequest(long id, String loginId, String email, String requestedAt) {}
}
